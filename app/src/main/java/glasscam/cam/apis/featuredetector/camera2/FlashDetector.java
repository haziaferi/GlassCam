package glasscam.cam.apis.featuredetector.camera2;

import android.hardware.camera2.CameraCharacteristics;
import android.os.Build;

import androidx.annotation.RequiresApi;

import com.hazia.glasscam.R;

import glasscam.GlassCamApplication;
import glasscam.settings.SettingKeys;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class FlashDetector extends BaseParameter2Detector {
    @Override
    protected void findAndFillSettings(CameraCharacteristics cameraCharacteristics) {
        if(!settingsManager.getIsFrontCamera())
            detectFlash(cameraCharacteristics);
    }


    private void detectFlash(CameraCharacteristics characteristics) {
        if (settingsManager.hasCamera2Features()) {
            //flash mode
            boolean flashavail = characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE);
            settingsManager.get(SettingKeys.FLASH_MODE).setIsSupported(flashavail);
            if (flashavail) {
                String[] lookupar = GlassCamApplication.getContext().getResources().getStringArray(R.array.flashextModes);
                // red-eye pre-flash moved here from the Auto Exposure Mode row (owner, purpose map 2026-10-20)
                int[] aeModes = characteristics.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_MODES);
                boolean redEye = false;
                if (aeModes != null)
                    for (int m : aeModes)
                        redEye |= m == CameraCharacteristics.CONTROL_AE_MODE_ON_AUTO_FLASH_REDEYE;
                if (redEye) {
                    String[] withRedEye = java.util.Arrays.copyOf(lookupar, lookupar.length + 1);
                    withRedEye[lookupar.length] = GlassCamApplication.getStringFromRessources(R.string.font_flash_redeye);
                    lookupar = withRedEye;
                }
                settingsManager.get(SettingKeys.FLASH_MODE).setValues(lookupar);
                settingsManager.get(SettingKeys.FLASH_MODE).set(GlassCamApplication.getStringFromRessources(R.string.font_flash_off));
            }
        }
    }
}
