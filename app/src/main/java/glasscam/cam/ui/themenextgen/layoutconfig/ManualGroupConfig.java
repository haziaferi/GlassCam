package glasscam.cam.ui.themenextgen.layoutconfig;

import android.graphics.Color;

import com.hazia.glasscam.R;

import java.util.ArrayList;
import java.util.List;

import glasscam.GlassCamApplication;
import glasscam.settings.SettingKeys;

public class ManualGroupConfig {

    public List<ManualItemConfig> getManualGroup()
    {
        List<ManualItemConfig> group = new ArrayList<>();
        group.add(new ManualItemConfig<String>(SettingKeys.M_ZOOM, GlassCamApplication.getStringFromRessources(R.string.font_zoom_plus) ));
        group.add(new ManualItemConfig<String>(SettingKeys.M_FOCUS, GlassCamApplication.getStringFromRessources(R.string.font_manual_focus) ));
        group.add(new ManualItemConfig<String>(SettingKeys.M_MANUAL_ISO, GlassCamApplication.getStringFromRessources(R.string.font_iso) ));
        group.add(new ManualItemConfig<String>(SettingKeys.M_EXPOSURE_TIME, GlassCamApplication.getStringFromRessources(R.string.font_exposuretime) ));
        group.add(new ManualItemConfig<String>(SettingKeys.M_EXPOSURE_COMPENSATION, GlassCamApplication.getStringFromRessources(R.string.font_ev) ));
        group.add(new ManualItemConfig<String>(SettingKeys.M_APERTURE, GlassCamApplication.getStringFromRessources(R.string.font_aperture) ));
        group.add(new ManualItemConfig<String>(SettingKeys.M_WHITEBALANCE, GlassCamApplication.getStringFromRessources(R.string.font_wb) ));
        group.add(new ManualItemConfig<String>(SettingKeys.M_BURST, GlassCamApplication.getStringFromRessources(R.string.font_burst) ));
        group.add(new ManualItemConfig<String>(SettingKeys.M_CONTRAST, GlassCamApplication.getStringFromRessources(R.string.font_contrast) ));
        group.add(new ManualItemConfig<String>(SettingKeys.M_Brightness, GlassCamApplication.getStringFromRessources(R.string.font_brightness) ));
        group.add(new ManualItemConfig<String>(SettingKeys.M_SATURATION, GlassCamApplication.getStringFromRessources(R.string.font_saturation) ));
        group.add(new ManualItemConfig<String>(SettingKeys.M_SHARPNESS, GlassCamApplication.getStringFromRessources(R.string.font_sharpness) ));
        group.add(new ManualItemConfig<String>(SettingKeys.M_ZEBRA_HIGH, GlassCamApplication.getStringFromRessources(R.string.font_clipping), Color.RED));
        group.add(new ManualItemConfig<String>(SettingKeys.M_ZEBRA_LOW, GlassCamApplication.getStringFromRessources(R.string.font_clipping),Color.BLUE));

        return group;
    }
}
