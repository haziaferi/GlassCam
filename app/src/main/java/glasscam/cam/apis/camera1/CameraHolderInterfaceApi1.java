package glasscam.cam.apis.camera1;

import android.view.Surface;

import glasscam.cam.apis.basecamera.CameraHolderInterface;

public interface CameraHolderInterfaceApi1 extends CameraHolderInterface {
    boolean setSurface(Surface texture);
    void resetPreviewCallback();
    void StartPreview();
    void StopPreview();
}
