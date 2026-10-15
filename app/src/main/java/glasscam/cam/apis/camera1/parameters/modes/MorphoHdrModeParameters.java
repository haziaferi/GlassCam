package glasscam.cam.apis.camera1.parameters.modes;

import android.hardware.Camera;

import com.hazia.glasscam.R;

import java.util.ArrayList;
import java.util.List;

import glasscam.GlassCamApplication;
import glasscam.cam.apis.basecamera.CameraWrapperInterface;
import glasscam.cam.apis.camera1.parameters.ParametersHandler;
import glasscam.settings.SettingKeys;
import glasscam.settings.mode.SettingMode;
import glasscam.utils.Log;

/**
 * Created by troop on 24.02.2017.
 */

public class MorphoHdrModeParameters extends BaseModeParameter {

    final String TAG = MorphoHdrModeParameters.class.getSimpleName();
    private boolean visible = true;
    private boolean supportauto;
    private boolean supporton;
    private String state = "";
    private final String format = "";
    private String curmodule = "";

    public MorphoHdrModeParameters(Camera.Parameters parameters, CameraWrapperInterface cameraUiWrapper,SettingKeys.Key settingMode) {
        super(parameters, cameraUiWrapper, settingMode);

        //cameraUiWrapper.getParameterHandler().get(SettingKeys.PictureFormat).addEventListner(this);
    }


    @Override
    public void setValue(String valueToSet, boolean setToCam) {
        if (valueToSet.equals(GlassCamApplication.getStringFromRessources(R.string.on_))) {
            parameters.set(GlassCamApplication.getStringFromRessources(R.string.morpho_hht), GlassCamApplication.getStringFromRessources(R.string.false_));
            cameraUiWrapper.getParameterHandler().get(SettingKeys.NIGHT_MODE).fireStringValueChanged(GlassCamApplication.getStringFromRessources(R.string.off_));
            parameters.set("capture-burst-exposures","-10,0,10");
            cameraUiWrapper.getParameterHandler().get(SettingKeys.AE_BRACKET).setStringValue(GlassCamApplication.getStringFromRessources(R.string.ae_bracket_hdr_values_aebracket), true);
            parameters.set(GlassCamApplication.getStringFromRessources(R.string.morpho_hdr), GlassCamApplication.getStringFromRessources(R.string.true_));
        } else {
            cameraUiWrapper.getParameterHandler().get(SettingKeys.AE_BRACKET).setStringValue(GlassCamApplication.getStringFromRessources(R.string.ae_bracket_hdr_values_off), true);
            parameters.set(GlassCamApplication.getStringFromRessources(R.string.morpho_hdr), GlassCamApplication.getStringFromRessources(R.string.false_));
        }
        if (setToCam)
            ((ParametersHandler) cameraUiWrapper.getParameterHandler()).SetParametersToCamera(parameters);
        ((SettingMode)settingsManager.get(key)).set(valueToSet);
        fireStringValueChanged(valueToSet);
    }

    @Override
    public String getStringValue() {

        if(cameraUiWrapper == null) {
            Log.d(TAG, "cameraUiWrapper null");
            setViewState(ViewState.Hidden);
            return GlassCamApplication.getStringFromRessources(R.string.off_);
        }
        if (parameters == null) {
            Log.d(TAG, "Parameters are null");
            setViewState(ViewState.Hidden);
            return GlassCamApplication.getStringFromRessources(R.string.off_);
        }
        if (parameters.get(GlassCamApplication.getStringFromRessources(R.string.morpho_hdr)) == null) {
            Log.d(TAG, "MorphoHdr is null");
            setViewState(ViewState.Hidden);
            return GlassCamApplication.getStringFromRessources(R.string.off_);
        }
        if (parameters.get(GlassCamApplication.getStringFromRessources(R.string.ae_bracket_hdr)) == null){
            Log.d(TAG, "Ae bracket is null");
            setViewState(ViewState.Hidden);
            return GlassCamApplication.getStringFromRessources(R.string.off_);
        }


        if (parameters.get(GlassCamApplication.getStringFromRessources(R.string.morpho_hdr)).equals(GlassCamApplication.getStringFromRessources(R.string.true_))
                && parameters.get(GlassCamApplication.getStringFromRessources(R.string.ae_bracket_hdr)).equals(GlassCamApplication.getStringFromRessources(R.string.ae_bracket_hdr_values_aebracket)))
            return GlassCamApplication.getStringFromRessources(R.string.on_);
        else
            return GlassCamApplication.getStringFromRessources(R.string.off_);
    }

    @Override
    public String[] getStringValues() {
        List<String> hdrVals =  new ArrayList<>();
        hdrVals.add(GlassCamApplication.getStringFromRessources(R.string.off_));
        hdrVals.add(GlassCamApplication.getStringFromRessources(R.string.on_));
        return hdrVals.toArray(new String[hdrVals.size()]);
    }

    @Override
    public void onModuleChanged(String module)
    {
        curmodule = module;
        if (curmodule.equals(GlassCamApplication.getStringFromRessources(R.string.module_video))|| curmodule.equals(GlassCamApplication.getStringFromRessources(R.string.module_video)))
        {
            Hide();
            setStringValue(GlassCamApplication.getStringFromRessources(R.string.off_),true);
        }
        else
        {
            if (format.contains(GlassCamApplication.getStringFromRessources(R.string.jpeg_))) {
                Show();
                setViewState(ViewState.Visible);
            }
            else
            {
                Hide();
                setStringValue(GlassCamApplication.getStringFromRessources(R.string.off_),true);
            }
        }
    }

    /*@Override
    public void onViewStateChanged(ViewState value) {

    }

    @Override
    public void onIntValueChanged(int current) {

    }

    @Override
    public void onValuesChanged(String[] values) {

    }

    @Override
    public void onStringValueChanged(String val) {
        format = val;
        if (val.contains(GlassCamApplication.getStringFromRessources(R.string.jpeg_))&&!visible &&!curmodule.equals(GlassCamApplication.getStringFromRessources(R.string.module_hdr)))
            Show();

        else if (!val.contains(GlassCamApplication.getStringFromRessources(R.string.jpeg_))&& visible) {
            Hide();
        }
    }*/

    private void Hide()
    {
        state = getStringValue();
        visible = false;
        setStringValue(GlassCamApplication.getStringFromRessources(R.string.off_),true);
        fireStringValueChanged(GlassCamApplication.getStringFromRessources(R.string.off_));
        setViewState(ViewState.Hidden);
    }
    private void Show()
    {
        visible = true;
        setStringValue(state,true);
        fireStringValueChanged(state);
        setViewState(ViewState.Visible);
    }
}
