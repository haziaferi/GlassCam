package glasscam.cam.apis.featuredetector.camera2;

import android.hardware.camera2.CameraCharacteristics;
import android.os.Build;

import com.hazia.glasscam.R;

import glasscam.GlassCamApplication;
import glasscam.cam.apis.featuredetector.Camera2Util;
import glasscam.settings.SettingKeys;

public class ShadingModesDetector extends BaseParameter2Detector {
    @Override
    protected void findAndFillSettings(CameraCharacteristics cameraCharacteristics) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Camera2Util.detectIntMode(cameraCharacteristics, CameraCharacteristics.SHADING_AVAILABLE_MODES, settingsManager.get(SettingKeys.LENS_SHADE), R.array.shadingmodes,settingsManager);
        }
    }
}
