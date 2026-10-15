package glasscam.cam.apis.basecamera.parameters.modes;

import com.hazia.glasscam.R;

import glasscam.GlassCamApplication;
import glasscam.cam.apis.basecamera.CameraWrapperInterface;
import glasscam.settings.SettingKeys;

/**
 * Created by KillerInk on 23.01.2018.
 */

public class ClippingMode extends FocusPeakMode {

    public ClippingMode(CameraWrapperInterface cameraUiWrapper, SettingKeys.Key settingMode) {
        super(cameraUiWrapper, settingMode);
    }

    @Override
    public void setStringValue(String valueToSet, boolean setToCamera)
    {
        currentString = valueToSet;
        boolean toset = valueToSet.equals(GlassCamApplication.getStringFromRessources(R.string.on_));
        previewController.setClipping(toset);
        settingsManager.get(settingMode).set(toset);
        fireStringValueChanged(valueToSet);
    }

    @Override
    public void set(boolean bool) {
        previewController.setClipping(bool);
        settingsManager.get(settingMode).set(bool);
        fireStringValueChanged(getStringValue());
    }
}
