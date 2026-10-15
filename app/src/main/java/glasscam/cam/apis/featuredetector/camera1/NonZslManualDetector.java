package glasscam.cam.apis.featuredetector.camera1;

import android.hardware.Camera;

import com.hazia.glasscam.R;

import glasscam.GlassCamApplication;
import glasscam.settings.SettingKeys;

public class NonZslManualDetector extends BaseParameter1Detector {
    @Override
    protected void findAndFillSettings(Camera.Parameters cameraCharacteristics) {
        detectNonZslmanual(cameraCharacteristics);
    }

    private void detectNonZslmanual(Camera.Parameters parameters) {
        if(parameters.get("non-zsl-manual-mode")!=null)
        {
            settingsManager.get(SettingKeys.NON_ZSL_MANUAL_MODE).setIsSupported(true);
            settingsManager.get(SettingKeys.NON_ZSL_MANUAL_MODE).setCamera1ParameterKEY("non-zsl-manual-mode");
            settingsManager.get(SettingKeys.NON_ZSL_MANUAL_MODE).setValues(new String[]{GlassCamApplication.getStringFromRessources(R.string.on_), GlassCamApplication.getStringFromRessources(R.string.off_)});
        }
    }
}
