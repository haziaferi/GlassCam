package glasscam.cam.apis.featuredetector.camera2;

import android.hardware.camera2.CameraCharacteristics;
import android.os.Build;

import androidx.annotation.RequiresApi;

import com.hazia.glasscam.R;

import glasscam.GlassCamApplication;
import glasscam.cam.apis.featuredetector.Camera2Util;
import glasscam.settings.SettingKeys;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class AutoExposureModeDetector extends BaseParameter2Detector {

    @Override
    protected void findAndFillSettings(CameraCharacteristics cameraCharacteristics) {
        Camera2Util.detectIntMode(cameraCharacteristics, CameraCharacteristics.CONTROL_AE_AVAILABLE_MODES, settingsManager.get(SettingKeys.EXPOSURE_MODE), R.array.aemodes,settingsManager);
        // on and off only: the flash variants (auto, always, red-eye) are set on the Flash toggle, which
        // writes the same CONTROL_AE_MODE (owner, purpose map 2026-10-20)
        String on = GlassCamApplication.getStringFromRessources(R.string.on);
        String off = GlassCamApplication.getStringFromRessources(R.string.off);
        java.util.List<String> keep = new java.util.ArrayList<>();
        String[] values = settingsManager.get(SettingKeys.EXPOSURE_MODE).getValues();
        if (values != null)
            for (String v : values) {
                // stored as "label,mode" pairs (Camera2Util.detectIntMode); compare the label
                String label = v.contains(",") ? v.substring(0, v.lastIndexOf(',')) : v;
                if (label.equals(on) || label.equals(off))
                    keep.add(v);
            }
        settingsManager.get(SettingKeys.EXPOSURE_MODE).setValues(keep.toArray(new String[0]));
        settingsManager.get(SettingKeys.EXPOSURE_MODE).set(GlassCamApplication.getStringFromRessources(R.string.on));

    }
}
