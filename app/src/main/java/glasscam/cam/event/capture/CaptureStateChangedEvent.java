package glasscam.cam.event.capture;

import glasscam.cam.event.MyEvent;

public interface CaptureStateChangedEvent extends MyEvent
{
    void onCaptureStateChanged(CaptureStates states);
}
