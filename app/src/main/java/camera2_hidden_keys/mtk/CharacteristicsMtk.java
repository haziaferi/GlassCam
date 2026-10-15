package camera2_hidden_keys.mtk;

import android.hardware.camera2.CameraCharacteristics;

import camera2_hidden_keys.AbstractCameraCharacteristics;

public class CharacteristicsMtk extends AbstractCameraCharacteristics {
    //[0, 1]
    public static final CameraCharacteristics.Key<int[]> com_mediatek_aovservicefeature_availableAovModes;

    static {
        com_mediatek_aovservicefeature_availableAovModes = getKeyType("com.mediatek.aovservicefeature.availableAovModes", int[].class);
    }
}
