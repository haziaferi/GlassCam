package glasscam.cam.apis.featuredetector.camera2.qcom;

import android.hardware.camera2.CameraCharacteristics;
import android.os.Build;

import androidx.annotation.RequiresApi;

import java.util.HashSet;

import camera2_hidden_keys.qcom.CaptureResultQcom;
import glasscam.cam.apis.featuredetector.Camera2FeatureDetectorTask;
import glasscam.cam.apis.featuredetector.camera2.BaseParameter2Detector;
import glasscam.cam.apis.featuredetector.camera2.VendorKeyDetector;
import glasscam.settings.SettingKeys;

public class HistogramSupportedDetector extends BaseParameter2Detector implements VendorKeyDetector {
    @Override
    protected void findAndFillSettings(CameraCharacteristics cameraCharacteristics) {

    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void checkIfVendorKeyIsSupported(HashSet<String> keys) {
        if (Camera2FeatureDetectorTask.isKeySupported(keys, CaptureResultQcom.HISTOGRAM_STATS)) {
            settingsManager.get(SettingKeys.HISTOGRAM_STATS_QCOM).setIsSupported(true);
            settingsManager.get(SettingKeys.HISTOGRAM_STATS_QCOM).set(false);
        }
    }
}
