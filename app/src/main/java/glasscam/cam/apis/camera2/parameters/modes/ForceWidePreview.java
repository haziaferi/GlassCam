package glasscam.cam.apis.camera2.parameters.modes;

import glasscam.cam.apis.basecamera.CameraThreadHandler;
import glasscam.cam.apis.basecamera.CameraWrapperInterface;
import glasscam.cam.apis.basecamera.parameters.AbstractParameter;
import glasscam.cam.apis.camera2.Camera2;
import glasscam.settings.SettingKeys;
import glasscam.settings.mode.BooleanSettingModeInterface;

public class ForceWidePreview extends AbstractParameter implements BooleanSettingModeInterface {
    BooleanSettingModeInterface booleanSettingModeInterface;

    public ForceWidePreview(CameraWrapperInterface cameraUiWrapper)
    {
        super(cameraUiWrapper, SettingKeys.FORCE_WIDE_PREVIEW);
        this.booleanSettingModeInterface = settingsManager.get(SettingKeys.FORCE_WIDE_PREVIEW);
    }

    @Override
    public boolean get() {
        if (booleanSettingModeInterface != null)
            return booleanSettingModeInterface.get();
        return false;
    }

    @Override
    public void set(boolean bool) {
        booleanSettingModeInterface.set(bool);
        CameraThreadHandler.restartPreviewAsync();
    }
}
