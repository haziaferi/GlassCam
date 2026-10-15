package glasscam.cam.apis.camera1.parameters.ae;

import android.hardware.Camera;

import com.hazia.glasscam.R;

import glasscam.GlassCamApplication;
import glasscam.cam.apis.basecamera.CameraWrapperInterface;
import glasscam.cam.apis.basecamera.parameters.AbstractParameter;
import glasscam.cam.apis.basecamera.parameters.ae.AeManager;
import glasscam.cam.apis.basecamera.parameters.ae.AeStates;
import glasscam.cam.apis.camera1.parameters.ParametersHandler;
import glasscam.settings.SettingKeys;
import glasscam.utils.GlassCamPool;
import glasscam.utils.Log;

/**
 * Created by KillerInk on 29.12.2017.
 */

public class AeManagerLgCamera1 extends AeManager
{
    protected final Camera.Parameters parameters;
    private boolean readMetaData = false;

    public AeManagerLgCamera1(CameraWrapperInterface cameraWrapperInterface,Camera.Parameters parameters) {
        super(cameraWrapperInterface);
        this.parameters = parameters;
    }

    @Override
    public void setExposureTime(int valueToSet, boolean setToCamera) {
        parameters.set(GlassCamApplication.getStringFromRessources(R.string.lg_shutterspeed), manualExposureTime.getStringValues()[valueToSet]);
        ((ParametersHandler) cameraWrapperInterface.getParameterHandler()).SetParametersToCamera(parameters);
    }

    @Override
    public void setIso(int valueToSet, boolean setToCamera) {
        if (valueToSet == 0)
        {
            setAeMode(AeStates.auto);
        }
        else
        {
            parameters.set(GlassCamApplication.getStringFromRessources(R.string.lg_iso), manualIso.getStringValues()[valueToSet]);
            ((ParametersHandler) cameraWrapperInterface.getParameterHandler()).SetParametersToCamera(parameters);
            setAeMode(AeStates.manual);
        }

    }

    @Override
    public void setExposureCompensation(int valueToSet, boolean setToCamera) {

    }

    @Override
    public void setAeMode(AeStates aeState) {
        if (activeAeState == aeState)
            return;
        activeAeState = aeState;
        switch (aeState)
        {
            case auto:
                setToAuto();
                break;
            case manual:
                setToManual();
                break;
        }
    }

    private void setToAuto()
    {
        //back in auto mode
        //set exposure ui item to enable
        parameters.set(GlassCamApplication.getStringFromRessources(R.string.lg_manual_mode_reset), "1");
        parameters.set(GlassCamApplication.getStringFromRessources(R.string.lg_iso), GlassCamApplication.getStringFromRessources(R.string.auto_));
        parameters.set(GlassCamApplication.getStringFromRessources(R.string.lg_shutterspeed), "0");
        ((ParametersHandler)cameraWrapperInterface.getParameterHandler()).SetParametersToCamera(parameters);

        String t = cameraWrapperInterface.getParameterHandler().get(SettingKeys.ISO_MODE).getStringValue();
        if (!t.equals(GlassCamApplication.getStringFromRessources(R.string.iso100_)))
            cameraWrapperInterface.getParameterHandler().get(SettingKeys.ISO_MODE).setStringValue(GlassCamApplication.getStringFromRessources(R.string.iso100_), true);
        else
            cameraWrapperInterface.getParameterHandler().get(SettingKeys.ISO_MODE).setStringValue(GlassCamApplication.getStringFromRessources(R.string.auto_), true);
        cameraWrapperInterface.getParameterHandler().get(SettingKeys.ISO_MODE).setStringValue(t, true);
        /*exposureCompensation.fireIsSupportedChanged(true);
        exposureCompensation.fireIsReadOnlyChanged(true);*/
        manualIso.setViewState(AbstractParameter.ViewState.Enabled);
        manualExposureTime.setViewState(AbstractParameter.ViewState.Disabled);
        startReadingMeta();
    }


    private void setToManual()
    {
        readMetaData = false;
        //hide manualexposuretime ui item
        /*exposureCompensation.fireIsSupportedChanged(false);*/
        //turn flash off when ae is off. else on some devices it applys only manual stuff only for a few frames
        parameters.set(GlassCamApplication.getStringFromRessources(R.string.lg_manual_mode_reset), "0");
        ((ParametersHandler)cameraWrapperInterface.getParameterHandler()).SetParametersToCamera(parameters);
        manualExposureTime.setValue(manualExposureTime.getIntValue(),true);
        //enable manualiso item in ui
        manualIso.setViewState(AbstractParameter.ViewState.Enabled);
        //enable manual exposuretime in ui
        manualExposureTime.setValue(manualExposureTime.getIntValue(),true);
        manualExposureTime.setViewState(AbstractParameter.ViewState.Enabled);
        manualExposureTime.fireStringValueChanged(manualExposureTime.getStringValue());
    }

    /**
     * updates the manual shutter/iso button in ui with the current iso and exposuretime values
     * when ae is in automode
     */
    private void startReadingMeta()
    {
        readMetaData = true;
        GlassCamPool.Execute(() -> {
            while (readMetaData)
            {
                try {
                    cameraWrapperInterface.getParameterHandler().get(SettingKeys.M_EXPOSURE_TIME).fireStringValueChanged("1/"+(int) cameraWrapperInterface.getParameterHandler().getCurrentExposuretime());
                    cameraWrapperInterface.getParameterHandler().get(SettingKeys.M_MANUAL_ISO).fireStringValueChanged(cameraWrapperInterface.getParameterHandler().getCurrentIso()+"");
                }
                catch (RuntimeException ex)
                {
                    readMetaData = false;
                    return;
                }
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    Log.WriteEx(e);
                }
            }
        });
    }
}
