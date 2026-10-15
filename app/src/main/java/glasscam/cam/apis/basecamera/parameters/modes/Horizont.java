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

package glasscam.cam.apis.basecamera.parameters.modes;


import android.text.TextUtils;

import com.hazia.glasscam.R;

import glasscam.GlassCamApplication;
import glasscam.cam.apis.basecamera.parameters.AbstractParameter;
import glasscam.settings.SettingKeys;

/**
 * Created by Ar4eR on 14.01.16.
 */
public class Horizont extends AbstractParameter {

    private String value;

    public Horizont() {
        super(SettingKeys.HORIZONT_LVL);
        // Batch 2 (2026-09-16): the saved value never reached this parameter on start-up (the
        // global SettingMode is not flagged supported, so SetAppSettingsToParameters skips it),
        // so Image Level always came back "Off" after a relaunch.
        try {
            value = settingsManager.getGlobal(SettingKeys.HORIZONT_LVL).get();
        } catch (NullPointerException ignored) {
            value = null;
        }
    }

    @Override
    public ViewState getViewState() {
        return ViewState.Visible;
    }

    @Override
    public void setStringValue(String valueToSet, boolean setToCam)
    {
        value = valueToSet;
        settingsManager.getGlobal(SettingKeys.HORIZONT_LVL).set(valueToSet);
        fireStringValueChanged(valueToSet);
    }

    @Override
    public String getStringValue()
    {
        if (value == null || TextUtils.isEmpty(value))
            return GlassCamApplication.getStringFromRessources(R.string.off);
        else
            return value;
    }

    @Override
    public String[] getStringValues()
    {
        return new String[]{GlassCamApplication.getStringFromRessources(R.string.off), GlassCamApplication.getStringFromRessources(R.string.on)};
    }

}
