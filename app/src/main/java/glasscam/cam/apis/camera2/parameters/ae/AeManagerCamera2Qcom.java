package glasscam.cam.apis.camera2.parameters.ae;

import android.hardware.camera2.CaptureRequest;
import android.os.Build;

import androidx.annotation.RequiresApi;

import com.hazia.glasscam.R;

import camera2_hidden_keys.qcom.CaptureRequestQcom;
import glasscam.GlassCamApplication;
import glasscam.cam.apis.basecamera.parameters.AbstractParameter;
import glasscam.cam.apis.basecamera.parameters.ParameterInterface;
import glasscam.cam.apis.basecamera.parameters.ae.AeStates;
import glasscam.cam.apis.basecamera.parameters.manual.AbstractManualShutter;
import glasscam.cam.apis.camera2.Camera2;
import glasscam.cam.apis.camera2.parameters.modes.BaseModeApi2;
import glasscam.settings.SettingKeys;
import glasscam.utils.Log;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class AeManagerCamera2Qcom extends AeManagerCamera2 {

    private boolean expotimeIsActive;
    private boolean isoIsActive;
    private final BaseModeApi2 aemode;
    private long exposuretime = 0L;
    private int isoVal = 0;

    public AeManagerCamera2Qcom(Camera2 cameraWrapperInterface) {
        super(cameraWrapperInterface);
        aemode = new BaseModeApi2(cameraWrapperInterface, SettingKeys.EXPOSURE_MODE, CaptureRequest.CONTROL_AE_MODE);
    }

    @Override
    public boolean isExposureTimeWriteable() {
        return true;
    }

    @Override
    public boolean isIsoWriteable() {
        return true;
    }

    @Override
    public AbstractParameter getAeMode() {
        return aemode;
    }

    @Override
    public void setAeMode(AeStates aeState) {
        activeAeState = aeState;
        switch (aeState)
        {
            case manual:
                exposureCompensation.setViewState(AbstractParameter.ViewState.Disabled);
                cameraWrapperInterface.captureSessionHandler.SetParameterRepeating(CaptureRequestQcom.org_codeaurora_qcamera3_iso_exp_priority_select_priority,CaptureRequestQcom.ExposureTimePriority_OFF,true);
                cameraWrapperInterface.captureSessionHandler.SetPreviewParameterRepeating(CaptureRequestQcom.org_codeaurora_qcamera3_iso_exp_priority_use_iso_exp_priority, CaptureRequestQcom.IsoPriority_OFF, true);
                cameraWrapperInterface.captureSessionHandler.SetParameterRepeating(CaptureRequest.CONTROL_AE_MODE,CaptureRequest.CONTROL_AE_MODE_OFF,true);
                // no flash under a manual exposure, as AeManagerCamera2.setToManual (a stored auto flash left FLASH_MODE_SINGLE)
                cameraWrapperInterface.captureSessionHandler.SetParameterRepeating(CaptureRequest.FLASH_MODE,CaptureRequest.FLASH_MODE_OFF,true);
                cameraWrapperInterface.captureSessionHandler.SetParameterRepeating(CaptureRequest.SENSOR_SENSITIVITY,isoVal,true);
                if (exposuretime > MAX_PREVIEW_EXPOSURETIME && !settingsManager.GetCurrentModule().equals(GlassCamApplication.getStringFromRessources(R.string.module_video))) {
                    cameraWrapperInterface.captureSessionHandler.SetParameterRepeating(CaptureRequest.SENSOR_EXPOSURE_TIME,MAX_PREVIEW_EXPOSURETIME,true);
                    cameraWrapperInterface.captureSessionHandler.SetCaptureParameter(CaptureRequest.SENSOR_EXPOSURE_TIME,exposuretime);
                    applyPostRawSensitivityBoost(exposuretime);
                }
                else
                    cameraWrapperInterface.captureSessionHandler.SetParameterRepeating(CaptureRequest.SENSOR_EXPOSURE_TIME,exposuretime,true);
                break;
            case iso_priority:
                cameraWrapperInterface.captureSessionHandler.SetParameterRepeating(CaptureRequestQcom.org_codeaurora_qcamera3_iso_exp_priority_use_iso_value,isoVal ,true);
                cameraWrapperInterface.captureSessionHandler.SetParameterRepeating(CaptureRequestQcom.org_codeaurora_qcamera3_iso_exp_priority_select_priority, CaptureRequestQcom.ExposureTimePriority_OFF,true);
                cameraWrapperInterface.captureSessionHandler.SetParameterRepeating(CaptureRequestQcom.org_codeaurora_qcamera3_iso_exp_priority_use_iso_exp_priority, CaptureRequestQcom.IsoPriority_ON,true);
                break;
            case shutter_priority:
                applyExpotime();
                cameraWrapperInterface.captureSessionHandler.SetParameterRepeating(CaptureRequestQcom.org_codeaurora_qcamera3_iso_exp_priority_select_priority, CaptureRequestQcom.ExposureTimePriority_ON,true);
                break;
            default:
                cameraWrapperInterface.captureSessionHandler.SetParameterRepeating(CaptureRequestQcom.org_codeaurora_qcamera3_iso_exp_priority_select_priority, CaptureRequestQcom.ExposureTimePriority_OFF,true);
                cameraWrapperInterface.captureSessionHandler.SetParameterRepeating(CaptureRequestQcom.org_codeaurora_qcamera3_iso_exp_priority_use_iso_exp_priority, CaptureRequestQcom.IsoPriority_OFF,true);
                exposureCompensation.setViewState(AbstractParameter.ViewState.Enabled);
                cameraWrapperInterface.captureSessionHandler.SetParameterRepeating(CaptureRequest.CONTROL_AE_MODE,CaptureRequest.CONTROL_AE_MODE_ON,true);
                // the flash state is an AE mode too: put the user's choice back, as AeManagerCamera2.setToAuto does
                ParameterInterface flash = cameraWrapperInterface.getParameterHandler().get(SettingKeys.FLASH_MODE);
                if (flash != null && settingsManager.get(SettingKeys.FLASH_MODE).isSupported())
                    flash.setStringValue(flash.getStringValue(), true);
                break;
        }
    }

    private void applyExpotime() {
        if (exposuretime > MAX_PREVIEW_EXPOSURETIME && !settingsManager.GetCurrentModule().equals(GlassCamApplication.getStringFromRessources(R.string.module_video))) {
            Log.d(manualExposureTime.TAG, "ExposureTime Exceed 100000000 for preview, set it to 100000000");
            cameraWrapperInterface.captureSessionHandler.SetPreviewParameterRepeating(CaptureRequestQcom.org_codeaurora_qcamera3_iso_exp_priority_use_iso_exp_priority, MAX_PREVIEW_EXPOSURETIME, true);
            cameraWrapperInterface.captureSessionHandler.SetCaptureParameter(CaptureRequestQcom.org_codeaurora_qcamera3_iso_exp_priority_use_iso_exp_priority, exposuretime);
        } else
            cameraWrapperInterface.captureSessionHandler.SetParameterRepeating(CaptureRequestQcom.org_codeaurora_qcamera3_iso_exp_priority_use_iso_exp_priority, exposuretime, true);
    }



    @Override
    public void setExposureTime(int valueToSet, boolean setToCamera) {
        if (valueToSet > 0) {
            exposuretime = AbstractManualShutter.getMilliSecondStringFromShutterString(manualExposureTime.getStringValues()[valueToSet])* 1000;
            expotimeIsActive = true;
        }
        else
        {
            exposuretime = 0;
            expotimeIsActive = false;
        }
        applyAeMode();
        manualExposureTime.fireIntValueChanged(valueToSet);
    }

    @Override
    public void setIso(int valueToSet, boolean setToCamera) {
        if (valueToSet == 0)
        {
            isoVal = 0;
            isoIsActive = false;
        }
        else
        {
            isoVal = Integer.parseInt(manualIso.getStringValues()[valueToSet]);
            isoIsActive =true;
        }
        applyAeMode();
        manualIso.fireIntValueChanged(valueToSet);
    }


    private void applyAeMode()
    {
        if (expotimeIsActive && isoIsActive)
            setAeMode(AeStates.manual);
        else if (!expotimeIsActive && isoIsActive)
            setAeMode(AeStates.iso_priority);
        else if (expotimeIsActive && !isoIsActive)
            setAeMode(AeStates.shutter_priority);
        else if (!expotimeIsActive && !isoIsActive)
            setAeMode(AeStates.auto);
    }
}
