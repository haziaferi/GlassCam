package glasscam.cam.apis.featuredetector;

import android.hardware.Camera;

import com.lge.hardware.LGCameraRef;
import com.hazia.glasscam.R;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import glasscam.GlassCamApplication;
import glasscam.cam.apis.camera1.cameraholder.CameraHolderMTK;
import glasscam.cam.apis.featuredetector.camera1.AeBracketDetector;
import glasscam.cam.apis.featuredetector.camera1.AntibandingDetector;
import glasscam.cam.apis.featuredetector.camera1.ApertureDetector;
import glasscam.cam.apis.featuredetector.camera1.AutoHdrDetector;
import glasscam.cam.apis.featuredetector.camera1.ChromaFlashDetector;
import glasscam.cam.apis.featuredetector.camera1.ColorModeDetector;
import glasscam.cam.apis.featuredetector.camera1.CorrelatedDoubleSamplingDetector;
import glasscam.cam.apis.featuredetector.camera1.DenoiseDetector;
import glasscam.cam.apis.featuredetector.camera1.DigitalImageStabDetector;
import glasscam.cam.apis.featuredetector.camera1.DualPrimaryCameraDetector;
import glasscam.cam.apis.featuredetector.camera1.ExposureModeDetector;
import glasscam.cam.apis.featuredetector.camera1.FlashModeDetector;
import glasscam.cam.apis.featuredetector.camera1.FocusModeDetector;
import glasscam.cam.apis.featuredetector.camera1.ImagePostProcessingDetector;
import glasscam.cam.apis.featuredetector.camera1.IsoModesDetector;
import glasscam.cam.apis.featuredetector.camera1.JpegQualityMode;
import glasscam.cam.apis.featuredetector.camera1.LensShadeModeDetector;
import glasscam.cam.apis.featuredetector.camera1.ManualBrightnessDetector;
import glasscam.cam.apis.featuredetector.camera1.ManualContrastDetector;
import glasscam.cam.apis.featuredetector.camera1.ManualExposureDetector;
import glasscam.cam.apis.featuredetector.camera1.ManualFocusDetector;
import glasscam.cam.apis.featuredetector.camera1.ManualIsoDetector;
import glasscam.cam.apis.featuredetector.camera1.ManualSaturationDetector;
import glasscam.cam.apis.featuredetector.camera1.ManualSharpnessDetector;
import glasscam.cam.apis.featuredetector.camera1.ManualWhiteBalanceDetector;
import glasscam.cam.apis.featuredetector.camera1.MemColorEnhancDetector;
import glasscam.cam.apis.featuredetector.camera1.NonZslManualDetector;
import glasscam.cam.apis.featuredetector.camera1.OptizoomDetector;
import glasscam.cam.apis.featuredetector.camera1.PdafDetector;
import glasscam.cam.apis.featuredetector.camera1.PictureFormatDetector;
import glasscam.cam.apis.featuredetector.camera1.PictureSizeDetector;
import glasscam.cam.apis.featuredetector.camera1.PreviewFormatDetector;
import glasscam.cam.apis.featuredetector.camera1.PreviewFpsDetector;
import glasscam.cam.apis.featuredetector.camera1.PreviewFpsRangeDetector;
import glasscam.cam.apis.featuredetector.camera1.PreviewSizeDetector;
import glasscam.cam.apis.featuredetector.camera1.RdiDetector;
import glasscam.cam.apis.featuredetector.camera1.ReFocusDetector;
import glasscam.cam.apis.featuredetector.camera1.SceneModeDetector;
import glasscam.cam.apis.featuredetector.camera1.SeeMoarDetector;
import glasscam.cam.apis.featuredetector.camera1.TemporalNoiseReductionDetector;
import glasscam.cam.apis.featuredetector.camera1.TruePortraitDetector;
import glasscam.cam.apis.featuredetector.camera1.VideoHdrDetector;
import glasscam.cam.apis.featuredetector.camera1.VideoHfrDetector;
import glasscam.cam.apis.featuredetector.camera1.VideoSizeModeDetector;
import glasscam.cam.apis.featuredetector.camera1.VideoStabDetector;
import glasscam.cam.apis.featuredetector.camera1.WhiteBalanceModeDetector;
import glasscam.cam.apis.featuredetector.camera1.ZeroShutterLagDetector;
import glasscam.settings.FrameworkDetector;
import glasscam.settings.Frameworks;
import glasscam.settings.SettingKeys;
import glasscam.settings.SettingsManager;
import glasscam.utils.Log;
import glasscam.utils.VideoMediaProfile;


/**
 * Created by troop on 23.01.2017.
 */

public class Camera1FeatureDetectorTask extends AbstractFeatureDetectorTask
{
    private static final  String TAG = Camera1FeatureDetectorTask.class.getSimpleName();

    public Camera1FeatureDetectorTask()
    {
        super();
    }

    @Override
    public List<Class> createParametersToCheckList() {
        List<Class> parameter1Detectors = new ArrayList<>();
        parameter1Detectors.add(PictureFormatDetector.class);
        parameter1Detectors.add(PictureSizeDetector.class);
        parameter1Detectors.add(FocusModeDetector.class);
        parameter1Detectors.add(WhiteBalanceModeDetector.class);
        parameter1Detectors.add(ExposureModeDetector.class);
        parameter1Detectors.add(ColorModeDetector.class);
        parameter1Detectors.add(FlashModeDetector.class);
        parameter1Detectors.add(IsoModesDetector.class);
        parameter1Detectors.add(AntibandingDetector.class);
        parameter1Detectors.add(ImagePostProcessingDetector.class);
        parameter1Detectors.add(PreviewSizeDetector.class);
        parameter1Detectors.add(JpegQualityMode.class);

        parameter1Detectors.add(AeBracketDetector.class);
        parameter1Detectors.add(PreviewFpsDetector.class);
        parameter1Detectors.add(PreviewFormatDetector.class);
        parameter1Detectors.add(SceneModeDetector.class);
        parameter1Detectors.add(LensShadeModeDetector.class);
        parameter1Detectors.add(ZeroShutterLagDetector.class);
        parameter1Detectors.add(MemColorEnhancDetector.class);
        parameter1Detectors.add(VideoSizeModeDetector.class);
        parameter1Detectors.add(CorrelatedDoubleSamplingDetector.class);
        parameter1Detectors.add(VideoHdrDetector.class);
        parameter1Detectors.add(VideoHfrDetector.class);
        parameter1Detectors.add(DigitalImageStabDetector.class);
        parameter1Detectors.add(DenoiseDetector.class);
        parameter1Detectors.add(TemporalNoiseReductionDetector.class);
        parameter1Detectors.add(PdafDetector.class);
        parameter1Detectors.add(SeeMoarDetector.class);
        parameter1Detectors.add(TruePortraitDetector.class);
        parameter1Detectors.add(ReFocusDetector.class);
        parameter1Detectors.add(OptizoomDetector.class);
        parameter1Detectors.add(ChromaFlashDetector.class);
        parameter1Detectors.add(RdiDetector.class);
        parameter1Detectors.add(VideoStabDetector.class);
        parameter1Detectors.add(NonZslManualDetector.class);
        parameter1Detectors.add(PreviewFpsRangeDetector.class);
        parameter1Detectors.add(AutoHdrDetector.class);

        parameter1Detectors.add(ManualSaturationDetector.class);
        parameter1Detectors.add(ManualFocusDetector.class);
        parameter1Detectors.add(ManualSharpnessDetector.class);
        parameter1Detectors.add(ManualBrightnessDetector.class);
        parameter1Detectors.add(ManualContrastDetector.class);
        parameter1Detectors.add(ManualExposureDetector.class);
        parameter1Detectors.add(ManualIsoDetector.class);
        parameter1Detectors.add(ManualWhiteBalanceDetector.class);
        parameter1Detectors.add(DualPrimaryCameraDetector.class);
        parameter1Detectors.add(ApertureDetector.class);
        return parameter1Detectors;
    }

    private String camstring(int id)
    {
        return GlassCamApplication.getStringFromRessources(id);
    }

    @Override
    public void preDetect() {
        settingsManager.setCamApi(SettingsManager.API_1);
        if (settingsManager.getFrameWork() == Frameworks.Default)
            settingsManager.setFramework(FrameworkDetector.getFramework());
    }

    @Override
    public List<String> findCameraIDs() {
        List<String> cam_ids = new ArrayList<>();

        // Fast path: ask the framework how many cameras it officially reports
        // instead of blindly opening ids 0-199. On the vast majority of devices
        // this alone finds every real camera without a single wasted
        // Camera.open()/release() cycle.
        int reportedCount = 0;
        try {
            reportedCount = Camera.getNumberOfCameras();
        }
        catch (RuntimeException ex)
        {
            Log.d(TAG, "getNumberOfCameras() failed, falling back to id 0 only for the reported range");
        }

        for (int i = 0; i < reportedCount; i++)
        {
            Camera.Parameters parameters = null;
            try {
                parameters = getParameters(i);
                if (parameters != null)
                    cam_ids.add(String.valueOf(i));
            }
            catch(RuntimeException ex)
            {
                Log.d(TAG, "Failed to get Parameters from Camera:" + i);
            }
        }

        // Some OEM firmwares expose extra "hidden" sensors beyond the officially
        // reported count. Keep probing past reportedCount for those cases, but
        // stop after a short run of consecutive failures instead of always
        // scanning up to id 199 - the old unconditional loop is what burned
        // ~9 seconds and ~170 real Camera.open()/release() cycles on every
        // single app launch, even on devices with no hidden cameras at all.
        final int MAX_ID = 200;
        final int MAX_CONSECUTIVE_FAILURES = 5;
        int consecutiveFailures = 0;
        for (int i = reportedCount; i < MAX_ID && consecutiveFailures < MAX_CONSECUTIVE_FAILURES; i++)
        {
            Camera.Parameters parameters = null;
            try {
                parameters = getParameters(i);
            }
            catch(RuntimeException ex)
            {
                Log.d(TAG, "Failed to get Parameters from Camera:" + i);
            }
            if (parameters != null)
            {
                cam_ids.add(String.valueOf(i));
                consecutiveFailures = 0;
            }
            else
            {
                consecutiveFailures++;
            }
        }
        return cam_ids;
    }

    @Override
    public void checkCameraID(int id, List<String> cameraids, List<Class> parametersToDetect) {
        super.checkCameraID(id,cameraids,parametersToDetect);

        Camera.Parameters parameters = null;
        try {
            detectFrontCamera(Integer.parseInt(cameraids.get(id)));
            parameters = getParameters(Integer.parseInt(cameraids.get(id)));
        }
        catch(RuntimeException ex)
        {
            Log.d(TAG, "Failed to get Parameters from Camera:" + id);
        }
        if(parameters == null)
            return;

        for (int i = 0; i < parametersToDetect.size(); i++) {
            try {
                BaseParameterDetector parameter2Detector = getInstance(parametersToDetect.get(i));
                parameter2Detector.checkIfSupported(parameters);
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            } catch (InstantiationException e) {
                e.printStackTrace();
            }
        }

        detectVideoMediaProfiles(id);
        detectQcomFocus(parameters);
    }



    @Override
    public void postDetect() {
        settingsManager.SetCurrentCamera(0);
    }


    private void detectQcomFocus(Camera.Parameters parameters)
    {
        settingsManager.get(SettingKeys.USE_QCOM_FOCUS).set(parameters.get(camstring(R.string.touch_af_aec))!= null);
    }

    public static String[] createWBStringArray(int min, int max, float step)
    {
        Log.d(TAG,"Create Wbvalues");
        ArrayList<String> t = new ArrayList<>();
        t.add(GlassCamApplication.getStringFromRessources(R.string.auto_));
        int intStep = Math.max(1, Math.round(step)); // a fractional step never advanced an int counter
        for (int i = min; i<=max;i+=intStep)
        {
            t.add(i+"");
        }
        return  t.toArray(new String[t.size()]);
    }

    public static String[] createIsoValues(int miniso, int maxiso, int step, boolean xiaomi)
    {
        Log.d(TAG,"Create Isovalues");
        ArrayList<String> s = new ArrayList<>();
        s.add(GlassCamApplication.getStringFromRessources(R.string.auto_));
        if (step < 1)
            step = 1; // a device XML without <step> reads 0, and the loop never ended
        for (int i =miniso; i <= maxiso; i +=step)
        {
            if (xiaomi)
                s.add("ISO"+i);
            else
                s.add(i + "");
        }
        String[] stringvalues = new String[s.size()];
        return s.toArray(stringvalues);
    }

    public static String[] createManualFocusValues(int min, int max, int step)
    {
        ArrayList<String> ar = new ArrayList<>();
        ar.add(GlassCamApplication.getStringFromRessources(R.string.auto_));
        if (step < 1)
            step = 1; // a device XML without <step> reads 0, and the loop never ended
        for (int i = min; i < max; i+= step)
        {
            ar.add(i+"");
        }
        return ar.toArray(new String[ar.size()]);
    }

    private void detectFrontCamera(int i) {
        Camera.CameraInfo info = new Camera.CameraInfo();
        Camera.getCameraInfo(i,info);
        settingsManager.setIsFrontCamera(info.facing != Camera.CameraInfo.CAMERA_FACING_BACK);
    }


    private LGCameraRef lgCamera = null;
    private Camera.Parameters getParameters(int currentcamera)
    {
        Camera camera = null;
        switch (settingsManager.getFrameWork())
        {
            case LG:
            {
                Log.d(TAG,"Open LG Camera");
                if (settingsManager.get(SettingKeys.OPEN_CAMERA_1_LEGACY).get())
                    lgCamera = new LGCameraRef(currentcamera, 256);
                if (lgCamera == null || lgCamera.getCamera() == null)
                    lgCamera = new LGCameraRef(currentcamera);
                Camera.Parameters parameters = lgCamera.getCamera().getParameters();
                lgCamera.getCamera().release();
                lgCamera.release();
                lgCamera = null;
                return parameters;
            }
            case Moto_Ext:
            {
                Log.d(TAG,"Open MOTO Camera");
                camera  = Camera.open(currentcamera);
                Camera.Parameters parameters = camera.getParameters();
                parameters.set("mot-app", "true");
                camera.setParameters(parameters);
                parameters = camera.getParameters();
                camera.release();
                return parameters;
            }
            case MTK:
            {
                Log.d(TAG,"Open MTK Camera");
                CameraHolderMTK.setMtkAppMode();
                camera = Camera.open(currentcamera);
                Camera.Parameters parameters = camera.getParameters();
                camera.release();
                return parameters;
            }
            default:
            {
                try {
                    if (camera != null)
                        camera.release();
                    camera = Camera.open(currentcamera);
                    Camera.Parameters parameters = camera.getParameters();
                    camera.release();
                    return parameters;
                }
                catch (RuntimeException ex)
                {
                    Log.d(TAG, "unsupported id: " + currentcamera);
                    return null;
                }
            }

        }
    }

    private void detectVideoMediaProfiles(int cameraid)
    {
        final String _720phfr = "720HFR";
        final String _2160p = "2160p";
        final String _2160pDCI = "2160pDCI";
        HashMap<String,VideoMediaProfile> supportedProfiles;
        SupportedVideoProfilesDetector videoProfilesDetector = new SupportedVideoProfilesDetector();
        if(settingsManager.getFrameWork() == Frameworks.LG)
            supportedProfiles =  videoProfilesDetector.getLGVideoMediaProfiles(cameraid);
        else
            supportedProfiles= videoProfilesDetector.getDefaultVideoMediaProfiles(cameraid);

        if (supportedProfiles.get(_720phfr) == null && settingsManager.get(SettingKeys.VIDEO_HIGH_FRAMERATE).isSupported() && settingsManager.get(SettingKeys.VIDEO_HIGH_FRAMERATE).contains("120"))
        {
            try {
                Log.d(TAG, "no 720phfr profile found, but hfr supported, try to add custom 720phfr");
                VideoMediaProfile t = supportedProfiles.get("720p").clone();
                t.videoFrameRate = 120;
                t.Mode = VideoMediaProfile.VideoMode.Highspeed;
                t.ProfileName = "720pHFR";
                supportedProfiles.put("720pHFR",t);
            }
            catch (NullPointerException ex)
            {
                Log.WriteEx(ex);
            }

        }
        if (settingsManager.get(SettingKeys.VIDEO_SIZE).isSupported() && settingsManager.get(SettingKeys.VIDEO_SIZE).contains("3840x2160")
                && settingsManager.get(SettingKeys.VIDEO_HIGH_FRAMERATE).isSupported()&& settingsManager.get(SettingKeys.VIDEO_HIGH_FRAMERATE).contains("60")) //<--- that line is not needed. when parameters contains empty hfr it gets filled!
        {
            if (supportedProfiles.containsKey("1080p"))
            {
                VideoMediaProfile uhdHFR = supportedProfiles.get("1080p").clone();
                uhdHFR.videoFrameWidth = 3840;
                uhdHFR.videoFrameHeight = 2160;
                uhdHFR.videoBitRate = 30000000;
                uhdHFR.Mode = VideoMediaProfile.VideoMode.Highspeed;
                uhdHFR.ProfileName = "UHD_2160p_60FPS";
                supportedProfiles.put("UHD_2160p_60FPS", uhdHFR);
                Log.d(TAG, "added custom 2160pHFR");
            }
        }
        if (supportedProfiles.get(_2160p) == null && settingsManager.get(SettingKeys.VIDEO_SIZE).isSupported()&& settingsManager.get(SettingKeys.VIDEO_SIZE).contains("3840x2160"))
        {
            if (supportedProfiles.containsKey("1080p"))
            {
                VideoMediaProfile uhd = supportedProfiles.get("1080p").clone();
                uhd.videoFrameWidth = 3840;
                uhd.videoFrameHeight = 2160;
                uhd.videoBitRate = 30000000;
                uhd.Mode = VideoMediaProfile.VideoMode.Normal;
                uhd.ProfileName = _2160p;
                supportedProfiles.put(_2160p, uhd);
                Log.d(TAG, "added custom 2160p");
            }
        }

        if (settingsManager.get(SettingKeys.VIDEO_SIZE).isSupported() && settingsManager.get(SettingKeys.VIDEO_SIZE).contains("1920x1080")
                && settingsManager.get(SettingKeys.VIDEO_HIGH_FRAMERATE).isSupported()&& settingsManager.get(SettingKeys.VIDEO_HIGH_FRAMERATE).contains("60")) //<--- that line is not needed. when parameters contains empty hfr it gets filled!
        {
            if (supportedProfiles.containsKey("1080p")) {
                VideoMediaProfile t = supportedProfiles.get("1080p").clone();
                t.videoFrameRate = 60;
                t.Mode = VideoMediaProfile.VideoMode.Highspeed;
                t.ProfileName = "1080pHFR";
                supportedProfiles.put("1080pHFR", t);
                Log.d(TAG, "added custom 1080pHFR");
            }

        }
        settingsManager.saveMediaProfiles(supportedProfiles);
        settingsManager.get(SettingKeys.VIDEO_PROFILES).set("720p");
    }
}
