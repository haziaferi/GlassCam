package glasscam.cam.apis.basecamera.parameters.modes;

import com.hazia.glasscam.R;

import glasscam.GlassCamApplication;
import glasscam.cam.apis.basecamera.CameraWrapperInterface;
import glasscam.cam.apis.basecamera.parameters.AbstractParameter;
import glasscam.settings.SettingKeys;
import glasscam.utils.Log;

/**
 * Created by Ingo on 24.09.2016.
 */
public class NightOverlayParameter extends AbstractParameter {

    private final String TAG = NightOverlayParameter.class.getSimpleName();

    public NightOverlayParameter(CameraWrapperInterface cameraWrapperInterface)
    {
        super(SettingKeys.NIGHT_OVERLAY);
        setViewState(ViewState.Visible);
    }

    @Override
    public String[] getStringValues() {
        return new String[] { GlassCamApplication.getStringFromRessources(R.string.off_), GlassCamApplication.getStringFromRessources(R.string.on_) };
    }

    @Override
    public String getStringValue() {
        boolean enable = settingsManager.getGlobal(SettingKeys.NIGHT_OVERLAY).get();
        if (enable)
            return GlassCamApplication.getStringFromRessources(R.string.on_);
        else
            return GlassCamApplication.getStringFromRessources(R.string.off_);
    }

    @Override
    public void setStringValue(String valueToSet, boolean setToCamera) {
        settingsManager.getGlobal(SettingKeys.NIGHT_OVERLAY).set(valueToSet.equals(GlassCamApplication.getStringFromRessources(R.string.on_)));
        Log.d(TAG, "Nightoverlay :" +settingsManager.getGlobal(SettingKeys.NIGHT_OVERLAY).get());
        fireStringValueChanged(valueToSet);

    }
}
