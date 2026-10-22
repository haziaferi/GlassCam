package glasscam.cam.apis.camera2.parameters.modes;

import android.hardware.camera2.CaptureRequest;
import android.os.Build;

import androidx.annotation.RequiresApi;

import com.hazia.glasscam.R;

import glasscam.GlassCamApplication;
import glasscam.cam.apis.basecamera.parameters.ae.AeManager;
import glasscam.cam.apis.basecamera.parameters.ae.AeStates;
import glasscam.cam.apis.camera2.Camera2;
import glasscam.cam.apis.camera2.parameters.ParameterHandlerApi2;
import glasscam.settings.SettingKeys;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class FlashMode extends BaseModeApi2 {
    public FlashMode(Camera2 cameraUiWrapper, SettingKeys.Key settingMode) {
        super(cameraUiWrapper, settingMode);
    }

/*
    @Override
    public String[] getStringValues() {
        return new String[]{"Auto","Always","Off","Torch"};
    }*/

    /** Full manual exposure: AE must stay OFF, and every Camera2 flash state is an AE mode (ON_*). */
    private boolean isManualExposure() {
        ParameterHandlerApi2 handler = cameraUiWrapper.getParameterHandler();
        AeManager ae = handler == null ? null : handler.getAeManagerCamera2();
        return ae != null && ae.getActiveAeState() == AeStates.manual;
    }

    @Override
    protected void setValue(String valueToSet, boolean setToCamera) {

        if (isManualExposure()) {
            // Restoring "off" after the manual ISO and shutter switched AE back on, so photos were taken
            // on auto exposure (6T bench, 2026-10-22). The choice is kept; leaving manual re-applies it.
            captureSessionHandler.SetParameterRepeating(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_OFF,true);
        }
        else if (valueToSet.equals(GlassCamApplication.getStringFromRessources(R.string.font_flash_auto)))
            setToAuto();
        else if (valueToSet.equals(GlassCamApplication.getStringFromRessources(R.string.font_flash_always)))
            setToAlwaysFlash();
        else if (valueToSet.equals(GlassCamApplication.getStringFromRessources(R.string.font_flash_off)))
            setToOff();
        else if (valueToSet.equals(GlassCamApplication.getStringFromRessources(R.string.font_flash_torch)))
            setToTorch();
        else if (valueToSet.equals(GlassCamApplication.getStringFromRessources(R.string.font_flash_redeye)))
            setToRedEye();
        currentString = valueToSet;
        settingMode.set(valueToSet);
        fireStringValueChanged(valueToSet);
    }

    private void setToAuto()
    {
        captureSessionHandler.SetParameterRepeating(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_OFF,true);
        captureSessionHandler.SetParameterRepeating(CaptureRequest.CONTROL_AE_MODE,CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH,false);
        captureSessionHandler.SetParameterRepeating(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_SINGLE,true);
    }

    private void setToAlwaysFlash()
    {
        captureSessionHandler.SetParameterRepeating(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_OFF,true);
        captureSessionHandler.SetParameterRepeating(CaptureRequest.CONTROL_AE_MODE,CaptureRequest.CONTROL_AE_MODE_ON_ALWAYS_FLASH,false);
        captureSessionHandler.SetParameterRepeating(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_SINGLE,true);

    }

    private void setToRedEye()
    {
        captureSessionHandler.SetParameterRepeating(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_OFF,true);
        captureSessionHandler.SetParameterRepeating(CaptureRequest.CONTROL_AE_MODE,CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH_REDEYE,false);
        captureSessionHandler.SetParameterRepeating(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_SINGLE,true);
    }

    private void setToOff()
    {
        captureSessionHandler.SetParameterRepeating(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_OFF,false);
        captureSessionHandler.SetParameterRepeating(CaptureRequest.CONTROL_AE_MODE,CaptureRequest.CONTROL_AE_MODE_ON,true);
    }

    private void setToTorch()
    {
        captureSessionHandler.SetParameterRepeating(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_OFF,true);
        captureSessionHandler.SetParameterRepeating(CaptureRequest.CONTROL_AE_MODE,CaptureRequest.CONTROL_AE_MODE_ON,false);
        captureSessionHandler.SetParameterRepeating(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_TORCH,true);

    }
}
