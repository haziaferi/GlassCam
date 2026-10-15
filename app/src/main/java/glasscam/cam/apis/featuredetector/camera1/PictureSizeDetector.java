package glasscam.cam.apis.featuredetector.camera1;

import android.hardware.Camera;

import com.hazia.glasscam.R;

import glasscam.settings.SettingKeys;

public class PictureSizeDetector extends BaseParameter1Detector {
    @Override
    protected void findAndFillSettings(Camera.Parameters cameraCharacteristics) {
        detectPictureSizes(cameraCharacteristics);
    }

    private void detectPictureSizes(Camera.Parameters parameters)
    {
        detectMode(parameters, R.string.picture_size,R.string.picture_size_values, settingsManager.get(SettingKeys.PICTURE_SIZE));
    }
}
