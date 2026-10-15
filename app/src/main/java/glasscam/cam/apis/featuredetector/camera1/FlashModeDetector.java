package glasscam.cam.apis.featuredetector.camera1;

import android.hardware.Camera;

import com.hazia.glasscam.R;

import glasscam.settings.SettingKeys;

public class FlashModeDetector extends BaseParameter1Detector{
    @Override
    protected void findAndFillSettings(Camera.Parameters cameraCharacteristics) {
        detectFlashModes(cameraCharacteristics);
    }

    private void detectFlashModes(Camera.Parameters parameters)
    {
        detectMode(parameters, R.string.flash_mode,R.string.flash_mode_values, settingsManager.get(SettingKeys.FLASH_MODE));
    }
}
