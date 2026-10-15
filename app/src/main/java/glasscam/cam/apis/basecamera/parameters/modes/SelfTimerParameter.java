package glasscam.cam.apis.basecamera.parameters.modes;

import glasscam.cam.apis.basecamera.CameraWrapperInterface;
import glasscam.cam.apis.basecamera.parameters.AbstractParameter;
import glasscam.settings.SettingKeys;

public class SelfTimerParameter extends AbstractParameter {
    public SelfTimerParameter(SettingKeys.Key key) {
        super(key);
    }

    public SelfTimerParameter(CameraWrapperInterface cameraUiWrapper, SettingKeys.Key settingMode) {
        super(cameraUiWrapper, settingMode);
    }

    @Override
    public String getStringValue() {
        return settingMode.get();
    }

    @Override
    public String[] getStringValues() {
        return settingMode.getValues();
    }

    @Override
    protected void setValue(String valueToSet, boolean setToCamera) {
        settingMode.set(valueToSet);
        fireStringValueChanged(valueToSet);
    }

    @Override
    public ViewState getViewState() {
        return ViewState.Visible;
    }
}
