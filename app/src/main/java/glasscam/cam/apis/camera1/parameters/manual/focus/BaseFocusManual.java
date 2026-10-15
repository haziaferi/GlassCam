/*
 *
 *     Copyright (C) 2015 Ingo Fuchs
 *     This program is free software; you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation; either version 2 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License along
 *     with this program; if not, write to the Free Software Foundation, Inc.,
 *     51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 * /
 */

package glasscam.cam.apis.camera1.parameters.manual.focus;

import android.hardware.Camera.Parameters;
import android.text.TextUtils;

import com.hazia.glasscam.R;

import glasscam.GlassCamApplication;
import glasscam.cam.apis.basecamera.CameraWrapperInterface;
import glasscam.cam.apis.camera1.parameters.ParametersHandler;
import glasscam.cam.apis.camera1.parameters.manual.BaseManualParameter;
import glasscam.settings.SettingKeys;
import glasscam.settings.mode.TypedSettingMode;
import glasscam.utils.Log;

/**
 * Created by troop on 05.03.2016.
 */
public class BaseFocusManual extends BaseManualParameter
{
    private final String TAG = BaseFocusManual.class.getSimpleName();
    protected String manualFocusModeString;
    private final int manualFocusType;

    public BaseFocusManual(Parameters parameters, CameraWrapperInterface cameraUiWrapper, SettingKeys.Key key)
    {
        super(parameters,cameraUiWrapper,key);
        TypedSettingMode settingMode1 =  (TypedSettingMode) settingsManager.get(key);
        settingMode = settingMode1;
        manualFocusType = settingMode1.getType();
        Log.d(TAG,"mf type:" +manualFocusType);
        manualFocusModeString = settingMode1.getMode();
        stringvalues = settingMode1.getValues();
        if(stringvalues.length ==0) {
            Log.d(TAG, "stringvalues are empty");
            this.fireViewStateChanged(ViewState.Hidden);
        }
        Log.d(TAG,"mf focus mode:" +manualFocusModeString);
    }


    @Override
    public void setValue(int valueToSet, boolean setToCamera)
    {
        currentInt = valueToSet;

        if (valueToSet == 0)
        {
            cameraUiWrapper.getParameterHandler().get(SettingKeys.FOCUS_MODE).setStringValue(GlassCamApplication.getStringFromRessources(R.string.auto_), true);
            Log.d(TAG, "Set Focus to : auto");
        }
        else
        {
            if ((!TextUtils.isEmpty(manualFocusModeString) || manualFocusModeString == null)
                    && !cameraUiWrapper.getParameterHandler().get(SettingKeys.FOCUS_MODE).getStringValue().equals(manualFocusModeString)) //do not set "manual" to "manual"
                cameraUiWrapper.getParameterHandler().get(SettingKeys.FOCUS_MODE).setStringValue(manualFocusModeString, false);
            if (manualFocusType > -1)
                parameters.set(GlassCamApplication.getStringFromRessources(R.string.manual_focus_pos_type), manualFocusType +"");

            ((TypedSettingMode) settingsManager.get(key)).set(stringvalues[currentInt]);
            parameters.set(key_value, stringvalues[currentInt]);
            Log.d(TAG, "Set "+ key_value +" to : " + stringvalues[currentInt]);
            ((ParametersHandler) cameraUiWrapper.getParameterHandler()).SetParametersToCamera(parameters);
        }
    }

}
