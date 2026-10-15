package glasscam.cam.apis.camera2.parameters.modes;

import android.annotation.TargetApi;
import android.hardware.camera2.CaptureRequest;
import android.os.Build;

import com.hazia.glasscam.R;

import glasscam.GlassCamApplication;
import glasscam.cam.apis.camera2.Camera2;
import glasscam.settings.mode.BooleanSettingModeInterface;

/**
 * Created by Ingo on 03.10.2016.
 */
@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class AeLockModeApi2 extends BaseModeApi2 implements BooleanSettingModeInterface {
    public AeLockModeApi2(Camera2 cameraUiWrapper) {
        super(cameraUiWrapper, null);
        setViewState(ViewState.Visible);
    }


    @Override
    public String getStringValue() {
        if (cameraUiWrapper == null || cameraUiWrapper.captureSessionHandler == null)
            return GlassCamApplication.getStringFromRessources(R.string.false_);
        try {
            if (cameraUiWrapper.captureSessionHandler.getPreviewParameter(CaptureRequest.CONTROL_AE_LOCK))
                return GlassCamApplication.getStringFromRessources(R.string.true_);
            else
                return GlassCamApplication.getStringFromRessources(R.string.false_);
        }
        catch (NullPointerException ex)
        {
            return GlassCamApplication.getStringFromRessources(R.string.false_);
        }

    }

    @Override
    public String[] getStringValues() {
        return new String[]{GlassCamApplication.getStringFromRessources(R.string.false_), GlassCamApplication.getStringFromRessources(R.string.true_)};
    }

    @Override
    public void setValue(String valueToSet, boolean setToCamera) {
        fireStringValueChanged(valueToSet);
        cameraUiWrapper.captureSessionHandler.SetParameterRepeating(CaptureRequest.CONTROL_AE_LOCK, valueToSet.equals(GlassCamApplication.getStringFromRessources(R.string.true_)), setToCamera);
    }

    @Override
    public boolean get() {
        if (cameraUiWrapper == null || cameraUiWrapper.captureSessionHandler == null)
            return false;
        try {
            return cameraUiWrapper.captureSessionHandler.getPreviewParameter(CaptureRequest.CONTROL_AE_LOCK);
        }
        catch (NullPointerException ex)
        {
        }
        return false;
    }

    @Override
    public void set(boolean bool) {
        cameraUiWrapper.captureSessionHandler.SetParameterRepeating(CaptureRequest.CONTROL_AE_LOCK,bool,true);
    }
}
