package glasscam.cam.apis.featuredetector;

import android.hardware.camera2.CameraCharacteristics;
import android.os.Build;

import com.hazia.glasscam.BuildConfig;
import com.hazia.glasscam.R;

import java.util.ArrayList;
import java.util.List;

import glasscam.GlassCamApplication;
import glasscam.cam.previewpostprocessing.PreviewPostProcessingModes;
import glasscam.cam.ui.ThemeManager;
import glasscam.gl.GlVersion;
import glasscam.settings.SettingKeys;
import glasscam.settings.SettingsManager;
import glasscam.update.ReleaseChecker;
import glasscam.utils.Log;

public class CameraFeatureDetector {
    private final String TAG = CameraFeatureDetector.class.getSimpleName();
    private final SettingsManager settingsManager;

    public CameraFeatureDetector()
    {
        settingsManager = GlassCamApplication.settingsManager();
    }

    /**
     * @param firstRun true on a first run or after a RESET: seed every default. False on a version
     *                 bump: refresh what the camera offers (value lists, supported flags, the cached
     *                 parameter strings the bump exists for) but keep the user's choices - the API,
     *                 theme, grid, manual strip, shutter sound, focus-stack merge, focus-peak colour
     *                 and preview post-processing used to be reset on every bump (owner, 4.3.91).
     */
    public void detectFeatures(boolean firstRun)
    {
        Log.d(TAG, "CameraFeatureRunner process, first run: " + firstRun);
        String previousApi = settingsManager.getCamApi();
        settingsManager.setCamApi(SettingsManager.API_1);
        Camera2FeatureDetectorTask task  = null;
        Camera1FeatureDetectorTask task1 = null;

        if (Build.VERSION.SDK_INT >= 21) {
            task =  new Camera2FeatureDetectorTask();
            task.detect();
        }
        task1 = new Camera1FeatureDetectorTask();
        task1.detect();
        if (settingsManager.hasCamera2Features()) {
            if (task.hwlvl == CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY)
                settingsManager.setCamApi(SettingsManager.API_1);
            else
                settingsManager.setCamApi(SettingsManager.API_2);
        }
        // a bump keeps a manually chosen API, unless the hardware no longer offers Camera2
        if (!firstRun && SettingsManager.API_1.equals(previousApi))
            settingsManager.setCamApi(SettingsManager.API_1);
        setGlobalDefaultSettings(firstRun);
        settingsManager.setAppVersion(BuildConfig.VERSION_CODE);
        settingsManager.setAreFeaturesDetected(true);
        settingsManager.save();
        Log.d(TAG, "Feature Detection done! Start GlassCam Api: " + settingsManager.getCamApi() + " app version:" + settingsManager.getAppVersion());
    }

    /** A value list is always refreshed; the selection is written only on a first run, or when the
     *  stored one is no longer in the list. */
    private static boolean keep(boolean firstRun, String current, String[] values)
    {
        return !firstRun && current != null && values != null && java.util.Arrays.asList(values).contains(current);
    }

    private void setGlobalDefaultSettings(boolean firstRun)
    {
        List<String> previewPostProcessingValues = new ArrayList();
        previewPostProcessingValues.add(PreviewPostProcessingModes.off.name());
        if (GlVersion.isMinGlVersion())
            previewPostProcessingValues.add(PreviewPostProcessingModes.OpenGL.name());

        if (previewPostProcessingValues.size() > 1) {
            String[] ppValues = previewPostProcessingValues.toArray(new String[previewPostProcessingValues.size()]);
            settingsManager.getGlobal(SettingKeys.PREVIEW_POST_PROCESSING_MODE).setValues(ppValues);
            if (!keep(firstRun, settingsManager.getGlobal(SettingKeys.PREVIEW_POST_PROCESSING_MODE).get(), ppValues))
                settingsManager.getGlobal(SettingKeys.PREVIEW_POST_PROCESSING_MODE).set(PreviewPostProcessingModes.off.name());
            settingsManager.getGlobal(SettingKeys.PREVIEW_POST_PROCESSING_MODE).setIsSupported(true);
            String[] peakValues = GlassCamApplication.getContext().getResources().getStringArray(R.array.focuspeakColors);
            settingsManager.getGlobal(SettingKeys.FOCUSPEAK_COLOR).setValues(peakValues);
            if (!keep(firstRun, settingsManager.getGlobal(SettingKeys.FOCUSPEAK_COLOR).get(), peakValues))
                settingsManager.getGlobal(SettingKeys.FOCUSPEAK_COLOR).set(peakValues[0]);
            settingsManager.getGlobal(SettingKeys.FOCUSPEAK_COLOR).setIsSupported(true);
        }
        else
            settingsManager.getGlobal(SettingKeys.PREVIEW_POST_PROCESSING_MODE).setIsSupported(false);

        String[] themes = GlassCamApplication.getStringArrayFromRessource(R.array.themes);
        settingsManager.getGlobal(SettingKeys.THEME).setValues(themes);
        if (!keep(firstRun, settingsManager.getGlobal(SettingKeys.THEME).get(), themes))
            settingsManager.getGlobal(SettingKeys.THEME).set(ThemeManager.NEXTGEN);
        settingsManager.getGlobal(SettingKeys.THEME).setIsSupported(true);

        String[] guides = GlassCamApplication.getStringArrayFromRessource(R.array.guidelist);
        settingsManager.getGlobal(SettingKeys.GUIDE_LIST).setValues(guides);
        if (!keep(firstRun, settingsManager.getGlobal(SettingKeys.GUIDE_LIST).get(), guides))
            settingsManager.getGlobal(SettingKeys.GUIDE_LIST).set(guides[0]);
        if (ReleaseChecker.isGithubRelease)
            settingsManager.getGlobal(SettingKeys.CHECKFORUPDATES).set(true);

        if (firstRun) {   // plain switches: a bump leaves them as the user set them
            settingsManager.getGlobal(SettingKeys.SHOWMANUALSETTINGS).set(true);
            settingsManager.getGlobal(SettingKeys.PLAY_SHUTTER_SOUND).set(false);
            settingsManager.getGlobal(SettingKeys.FOCUS_STACK_MERGE).set(false);
        }
    }
}
