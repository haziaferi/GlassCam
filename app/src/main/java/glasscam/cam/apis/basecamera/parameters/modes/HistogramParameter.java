package glasscam.cam.apis.basecamera.parameters.modes;

import com.hazia.glasscam.R;

import glasscam.GlassCamApplication;
import glasscam.cam.apis.basecamera.CameraWrapperInterface;
import glasscam.settings.SettingKeys;

/**
 * Created by KillerInk on 15.01.2018.
 */

public class HistogramParameter extends FocusPeakMode {

    private final String state = "off";

    public HistogramParameter(CameraWrapperInterface cameraUiWrapper) {
        super(cameraUiWrapper, SettingKeys.HISTOGRAM);
        settingMode = SettingKeys.HISTOGRAM;
    }
    @Override
    public void setStringValue(String valueToSet, boolean setToCamera)
    {
        currentString = valueToSet;
        boolean toset = valueToSet.equals(GlassCamApplication.getStringFromRessources(R.string.on_));
        previewController.setHistogram(toset);
        settingsManager.get(settingMode).set(toset);
        fireStringValueChanged(valueToSet);

    }

    @Override
    public void set(boolean bool) {
        previewController.setHistogram(bool);
        settingsManager.get(settingMode).set(bool);
        fireStringValueChanged(getStringValue());
    }
}
