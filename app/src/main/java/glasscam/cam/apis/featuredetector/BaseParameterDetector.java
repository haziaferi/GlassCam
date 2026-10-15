package glasscam.cam.apis.featuredetector;

import glasscam.GlassCamApplication;
import glasscam.settings.SettingsManager;
import glasscam.utils.Log;

public abstract class BaseParameterDetector<T> {

    protected SettingsManager settingsManager;

    public BaseParameterDetector()
    {
        settingsManager = GlassCamApplication.settingsManager();
    }

    public void checkIfSupported(T cameraCharacteristics)
    {
        try
        {
            findAndFillSettings(cameraCharacteristics);
        }
        catch (IllegalArgumentException | NullPointerException | ClassCastException e)
        {
            Log.WriteEx(e);
        }
    }

    protected abstract void findAndFillSettings(T cameraCharacteristics);


}
