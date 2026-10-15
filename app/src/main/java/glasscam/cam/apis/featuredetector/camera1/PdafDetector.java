package glasscam.cam.apis.featuredetector.camera1;

import android.hardware.Camera;

import com.hazia.glasscam.R;

import glasscam.settings.Frameworks;
import glasscam.settings.SettingKeys;

public class PdafDetector extends BaseParameter1Detector{
    @Override
    protected void findAndFillSettings(Camera.Parameters cameraCharacteristics) {
        detectPDAF(cameraCharacteristics);
    }

    private void detectPDAF(Camera.Parameters parameters)
    {
        if (settingsManager.getFrameWork() == Frameworks.MTK)
        {
            settingsManager.get(SettingKeys.PDAF).setIsSupported(false);
            return;
        }
        else
        {
            detectMode(parameters, R.string.pdaf,R.string.pdaf_mode, settingsManager.get(SettingKeys.PDAF));
        }

    }
}
