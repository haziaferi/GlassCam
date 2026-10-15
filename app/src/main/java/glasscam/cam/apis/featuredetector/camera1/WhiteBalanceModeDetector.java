package glasscam.cam.apis.featuredetector.camera1;

import android.hardware.Camera;

import com.hazia.glasscam.R;

import glasscam.settings.SettingKeys;

public class WhiteBalanceModeDetector extends BaseParameter1Detector{
    @Override
    protected void findAndFillSettings(Camera.Parameters cameraCharacteristics) {
        detectWhiteBalanceModes(cameraCharacteristics);
    }

    private void detectWhiteBalanceModes(Camera.Parameters parameters)
    {
        detectMode(parameters, R.string.whitebalance,R.string.whitebalance_values, settingsManager.get(SettingKeys.WHITE_BALANCE_MODE));
    }
}
