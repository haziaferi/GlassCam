package glasscam.cam.apis.basecamera.parameters.modes;

import com.hazia.glasscam.R;

import glasscam.GlassCamApplication;
import glasscam.cam.apis.basecamera.CameraWrapperInterface;
import glasscam.cam.apis.basecamera.parameters.AbstractParameter;
import glasscam.settings.SettingKeys;

public class VideoAudioSourceMode extends AbstractParameter {


    public VideoAudioSourceMode(CameraWrapperInterface cameraUiWrapper, SettingKeys.Key settingMode) {
        super(cameraUiWrapper, settingMode);
    }

    @Override
    protected void setValue(String valueToSet, boolean setToCamera) {
        super.setValue(valueToSet, setToCamera);
        if (cameraUiWrapper.getModuleHandler().getCurrentModuleName() == GlassCamApplication.getStringFromRessources(R.string.module_video) && setToCamera)
        {
            cameraUiWrapper.getModuleHandler().getCurrentModule().DestroyModule();
            cameraUiWrapper.getModuleHandler().getCurrentModule().InitModule();
        }
    }
}
