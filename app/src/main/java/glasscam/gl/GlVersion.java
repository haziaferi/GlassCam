package glasscam.gl;

import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.ConfigurationInfo;

import glasscam.GlassCamApplication;

public class GlVersion {

    public static float getGlesVersion()
    {
        final ActivityManager activityManager = (ActivityManager) GlassCamApplication.getContext().getSystemService(Context.ACTIVITY_SERVICE);
        final ConfigurationInfo configurationInfo = activityManager.getDeviceConfigurationInfo();
        String ver = configurationInfo.getGlEsVersion();
        float glv;
        try {
            glv = Float.parseFloat(ver);
        }
        catch (NumberFormatException ex)
        {
            glv = 1;
        }
        return glv;
    }

    public static boolean isMinGlVersion()
    {
        return getGlesVersion() >=3.1;
    }
}
