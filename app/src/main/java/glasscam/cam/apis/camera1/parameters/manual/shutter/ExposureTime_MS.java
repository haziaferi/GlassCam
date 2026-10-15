package glasscam.cam.apis.camera1.parameters.manual.shutter;

import android.hardware.Camera;

import com.hazia.glasscam.R;

import glasscam.GlassCamApplication;
import glasscam.cam.apis.basecamera.CameraWrapperInterface;
import glasscam.cam.apis.basecamera.parameters.AbstractParameter;
import glasscam.cam.apis.camera1.parameters.ParametersHandler;
import glasscam.settings.SettingKeys;
import glasscam.utils.Log;

/**
 * Created by troop on 29.01.2017.
 */

public class ExposureTime_MS extends AbstractParameter {
    private final String TAG = ExposureTime_MS.class.getSimpleName();
    private final Camera.Parameters parameters;
    public ExposureTime_MS(CameraWrapperInterface cameraUiWrapper, Camera.Parameters parameters,SettingKeys.Key settingMode) {
        super(cameraUiWrapper,settingMode);
        this.parameters = parameters;
        setViewState(ViewState.Visible);
    }

    @Override
    public void setValue(int valueToset, boolean setToCamera)
    {
        super.setValue(valueToset,setToCamera);
        currentInt = valueToset;
        String shutterstring = stringvalues[currentInt];
        if(!shutterstring.equals(GlassCamApplication.getStringFromRessources(R.string.auto_)))
        {
            if (stringvalues[currentInt].contains("/")) {
                String[] split = stringvalues[currentInt].split("/");
                Double a = Double.parseDouble(split[0]) / Double.parseDouble(split[1]);
                shutterstring = "" + a;
            }
            shutterstring = Double.parseDouble(shutterstring) * 1000 +"";
            Log.d(TAG, "set exposure time to " + shutterstring);
            parameters.set(settingsManager.get(SettingKeys.M_EXPOSURE_TIME).getCamera1ParameterKEY(), shutterstring);
        }
        else
        {
            parameters.set(settingsManager.get(SettingKeys.M_EXPOSURE_TIME).getCamera1ParameterKEY(), "0");
            Log.d(TAG, "set exposure time to auto");
        }
        ((ParametersHandler) cameraUiWrapper.getParameterHandler()).SetParametersToCamera(parameters);
    }
}
