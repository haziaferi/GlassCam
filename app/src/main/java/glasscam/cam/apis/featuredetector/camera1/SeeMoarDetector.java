package glasscam.cam.apis.featuredetector.camera1;

import android.hardware.Camera;

import com.hazia.glasscam.R;

import glasscam.settings.Frameworks;
import glasscam.settings.SettingKeys;

public class SeeMoarDetector extends BaseParameter1Detector{
    @Override
    protected void findAndFillSettings(Camera.Parameters cameraCharacteristics) {
        detectSEEMoar(cameraCharacteristics);
    }

    private void detectSEEMoar(Camera.Parameters parameters)
    {
        if (settingsManager.getFrameWork() == Frameworks.MTK)
        {
            settingsManager.get(SettingKeys.SEE_MORE).setIsSupported(false);
            return;
        }
        else
        {
            detectMode(parameters, R.string.seemore,R.string.seemore_mode, settingsManager.get(SettingKeys.SEE_MORE));
        }

    }
}
