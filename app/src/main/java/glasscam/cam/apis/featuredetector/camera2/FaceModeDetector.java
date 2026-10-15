package glasscam.cam.apis.featuredetector.camera2;

import android.hardware.camera2.CameraCharacteristics;
import android.os.Build;

import androidx.annotation.RequiresApi;

import com.hazia.glasscam.R;

import glasscam.GlassCamApplication;
import glasscam.cam.apis.featuredetector.Camera2Util;
import glasscam.settings.SettingKeys;

public class FaceModeDetector extends BaseParameter2Detector {
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void findAndFillSettings(CameraCharacteristics cameraCharacteristics) {
        Camera2Util.detectIntMode(cameraCharacteristics, CameraCharacteristics.STATISTICS_INFO_AVAILABLE_FACE_DETECT_MODES, settingsManager.get(SettingKeys.FACE_DETECTOR_MODE), R.array.facemodes,settingsManager);
    }
}
