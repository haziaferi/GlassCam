package glasscam.cam.apis.featuredetector.camera2.mtk;

import android.hardware.camera2.CameraCharacteristics;

import camera2_hidden_keys.mtk.CharacteristicsMtk;
import glasscam.cam.apis.featuredetector.camera2.BaseParameter2Detector;
import glasscam.settings.SettingKeys;
import glasscam.settings.SettingsManager;

public class AovDetector extends BaseParameter2Detector {
    @Override
    protected void findAndFillSettings(CameraCharacteristics cameraCharacteristics) {
        int[] aovmodes = cameraCharacteristics.get(CharacteristicsMtk.com_mediatek_aovservicefeature_availableAovModes);
        if(aovmodes != null)
        {
            settingsManager.get(SettingKeys.MTK_AOV_MODE).setIsSupported(true);
            settingsManager.get(SettingKeys.MTK_AOV_MODE).setValues(new String[]{"off","on"});
            settingsManager.get(SettingKeys.MTK_AOV_MODE).set("off");
        }
        else
            settingsManager.get(SettingKeys.MTK_AOV_MODE).setIsSupported(false);
    }
}
