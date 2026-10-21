package glasscam.cam.ui.themenextgen.view;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.hazia.glasscam.R;

import glasscam.GlassCamApplication;
import glasscam.file.FileListController;
import glasscam.file.holder.BaseHolder;
import glasscam.utils.GlassCamPool;
import glasscam.utils.Log;
import glasscam.viewer.ActivityGlassCamViewer;

/**
 * Shutter-column cell that opens GlassGallery and shows the last capture as a thumbnail.
 * Added in Batch 2 (2026-09-16): the viewfinder had no way into the gallery at all - GlassGallery
 * was only a second launcher icon. Until the first capture the cell shows the icon-font gallery
 * glyph; after every save (FileListController.NotifyFilesChanged) it reloads the newest file's
 * thumbnail on the pool thread and swaps it in on the UI thread.
 */
public class NextGenGalleryButton extends FrameLayout implements FileListController.NotifyFilesChanged {

    private static final String TAG = NextGenGalleryButton.class.getSimpleName();
    private ImageView thumb;
    private TextView glyph;

    public NextGenGalleryButton(Context context) {
        this(context, null);
    }

    public NextGenGalleryButton(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        LayoutInflater.from(context).inflate(R.layout.nextgen_gallery_button, this, true);
        thumb = findViewById(R.id.imageView_gallery_thumb);
        glyph = findViewById(R.id.textView_gallery_glyph);
        setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), ActivityGlassCamViewer.class);
            intent.putExtra(ActivityGlassCamViewer.EXTRA_OPEN_NEWEST_PHOTO, true);
            getContext().startActivity(intent);
        });
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        GlassCamApplication.fileListController().setNotifyFilesChanged(this);
        refresh();
    }

    @Override
    protected void onDetachedFromWindow() {
        GlassCamApplication.fileListController().removeNotifyFilesChanged(this);
        super.onDetachedFromWindow();
    }

    @Override
    public void onFilesChanged() {
        refresh();
    }

    @Override
    public void onFileDeleted(int id) {
        refresh();
    }

    /** Bumped per refresh; a decode that finishes after a newer one started is dropped, so two
     *  quick saves cannot leave the older thumbnail on top (fresh-eyes 2026-09-28). */
    private final java.util.concurrent.atomic.AtomicInteger generation = new java.util.concurrent.atomic.AtomicInteger();

    private void refresh() {
        final int mine = generation.incrementAndGet();
        GlassCamPool.Execute(() -> {
            // read under the controller's own lock (synchronizing on the list was another monitor)
            BaseHolder newest = GlassCamApplication.fileListController().newestFile();
            Bitmap bitmap = null;
            if (newest != null) {
                try {
                    bitmap = GlassCamApplication.bitmapHelper().getBitmap(newest, true);
                } catch (Exception ex) {
                    Log.WriteEx(ex);
                }
            }
            final Bitmap b = bitmap;
            post(() -> {
                if (mine != generation.get())
                    return;
                if (b != null) {
                    thumb.setImageBitmap(b);
                    thumb.setVisibility(VISIBLE);
                    glyph.setVisibility(GONE);
                } else {
                    thumb.setVisibility(GONE);
                    glyph.setVisibility(VISIBLE);
                }
            });
        });
    }
}
