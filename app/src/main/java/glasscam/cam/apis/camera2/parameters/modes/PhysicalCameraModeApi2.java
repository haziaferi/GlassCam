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

package glasscam.cam.apis.camera2.parameters.modes;

import com.hazia.glasscam.R;

import glasscam.GlassCamApplication;
import glasscam.cam.apis.basecamera.CameraThreadHandler;
import glasscam.cam.apis.camera2.Camera2;
import glasscam.cam.apis.featuredetector.camera2.PhysicalCameraDetector;
import glasscam.settings.SettingKeys;
import glasscam.utils.Log;

/**
 * Pins capture+preview to one physical sensor of a logical multi-camera
 * (OutputConfiguration.setPhysicalCameraId under the hood, applied in
 * CaptureSessionHandler) instead of letting the vendor's logical-camera fusion
 * choose. Values are "<physical id> (<focal length>mm)" as built by
 * PhysicalCameraDetector - "Auto" clears the pin and restores default behaviour.
 *
 * Changing this requires rebuilding the capture session's output configurations,
 * same as picture size - see PictureSizeModeApi2/SecondarySensorSizeModeApi2 for
 * the identical restartPreviewAsync() pattern.
 */
public class PhysicalCameraModeApi2 extends BaseModeApi2 {

    private final String TAG = PhysicalCameraModeApi2.class.getSimpleName();
    private String current;

    public PhysicalCameraModeApi2(Camera2 cameraUiWrapper) {
        super(cameraUiWrapper, SettingKeys.PHYSICAL_CAMERA_ID);
        setViewState(ViewState.Visible);
        current = settingsManager.get(SettingKeys.PHYSICAL_CAMERA_ID).get();
        applyToSessionHandler(current);
    }

    @Override
    public void setStringValue(String valueToSet, boolean setToCamera)
    {
        fireStringValueChanged(valueToSet);
        settingsManager.get(SettingKeys.PHYSICAL_CAMERA_ID).set(valueToSet);
        current = valueToSet;
        applyToSessionHandler(valueToSet);
        if (setToCamera)
        {
            CameraThreadHandler.restartPreviewAsync();
        }
    }

    @Override
    public String getStringValue()
    {
        return current;
    }

    @Override
    public String[] getStringValues()
    {
        return settingsManager.get(SettingKeys.PHYSICAL_CAMERA_ID).getValues();
    }

    private void applyToSessionHandler(String value)
    {
        String autoLabel = GlassCamApplication.getStringFromRessources(R.string.physicalcamera_auto);
        if (value == null || value.equals(autoLabel)) {
            cameraUiWrapper.captureSessionHandler.SetPhysicalCameraId(null);
            return;
        }
        String physicalId = PhysicalCameraDetector.physicalIdForLabel(value);
        if (physicalId == null) {
            // Map miss shouldn't normally happen (the detector populates it before
            // this class ever sees these values), but fall back to the old
            // space-split rather than silently pinning nothing.
            Log.w(TAG, "no mapped physical id for label \"" + value + "\", falling back to label parsing");
            int space = value.indexOf(' ');
            physicalId = space > 0 ? value.substring(0, space) : value;
        }
        cameraUiWrapper.captureSessionHandler.SetPhysicalCameraId(physicalId);
    }
}
