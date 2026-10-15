package glasscam.cam.apis.featuredetector.camera1;

import android.hardware.Camera;

import com.hazia.glasscam.R;

import glasscam.settings.Frameworks;
import glasscam.settings.SettingKeys;

public class RdiDetector extends BaseParameter1Detector{
    @Override
    protected void findAndFillSettings(Camera.Parameters cameraCharacteristics) {
        detectRDI(cameraCharacteristics);
    }

    private void detectRDI(Camera.Parameters parameters)
    {
        if (settingsManager.getFrameWork() == Frameworks.MTK)
        {
            settingsManager.get(SettingKeys.RDI).setIsSupported(false);
            return;
        }
        else
        {
            detectMode(parameters, R.string.rdi,R.string.rdi_mode, settingsManager.get(SettingKeys.RDI));
        }

    }
}
