package glasscam.cam.apis.featuredetector.camera1;

import android.hardware.Camera;

import com.hazia.glasscam.R;

import glasscam.settings.Frameworks;
import glasscam.settings.SettingKeys;

public class DigitalImageStabDetector extends BaseParameter1Detector{
    @Override
    protected void findAndFillSettings(Camera.Parameters cameraCharacteristics) {
        detectDisModes(cameraCharacteristics);
    }

    private void detectDisModes(Camera.Parameters parameters) {
        if (settingsManager.get(SettingKeys.DIGITAL_IMAGE_STABILIZATION).isPresetted())
            return;
        if (settingsManager.getFrameWork() == Frameworks.MTK) {
            settingsManager.get(SettingKeys.DIGITAL_IMAGE_STABILIZATION).setIsSupported(false);
        } else{
            detectMode(parameters, R.string.dis,R.string.dis_values, settingsManager.get(SettingKeys.DIGITAL_IMAGE_STABILIZATION));
        }
    }
}
