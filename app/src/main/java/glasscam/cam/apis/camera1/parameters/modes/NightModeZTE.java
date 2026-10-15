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

package glasscam.cam.apis.camera1.parameters.modes;

import android.hardware.Camera.Parameters;

import com.hazia.glasscam.R;

import glasscam.GlassCamApplication;
import glasscam.cam.apis.basecamera.CameraWrapperInterface;
import glasscam.cam.apis.camera1.parameters.ParametersHandler;
import glasscam.settings.SettingKeys;

/**
 * Created by troop on 26.09.2014.
 */
public class NightModeZTE extends BaseModeParameter
{
    final String TAG = NightModeZTE.class.getSimpleName();
    private final boolean visible = true;
    private final String state = "";
    private final String curmodule = "";
    public NightModeZTE(Parameters parameters, CameraWrapperInterface cameraUiWrapper) {
        super(parameters, cameraUiWrapper,SettingKeys.NIGHT_MODE);
        setViewState(ViewState.Visible);
        //cameraUiWrapper.getParameterHandler().get(SettingKeys.PictureFormat).addEventListner(this);
    }

    @Override
    public void setStringValue(String valueToSet, boolean setToCam)
    {
        parameters.set(GlassCamApplication.getStringFromRessources(R.string.night_key), valueToSet);
        ((ParametersHandler) cameraUiWrapper.getParameterHandler()).SetParametersToCamera(parameters);
        fireStringValueChanged(valueToSet);

    }

    @Override
    public String getStringValue() {
            return parameters.get(GlassCamApplication.getStringFromRessources(R.string.night_key));
    }

    @Override
    public String[] getStringValues() {
        return new String[] {
                GlassCamApplication.getStringFromRessources(R.string.off_),
                GlassCamApplication.getStringFromRessources(R.string.on_),
                GlassCamApplication.getStringFromRessources(R.string.night_mode_tripod)};
    }
}