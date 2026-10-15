package glasscam.cam.ui.cameraui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;

import androidx.core.content.ContextCompat;

import com.hazia.glasscam.R;

/**
 * Single-bar horizon level (Batch 2, 2026-09-16). Replaces the arc gauge + rotated line + pitch
 * arrows, which showed two indicators that could disagree and redrew on every sensor event.
 *
 * One bar rotated by the roll angle, a fixed centre tick, and the angle in small text. The bar
 * turns lime within LEVEL_TOLERANCE degrees. Every stroke is drawn twice - a dark outline under
 * the light stroke - so it reads over a bright scene as well as a dark one. The view invalidates
 * when the reading moves by MIN_CHANGE degrees or when it crosses LEVEL_TOLERANCE (the colour
 * flips, however small the move), and it fades itself out after
 * HIDE_AFTER_MS of being level (the fragment brings it back on the next tilt).
 */
public class LevelBarView extends View {

    public static final float LEVEL_TOLERANCE = 0.5f;
    private static final float MIN_CHANGE = 0.2f;
    private static final long HIDE_AFTER_MS = 2000;

    private float roll = 0f;          // signed degrees, 0 = level
    private boolean level = false;
    private boolean autoHide = true;
    private final Paint bar = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint outline = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textOutline = new Paint(Paint.ANTI_ALIAS_FLAG);
    private int colorLevel, colorTilted;
    private float dp;

    private final Runnable hide = () -> animate().alpha(0f).setDuration(300).start();

    public LevelBarView(Context context) {
        this(context, null);
    }

    public LevelBarView(Context context, AttributeSet attrs) {
        super(context, attrs);
        dp = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1, getResources().getDisplayMetrics());
        colorLevel = ContextCompat.getColor(context, R.color.manual_button_active);
        colorTilted = ContextCompat.getColor(context, R.color.nextgen_menu_right_text);
        bar.setStyle(Paint.Style.STROKE);
        bar.setStrokeWidth(2 * dp);
        bar.setStrokeCap(Paint.Cap.ROUND);
        outline.setStyle(Paint.Style.STROKE);
        outline.setStrokeWidth(4 * dp);
        outline.setStrokeCap(Paint.Cap.ROUND);
        outline.setColor(Color.argb(160, 0, 0, 0));
        text.setTextSize(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 10, getResources().getDisplayMetrics()));
        text.setTextAlign(Paint.Align.CENTER);
        textOutline.set(text);
        textOutline.setStyle(Paint.Style.STROKE);
        textOutline.setStrokeWidth(2 * dp);
        textOutline.setColor(Color.argb(200, 0, 0, 0));
    }

    public void setAutoHide(boolean autoHide) {
        this.autoHide = autoHide;
    }

    /** Signed roll in degrees; called from the sensor thread's UI post. */
    public void setRoll(float signedRoll) {
        if (Math.abs(signedRoll - roll) < MIN_CHANGE && level == (Math.abs(signedRoll) <= LEVEL_TOLERANCE))
            return;
        roll = signedRoll;
        boolean nowLevel = Math.abs(roll) <= LEVEL_TOLERANCE;
        if (nowLevel != level) {
            level = nowLevel;
            removeCallbacks(hide);
            if (level && autoHide)
                postDelayed(hide, HIDE_AFTER_MS);
        }
        if (!level && getAlpha() < 1f) {
            animate().cancel();
            setAlpha(1f);
        }
        invalidate();
    }

    @Override
    protected void onDetachedFromWindow() {
        removeCallbacks(hide);
        super.onDetachedFromWindow();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float w = getWidth(), h = getHeight();
        float cx = w / 2f, cy = h / 2f;
        float half = w * 0.42f;
        int color = level ? colorLevel : colorTilted;
        bar.setColor(color);
        text.setColor(color);

        // centre tick: fixed, so the bar's rotation is read against something
        canvas.drawLine(cx, cy - 10 * dp, cx, cy - 4 * dp, outline);
        canvas.drawLine(cx, cy - 10 * dp, cx, cy - 4 * dp, bar);

        canvas.save();
        canvas.rotate(roll, cx, cy);
        canvas.drawLine(cx - half, cy, cx + half, cy, outline);
        canvas.drawLine(cx - half, cy, cx + half, cy, bar);
        canvas.restore();

        String label = level ? "0°" : String.format(java.util.Locale.US, "%+.1f°", roll);
        float ty = h - 8 * dp;          // below the bar's full sweep (the view is square)
        canvas.drawText(label, cx, ty, textOutline);
        canvas.drawText(label, cx, ty, text);
    }
}
