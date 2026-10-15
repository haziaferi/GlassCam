package glasscam.cam.apis.basecamera.parameters.modes;

import java.util.Arrays;

import glasscam.cam.ActivityGlassCamMain;
import glasscam.cam.apis.basecamera.parameters.AbstractParameter;
import glasscam.settings.SettingKeys;
import glasscam.utils.Log;

public class ThemeMode extends AbstractParameter {

    private static final String TAG = ThemeMode.class.getSimpleName();

    public ThemeMode(SettingKeys.Key key) {
        super(key);

        stringvalues = settingsManager.get(SettingKeys.THEME).getValues();
        Log.d(TAG,"values:" + Arrays.toString(stringvalues));
    }

    @Override
    protected void setValue(String valueToSet, boolean setToCamera) {
        super.setValue(valueToSet, setToCamera);
        // Persist immediately: the app also saves settings in onPauseTasks(), but that
        // only fires on a normal pause/stop. Saving here too means the choice survives
        // even if the process is killed before that lifecycle callback runs.
        settingsManager.save();
        ActivityGlassCamMain.themeManager().changeTheme(valueToSet);
    }

    @Override
    public ViewState getViewState() {
        return ViewState.Visible;
    }
}
