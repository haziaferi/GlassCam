package glasscam.cam.apis.basecamera.parameters.modes;

import glasscam.cam.apis.basecamera.CameraThreadHandler;
import glasscam.cam.apis.basecamera.CameraWrapperInterface;
import glasscam.cam.apis.basecamera.parameters.AbstractParameter;
import glasscam.cam.apis.camera1.Camera1;
import glasscam.cam.apis.camera1.parameters.ParametersHandler;
import glasscam.cam.apis.camera2.Camera2;
import glasscam.settings.SettingKeys;

public class OrientationHackParameter extends AbstractParameter {
    public OrientationHackParameter(SettingKeys.Key key) {
        super(key);
    }

    public OrientationHackParameter(CameraWrapperInterface cameraUiWrapper, SettingKeys.Key settingMode) {
        super(cameraUiWrapper, settingMode);
    }

    @Override
    protected void setValue(String valueToSet, boolean setToCamera) {
        super.setValue(valueToSet,setToCamera);
        settingsManager.get(SettingKeys.ORIENTATION_HACK).set(valueToSet);
        if (cameraUiWrapper instanceof Camera1) {
            ((ParametersHandler) cameraUiWrapper.getParameterHandler()).SetCameraRotation();
            cameraUiWrapper.getParameterHandler().SetPictureOrientation(0);
        }
        else if(cameraUiWrapper instanceof Camera2)
        {
            CameraThreadHandler.restartCameraAsync();
        }
    }

    @Override
    public String getStringValue() {
        return settingsManager.get(SettingKeys.ORIENTATION_HACK).get();
    }
}
