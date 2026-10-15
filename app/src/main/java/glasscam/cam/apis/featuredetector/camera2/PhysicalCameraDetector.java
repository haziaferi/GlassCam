package glasscam.cam.apis.featuredetector.camera2;

import android.content.Context;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.os.Build;

import androidx.annotation.RequiresApi;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.hazia.glasscam.R;

import glasscam.GlassCamApplication;
import glasscam.settings.SettingKeys;
import glasscam.utils.Log;

/**
 * Detects whether the active camera is a logical multi-camera
 * (REQUEST_AVAILABLE_CAPABILITIES_LOGICAL_MULTI_CAMERA) exposing more than one
 * physical sensor, and if so builds the value list for SettingKeys.PHYSICAL_CAMERA_ID:
 * "Auto" (let the logical camera's own fusion pick a sensor, the existing default
 * behaviour - unset physical camera id on the output configs) followed by one entry
 * per physical sensor, labelled with its focal length since that's a real, honest
 * number rather than a guessed-at vendor marketing name ("ultrawide"/"tele" mean
 * different things on different phones).
 */
@RequiresApi(api = Build.VERSION_CODES.P)
public class PhysicalCameraDetector extends BaseParameter2Detector {

    private final String TAG = PhysicalCameraDetector.class.getSimpleName();

    // Label -> raw physical camera id, rebuilt every findAndFillSettings() run.
    // PhysicalCameraModeApi2 reads this instead of parsing the id back out of the
    // display label, since Camera2 doesn't guarantee physical camera ids never
    // contain a space (they're vendor-defined strings) - only this class, which
    // builds the label in the first place, actually knows the real id for certain.
    //
    // volatile + build-then-publish rather than a shared mutable map: nothing
    // guarantees findAndFillSettings() (feature-detection pass) and
    // physicalIdForLabel() (read from wherever PhysicalCameraModeApi2 lives) run on
    // the same thread. Mutating one shared map in place (even a ConcurrentHashMap)
    // would still let a reader observe a half-rebuilt map mid-clear/repopulate.
    // Building a brand-new map and publishing it with one volatile write means a
    // reader only ever sees a complete old map or a complete new one.
    // "Auto" is intentionally never put() into this map - it's handled by callers
    // before they ever call physicalIdForLabel(). A miss on lookup returns null,
    // same as an explicit null entry would have.
    private static volatile Map<String, String> labelToPhysicalId = Collections.emptyMap();

    public static String physicalIdForLabel(String label) {
        return labelToPhysicalId.get(label);
    }

    @Override
    protected void findAndFillSettings(CameraCharacteristics cameraCharacteristics) {
        Set<String> physicalIds = cameraCharacteristics.getPhysicalCameraIds();
        if (physicalIds == null || physicalIds.size() < 2) {
            // Not a logical multi-camera (or only exposes one physical sensor) -
            // nothing useful to offer here.
            settingsManager.get(SettingKeys.PHYSICAL_CAMERA_ID).setIsSupported(false);
            return;
        }

        CameraManager manager = (CameraManager) GlassCamApplication.getContext().getSystemService(Context.CAMERA_SERVICE);
        List<String> values = new ArrayList<>();
        String autoLabel = GlassCamApplication.getStringFromRessources(R.string.physicalcamera_auto);
        values.add(autoLabel);

        Map<String, String> newLabelToPhysicalId = new HashMap<>();

        for (String physicalId : physicalIds) {
            String label = labelFor(manager, physicalId);
            values.add(label);
            newLabelToPhysicalId.put(label, physicalId);
        }

        labelToPhysicalId = newLabelToPhysicalId;

        settingsManager.get(SettingKeys.PHYSICAL_CAMERA_ID).setValues(values.toArray(new String[0]));
        settingsManager.get(SettingKeys.PHYSICAL_CAMERA_ID).set(values.get(0));
        settingsManager.get(SettingKeys.PHYSICAL_CAMERA_ID).setIsSupported(true);
        Log.d(TAG, "physical camera ids: " + physicalIds);
    }

    private String labelFor(CameraManager manager, String physicalId) {
        try {
            CameraCharacteristics physicalCharacteristics = manager.getCameraCharacteristics(physicalId);
            float[] focalLengths = physicalCharacteristics.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS);
            if (focalLengths != null && focalLengths.length > 0)
                return physicalId + " (" + focalLengths[0] + "mm)";
        } catch (CameraAccessException | IllegalArgumentException ex) {
            Log.WriteEx(ex);
        }
        return physicalId;
    }
}
