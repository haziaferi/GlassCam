package glasscam.cam.apis.featuredetector.camera1;

import android.hardware.Camera;

import com.hazia.glasscam.R;

import glasscam.GlassCamApplication;
import glasscam.settings.SettingKeys;
import glasscam.utils.Log;

public class VideoStabDetector extends BaseParameter1Detector {
    @Override
    protected void findAndFillSettings(Camera.Parameters cameraCharacteristics) {
        try {
            if (cameraCharacteristics.get(camstring(R.string.video_stabilization_supported)).equals(camstring(R.string.true_)))
            {
                settingsManager.get(SettingKeys.VIDEO_STABILIZATION).setIsSupported(true);
                settingsManager.get(SettingKeys.VIDEO_STABILIZATION).setCamera1ParameterKEY(GlassCamApplication.getStringFromRessources(R.string.video_stabilization));
                settingsManager.get(SettingKeys.VIDEO_STABILIZATION).setValues(new String[]{GlassCamApplication.getStringFromRessources(R.string.true_), GlassCamApplication.getStringFromRessources(R.string.false_)});
            }
        } catch (NullPointerException e) {
            e.printStackTrace();
            settingsManager.get(SettingKeys.VIDEO_STABILIZATION).setIsSupported(false);
        }
        catch (NumberFormatException ex)
        {
            Log.WriteEx(ex);
            settingsManager.get(SettingKeys.VIDEO_STABILIZATION).setIsSupported(false);
        }
        catch(ArrayIndexOutOfBoundsException ex)
        {
            Log.WriteEx(ex);
            settingsManager.get(SettingKeys.VIDEO_STABILIZATION).setIsSupported(false);
        }
    }


}
