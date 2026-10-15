package glasscam.cam.ui.themenextgen.view.button;

import glasscam.cam.apis.basecamera.parameters.AbstractParameter;

public interface ManualButtonInterface {
    void SetActive(boolean active);
    AbstractParameter getParameter();
    String[] getStringValues();
    int getCurrentItem();
    void setValueToParameters(final int value);
}
