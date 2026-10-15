package glasscam.cam.event.camera;


import glasscam.cam.apis.basecamera.Size;
import glasscam.cam.event.MyEvent;

public interface CameraHolderEvent extends MyEvent {
    void onCameraOpen();
    void onCameraOpenFinished();
    void onCameraClose();
    void onCameraError(String error);
    void onCameraChangedAspectRatioEvent(Size size);
}
