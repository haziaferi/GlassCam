package glasscam.cam.apis.basecamera.parameters.modes;

import android.text.TextUtils;

import com.hazia.glasscam.R;

import java.util.HashMap;

import glasscam.GlassCamApplication;
import glasscam.cam.apis.basecamera.parameters.AbstractParameter;
import glasscam.dng.ToneMapProfile;
import glasscam.settings.SettingKeys;

/**
 * Created by troop on 10.07.2017.
 */

public class ToneMapChooser extends AbstractParameter {
    private final HashMap<String, ToneMapProfile> toneMapProfileHashMap;
    public ToneMapChooser(HashMap<String, ToneMapProfile> toneMapProfileHashMap)
    {
        super(SettingKeys.TONEMAP_SET);
        this.toneMapProfileHashMap = toneMapProfileHashMap;
        setViewState(ViewState.Visible);
        currentString = settingsManager.get(SettingKeys.TONEMAP_SET).get();
        if (TextUtils.isEmpty(currentString))
            currentString = GlassCamApplication.getStringFromRessources(R.string.off_);
    }

    @Override
    public void setStringValue(String valueToSet, boolean setToCamera)
    {
        currentString = valueToSet;
        fireStringValueChanged(currentString);
        settingsManager.get(SettingKeys.TONEMAP_SET).set(valueToSet);
    }

    @Override
    public String[] getStringValues()
    {
        return toneMapProfileHashMap.keySet().toArray(new String[toneMapProfileHashMap.size()]);
    }

    public ToneMapProfile getToneMap()
    {
        return toneMapProfileHashMap.get(currentString);
    }
}
