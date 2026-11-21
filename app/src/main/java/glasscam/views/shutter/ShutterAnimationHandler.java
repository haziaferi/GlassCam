package glasscam.views.shutter;

import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import java.lang.ref.WeakReference;

import glasscam.cam.event.capture.CaptureStates;
import glasscam.utils.Log;

/**
 * Created by KillerInk on 04.12.2017.
 */

public class ShutterAnimationHandler extends Handler
{
    private volatile CaptureStates currentShow = CaptureStates.image_capture_stop;   // read by the draw thread without the lock
    int red_top, red_bottom, red_right,red_left, padding_red, red_radius, halfsize;

    private final int MSG_START_ANIMATION = 0;


    //shutter_open_radius for the Transparent Radius to draw to simulate shutter open
    private float shutter_open_radius = 0.0f;
    //true when the red recording button should get shown, used for continouse capture and video
    private volatile boolean drawRecordingImage = false;
    //current size of the red circle to draw
    private int recordingRadiusCircle;
    //current size of the red rectangle to draw
    private int recordingRadiusRectangle;

    //frames to draw
    private final int MAXFRAMES = 30;
    //holds the currentframe number
    private int currentframe = 0;

    private volatile boolean running = false;

    private volatile boolean stopTimer = false;
    private int MAX_SHUTTER_OPEN;
    private int MAX_RECORDING_OPEN;
    private int RECORDING_OPEN_STEP;

    //the step wich the shutter_open_radius gets increased/decrased
    private int SHUTTER_OPEN_STEP;
    private volatile boolean shutteractive = false;

// used to track how long values calc took, and reduce depending on it the sleep time for next calc/draw
    private long calcstartTime;


    private final Paint transparent;
    private final Paint red;
    private final ShutterButton shutterButton;
    private final UIHandler uiHandler;


    public boolean isRunning()
    {
        return running;
    }

    private final int FPS = 1000/ MAXFRAMES;

    public ShutterAnimationHandler(Looper looper, Resources resources,ShutterButton shutterButton)
    {

        super(looper);
        halfsize = shutterButton.getWidth()/2;
        uiHandler = new UIHandler(shutterButton);
        this.shutterButton = shutterButton;
        // The elapsed-time text that used to be drawn across the stop button now shows in the
        // info pill (SPEC C6, owner 4.3.103); the shutter draws only its open and stop states.

        //used to open the shutter
        transparent = new Paint();
        transparent.setColor(Color.TRANSPARENT);
        transparent.setStyle(Paint.Style.FILL);
        transparent.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
        transparent.setAntiAlias(true);

        //used to for the recording button
        red = new Paint();
        red.setColor(Color.RED);
        red.setStyle(Paint.Style.FILL);
        red.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_OVER));
        red.setAntiAlias(true);
    }

    public void setCaptureState(CaptureStates captureState)
    {
        synchronized (this) {
            currentShow = captureState;
        }
    }

    private void startAnimation()
    {
        Message msg  =new Message();
        msg.what = MSG_START_ANIMATION;
        this.sendMessage(msg);
    }

    public void startDrawing() {
        shutteractive = true;
        stopTimer = false;
        if (!isRunning())
            startAnimation();
    }

    @Override
    public void handleMessage(Message msg) {
        switch(msg.what)
        {
            case MSG_START_ANIMATION:
                if (!isRunning())
                    run();
                break;
            default:
                super.handleMessage(msg);
                break;
        }
    }

    public void stopShutterTimer() {
        synchronized (this) {
            stopTimer = true;
        }
    }

    private void run()
    {
        halfsize = shutterButton.getWidth()/2;
        MAX_SHUTTER_OPEN = (shutterButton.getWidth() - 100) / 2;
        SHUTTER_OPEN_STEP = (MAX_SHUTTER_OPEN) / MAXFRAMES;
        MAX_RECORDING_OPEN = shutterButton.getWidth() /4;
        RECORDING_OPEN_STEP = MAX_RECORDING_OPEN/MAXFRAMES;

        recordingRadiusCircle = MAX_RECORDING_OPEN;
        recordingRadiusRectangle = 0;
        running = true;
        while (shutteractive)
        {
            // each frame's pause is measured from that frame's start (SPEC C6). Measured from the
            // loop's start it reached zero after the first frame, and the loop then posted redraws
            // as fast as it could for a whole recording; the timer text it used to format each
            // frame had hidden that until 4.3.103 (6T: a stop tap answered 34 s late).
            calcstartTime = System.nanoTime();
            draw();
            if (currentframe >= MAXFRAMES)
            {
                currentframe = 0;
                if (stopTimer) {
                    shutteractive = false;
                }
            }
            uiHandler.obtainMessage(UIHandler.MSG_INVALIDATE).sendToTarget();
            currentframe++;
            try {
                long sleep = FPS-((System.nanoTime()-calcstartTime)/1000000L);
                if (sleep > 0)
                    Thread.sleep(sleep);
            } catch (InterruptedException e) {
                Log.WriteEx(e);
            }
        }
        running = false;

        uiHandler.obtainMessage(UIHandler.MSG_INVALIDATE).sendToTarget();
    }

    private void draw() {
       /* synchronized (this) {*/
            switch (currentShow) {
                case video_recording_stop:
                    shutter_open_radius = 0;
                    recordingRadiusCircle += RECORDING_OPEN_STEP;
                    if (recordingRadiusCircle > MAX_RECORDING_OPEN || currentframe == MAXFRAMES)
                        recordingRadiusCircle = MAX_RECORDING_OPEN;

                    recordingRadiusRectangle -= RECORDING_OPEN_STEP;
                    if (recordingRadiusRectangle < 0 || currentframe == MAXFRAMES)
                        recordingRadiusRectangle = 0;
                    drawRecordingImage = true;
                    break;
                case video_recording_start:
                    shutter_open_radius = 0;
                    recordingRadiusCircle -= RECORDING_OPEN_STEP;
                    if (recordingRadiusCircle < 0 || currentframe == MAXFRAMES)
                        recordingRadiusCircle = 0;

                    recordingRadiusRectangle += RECORDING_OPEN_STEP;
                    if (recordingRadiusRectangle > MAX_RECORDING_OPEN || currentframe == MAXFRAMES)
                        recordingRadiusRectangle = MAX_RECORDING_OPEN;
                    drawRecordingImage = true;
                    break;
                case image_capture_stop:
                    drawRecordingImage = false;
                    shutter_open_radius -= SHUTTER_OPEN_STEP;
                    if (shutter_open_radius < 0 || currentframe == MAXFRAMES)
                        shutter_open_radius = 0;
                    break;
                case image_capture_start:
                    drawRecordingImage = false;
                    shutter_open_radius += SHUTTER_OPEN_STEP;
                    if (shutter_open_radius > MAX_SHUTTER_OPEN || currentframe == MAXFRAMES)
                        shutter_open_radius = MAX_SHUTTER_OPEN;
                    break;
                case continouse_capture_start:
                    drawRecordingImage = true;
                    shutter_open_radius += SHUTTER_OPEN_STEP;
                    if (shutter_open_radius > MAX_SHUTTER_OPEN || currentframe == MAXFRAMES)
                        shutter_open_radius = MAX_SHUTTER_OPEN;

                    recordingRadiusCircle -= RECORDING_OPEN_STEP;
                    if (recordingRadiusCircle < 0 || currentframe == MAXFRAMES)
                        recordingRadiusCircle = 0;
                    recordingRadiusRectangle += RECORDING_OPEN_STEP;
                    if (recordingRadiusRectangle > MAX_RECORDING_OPEN || currentframe == MAXFRAMES)
                        recordingRadiusRectangle = MAX_RECORDING_OPEN;
                    break;
                case cont_capture_stop_while_working:
                    drawRecordingImage = true;
                    //shutter_open_radius += SHUTTER_OPEN_STEP;
                    recordingRadiusCircle += RECORDING_OPEN_STEP;
                    if (recordingRadiusCircle > MAX_RECORDING_OPEN || currentframe == MAXFRAMES)
                        recordingRadiusCircle = MAX_RECORDING_OPEN;
                    recordingRadiusRectangle -= RECORDING_OPEN_STEP;
                    if (recordingRadiusRectangle < 0 || currentframe == MAXFRAMES)
                        recordingRadiusRectangle = 0;
                    break;
                case cont_capture_stop_while_notworking:
                    shutter_open_radius = 0;
                    recordingRadiusCircle += RECORDING_OPEN_STEP;
                    if (recordingRadiusCircle > MAX_RECORDING_OPEN || currentframe == MAXFRAMES)
                        recordingRadiusCircle = MAX_RECORDING_OPEN;
                    recordingRadiusRectangle -= RECORDING_OPEN_STEP;
                    if (recordingRadiusRectangle < 0 || currentframe == MAXFRAMES)
                        recordingRadiusRectangle = 0;
                    drawRecordingImage = true;
                    break;
                case continouse_capture_stop:
                    recordingRadiusCircle += RECORDING_OPEN_STEP;
                    if (recordingRadiusCircle > MAX_RECORDING_OPEN || currentframe == MAXFRAMES)
                        recordingRadiusCircle = MAX_RECORDING_OPEN;
                    recordingRadiusRectangle -= RECORDING_OPEN_STEP;
                    if (recordingRadiusRectangle < 0 || currentframe == MAXFRAMES)
                        recordingRadiusRectangle = 0;
                    drawRecordingImage = true;
                    break;
                case continouse_capture_work_start:
                    drawRecordingImage = true;
                    shutter_open_radius += SHUTTER_OPEN_STEP;
                    if (shutter_open_radius > MAX_SHUTTER_OPEN)
                        shutter_open_radius = MAX_SHUTTER_OPEN;

                    break;
                case continouse_capture_work_stop:
                    drawRecordingImage = true;
                    shutter_open_radius -= SHUTTER_OPEN_STEP;
                    if (shutter_open_radius < 0 || currentframe == MAXFRAMES)
                        shutter_open_radius = 0;
                    break;
            }

            padding_red = halfsize;
            red_radius = recordingRadiusRectangle / 2;
            if (drawRecordingImage) {
                red_top = padding_red - red_radius;
                red_bottom = padding_red + red_radius;
                red_left = padding_red - red_radius;
                red_right = padding_red + red_radius;
            }
        //}
        //Log.d(TAG,"shutter_open:" + shutter_open_radius + " recCircle:" + recordingRadiusCircle + " recRect:" + recordingRadiusRectangle +  " captureState:" + currentShow);
    }

    public void onDraw(Canvas canvas)
    {
        //synchronized (this) {
            canvas.drawCircle(halfsize, halfsize, shutter_open_radius, transparent);
            if (drawRecordingImage) {
                canvas.drawCircle(halfsize, halfsize, recordingRadiusCircle / 2, red);
                canvas.drawRect(red_left, red_top, red_right, red_bottom, red);

            }
        //}
    }

    private static class UIHandler extends Handler
    {
        public static final int MSG_INVALIDATE = 2;
        private final WeakReference<ShutterButton> shutterButtonWeakReference;
        public UIHandler(ShutterButton shutterButton)
        {
            super(Looper.getMainLooper());
            shutterButtonWeakReference = new WeakReference<>(shutterButton);
        }

        @Override
        public void handleMessage(Message msg) {

            switch (msg.what)
            {
                case MSG_INVALIDATE:
                    ShutterButton button = shutterButtonWeakReference.get();
                    if (button != null)
                        button.invalidate();
                    break;
                default:
                    super.handleMessage(msg);
                    break;
            }

        }
    }
}
