package glasscam.cam.apis.featuredetector.camera2.xiaomi;

import android.hardware.camera2.CameraCharacteristics;
import android.os.Build;

import androidx.annotation.RequiresApi;

import java.util.HashSet;

import camera2_hidden_keys.devices.pocof2.CaptureRequestDump;
import glasscam.cam.apis.featuredetector.Camera2FeatureDetectorTask;
import glasscam.cam.apis.featuredetector.camera2.BaseParameter2Detector;
import glasscam.cam.apis.featuredetector.camera2.VendorKeyDetector;
import glasscam.settings.SettingKeys;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class XiaomiMfnrDetector extends BaseParameter2Detector implements VendorKeyDetector {
    @Override
    protected void findAndFillSettings(CameraCharacteristics cameraCharacteristics) {

    }


    @Override
    public void checkIfVendorKeyIsSupported(HashSet<String> keys) {
        settingsManager.get(SettingKeys.XIAOMI_MFNR).setIsSupported(Camera2FeatureDetectorTask.isKeySupported(keys, CaptureRequestDump.xiaomi_mfnr_enabled));
    }
}
