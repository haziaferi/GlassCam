package glasscam.imaging;

import android.app.ActivityManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapRegionDecoder;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.os.Build;

import androidx.annotation.RequiresApi;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import glasscam.GlassCamApplication;
import glasscam.file.FileListController;
import glasscam.file.holder.BaseHolder;
import glasscam.utils.Log;
import glasscam.utils.StorageFileManager;

/**
 * Merges a set of already-saved JPEG frames (e.g. an Af-Bracket burst shot across
 * a focus range) into a single focus-stacked JPEG, entirely locally - no cloud,
 * no ML Kit.
 *
 * Deliberately decoupled from the live capture/save pipeline (CaptureController,
 * ImageTask, etc.): it only reads back files that have already finished saving via
 * the normal path, and writes exactly one new file through the normal
 * FileListController/SAF/MediaStore path. Nothing about the capture pipeline itself
 * changes, which keeps this additive rather than a modification of code every other
 * capture mode also depends on.
 *
 * Approach: for each output tile, decode that tile's region from every source frame
 * (via BitmapRegionDecoder, so we never hold more than one tile per source in memory
 * at a time - N full-resolution bitmaps simultaneously would risk OOM on a phone),
 * score each with a cheap sharpness proxy, and keep the sharpest one. No alignment
 * pass - this assumes a static scene (tripod/macro rig), same assumption most phone
 * focus-stacking tools make for a first version. Hard tile boundaries, no feathering.
 *
 * Only meaningful for JPEG-producing formats - BitmapRegionDecoder doesn't understand
 * camera RAW/DNG. The caller (AfBracketApi2) is responsible for checking the active
 * picture format before invoking this.
 */
@RequiresApi(api = Build.VERSION_CODES.O)
public class FocusStackProcessor {

    private static final String TAG = FocusStackProcessor.class.getSimpleName();
    private static final int TILE = 128;
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();

    public interface Callback {
        void onFinished(BaseHolder result);
        void onFailed(Exception ex);

        /**
         * Called roughly every 10% of tiles processed, on the same background thread
         * doing the merge - not the main thread. Default no-op so existing callers
         * don't have to implement it. Implementations that want to update UI should
         * hop to the main thread themselves (UserMessageHandler.sendMSG() already
         * does this internally via View.post(), so it's safe to call directly from
         * here without any extra handling).
         */
        default void onProgress(int percent) {}
    }

    public static void stackAsync(BaseHolder[] frames, Callback callback) {
        EXECUTOR.execute(() -> {
            try {
                BaseHolder result = stack(frames, callback);
                callback.onFinished(result);
            } catch (Throwable ex) {
                // Throwable, not Exception: Bitmap.createBitmap()/decodeRegion() throw
                // OutOfMemoryError on a large enough image, which is an Error, not an
                // Exception - catching only Exception let that escape uncaught on this
                // background thread and take the whole app down instead of reporting a
                // clean failure through the callback like every other failure mode here.
                Log.WriteEx(ex);
                callback.onFailed(ex instanceof Exception ? (Exception) ex : new Exception(ex));
            }
        });
    }

    private static BaseHolder stack(BaseHolder[] frames, Callback callback) throws IOException {
        if (frames == null || frames.length < 2)
            throw new IllegalArgumentException("FocusStackProcessor needs at least 2 frames");

        BitmapRegionDecoder[] decoders = new BitmapRegionDecoder[frames.length];
        int width = -1;
        int height = -1;
        Bitmap output = null;
        try {
            for (int i = 0; i < frames.length; i++) {
                try (InputStream in = frames[i].getInputStream()) {
                    if (in == null)
                        throw new IOException("could not open frame " + i + " (" + frames[i].getName() + ")");
                    decoders[i] = BitmapRegionDecoder.newInstance(in, false);
                }
                if (decoders[i] == null)
                    throw new IOException("frame " + i + " isn't a region-decodable image (RAW/DNG bracket? focus stacking only supports JPEG)");
                if (width == -1) {
                    width = decoders[i].getWidth();
                    height = decoders[i].getHeight();
                } else if (decoders[i].getWidth() != width || decoders[i].getHeight() != height) {
                    throw new IOException("frame " + i + " is " + decoders[i].getWidth() + "x" + decoders[i].getHeight()
                            + ", expected " + width + "x" + height + " - aborting stack");
                }
            }

            // Fail cleanly here instead of finding out via a caught OutOfMemoryError
            // below: leave a margin for the tile-decode bitmaps, the Canvas, and GC
            // overhead rather than only checking against the bare bitmap size.
            //
            // Two signals, not one: Runtime's heap numbers only cover the Java/ART
            // heap, but since API 26 a Bitmap's actual pixel buffer lives on the
            // native heap and barely shows up there - a device that's genuinely low
            // on RAM could sail past a heap-only check. ActivityManager's
            // availMem/lowMemory reflect real system memory pressure instead, so take
            // whichever of the two signals is more pessimistic.
            long requiredBytes = (long) width * height * 4L; // ARGB_8888
            long availableBytes = Runtime.getRuntime().maxMemory()
                    - (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory());
            boolean systemLowMemory = false;
            ActivityManager am = (ActivityManager) GlassCamApplication.getContext().getSystemService(Context.ACTIVITY_SERVICE);
            if (am != null) {
                ActivityManager.MemoryInfo memInfo = new ActivityManager.MemoryInfo();
                am.getMemoryInfo(memInfo);
                systemLowMemory = memInfo.lowMemory;
                availableBytes = Math.min(availableBytes, memInfo.availMem);
            }
            if (systemLowMemory || requiredBytes > availableBytes * 0.7) {
                throw new IOException("image too large to focus-stack in memory (" + width + "x" + height
                        + " needs ~" + (requiredBytes / (1024 * 1024)) + "MB, ~"
                        + (availableBytes / (1024 * 1024)) + "MB available) - try a lower Af-Bracket picture size");
            }

            output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(output);
            BitmapFactory.Options opts = new BitmapFactory.Options();

            int tilesX = (width + TILE - 1) / TILE;
            int tilesY = (height + TILE - 1) / TILE;
            int totalTiles = Math.max(1, tilesX * tilesY);
            int tilesDone = 0;
            int lastReportedDecile = -1;

            for (int y = 0; y < height; y += TILE) {
                for (int x = 0; x < width; x += TILE) {
                    Rect tileRect = new Rect(x, y, Math.min(x + TILE, width), Math.min(y + TILE, height));
                    Bitmap best = null;
                    double bestScore = -1;
                    for (BitmapRegionDecoder decoder : decoders) {
                        Bitmap tile = decoder.decodeRegion(tileRect, opts);
                        if (tile == null)
                            continue;
                        double score = sharpness(tile);
                        if (score > bestScore) {
                            bestScore = score;
                            if (best != null)
                                best.recycle();
                            best = tile;
                        } else {
                            tile.recycle();
                        }
                    }
                    if (best != null) {
                        canvas.drawBitmap(best, tileRect.left, tileRect.top, null);
                        best.recycle();
                    }
                    tilesDone++;
                    // Threshold-crossing rather than "percent is an exact multiple of 10":
                    // with a small totalTiles (a deliberately downscaled Af-Bracket size,
                    // e.g. for the OOM guard above), the percent sequence can jump straight
                    // over every multiple of 10, and onProgress would never fire before
                    // 100%. Decile crossing guarantees ~10 calls regardless of totalTiles.
                    int decile = (tilesDone * 10) / totalTiles;
                    if (decile != lastReportedDecile) {
                        lastReportedDecile = decile;
                        callback.onProgress(Math.min(decile * 10, 100));
                    }
                }
            }

            return save(output);
        } finally {
            for (BitmapRegionDecoder decoder : decoders)
                if (decoder != null)
                    decoder.recycle();
            if (output != null)
                output.recycle();
        }
    }

    private static BaseHolder save(Bitmap merged) throws IOException {
        FileListController fileListController = GlassCamApplication.fileListController();
        String name = "focusstack_" + StorageFileManager.getStringDatePAttern().format(new Date()) + ".jpg";
        // getNewImgFileHolder() only reads file.getName() to pick the destination
        // (SAF DocumentFile or MediaStore entry, whichever the person has configured) -
        // it doesn't touch this placeholder File's actual bytes.
        BaseHolder resultHolder = fileListController.getNewImgFileHolder(new File(name));
        if (resultHolder == null)   // refused: the GlassCam/ folder is unavailable (the user was told)
            throw new IOException("GlassCam/ folder unavailable; focus stack not saved: " + name);
        try (OutputStream out = resultHolder.getOutputStream()) {
            if (out == null)
                throw new IOException("could not open output stream for " + name);
            merged.compress(Bitmap.CompressFormat.JPEG, 95, out);
        }
        fileListController.addFromEventFile(resultHolder);
        Log.d(TAG, "focus stack saved: " + name);
        return resultHolder;
    }

    /**
     * Cheap edge-strength proxy (sum of squared horizontal+vertical luminance
     * gradients, subsampled 2x in both axes for speed) - not a true Laplacian, but
     * good enough to rank "which of N near-identical tiles is sharpest", which is
     * all this needs to do.
     */
    private static double sharpness(Bitmap tile) {
        int w = tile.getWidth();
        int h = tile.getHeight();
        if (w < 3 || h < 3)
            return 0;
        int[] pixels = new int[w * h];
        tile.getPixels(pixels, 0, w, 0, 0, w, h);
        double sum = 0;
        for (int y = 1; y < h - 1; y += 2) {
            for (int x = 1; x < w - 1; x += 2) {
                int c = luminance(pixels[y * w + x]);
                int right = luminance(pixels[y * w + x + 1]);
                int down = luminance(pixels[(y + 1) * w + x]);
                int dx = c - right;
                int dy = c - down;
                sum += (double) (dx * dx + dy * dy);
            }
        }
        return sum;
    }

    private static int luminance(int argb) {
        int r = (argb >> 16) & 0xFF;
        int g = (argb >> 8) & 0xFF;
        int b = argb & 0xFF;
        return (r * 299 + g * 587 + b * 114) / 1000;
    }
}
