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


import com.hazia.glasscam.R;

import glasscam.GlassCamApplication;
import glasscam.cam.ActivityGlassCamMain;
import glasscam.cam.apis.basecamera.CameraWrapperInterface;
import glasscam.cam.apis.basecamera.parameters.AbstractParameter;
import glasscam.cam.previewpostprocessing.PreviewController;
import glasscam.cam.previewpostprocessing.PreviewPostProcessingModes;
import glasscam.settings.SettingKeys;
import glasscam.settings.mode.ApiBooleanSettingMode;
import glasscam.settings.mode.BooleanSettingModeInterface;
import glasscam.settings.mode.SettingMode;

/**
 * Created by troop on 10.09.2015.
 */
public class FocusPeakMode extends AbstractParameter implements BooleanSettingModeInterface {
    protected PreviewController previewController;

    protected SettingKeys.Key<ApiBooleanSettingMode> settingMode;
    public FocusPeakMode(CameraWrapperInterface cameraUiWrapper, SettingKeys.Key<ApiBooleanSettingMode> settingMode) {
        super(cameraUiWrapper, settingMode);
        previewController = ActivityGlassCamMain.previewController();
        this.settingMode = settingMode;
    }

    @Override
    public String getStringValue()
    {
        if (!settingsManager.get(settingMode).get())
            return GlassCamApplication.getStringFromRessources(R.string.off_);
        return GlassCamApplication.getStringFromRessources(R.string.on_);
    }

    @Override
    public ViewState getViewState() {
        if (settingsManager != null) {
            SettingMode settingMode = settingsManager.get(SettingKeys.PREVIEW_POST_PROCESSING_MODE);
            if (settingMode != null) {
                String s = settingsManager.get(SettingKeys.PREVIEW_POST_PROCESSING_MODE).get();
                if (s != null && !s.equals(PreviewPostProcessingModes.off.name()))
                    return ViewState.Visible;
            }
        }
        return ViewState.Hidden;
    }

    @Override
    public void setStringValue(String valueToSet, boolean setToCamera)
    {
        currentString = valueToSet;
        boolean toset = valueToSet.equals(GlassCamApplication.getStringFromRessources(R.string.on_));
        previewController.setFocusPeak(toset);
        settingsManager.get(settingMode).set(toset);
        fireStringValueChanged(valueToSet);

    }

    @Override
    public String[] getStringValues() {
        return new String[] { GlassCamApplication.getStringFromRessources(R.string.off_), GlassCamApplication.getStringFromRessources(R.string.on_) };
    }

    @Override
    public boolean get() {
        return settingsManager.get(settingMode).get();
    }

    @Override
    public void set(boolean bool) {
        previewController.setFocusPeak(bool);
        settingsManager.get(settingMode).set(bool);
        fireStringValueChanged(getStringValue());
    }
}
