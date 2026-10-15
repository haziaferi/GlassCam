package glasscam.cam.apis.camera2.parameters.modes;

import com.hazia.glasscam.R;

import glasscam.GlassCamApplication;
import glasscam.cam.ActivityGlassCamMain;
import glasscam.cam.apis.basecamera.CameraWrapperInterface;
import glasscam.cam.apis.basecamera.parameters.modes.FocusPeakMode;
import glasscam.cam.histogram.HistogramController;
import glasscam.cam.previewpostprocessing.PreviewPostProcessingModes;
import glasscam.settings.SettingKeys;
import glasscam.settings.mode.ApiBooleanSettingMode;
import glasscam.settings.mode.SettingMode;

public class HistogramQcom extends FocusPeakMode {

    private HistogramController histogramController;
    public HistogramQcom(CameraWrapperInterface cameraUiWrapper, SettingKeys.Key<ApiBooleanSettingMode> settingMode) {
        super(cameraUiWrapper, settingMode);
        this.settingMode = SettingKeys.HISTOGRAM_STATS_QCOM;
        this.histogramController = ActivityGlassCamMain.histogramController();
    }

    @Override
    public void setStringValue(String valueToSet, boolean setToCamera)
    {
        currentString = valueToSet;
        boolean toset = valueToSet.equals(GlassCamApplication.getStringFromRessources(R.string.on_));
        histogramController.enableCameraHistogram(toset);
        settingsManager.get(settingMode).set(toset);
        fireStringValueChanged(valueToSet);

    }

    @Override
    public void set(boolean bool) {
        histogramController.enableCameraHistogram(bool);
        settingsManager.get(settingMode).set(bool);
        fireStringValueChanged(getStringValue());
    }

    @Override
    public ViewState getViewState() {
        return ViewState.Visible;
    }
}
