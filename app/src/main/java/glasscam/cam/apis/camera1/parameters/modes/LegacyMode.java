package glasscam.cam.apis.camera1.parameters.modes;

import android.os.Build;

import glasscam.cam.ActivityGlassCamMain;
import glasscam.cam.apis.basecamera.CameraWrapperInterface;
import glasscam.cam.apis.basecamera.parameters.AbstractParameter;
import glasscam.settings.mode.ApiBooleanSettingMode;
import glasscam.settings.mode.BooleanSettingModeInterface;

/**
 * Created by KillerInk on 22.02.2018.
 */

public class LegacyMode extends AbstractParameter implements BooleanSettingModeInterface {

    ApiBooleanSettingMode settingMode;

    public LegacyMode(CameraWrapperInterface cameraUiWrapper,  ApiBooleanSettingMode settingMode) {
        super(cameraUiWrapper,null);
        this.settingMode = settingMode;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
            setViewState(ViewState.Visible);
    }

    @Override
    public boolean get() {
        return settingMode.get();
    }

    @Override
    public void set(boolean bool) {
        settingMode.set(bool);
        ActivityGlassCamMain.cameraApiManager().runFeatureDetector();
    }
}
