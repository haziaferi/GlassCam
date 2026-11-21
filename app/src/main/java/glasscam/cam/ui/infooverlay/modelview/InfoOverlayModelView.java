package glasscam.cam.ui.infooverlay.modelview;

import android.content.Context;
import android.os.Build;
import android.os.Environment;
import android.os.StatFs;
import android.os.SystemClock;

import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;
import androidx.lifecycle.ViewModel;

import com.hazia.glasscam.R;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import glasscam.GlassCamApplication;
import glasscam.cam.apis.CameraApiManager;
import glasscam.cam.apis.basecamera.parameters.ParameterInterface;
import glasscam.cam.ui.infooverlay.model.InfoOverlayModel;
import glasscam.cam.ui.cameraui.service.BatteryService;
import glasscam.settings.SettingKeys;
import glasscam.settings.SettingsManager;
import glasscam.utils.BackgroundHandlerThread;
import glasscam.utils.LocationManager;

public class InfoOverlayModelView extends ViewModel implements LifecycleObserver, glasscam.cam.event.capture.CaptureStateChangedEvent {

    //this holds the format for video or picture
    private String format;
    //this holds the size for video/picture
    String size;

    String storageSpace;
    private DecimalFormat decimalFormat;

    private final String[] units = { "B", "KB", "MB", "GB", "TB" };
    private boolean started;
    private boolean isStopped;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");

    private SettingsManager settingsManager;
    private final BatteryService batteryService;
    private CameraApiManager cameraApiManager;
    private final InfoOverlayModel infoOverlayModel;
    private LocationManager locationManager;
    private final BackgroundHandlerThread backgroundHandlerThread;

    public InfoOverlayModelView(Context context)
    {
        this.batteryService = new BatteryService(context);
        setCameraApiManager(cameraApiManager);
        infoOverlayModel = new InfoOverlayModel();
        backgroundHandlerThread = new BackgroundHandlerThread("InfoOverlay");
    }

    public void setCameraApiManager(CameraApiManager cameraApiManager)
    {
        this.cameraApiManager = cameraApiManager;
    }

    public void setSettingsManager(SettingsManager settingsManager)
    {
        this.settingsManager = settingsManager;
    }

    public InfoOverlayModel getInfoOverlayModel() {
        return infoOverlayModel;
    }

    public CameraApiManager getCameraApiManager() {
        return cameraApiManager;
    }

    public SettingsManager getSettingsManager() {
        return settingsManager;
    }

    public void setLocationManager(LocationManager locationManager) {
        this.locationManager = locationManager;
    }

    BatteryService.BatteryEvent batteryEvent = new BatteryService.BatteryEvent() {
        @Override
        public void onBatteryChanged(String batterylvl) {
            infoOverlayModel.setBatteryLvl(batterylvl);
        }
    };


    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    public void start()
    {
        batteryService.setBatteryEventListner(batteryEvent);
        batteryService.startListen();
        backgroundHandlerThread.create();
        started = true;
        // a stop event lost while paused must not leave an old count on screen (SPEC C6)
        countingSince = 0;
        recording = false;
        if (cameraApiManager != null)
            cameraApiManager.addCaptureStateChangedEventListner(this);
        startLooperThread();
    }

    // When the capture in progress started (SystemClock.elapsedRealtime), 0 when none runs. While a
    // capture has run for 1 s or more, the pill's clock shows its elapsed time (SPEC C6, owner
    // 4.3.103); it used to be drawn in green across the shutter's stop button, at 2.80:1.
    private volatile long countingSince = 0;
    private volatile boolean recording = false;

    @Override
    public void onCaptureStateChanged(glasscam.cam.event.capture.CaptureStates states) {
        switch (states) {
            case video_recording_start:
                recording = true;
                infoOverlayModel.setRecording(true);
                countingSince = SystemClock.elapsedRealtime();
                break;
            case video_recording_stop:
                recording = false;
                infoOverlayModel.setRecording(false);
                countingSince = 0;
                break;
            case image_capture_start:
            case continouse_capture_work_start:
            case selftimerstart:
                if (!recording)   // a still taken during a recording keeps the recording's count
                    countingSince = SystemClock.elapsedRealtime();
                break;
            case image_capture_stop:
            case continouse_capture_work_stop:
            case cont_capture_stop_while_notworking:
            case selftimerstop:
                if (!recording)
                    countingSince = 0;
                break;
            default:
                break;
        }
    }

    // m:ss below an hour, h:mm:ss from an hour; no milliseconds (the shutter's timer showed mm:ss:SSS)
    static String elapsed(long ms) {
        long s = ms / 1000;
        long h = s / 3600, m = (s % 3600) / 60, sec = s % 60;
        return h > 0 ? String.format(Locale.ROOT, "%d:%02d:%02d", h, m, sec)
                     : String.format(Locale.ROOT, "%d:%02d", m, sec);
    }
    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    public void stop()
    {
        started = false;
        if (cameraApiManager != null)
            cameraApiManager.removeCaptureStateChangedListner(this);
        infoOverlayModel.setRecording(false);
        recording = false;
        countingSince = 0;
        batteryService.stopListen();
        backgroundHandlerThread.destroy();
        batteryService.setBatteryEventListner(null);
    }

    private void startLooperThread()
    {
        if (started)
            backgroundHandlerThread.executeDelayed(runner, 1000);
    }

    private final Runnable runner = new Runnable() {
        @Override
        public void run()
        {
            long since = countingSince;
            long ms = since == 0 ? 0 : SystemClock.elapsedRealtime() - since;
            // a short exposure never reaches 1 s, so the clock does not flash "0:00"
            infoOverlayModel.setTime(ms >= 1000 ? elapsed(ms) : dateFormat.format(new Date()));
            if (cameraApiManager.getCamera() != null){
                try {
                    getFormat();
                }
                catch (NullPointerException ex)
                {
                    //Log.WriteEx(ex);
                }
                getStorageSpace();
            }
            else
            {
                format = "";
            }
            infoOverlayModel.setStorageSpace(storageSpace);
            infoOverlayModel.setSize(size);
            infoOverlayModel.setGps(getGps(locationManager));
            infoOverlayModel.setFormat(format);
            startLooperThread();
        }
    };

    private String getGps(LocationManager locationManager)
    {
        if (locationManager.getCurrentLocation() != null)
        {
            return GlassCamApplication.getStringFromRessources(R.string.font_gps)  +locationManager.getCurrentLocation().getAccuracy();
        }
        else return "";
    }

    private void getFormat()
    {
        if (cameraApiManager.getCamera().getModuleHandler().getCurrentModuleName().equals(GlassCamApplication.getStringFromRessources(R.string.module_video)))
        {
            ParameterInterface videoprofile = cameraApiManager.getCamera().getParameterHandler().get(SettingKeys.VIDEO_PROFILES);
            if (videoprofile != null)
                size = videoprofile.getStringValue();
            else
                size = "";
            // the picture format is meaningless while recording; it used to stay on screen stale
            format = GlassCamApplication.getStringFromRessources(R.string.overlay_video_label);
        }
        else
        {
            ParameterInterface pictureFormat = cameraApiManager.getCamera().getParameterHandler().get(SettingKeys.PICTURE_FORMAT);
            if (pictureFormat != null)
                format = pictureFormat.getStringValue();
            else
                format = "";

            ParameterInterface pictureSize = cameraApiManager.getCamera().getParameterHandler().get(SettingKeys.PICTURE_SIZE);
            if (pictureSize != null)
                size = pictureSize.getStringValue();
            else
                size = "";
        }
    }

    private void getStorageSpace()
    {
        try
        {
            //defcomg was here 24/01/2015
            if(!cameraApiManager.getCamera().getModuleHandler().getCurrentModuleName().equals(GlassCamApplication.getStringFromRessources(R.string.module_video)))
                storageSpace = Avail4PIC();
            else
                storageSpace = readableFileSize(SDspace());
        }
        catch (Exception ex)
        {
            storageSpace = "";
        }


    }

    private String readableFileSize(long size) {
        if( size < 524288000 ){ //at least leave 500MB so that OS can work properly.
            //set not enough storage recording state
            cameraApiManager.getCamera().getModuleHandler().SetIsLowStorage(true);
            if( !isStopped ) {
                //low storage reached; automatically stop the video.
                cameraApiManager.getCamera().getModuleHandler().startWork();
                isStopped = true;
            }
        }
        else{
            isStopped = false;
            cameraApiManager.getCamera().getModuleHandler().SetIsLowStorage(false);
        }

        if(size <= 0) return "0";
        int digitGroups = (int) (Math.log10(size)/Math.log10(1024));
        return decimalFormat.format(size/Math.pow(1024, digitGroups)) + " " + units[digitGroups];
    }

    private  String Avail4PIC()
    {
        // double calc;
        long done;
        done = (long) Calc();
        long a = SDspace() / done;
        return  a + " left";
    }
    private double Calc()
    {
        String[] res = settingsManager.get(SettingKeys.PICTURE_SIZE).get().split("x");

        if(settingsManager.get(SettingKeys.PICTURE_FORMAT).get().contains(GlassCamApplication.getStringFromRessources(R.string.bayer_)))
        {
            if (Build.MANUFACTURER.contains("HTC"))
                return Integer.parseInt(res[0]) * 2 *Integer.parseInt(res[1]) * 16 / 8;
            else
                return Integer.parseInt(res[0]) *Integer.parseInt(res[1]) * 10 / 8;
        }
        else
            return Integer.parseInt(res[0]) *Integer.parseInt(res[1]) * 8 / 8;
    }

    private long SDspace()
    {
        long bytesAvailable = 0;
        if (!settingsManager.GetWriteExternal()) {
            bytesAvailable = Environment.getExternalStorageDirectory().getUsableSpace();
        }
        else
        {
            StatFs stat = new StatFs(System.getenv("SECONDARY_STORAGE"));
            if(Build.VERSION.SDK_INT > 17)
                bytesAvailable = stat.getFreeBytes();
            else
            {
                bytesAvailable = stat.getAvailableBlocks() * stat.getBlockSize();
            }

        }
        return bytesAvailable;
    }
}
