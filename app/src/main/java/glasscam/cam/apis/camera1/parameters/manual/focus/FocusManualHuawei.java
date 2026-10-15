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

import com.hazia.glasscam.R;

import glasscam.GlassCamApplication;
import glasscam.cam.apis.basecamera.CameraWrapperInterface;
import glasscam.cam.apis.camera1.parameters.ParametersHandler;
import glasscam.settings.SettingKeys;
import glasscam.utils.Log;

/**
 * Created by GeorgeKiarie on 02/04/2016.
 */
public class FocusManualHuawei extends BaseFocusManual
{
    private final String TAG = FocusManualHuawei.class.getSimpleName();
    public FocusManualHuawei(Parameters parameters, CameraWrapperInterface cameraUiWrapper, SettingKeys.Key settingMode) {
        super(parameters,cameraUiWrapper,settingMode);
    }

    @Override
    public void setValue(int valueToSet, boolean setToCamera)
    {
        currentInt = valueToSet;

        if (valueToSet == 0)
        {
            parameters.set(GlassCamApplication.getStringFromRessources(R.string.hw_hwcamera_flag),GlassCamApplication.getStringFromRessources(R.string.on_));
            parameters.set(GlassCamApplication.getStringFromRessources(R.string.hw_manual_focus_mode),GlassCamApplication.getStringFromRessources(R.string.off_));
        }
        else
        {
            parameters.set(GlassCamApplication.getStringFromRessources(R.string.hw_hwcamera_flag),GlassCamApplication.getStringFromRessources(R.string.on_));
            parameters.set(GlassCamApplication.getStringFromRessources(R.string.hw_manual_focus_mode),GlassCamApplication.getStringFromRessources(R.string.on_));
            parameters.set(key_value, stringvalues[currentInt]);
            Log.d(TAG, "Set " + key_value + " to : " + stringvalues[currentInt]);
            ((ParametersHandler) cameraUiWrapper.getParameterHandler()).SetParametersToCamera(parameters);

        }
    }
}