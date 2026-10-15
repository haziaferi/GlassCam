package glasscam.cam.apis.basecamera.parameters;

import android.graphics.Rect;

import glasscam.settings.SettingKeys;

public interface ParameterHandler
{
    void add(SettingKeys.Key parameters, ParameterInterface parameterInterface);
    ParameterInterface get(SettingKeys.Key parameters);
    void SetFocusAREA(Rect focusAreas);
    void SetPictureOrientation(int or);
    float getCurrentExposuretime();
    int getCurrentIso();
    void SetAppSettingsToParameters();
    void setManualSettingsToParameters();
}
