package glasscam.cam.apis.featuredetector.camera1;

import android.hardware.Camera;

import com.hazia.glasscam.R;

import glasscam.settings.SettingKeys;

public class PreviewSizeDetector extends BaseParameter1Detector{
    @Override
    protected void findAndFillSettings(Camera.Parameters cameraCharacteristics) {
        detectMode(cameraCharacteristics, R.string.preview_size,R.string.preview_size_values, settingsManager.get(SettingKeys.PREVIEW_SIZE));
    }
}
