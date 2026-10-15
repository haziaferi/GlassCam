package glasscam.cam.apis.basecamera.parameters.manual;

import glasscam.cam.apis.basecamera.parameters.AbstractParameter;
import glasscam.cam.previewpostprocessing.PreviewController;
import glasscam.settings.SettingKeys;

public class ZebraManualParameter extends AbstractParameter {

    protected PreviewController previewController;

    public ZebraManualParameter(SettingKeys.Key key, PreviewController previewController) {
        super(key);
        this.previewController = previewController;
    }
}
