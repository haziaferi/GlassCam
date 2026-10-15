package glasscam.cam.apis.featuredetector;

import android.content.Context;
import android.graphics.ImageFormat;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.os.Build;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import camera2_hidden_keys.VendorKeyParser;
import camera2_hidden_keys.qcom.CameraCharacteristicsQcom;
import glasscam.GlassCamApplication;
import glasscam.cam.apis.featuredetector.camera2.AeMeteringModeDetector;
import glasscam.cam.apis.featuredetector.camera2.AeTargetFpsDetector;
import glasscam.cam.apis.featuredetector.camera2.AntiBadindingModeDetector;
import glasscam.cam.apis.featuredetector.camera2.ApertureDetector;
import glasscam.cam.apis.featuredetector.camera2.AutoExposureModeDetector;
import glasscam.cam.apis.featuredetector.camera2.AutoFocusModeDetector;
import glasscam.cam.apis.featuredetector.camera2.AwbModesDetector;
import glasscam.cam.apis.featuredetector.camera2.BaseParameter2Detector;
import glasscam.cam.apis.featuredetector.camera2.CameraControlModeDetector;
import glasscam.cam.apis.featuredetector.camera2.ColorCorrectionModeDetector;
import glasscam.cam.apis.featuredetector.camera2.ColorModeDetector;
import glasscam.cam.apis.featuredetector.camera2.DenoisParameterDetector;
import glasscam.cam.apis.featuredetector.camera2.DistortionCorrectionDetector;
import glasscam.cam.apis.featuredetector.camera2.EdgeModeDetector;
import glasscam.cam.apis.featuredetector.camera2.EvDetector;
import glasscam.cam.apis.featuredetector.camera2.ExposureTimeDetector;
import glasscam.cam.apis.featuredetector.camera2.FaceModeDetector;
import glasscam.cam.apis.featuredetector.camera2.FlashDetector;
import glasscam.cam.apis.featuredetector.camera2.HotPixelModeDetector;
import glasscam.cam.apis.featuredetector.camera2.IsoDetector;
import glasscam.cam.apis.featuredetector.camera2.ManualFocusDetector;
import glasscam.cam.apis.featuredetector.camera2.OisDetector;
import glasscam.cam.apis.featuredetector.camera2.PhysicalCameraDetector;
import glasscam.cam.apis.featuredetector.camera2.PictureFormatDetector;
import glasscam.cam.apis.featuredetector.camera2.PictureSizeDetector;
import glasscam.cam.apis.featuredetector.camera2.PostRawSensitivityBoostDetector;
import glasscam.cam.apis.featuredetector.camera2.SaturationDetector;
import glasscam.cam.apis.featuredetector.camera2.SceneModeDetector;
import glasscam.cam.apis.featuredetector.camera2.ShadingModesDetector;
import glasscam.cam.apis.featuredetector.camera2.SharpnessDetector;
import glasscam.cam.apis.featuredetector.camera2.ToneMapModesDetector;
import glasscam.cam.apis.featuredetector.camera2.VendorKeyDetector;
import glasscam.cam.apis.featuredetector.camera2.VideoMediaProfilesDetector;
import glasscam.cam.apis.featuredetector.camera2.VideoStabilizationModeDetector;
import glasscam.cam.apis.featuredetector.camera2.debug.DumpQCFA;
import glasscam.cam.apis.featuredetector.camera2.debug.DumpScalerStreamConfigurationMap;
import glasscam.cam.apis.featuredetector.camera2.huawei.DualPrimaryCameraDetector;
import glasscam.cam.apis.featuredetector.camera2.huawei.IsoExposureTimeDetector;
import glasscam.cam.apis.featuredetector.camera2.huawei.Raw12bitDetector;
import glasscam.cam.apis.featuredetector.camera2.huawei.SecondarySensorSizeDetector;
import glasscam.cam.apis.featuredetector.camera2.huawei.WhitebalanceRangeDetector;
import glasscam.cam.apis.featuredetector.camera2.mtk.AovDetector;
import glasscam.cam.apis.featuredetector.camera2.qcom.HistogramSupportedDetector;
import glasscam.cam.apis.featuredetector.camera2.qcom.MFNRDetector;
import glasscam.cam.apis.featuredetector.camera2.qcom.QcomAeDetector;
import glasscam.cam.apis.featuredetector.camera2.qcom.VideoHdr10Detector;
import glasscam.cam.apis.featuredetector.camera2.xiaomi.ArcDistortionDetector;
import glasscam.cam.apis.featuredetector.camera2.xiaomi.ProVideoLogDetector;
import glasscam.cam.apis.featuredetector.camera2.xiaomi.SuperLowLightRawDetector;
import glasscam.cam.apis.featuredetector.camera2.xiaomi.VideoRecordControl;
import glasscam.cam.apis.featuredetector.camera2.xiaomi.XiaomiMfnrDetector;
import glasscam.cam.ui.videoprofileeditor.MediaCodecInfoParser;
import glasscam.settings.SettingKeys;
import glasscam.settings.SettingsManager;
import glasscam.utils.Log;


/**
 * Created by troop on 23.01.2017.
 */

@androidx.annotation.RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class Camera2FeatureDetectorTask extends AbstractFeatureDetectorTask {

    private final String TAG = Camera2FeatureDetectorTask.class.getSimpleName();
    boolean hasCamera2Features;

    public int hwlvl = CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY;
    private CameraManager manager;

    public Camera2FeatureDetectorTask() {
        super();
    }

    @Override
    public List<Class> createParametersToCheckList() {
        List<Class> parameter2Detectors = new ArrayList<>();
        parameter2Detectors.add(FlashDetector.class);
        parameter2Detectors.add(SceneModeDetector.class);
        parameter2Detectors.add(AntiBadindingModeDetector.class);
        parameter2Detectors.add(ColorModeDetector.class);
        parameter2Detectors.add(EdgeModeDetector.class);
        parameter2Detectors.add(OisDetector.class);
        parameter2Detectors.add(AutoFocusModeDetector.class);
        parameter2Detectors.add(HotPixelModeDetector.class);
        parameter2Detectors.add(VideoStabilizationModeDetector.class);
        parameter2Detectors.add(DenoisParameterDetector.class);
        parameter2Detectors.add(PictureFormatDetector.class);
        parameter2Detectors.add(DistortionCorrectionDetector.class);
        parameter2Detectors.add(FaceModeDetector.class);
        parameter2Detectors.add(ArcDistortionDetector.class);
        parameter2Detectors.add(SuperLowLightRawDetector.class);
        parameter2Detectors.add(PictureSizeDetector.class);
        parameter2Detectors.add(PhysicalCameraDetector.class);
        //call this after PictureSizes got detected. it depends on the picturesizes
        parameter2Detectors.add(VideoMediaProfilesDetector.class);
        parameter2Detectors.add(AutoExposureModeDetector.class);
        parameter2Detectors.add(ColorCorrectionModeDetector.class);
        parameter2Detectors.add(ToneMapModesDetector.class);
        parameter2Detectors.add(ShadingModesDetector.class);
        parameter2Detectors.add(AwbModesDetector.class);
        parameter2Detectors.add(CameraControlModeDetector.class);
        parameter2Detectors.add(AeTargetFpsDetector.class);
        parameter2Detectors.add(AeMeteringModeDetector.class);
        //N
        parameter2Detectors.add(PostRawSensitivityBoostDetector.class);

        //qcom
        parameter2Detectors.add(VideoHdr10Detector.class);
        parameter2Detectors.add(HistogramSupportedDetector.class);
        parameter2Detectors.add(MFNRDetector.class);
        //run this bevor exposuretime get detected
        parameter2Detectors.add(QcomAeDetector.class);
        //manuals
        parameter2Detectors.add(ApertureDetector.class);
        parameter2Detectors.add(EvDetector.class);
        parameter2Detectors.add(ExposureTimeDetector.class);
        parameter2Detectors.add(IsoDetector.class);
        parameter2Detectors.add(ManualFocusDetector.class);
        parameter2Detectors.add(SharpnessDetector.class);
        parameter2Detectors.add(SaturationDetector.class);
        //xiaomi
        parameter2Detectors.add(VideoRecordControl.class);
        parameter2Detectors.add(ProVideoLogDetector.class);
        parameter2Detectors.add(XiaomiMfnrDetector.class);

        //huawei
        parameter2Detectors.add(DualPrimaryCameraDetector.class);
        parameter2Detectors.add(IsoExposureTimeDetector.class);
        parameter2Detectors.add(Raw12bitDetector.class);
        parameter2Detectors.add(SecondarySensorSizeDetector.class);
        parameter2Detectors.add(WhitebalanceRangeDetector.class);

        //mtk
        parameter2Detectors.add(AovDetector.class);

        //debug
        parameter2Detectors.add(DumpQCFA.class);
        parameter2Detectors.add(DumpScalerStreamConfigurationMap.class);



        return parameter2Detectors;
    }

    @Override
    public void preDetect() {
        new MediaCodecInfoParser().logMediaCodecInfos();

        settingsManager.setCamApi(SettingsManager.API_2);
        manager = (CameraManager) GlassCamApplication.getContext().getSystemService(Context.CAMERA_SERVICE);
    }

    @Override
    public List<String> findCameraIDs() {
        List<String> cameraids =new ArrayList<>();
        findCameraIds(manager, cameraids);
        return cameraids;
    }

    @Override
    public void checkCameraID(int id, List<String> cameraids, List<Class> parametersToDetect) {
        super.checkCameraID(id,cameraids,parametersToDetect);
        CameraCharacteristics characteristics = null;
        try {
            characteristics = manager.getCameraCharacteristics(cameraids.get(id));
        } catch (CameraAccessException e) {
            Log.WriteEx(e);
        }
        if (characteristics == null) {
            Log.e(TAG, "Failed to get Characteristics for camera id:" + id);
            return;
        }
        boolean front = characteristics.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_FRONT;
        settingsManager.setIsFrontCamera(front);
        VendorKeyParser vendorKeyParser = new VendorKeyParser();
        HashSet<String> vendorkeys = null;
        try {
            vendorKeyParser.readVendorKeys(characteristics,CaptureRequest.Key.class);
            vendorkeys = vendorKeyParser.getRequests();
            /*VendorKeyTestLog vendorKeyTestLog = new VendorKeyTestLog(vendorKeyParser,characteristics,null,null);
            vendorKeyTestLog.testKeys();*/
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }

        hwlvl = characteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL);
        //check first if a already checked cam have camera2features and if its now the front cam that dont have a camera2feature.
        //in that case set it to true
        //else it would override the already detected featureset from last cam and disable api2
        if (settingsManager.hasCamera2Features() && front) {
            hasCamera2Features = true;
            Log.d(TAG,"Front cam has no camera2 featureset, try to find supported things anyway");
        }
        else
            hasCamera2Features = true;
        settingsManager.setHasCamera2Features(hasCamera2Features);

        if (hasCamera2Features) {

            for (int i = 0; i < parametersToDetect.size(); i++) {
                try {
                    BaseParameter2Detector parameter2Detector = getInstance(parametersToDetect.get(i));
                    if (parameter2Detector instanceof VendorKeyDetector) {
                        ((VendorKeyDetector) parameter2Detector).checkIfVendorKeyIsSupported(vendorkeys);
                    } else
                        parameter2Detector.checkIfSupported(characteristics);
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

            if (settingsManager.get(SettingKeys.M_FOCUS).isSupported()) {
                settingsManager.get(SettingKeys.ZOOM_ON_MANUALFOCUS).setIsSupported(true);
                settingsManager.get(SettingKeys.ZOOM_ON_MANUALFOCUS).set(true);
                settingsManager.get(SettingKeys.ZOOM_ON_MANUALFOCUS_ZOOMFACTOR).setIsSupported(true);
                String[] zoom = new String[]{"0", "10", "20", "30", "40", "50", "60", "70", "80", "90", "100"};
                settingsManager.get(SettingKeys.ZOOM_ON_MANUALFOCUS_ZOOMFACTOR).setValues(zoom);
                settingsManager.get(SettingKeys.ZOOM_ON_MANUALFOCUS_ZOOMFACTOR).set("50");
                settingsManager.get(SettingKeys.ZOOM_ON_MANUALFOCUS_ZOOMDURATION).setIsSupported(true);
                String[] duration = new String[]{"0", "1", "2", "3", "4", "5"};
                settingsManager.get(SettingKeys.ZOOM_ON_MANUALFOCUS_ZOOMDURATION).setValues(duration);
                settingsManager.get(SettingKeys.ZOOM_ON_MANUALFOCUS_ZOOMDURATION).set("1");
            }

            String[] templates = new String[]{"Preview","StillCapture","Record","VideoSnapshot","ZeroShutterLag","Manual"};
            settingsManager.get(SettingKeys.PREVIEW_TEMPLATE).setIsSupported(true);
            settingsManager.get(SettingKeys.PREVIEW_TEMPLATE).setValues(templates);
            settingsManager.get(SettingKeys.PREVIEW_TEMPLATE).set(templates[0]);

            settingsManager.get(SettingKeys.CAPTURE_TEMPLATE).setIsSupported(true);
            settingsManager.get(SettingKeys.CAPTURE_TEMPLATE).setValues(templates);
            settingsManager.get(SettingKeys.CAPTURE_TEMPLATE).set(templates[1]);
        }
    }

    @Override
    public void postDetect() {
        settingsManager.SetCurrentCamera(0);
        if (!hasCamera2Features || hwlvl == CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY) {
            settingsManager.setCamApi(SettingsManager.API_1);
        }
    }

    public static boolean isKeySupported(HashSet<String> vendorkeys, CaptureRequest.Key key)
    {
        return vendorkeys != null && vendorkeys.contains(key.getName());
    }

    public static boolean isKeySupported(HashSet<String> vendorkeys, CameraCharacteristics.Key key)
    {
        return vendorkeys != null && vendorkeys.contains(key.getName());
    }

    public static boolean isKeySupported(HashSet<String> vendorkeys, CaptureResult.Key key)
    {
        return vendorkeys != null && vendorkeys.contains(key.getName());
    }

    private void findCameraIds(CameraManager manager, List<String> cameraids) {
        List<String> allcameraids = new ArrayList<>();
        List<String> nonlogicids = new ArrayList<>();
        String[] reportedids = null;
        try {
            reportedids = manager.getCameraIdList();
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        if (reportedids == null)
            reportedids = new String[0];

        // Fast path: every id the framework already reports is guaranteed
        // valid, so evaluate those first with zero wasted camera-service
        // round trips.
        int highestReportedId = -1;
        for (String reportedid : reportedids) {
            try {
                highestReportedId = Math.max(highestReportedId, Integer.parseInt(reportedid));
            } catch (NumberFormatException ex) {
                // Non-numeric ids (seen on some OEM HALs) just skip the
                // "highest reported id" bookkeeping below; they're already
                // accounted for via inspectCameraId() above.
            }
            inspectCameraId(manager, reportedid, allcameraids, nonlogicids);
        }

        // Some OEM firmwares expose extra "hidden" logical/physical sensors
        // beyond the officially reported ids. Keep probing past the highest
        // reported id for those cases, but stop after a short run of
        // consecutive misses instead of always scanning up to id 199 - the
        // old unconditional loop fired ~200 getCameraCharacteristics() calls
        // against the camera service (most throwing) on every single app
        // launch, which is what made startup slow.
        final int MAX_ID = 200;
        final int MAX_CONSECUTIVE_MISSES = 5;
        int consecutiveMisses = 0;
        for (int i = highestReportedId + 1; i < MAX_ID && consecutiveMisses < MAX_CONSECUTIVE_MISSES; i++) {
            boolean found = inspectCameraId(manager, String.valueOf(i), allcameraids, nonlogicids);
            consecutiveMisses = found ? 0 : consecutiveMisses + 1;
        }

        if (nonlogicids.size() >= reportedids.length || allcameraids.size() >= reportedids.length) {
            if (nonlogicids.size() == 0)
                cameraids.addAll(allcameraids);
            else
                cameraids.addAll(nonlogicids);
        }
        else
            cameraids.addAll(Arrays.asList(reportedids));

    }

    /**
     * Inspects a single camera id, recording it into allcameraids (any valid
     * id) and, when it exposes raw+yuv+jpeg outputs and isn't a logical
     * camera, into nonlogicids too. Returns whether the id resolved to real
     * characteristics, so callers can detect a run of consecutive misses
     * without unwinding an exception at every call site.
     */
    private boolean inspectCameraId(CameraManager manager, String id, List<String> allcameraids, List<String> nonlogicids) {
        try {
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(id);

            if (characteristics != null) {
                boolean raw = false;
                boolean yuv = false;
                boolean jpeg = false;
                boolean logical = false;
                allcameraids.add(id);
                StreamConfigurationMap smap = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                int[] outputformats =  smap.getOutputFormats();
                try {
                    if(characteristics.get(CameraCharacteristicsQcom.is_logical_camera) != null) {
                        int logical_b = (int) characteristics.get(CameraCharacteristicsQcom.is_logical_camera);
                        Log.d(TAG, "logical id = " + logical_b);
                        if (logical_b > 5)
                            logical = true;
                    }
                }
                catch (IllegalArgumentException ex)
                {
                    Log.d(TAG, "CameraCharacteristicsQcom.is_logical_camera not supported");
                }
                catch (NullPointerException ex)
                {
                    Log.d(TAG, "CameraCharacteristicsQcom.is_logical_camera not supported");
                }
                catch (Exception ex)
                {
                    Log.d(TAG, "CameraCharacteristicsQcom.is_logical_camera not supported");
                }

                for(int outformat : outputformats)
                {
                    switch (outformat)
                    {
                        case ImageFormat.RAW_SENSOR:
                        case ImageFormat.RAW10:
                        case ImageFormat.RAW12:
                            raw = true;
                            break;
                        case ImageFormat.JPEG:
                            jpeg =true;
                            break;
                            case ImageFormat.YUV_420_888:
                                yuv = true;
                                break;
                    }
                }
                if (yuv && raw && jpeg && !logical)
                    nonlogicids.add(id);
                return true;
            }
        }
        catch (IllegalArgumentException ex)
        {
        }
        catch (CameraAccessException ex)
        {
        }
        catch (Exception ex)
        {
        }
        return false;
    }
}
