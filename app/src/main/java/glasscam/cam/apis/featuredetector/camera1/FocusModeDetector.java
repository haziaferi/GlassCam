package glasscam.cam.apis.featuredetector.camera1;

import android.hardware.Camera;

import com.hazia.glasscam.R;

import glasscam.settings.SettingKeys;

public class FocusModeDetector extends BaseParameter1Detector {
    @Override
    protected void findAndFillSettings(Camera.Parameters cameraCharacteristics) {
        detectFocusModes(cameraCharacteristics);
    }

    private void detectFocusModes(Camera.Parameters parameters)
    {
        detectMode(parameters, R.string.focus_mode,R.string.focus_mode_values, settingsManager.get(SettingKeys.FOCUS_MODE));
    }
}
