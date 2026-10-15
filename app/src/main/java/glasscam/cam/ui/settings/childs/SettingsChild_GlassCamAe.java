package glasscam.cam.ui.settings.childs;

import android.content.Context;
import android.widget.CompoundButton;

import glasscam.cam.apis.basecamera.CameraThreadHandler;
import glasscam.settings.mode.BooleanSettingModeInterface;

public class SettingsChild_GlassCamAe extends SettingsChild_BooleanSetting {
    public SettingsChild_GlassCamAe(Context context, BooleanSettingModeInterface booleanSettingMode, int headerid, int descriptionid) {
        super(context, booleanSettingMode, headerid, descriptionid);
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        super.onCheckedChanged(buttonView, isChecked);
        if(!buttonView.isPressed()) {
            return;
        }
        CameraThreadHandler.restartCameraAsync();
    }
}
