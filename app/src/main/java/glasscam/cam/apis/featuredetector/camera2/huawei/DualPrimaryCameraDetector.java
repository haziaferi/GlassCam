package glasscam.cam.apis.featuredetector.camera2.huawei;

import android.hardware.camera2.CameraCharacteristics;
import android.os.Build;

import androidx.annotation.RequiresApi;

import com.hazia.glasscam.R;

import camera2_hidden_keys.huawei.CameraCharacteristicsHuawei;
import glasscam.GlassCamApplication;
import glasscam.cam.apis.featuredetector.Camera2Util;
import glasscam.cam.apis.featuredetector.camera2.BaseParameter2Detector;
import glasscam.settings.SettingKeys;

public class DualPrimaryCameraDetector extends BaseParameter2Detector {
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void findAndFillSettings(CameraCharacteristics cameraCharacteristics) {
        Camera2Util.detectByteMode(cameraCharacteristics, CameraCharacteristicsHuawei.HUAWEI_AVAILABLE_DUAL_PRIMARY, settingsManager.get(SettingKeys.DUAL_PRIMARY_CAMERA_MODE), GlassCamApplication.getStringArrayFromRessource(R.array.dual_camera_mode),settingsManager);

    }
}
