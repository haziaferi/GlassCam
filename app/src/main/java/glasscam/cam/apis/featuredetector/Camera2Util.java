package glasscam.cam.apis.featuredetector;

import android.hardware.camera2.CameraCharacteristics;
import android.os.Build;
import android.util.Size;

import androidx.annotation.RequiresApi;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import glasscam.GlassCamApplication;
import glasscam.settings.SettingsManager;
import glasscam.settings.mode.SettingMode;
import glasscam.utils.Log;
import glasscam.utils.StringUtils;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class Camera2Util
{
    public static Size[] getOutputSizeForImageFormat(int[] arraysize, int imgFormat)
    {
        List<Size> sizes =new ArrayList<>();
        for (int i = 0; i< arraysize.length;i+=4)
        {
            if (arraysize[i] == imgFormat) {
                Size s = new Size(arraysize[i + 1], arraysize[i + 2]);
                if (!sizes.contains(s))
                    sizes.add(s);
            }
        }
        return sizes.toArray(new Size[sizes.size()]);
    }

    /** The default is picked by the English label of each mode, in this order of preference. It used to
     *  be looked up by the translated label, so an Italian phone never found "Fast" ("Veloce") and fell
     *  through to "Off": noise reduction Off, which freezes the 6T's preview (fresh-eyes 2026-09-28). */
    private static final String[] DEFAULT_PREFERENCE = {"off", "Auto", "Fast", "Off", "Disable"};

    public static void detectIntMode(CameraCharacteristics characteristics, CameraCharacteristics.Key<int[]> requestKey, SettingMode settingMode, int lookupArrayId, SettingsManager settingsManager)
    {
        String[] lookupar = GlassCamApplication.getStringArrayFromRessource(lookupArrayId);
        String[] english = GlassCamApplication.getDefaultLocaleStringArray(lookupArrayId);
        if (settingsManager.hasCamera2Features() && characteristics.get(requestKey) != null) {
            int[]  scenes = characteristics.get(requestKey);
            if (scenes.length >1)
                settingMode.setIsSupported(true);
            else {
                settingMode.setIsSupported(false);
                return;
            }
            HashMap<String,Integer> map = new HashMap<>();
            for (int t : scenes)
            {
                if (t <lookupar.length)
                    map.put(lookupar[t], t);
            }
            settingMode.setValues(StringUtils.IntHashmapToStringArray(map));
            String def = defaultLabel(scenes, lookupar, english);
            if (def != null)
                settingMode.set(def);
        }
    }

    /** The translated label of the first available mode whose English label is in DEFAULT_PREFERENCE. */
    static String defaultLabel(int[] available, String[] labels, String[] english)
    {
        for (String wanted : DEFAULT_PREFERENCE)
            for (int t : available)
                if (t < labels.length && t < english.length && wanted.equals(english[t]))
                    return labels[t];
        return null;
    }

    public static void detectByteMode(CameraCharacteristics characteristics, CameraCharacteristics.Key<byte[]> requestKey, SettingMode settingMode, String[] lookupar,SettingsManager settingsManager)
    {
        if (settingsManager.hasCamera2Features() && characteristics.get(requestKey) != null) {

            byte[] scenes = characteristics.get(requestKey);
            if (scenes == null ||scenes.length == 0) {
                settingMode.setIsSupported(false);
                return;
            }
            HashMap<String,Integer> map = new HashMap<>();
            for (int i = 0; i< scenes.length; i++)
            {
                map.put(lookupar[i], (int)scenes[i]);
            }
            settingMode.setIsSupported(true);
            settingMode.set(lookupar[0]);
            lookupar = StringUtils.IntHashmapToStringArray(map);
            settingMode.setValues(lookupar);

        }
    }

    public static void dumpIntArray(int[] array,String TAG,String name)
    {
        if (array != null)
            Log.d(TAG, name +":"+ Arrays.toString(array));
        else
            Log.d(TAG, name +": isNull");
    }
}
