package glasscam.cam.ui.themenextgen.adapter.customclicks;

import glasscam.cam.ActivityGlassCamMain;
import glasscam.cam.ui.themenextgen.view.button.NextGenSettingButton;

public class RunFeatureDetectorClick implements NextGenSettingButton.NextGenSettingButtonClick {

    @Override
    public void onSettingButtonClick() {
        ActivityGlassCamMain.cameraApiManager().runFeatureDetector();
    }
}
