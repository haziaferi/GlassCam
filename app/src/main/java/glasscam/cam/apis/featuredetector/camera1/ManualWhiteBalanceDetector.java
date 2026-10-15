package glasscam.cam.apis.featuredetector.camera1;

import android.hardware.Camera;
import android.text.TextUtils;

import com.hazia.glasscam.R;

import glasscam.GlassCamApplication;
import glasscam.cam.apis.featuredetector.Camera1FeatureDetectorTask;
import glasscam.settings.Frameworks;
import glasscam.settings.SettingKeys;
import glasscam.utils.Log;

public class ManualWhiteBalanceDetector extends BaseParameter1Detector{

    private final String TAG = ManualWhiteBalanceDetector.class.getSimpleName();
    @Override
    protected void findAndFillSettings(Camera.Parameters cameraCharacteristics) {
        detectManualWhiteBalance(cameraCharacteristics);
    }

    private boolean arrayContainsString(String[] ar,String dif)
    {
        if (ar == null)
            return false;
        boolean ret = false;
        for (String s: ar)
            if (s.equals(dif))
                ret = true;
        return ret;
    }

    private void detectManualWhiteBalance(Camera.Parameters parameters) {
        if (settingsManager.get(SettingKeys.M_WHITEBALANCE).isPresetted())
            return;
        if (settingsManager.getFrameWork() == Frameworks.MTK)
            settingsManager.get(SettingKeys.M_WHITEBALANCE).setIsSupported(false);
        else if (settingsManager.get(SettingKeys.M_WHITEBALANCE).isSupported()) // happens when its already set due supportedevices.xml
            return;
        else
        {
            // looks like wb-current-cct is loaded when the preview is up. this could be also for the other parameters
            String wbModeval ="", wbmax = "",wbmin = "";

            if (parameters.get(GlassCamApplication.getStringFromRessources(R.string.max_wb_cct)) != null) {
                wbmax = GlassCamApplication.getStringFromRessources(R.string.max_wb_cct);
            }
            else if (parameters.get(GlassCamApplication.getStringFromRessources(R.string.max_wb_ct))!= null)
                wbmax = GlassCamApplication.getStringFromRessources(R.string.max_wb_ct);

            if (parameters.get(GlassCamApplication.getStringFromRessources(R.string.min_wb_cct))!= null) {
                wbmin = GlassCamApplication.getStringFromRessources(R.string.min_wb_cct);
            } else if (parameters.get(GlassCamApplication.getStringFromRessources(R.string.min_wb_ct))!= null)
                wbmin = GlassCamApplication.getStringFromRessources(R.string.min_wb_ct);

            if (arrayContainsString(settingsManager.get(SettingKeys.WHITE_BALANCE_MODE).getValues(), GlassCamApplication.getStringFromRessources(R.string.manual)))
                wbModeval = GlassCamApplication.getStringFromRessources(R.string.manual);
            else if (arrayContainsString(settingsManager.get(SettingKeys.WHITE_BALANCE_MODE).getValues(), GlassCamApplication.getStringFromRessources(R.string.manual_cct)))
                wbModeval = GlassCamApplication.getStringFromRessources(R.string.manual_cct);

            try {
                if (!TextUtils.isEmpty(wbmax) && !TextUtils.isEmpty(wbmin) && !TextUtils.isEmpty(wbModeval)) {
                    Log.d(TAG, "Found all wbct values:" +wbmax + " " + wbmin + " " +wbModeval);
                    settingsManager.get(SettingKeys.M_WHITEBALANCE).setIsSupported(true);
                    settingsManager.get(SettingKeys.M_WHITEBALANCE).setMode(wbModeval);
                    int min = Integer.parseInt(parameters.get(wbmin));
                    int max = Integer.parseInt(parameters.get(wbmax));
                    settingsManager.get(SettingKeys.M_WHITEBALANCE).setValues(Camera1FeatureDetectorTask.createWBStringArray(min,max,100));
                }
                else {
                    Log.d(TAG, "Failed to lookup wbct:" + " " +wbmax + " " + wbmin + " " +wbModeval);
                    settingsManager.get(SettingKeys.M_WHITEBALANCE).setIsSupported(false);
                }
            }
            catch (NumberFormatException ex)
            {
                Log.WriteEx(ex);
                settingsManager.get(SettingKeys.M_WHITEBALANCE).setIsSupported(false);
            }
            catch(ArrayIndexOutOfBoundsException ex)
            {
                Log.WriteEx(ex);
                settingsManager.get(SettingKeys.M_WHITEBALANCE).setIsSupported(false);
            }

        }
    }
}
