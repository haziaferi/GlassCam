package glasscam.utils;

import glasscam.GlassCamApplication;
import glasscam.settings.SettingKeys;

public class OrientationUtil {
    public static int getOrientation(int rotation)
    {
        return  (360 + rotation+Integer.parseInt(GlassCamApplication.settingsManager().get(SettingKeys.ORIENTATION_HACK).get()))%360;
    }
}
