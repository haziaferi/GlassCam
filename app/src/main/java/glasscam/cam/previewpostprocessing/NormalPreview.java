package glasscam.cam.previewpostprocessing;

import android.content.Context;
import android.graphics.Matrix;
import android.graphics.Point;

import glasscam.cam.histogram.HistogramFeed;
import glasscam.utils.DisplayUtil;
import glasscam.utils.MatrixUtil;
import glasscam.views.AutoFitTextureView;

public class NormalPreview extends AutoFitTexturviewPreview {

    Point displaySize;
    // the last frame size and rotation a module asked for, re-applied when the view's size changes
    private int frameW, frameH, frameRotation;
    private boolean hasFrame = false;

    public NormalPreview(Context context) {
        super(context);
        displaySize = DisplayUtil.getDisplaySize();
        // The view's size can change after the transform was set (the first layout, or the system
        // bars shown or hidden on a focus change): map the frame again into the new size, or the
        // squeeze SPEC C18 removed comes back until the next module start.
        getAutoFitTextureView().addOnLayoutChangeListener((v, l, t, r, b, ol, ot, or, ob) -> {
            if (hasFrame && (r - l != or - ol || b - t != ob - ot))
                applyTransform(frameW, frameH, frameRotation);
        });
    }

    @Override
    public void setBlue(boolean blue) {

    }

    @Override
    public void setRed(boolean red) {

    }

    @Override
    public void setGreen(boolean green) {

    }

    @Override
    public void setFocusPeak(boolean on) {

    }

    @Override
    public boolean isFocusPeak() {
        return false;
    }

    @Override
    public void setClipping(boolean on) {

    }

    @Override
    public boolean isClipping() {
        return false;
    }

    @Override
    public void setHistogram(boolean on) {

    }

    @Override
    public boolean isHistogram() {
        return false;
    }

    @Override
    public void setColorWaveForm(boolean on) {

    }

    @Override
    public boolean isColorWaveForm() {
        return false;
    }

    @Override
    public void start() {

    }

    @Override
    public void stop() {

    }

    @Override
    public void setRotation(int width, int height, int rotation) {
        frameW = width;
        frameH = height;
        frameRotation = rotation;
        hasFrame = true;
        AutoFitTextureView view = getAutoFitTextureView();
        // not laid out yet: the layout listener applies it once the view has a size
        if (view.getWidth() > 0 && view.getHeight() > 0)
            applyTransform(width, height, rotation);
    }

    // The frame is mapped into the preview view's own size, not the screen's (SPEC C18). Until
    // 4.3.103 the screen size was used: 2412 px for a 2316 px view on the 9 Pro (the left inset
    // takes 96 px), so after the 270-degree rotation the 4:3 frame was drawn 1440 x 1037 px,
    // squeezed 4 % vertically with a black band on top.
    private void applyTransform(int width, int height, int rotation) {
        AutoFitTextureView view = getAutoFitTextureView();
        int viewW = view.getWidth() > 0 ? view.getWidth() : displaySize.x;
        int viewH = view.getHeight() > 0 ? view.getHeight() : displaySize.y;
        Matrix matrix = MatrixUtil.getTransFormMatrix(width, height, Math.max(viewW, viewH), Math.min(viewW, viewH), rotation, false);
        view.setTransform(matrix);
    }

    @Override
    public void setHistogramFeed(HistogramFeed feed) {

    }

    @Override
    public void setZebraHigh(float high) {

    }

    @Override
    public void setZebraLow(float low) {

    }


}
