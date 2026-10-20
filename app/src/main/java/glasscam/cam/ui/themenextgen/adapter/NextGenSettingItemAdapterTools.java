package glasscam.cam.ui.themenextgen.adapter;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.hazia.glasscam.R;

import java.util.ArrayList;
import java.util.List;

import glasscam.cam.apis.CameraApiManager;
import glasscam.cam.apis.basecamera.parameters.AbstractParameter;
import glasscam.cam.apis.basecamera.parameters.ParameterHandler;
import glasscam.cam.apis.basecamera.parameters.ParameterInterface;
import glasscam.cam.apis.basecamera.parameters.modes.ApiParameter;
import glasscam.cam.apis.basecamera.parameters.modes.SDModeParameter;
import glasscam.cam.apis.basecamera.parameters.modes.SettingModeParamter;
import glasscam.cam.apis.camera2.Camera2;
import glasscam.cam.previewpostprocessing.PreviewPostProcessingModes;
import glasscam.cam.ui.themenextgen.adapter.customclicks.AspectRatioCheckedChanged;
import glasscam.cam.ui.themenextgen.adapter.customclicks.Camera2VendorButtonClick;
import glasscam.cam.ui.themenextgen.adapter.customclicks.RunFeatureDetectorClick;
import glasscam.cam.ui.themenextgen.adapter.customclicks.SaveCameraParametersClick;
import glasscam.cam.ui.themenextgen.adapter.customclicks.VideoEditorClick;
import glasscam.cam.ui.themenextgen.layoutconfig.SettingItemConfig;
import glasscam.cam.ui.themenextgen.view.button.NextGenSettingBoolItem;
import glasscam.cam.ui.themenextgen.view.button.NextGenSettingButton;
import glasscam.cam.ui.themenextgen.view.button.NextGenSettingItem;
import glasscam.cam.ui.settings.childs.SettingsChildMenuTimeLapseFrames;
import glasscam.cam.ui.settings.childs.SettingsChild_GlassCamAe;
import glasscam.settings.SettingKeys;
import glasscam.settings.SettingsManager;
import glasscam.settings.mode.AbstractSettingMode;
import glasscam.settings.mode.BooleanSettingModeInterface;
import glasscam.utils.Log;

public class NextGenSettingItemAdapterTools
{

    private static final String TAG = NextGenSettingItemAdapterTools.class.getSimpleName();

    public List<SettingItemConfig> getValidSettingItemConfigList(List<SettingItemConfig> keyList, CameraApiManager cameraApiManager, SettingsManager settingsManager)
    {
        List<SettingItemConfig> validList = new ArrayList<>();
        for (SettingItemConfig conf : keyList)
        {
            ParameterInterface p = null;
            AbstractSettingMode s = null;
            if (conf.getHeader() == R.string.setting_savecaminfo_header) {
                // one row for both APIs (owner, purpose map 2026-10-20): Camera2 dumps the vendor keys,
                // Camera1 saves the driver's parameter string; any other camera has nothing to save
                if (cameraApiManager.getCamera() instanceof Camera2 || settingsManager.getCamApi().equals(SettingsManager.API_1))
                    validList.add(conf);
            }
            else if (conf.getHeader() == R.string.setting_api_header)
                validList.add(conf);
            else if (conf.getKey() == SettingKeys.USE_GLASSCAM_AE || conf.getKey() == SettingKeys.MAX_ISO || conf.getKey() == SettingKeys.MIN_ISO || conf.getKey() == SettingKeys.MAX_EXPOSURE || conf.getKey() == SettingKeys.MIN_EXPOSURE)
            {
                if(settingsManager.getGlobal(SettingKeys.PREVIEW_POST_PROCESSING_MODE) != null)
                    if (settingsManager.getGlobal(SettingKeys.PREVIEW_POST_PROCESSING_MODE).get() != null &&
                            settingsManager.getGlobal(SettingKeys.PREVIEW_POST_PROCESSING_MODE).get().equals(PreviewPostProcessingModes.OpenGL.name()))
                        validList.add(conf);
            }
            else if (conf.getKey() == null) {
                    validList.add(conf);
            }
            else {
                if (!conf.getFromSettingManager()) {
                    p = cameraApiManager.getCamera().getParameterHandler().get(conf.getKey());
                }
                else
                    s = (AbstractSettingMode) settingsManager.get(conf.getKey());
                if (p != null || s != null)
                    validList.add(conf);
            }
        }
        // a section label with no surviving rows under it (the device filtered them all) is noise
        List<SettingItemConfig> pruned = new ArrayList<>();
        for (int i = 0; i < validList.size(); i++) {
            SettingItemConfig c = validList.get(i);
            if (c.getViewType() == SettingItemConfig.ViewType.Section) {
                boolean hasRows = i + 1 < validList.size() && validList.get(i + 1).getViewType() != SettingItemConfig.ViewType.Section;
                if (!hasRows)
                    continue;
            }
            pruned.add(c);
        }
        return pruned;
    }

    public void fillConfigsWithViews(SettingItemConfig[] configs, CameraApiManager cameraApiManager, SettingsManager settingsManager, Context context)
    {
        for (SettingItemConfig config : configs)
        {
            View v = getViewFromKey(config, cameraApiManager,settingsManager,context);

            if (v == null) {
                if (config.getKey() != null)
                    Log.e(TAG, "Failed to get view for:" + config.getKey().toString());
                else
                    Log.e(TAG, "Failed to get view for:" + context.getResources().getText(config.getHeader()));
            }
            else
            {
                config.setView(v);
            }
        }
    }

    private View getViewFromKey(SettingItemConfig key, CameraApiManager cameraApiManager, SettingsManager settingsManager, Context context)
    {
        if (key.getViewType() == SettingItemConfig.ViewType.Section) {
            View v = android.view.LayoutInflater.from(context).inflate(R.layout.nextgen_setting_section, null, false);
            ((android.widget.TextView) v.findViewById(R.id.textView_section)).setText(key.getHeader());
            return v;
        }
        if (key.getHeader() == R.string.setting_videoprofileeditor_header)
            return NextGenSettingButton.getInstance(context, key.getHeader(),key.getDescription(), new VideoEditorClick(context));
        if (key.getHeader() == R.string.setting_sdcard_header) {
            ((SDModeParameter)cameraApiManager.getCamera().getParameterHandler().get(SettingKeys.SD_SAVE_LOCATION)).setContext(context);
        }
        if (key.getHeader() == R.string.setting_savecaminfo_header)
            return NextGenSettingButton.getInstance(context, key.getHeader(), key.getDescription(),
                    cameraApiManager.getCamera() instanceof Camera2
                            ? new Camera2VendorButtonClick((Camera2) cameraApiManager.getCamera())
                            : new SaveCameraParametersClick(cameraApiManager, settingsManager, context));
        if (key.getHeader() == R.string.setting_featuredetector_header)
            return NextGenSettingButton.getInstance(context,R.string.setting_featuredetector_header,R.string.setting_featuredetector_description, new RunFeatureDetectorClick());
        if (key.getHeader() == R.string.setting_api_header)
            return NextGenSettingItem.getInstance(context,R.string.setting_api_header,R.string.setting_api_description,new ApiParameter(settingsManager,cameraApiManager));

        if (key.getKey() != null) {
            if (key.getViewType() == SettingItemConfig.ViewType.Custom) {
                if (key.getKey() == SettingKeys.TIMELAPSE_FRAMES)
                    return new SettingsChildMenuTimeLapseFrames(context);
                if ((key.getKey() == SettingKeys.USE_GLASSCAM_AE))
                    return new SettingsChild_GlassCamAe(context, settingsManager.getGlobal(SettingKeys.USE_GLASSCAM_AE), key.getHeader(), key.getDescription());
                if (key.getKey() == SettingKeys.SWITCH_ASPECT_RATIO)
                    return NextGenSettingBoolItem.getInstance(context, R.string.setting_switch_aspect_header, R.string.setting_switch_aspect_text, settingsManager.get(SettingKeys.SWITCH_ASPECT_RATIO), new AspectRatioCheckedChanged());
            }
            return getView(key,cameraApiManager.getCamera().getParameterHandler(),settingsManager,context);
        }
        return null;
    }

    private View getView(SettingItemConfig key, ParameterHandler parameterHandler, SettingsManager settingsManager,Context c)
    {
        if (key.getFromSettingManager()) {
            return getViewFromSetting(key,settingsManager,c);
        } else {
            return getViewFromParameterHandler(key, parameterHandler,c);
        }
    }

    @Nullable
    private View getViewFromParameterHandler(SettingItemConfig key, ParameterHandler parameterHandler,Context context) {
        ParameterInterface parameterInterface = parameterHandler.get(key.getKey());
        if (parameterInterface != null) {
            if (parameterInterface instanceof BooleanSettingModeInterface)
                return NextGenSettingBoolItem.getInstance(context, key.getHeader(), key.getDescription(), (BooleanSettingModeInterface) parameterInterface);
            return NextGenSettingItem.getInstance(context, key.getHeader(), key.getDescription(), (AbstractParameter) parameterInterface);
        }
        return null;
    }

    @NonNull
    private ViewGroup getViewFromSetting(SettingItemConfig key, SettingsManager settingsManager, Context context) {
        AbstractSettingMode parameterInterface = (AbstractSettingMode) settingsManager.get(key.getKey());
        if (parameterInterface instanceof BooleanSettingModeInterface)
            return NextGenSettingBoolItem.getInstance(context, key.getHeader(), key.getDescription(), (BooleanSettingModeInterface) parameterInterface);
        return NextGenSettingItem.getInstance(context, key.getHeader(), key.getDescription(), new SettingModeParamter(key.getKey()));
    }

}
