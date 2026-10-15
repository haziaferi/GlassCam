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
import android.text.TextUtils;

import com.hazia.glasscam.R;

import glasscam.GlassCamApplication;
import glasscam.cam.apis.basecamera.CameraWrapperInterface;
import glasscam.settings.SettingKeys;

/**
 * Created by Ar4eR on 10.12.15.
 */
public class VideoStabilizationParameter extends  BaseModeParameter {
    private final String[] vs_values = {GlassCamApplication.getStringFromRessources(R.string.true_), GlassCamApplication.getStringFromRessources(R.string.false_)};
    public VideoStabilizationParameter(Parameters parameters, CameraWrapperInterface parameterChanged)
    {
        super(parameters, parameterChanged, SettingKeys.VIDEO_STABILIZATION);
        if (parameters.get(GlassCamApplication.getStringFromRessources(R.string.video_stabilization_supported)) != null
                && parameters.get(GlassCamApplication.getStringFromRessources(R.string.video_stabilization_supported)).equals(GlassCamApplication.getStringFromRessources(R.string.true_)))
        {
            setViewState(ViewState.Visible);
            key_value = GlassCamApplication.getStringFromRessources(R.string.video_stabilization);
        }

    }

    @Override
    public String[] getStringValues() {
        return vs_values;
    }

    @Override
    public String getStringValue()
    {
        String vs = parameters.get(GlassCamApplication.getStringFromRessources(R.string.video_stabilization));
        if (vs != null && !TextUtils.isEmpty(vs))
            return vs;
        else
            return "error";
    }

}