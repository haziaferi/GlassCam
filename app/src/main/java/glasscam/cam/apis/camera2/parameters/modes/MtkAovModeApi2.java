package glasscam.cam.apis.camera2.parameters.modes;

import camera2_hidden_keys.mtk.CaptureRequestMtk;
import glasscam.cam.apis.basecamera.CameraThreadHandler;
import glasscam.cam.apis.camera2.Camera2;
import glasscam.settings.SettingKeys;

public class MtkAovModeApi2 extends BaseModeApi2 {
    public MtkAovModeApi2(Camera2 cameraUiWrapper, SettingKeys.Key settingMode) {
        super(cameraUiWrapper, settingMode);
    }

    @Override
    protected void setValue(String valueToSet, boolean setToCamera) {
        if(setToCamera)
            cameraUiWrapper.stopPreview();
        settingMode.set(valueToSet);
        if(valueToSet.equals("on"))
            cameraUiWrapper.captureSessionHandler.SetParameterRepeating(CaptureRequestMtk.com_mediatek_aovservicefeature_aovMode, new int[]{1}, setToCamera);
        else
            cameraUiWrapper.captureSessionHandler.SetParameterRepeating(CaptureRequestMtk.com_mediatek_aovservicefeature_aovMode, new int[]{0}, setToCamera);
        if(setToCamera)
            cameraUiWrapper.startPreview();
        fireStringValueChanged(valueToSet);
    }
}
