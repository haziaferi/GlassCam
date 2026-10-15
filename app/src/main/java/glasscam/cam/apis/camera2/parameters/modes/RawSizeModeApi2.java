package glasscam.cam.apis.camera2.parameters.modes;

import android.hardware.camera2.CaptureRequest;

import glasscam.cam.apis.basecamera.CameraThreadHandler;
import glasscam.cam.apis.camera2.Camera2;
import glasscam.settings.SettingKeys;

public class RawSizeModeApi2 extends BaseModeApi2 {
    public RawSizeModeApi2(Camera2 cameraUiWrapper, SettingKeys.Key settingMode) {
        super(cameraUiWrapper, settingMode);
    }

    public RawSizeModeApi2(Camera2 cameraUiWrapper, SettingKeys.Key key, CaptureRequest.Key<Integer> parameterKey) {
        super(cameraUiWrapper, key, parameterKey);
    }

    @Override
    public void setStringValue(String valueToSet, boolean setToCamera)
    {
        fireStringValueChanged(valueToSet);
        settingsManager.get(SettingKeys.RAW_SIZE).set(valueToSet);
        if (setToCamera)
        {
            CameraThreadHandler.restartPreviewAsync();
        }
    }

    @Override
    public String getStringValue()
    {
        return  settingsManager.get(SettingKeys.RAW_SIZE).get();
    }

    @Override
    public String[] getStringValues()
    {
        return settingsManager.get(SettingKeys.RAW_SIZE).getValues();
    }
}
