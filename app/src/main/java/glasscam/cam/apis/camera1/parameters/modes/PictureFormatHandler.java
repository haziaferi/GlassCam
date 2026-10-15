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
import glasscam.cam.apis.camera1.parameters.ParametersHandler;
import glasscam.settings.Frameworks;
import glasscam.settings.SettingKeys;
import glasscam.utils.Log;

/**
 * Created by troop on 05.02.2016.
 */
public class PictureFormatHandler extends BaseModeParameter
{
    private final String TAG = PictureFormatHandler.class.getSimpleName();
    private String captureMode = GlassCamApplication.getStringFromRessources(R.string.jpeg_);
    private String rawFormat;

    private String[] rawFormats;


    /***
     * @param parameters   Hold the Camera Parameters
     * @param cameraUiWrapper Hold the camera object
     */
    public PictureFormatHandler(Parameters parameters, CameraWrapperInterface cameraUiWrapper, ParametersHandler parametersHandler)
    {
        super(parameters, cameraUiWrapper,SettingKeys.PICTURE_FORMAT);
        settingsManager.get(SettingKeys.PICTURE_FORMAT).isSupported();
        setViewState(ViewState.Visible);
        boolean rawpicformatsupported = settingsManager.get(SettingKeys.RAW_PICTURE_FORMAT_SETTING).isSupported();
        boolean dngprofilessupported = settingsManager.getDngProfilesMap() != null && settingsManager.getDngProfilesMap().size() > 0;
        boolean rawSupported = rawpicformatsupported || dngprofilessupported;
        if (rawSupported) {
            rawFormat = settingsManager.get(SettingKeys.RAW_PICTURE_FORMAT_SETTING).get();
            rawFormats = settingsManager.get(SettingKeys.RAW_PICTURE_FORMAT_SETTING).getValues();
            BayerFormat bayerFormats = new BayerFormat(parameters, cameraUiWrapper, "");
            if (bayerFormats.getStringValues() != null && bayerFormats.getStringValues().length > 0)
                bayerFormats.setViewState(ViewState.Visible);
            if (TextUtils.isEmpty(rawFormat) && rawFormats != null && rawFormats.length >0)
                rawFormat = rawFormats[0];
            parametersHandler.add(SettingKeys.BAYERFORMAT, bayerFormats);
            settingsManager.get(SettingKeys.RAW_PICTURE_FORMAT_SETTING).setIsSupported((rawFormats != null && rawFormats.length > 0) || settingsManager.getFrameWork() == Frameworks.MTK);
            if (!contains(settingsManager.get(SettingKeys.RAW_PICTURE_FORMAT_SETTING).getValues(), GlassCamApplication.getStringFromRessources(R.string.dng_))
                    && dngprofilessupported)
            settingsManager.get(SettingKeys.PICTURE_FORMAT).setValues(new String[]
                        {
                                GlassCamApplication.getStringFromRessources(R.string.jpeg_),
                                GlassCamApplication.getStringFromRessources(R.string.dng_),
                                GlassCamApplication.getStringFromRessources(R.string.bayer_)
                        });
        }
        Log.d(TAG, "rawsupported:" + rawSupported + "isSupported:"+ (getViewState() == ViewState.Visible));
    }

    private boolean contains(String[] arr, String tofind)
    {
        for (int i = 0; i< arr.length; i++)
        {
            if (arr[i].equals(tofind))
                return true;
        }
        return false;
    }

    @Override
    public void setStringValue(String valueToSet, boolean setToCam)
    {
        Log.d(TAG, "SetValue:" + valueToSet);
        settingsManager.get(SettingKeys.PICTURE_FORMAT).set(valueToSet);
        captureMode = valueToSet;
        if (settingsManager.getFrameWork() != Frameworks.MTK)
        {
            if (valueToSet.equals(GlassCamApplication.getStringFromRessources(R.string.jpeg_)))
                setString(valueToSet,setToCam);
            else if(valueToSet.equals(GlassCamApplication.getStringFromRessources(R.string.bayer_)))
            {
                setString(rawFormat,setToCam);
            }
            else if(valueToSet.equals(GlassCamApplication.getStringFromRessources(R.string.dng_)))
            {
                setString(rawFormat,setToCam);
            }
        }
        fireStringValueChanged(valueToSet);
    }

    private void setString(String val, boolean setTocam)
    {
        Log.d(TAG, "setApiString:" +val);
        parameters.set(GlassCamApplication.getStringFromRessources(R.string.picture_format), val);
        if (setTocam)
            ((ParametersHandler) cameraUiWrapper.getParameterHandler()).SetParametersToCamera(parameters);
    }


    @Override
    public String getStringValue() {
        return captureMode;
    }

    @Override
    public String[] getStringValues()
    {
        return settingsManager.get(SettingKeys.PICTURE_FORMAT).getValues();
    }

    @Override
    public void onModuleChanged(String module)
    {
        if (module.equals(GlassCamApplication.getStringFromRessources(R.string.module_video)))
        {
            setViewState(ViewState.Hidden);
        }
        else
        {
            setViewState(ViewState.Visible);
        }
    }

    public class BayerFormat extends BaseModeParameter
    {

        /***
         * @param parameters   Hold the Camera Parameters
         * @param cameraHolder Hold the camera object
         * @param values
         */
        public BayerFormat(Parameters parameters, CameraWrapperInterface cameraHolder, String values) {
            super(parameters, cameraHolder,SettingKeys.BAYERFORMAT);
        }

        @Override
        public String getStringValue()
        {
            return rawFormat;
        }

        @Override
        public String[] getStringValues() {
            return rawFormats;
        }

        @Override
        public ViewState getViewState() {
            if ( rawFormats != null && rawFormats.length>0)
                return ViewState.Visible;
            else
                return ViewState.Hidden;
        }

        @Override
        public void setStringValue(String valueToSet, boolean setToCam)
        {
            rawFormat = valueToSet;
            if (captureMode.equals(GlassCamApplication.getStringFromRessources(R.string.bayer_))|| captureMode.equals(GlassCamApplication.getStringFromRessources(R.string.dng_))) {
                PictureFormatHandler.this.setStringValue(captureMode, true);
            }
        }
    }
}
