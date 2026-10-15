package glasscam.cam.apis.featuredetector.camera1;

import android.hardware.Camera;

import com.hazia.glasscam.R;

import glasscam.settings.SettingKeys;

public class JpegQualityMode extends BaseParameter1Detector {
    @Override
    protected void findAndFillSettings(Camera.Parameters cameraCharacteristics) {
        detectJpeqQualityModes(cameraCharacteristics);
    }

    private void detectJpeqQualityModes(Camera.Parameters parameters)
    {
        if (parameters.get(camstring(R.string.jpeg_quality)) == null)
        {
            settingsManager.get(SettingKeys.JPEG_QUALITY).setIsSupported(false);
            return;
        }
        String[] valuetoreturn = new String[20];
        for (int i = 1; i < 21; i++)
        {
            valuetoreturn[i-1] = "" + i*5;
        }
        settingsManager.get(SettingKeys.JPEG_QUALITY).setValues(valuetoreturn);
        settingsManager.get(SettingKeys.JPEG_QUALITY).set(parameters.get(camstring(R.string.jpeg_quality)));
        settingsManager.get(SettingKeys.JPEG_QUALITY).setCamera1ParameterKEY(camstring(R.string.jpeg_quality));
        if (valuetoreturn.length >0)
            settingsManager.get(SettingKeys.JPEG_QUALITY).setIsSupported(true);
    }
}
