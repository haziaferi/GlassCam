package glasscam.cam.apis.camera1.parameters.modes;

import android.hardware.Camera;
import android.text.TextUtils;

import com.hazia.glasscam.R;

import java.util.ArrayList;
import java.util.Arrays;
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

public class AutoHdrMode extends BaseModeParameter {

    final String TAG = AutoHdrMode.class.getSimpleName();
    private boolean visible = true;
    private String state = "";
    private final String format = "";
    private String curmodule = "";
    public AutoHdrMode(Camera.Parameters parameters, CameraWrapperInterface cameraUiWrapper, SettingKeys.Key  settingMode) {
        super(parameters, cameraUiWrapper, settingMode);

        if (parameters.get(GlassCamApplication.getStringFromRessources(R.string.auto_hdr_supported))!=null)
            setViewState(ViewState.Hidden);
        String autohdr = parameters.get(GlassCamApplication.getStringFromRessources(R.string.auto_hdr_supported));
        if (autohdr != null && !TextUtils.isEmpty(autohdr) && autohdr.equals(GlassCamApplication.getStringFromRessources(R.string.true_))
                && parameters.get(GlassCamApplication.getStringFromRessources(R.string.auto_hdr_enable)) != null) {

            List<String> Scenes = new ArrayList<>(Arrays.asList(parameters.get(GlassCamApplication.getStringFromRessources(R.string.scene_mode_values)).split(",")));
            if (Scenes.contains(GlassCamApplication.getStringFromRessources(R.string.scene_mode_hdr))) {
                setViewState(ViewState.Visible);
            }
            if (Scenes.contains(GlassCamApplication.getStringFromRessources(R.string.scene_mode_asd))) {
                setViewState(ViewState.Visible);
            }

        }
        else
            setViewState(ViewState.Hidden);

    }

    @Override
    public void setValue(String valueToSet, boolean setToCam) {
        if (valueToSet.equals(GlassCamApplication.getStringFromRessources(R.string.on_)))
        {
            parameters.set(GlassCamApplication.getStringFromRessources(R.string.scene_mode), GlassCamApplication.getStringFromRessources(R.string.auto));
            parameters.set(GlassCamApplication.getStringFromRessources(R.string.auto_hdr_enable), GlassCamApplication.getStringFromRessources(R.string.disable_));

        }
        else if (valueToSet.equals(GlassCamApplication.getStringFromRessources(R.string.off_)))
        {
            parameters.set(GlassCamApplication.getStringFromRessources(R.string.scene_mode), GlassCamApplication.getStringFromRessources(R.string.auto));
            parameters.set(GlassCamApplication.getStringFromRessources(R.string.auto_hdr_enable), GlassCamApplication.getStringFromRessources(R.string.disable_));
        }
        else if (valueToSet.equals(GlassCamApplication.getStringFromRessources(R.string.auto_)))
        {
            parameters.set(GlassCamApplication.getStringFromRessources(R.string.scene_mode), GlassCamApplication.getStringFromRessources(R.string.scene_mode_asd));
            parameters.set(GlassCamApplication.getStringFromRessources(R.string.auto_hdr_enable), GlassCamApplication.getStringFromRessources(R.string.enable_));
        }
        Log.d(TAG, "set auto hdr");
        if(setToCam)
            ((ParametersHandler) cameraUiWrapper.getParameterHandler()).SetParametersToCamera(parameters);
        ((SettingMode)settingsManager.get(key)).set(valueToSet);
        fireStringValueChanged(valueToSet);
    }

    @Override
    public String getStringValue() {
        if(parameters.get(GlassCamApplication.getStringFromRessources(R.string.auto_hdr_enable))!= null)
        {
            if (parameters.get(GlassCamApplication.getStringFromRessources(R.string.auto_hdr_enable)).equals(GlassCamApplication.getStringFromRessources(R.string.enable_))
                    && parameters.get(GlassCamApplication.getStringFromRessources(R.string.scene_mode)).equals(GlassCamApplication.getStringFromRessources(R.string.scene_mode_hdr)))
                return GlassCamApplication.getStringFromRessources(R.string.on_);
            else if (parameters.get(GlassCamApplication.getStringFromRessources(R.string.auto_hdr_enable)).equals(GlassCamApplication.getStringFromRessources(R.string.enable_))
                    && parameters.get(GlassCamApplication.getStringFromRessources(R.string.scene_mode)).equals(GlassCamApplication.getStringFromRessources(R.string.scene_mode_asd)))
                return GlassCamApplication.getStringFromRessources(R.string.auto_);
            else
                return GlassCamApplication.getStringFromRessources(R.string.off_);
        }
        else
            return GlassCamApplication.getStringFromRessources(R.string.off_);
    }

    @Override
    public void onModuleChanged(String module)
    {
        curmodule = module;
        if (curmodule.equals(GlassCamApplication.getStringFromRessources(R.string.module_video))|| curmodule.equals(GlassCamApplication.getStringFromRessources(R.string.module_video)))
        {
            hide();
            setStringValue(GlassCamApplication.getStringFromRessources(R.string.off_),true);
        }
        else
        {
            if (format.contains(GlassCamApplication.getStringFromRessources(R.string.jpeg_))) {
                show();
                setViewState(ViewState.Visible);
            }
            else
            {
                hide();
                setStringValue(GlassCamApplication.getStringFromRessources(R.string.off_),true);
            }
        }
    }


    private void hide()
    {
        state = getStringValue();
        visible = false;
        setStringValue(GlassCamApplication.getStringFromRessources(R.string.off_),true);
        fireStringValueChanged(GlassCamApplication.getStringFromRessources(R.string.off_));
        setViewState(ViewState.Hidden);
    }
    private void show()
    {
        visible = true;
        setStringValue(state,true);
        fireStringValueChanged(state);
        setViewState(ViewState.Visible);
    }
}
