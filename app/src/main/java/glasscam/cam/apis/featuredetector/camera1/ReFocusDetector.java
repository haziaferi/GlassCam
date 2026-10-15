package glasscam.cam.apis.featuredetector.camera1;

import android.hardware.Camera;

import com.hazia.glasscam.R;

import glasscam.settings.Frameworks;
import glasscam.settings.SettingKeys;

public class ReFocusDetector extends BaseParameter1Detector{
    @Override
    protected void findAndFillSettings(Camera.Parameters cameraCharacteristics) {
        detectRefocus(cameraCharacteristics);
    }

    private void detectRefocus(Camera.Parameters parameters)
    {
        if (settingsManager.getFrameWork() == Frameworks.MTK)
        {
            settingsManager.get(SettingKeys.RE_FOCUS).setIsSupported(false);
            return;
        }
        else
        {
            detectMode(parameters, R.string.refocus,R.string.refocus_mode, settingsManager.get(SettingKeys.RE_FOCUS));
        }

    }
}
