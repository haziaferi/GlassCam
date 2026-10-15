package glasscam.cam.apis.featuredetector.camera2;

import android.hardware.camera2.CameraCharacteristics;
import android.os.Build;

import androidx.annotation.RequiresApi;

import com.hazia.glasscam.R;

import glasscam.GlassCamApplication;
import glasscam.cam.apis.featuredetector.Camera2Util;
import glasscam.settings.SettingKeys;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class AutoFocusModeDetector extends BaseParameter2Detector {
    @Override
    protected void findAndFillSettings(CameraCharacteristics cameraCharacteristics) {
        Camera2Util.detectIntMode(cameraCharacteristics, CameraCharacteristics.CONTROL_AF_AVAILABLE_MODES, settingsManager.get(SettingKeys.FOCUS_MODE), R.array.focusModes,settingsManager);
        settingsManager.get(SettingKeys.FOCUS_MODE).set(GlassCamApplication.getStringFromRessources(R.string.focus_mode_continousepicture));
    }
}
