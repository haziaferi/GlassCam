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

package glasscam.cam.apis.camera1.parameters.manual.whitebalance;

import android.hardware.Camera.Parameters;
import android.os.Handler;
import android.text.TextUtils;

import com.hazia.glasscam.R;

import java.util.ArrayList;
import java.util.Arrays;

import glasscam.GlassCamApplication;
import glasscam.cam.apis.basecamera.CameraWrapperInterface;
import glasscam.cam.apis.basecamera.parameters.ParameterInterface;
import glasscam.cam.apis.camera1.CameraHolder;
import glasscam.cam.apis.camera1.parameters.ParametersHandler;
import glasscam.cam.apis.camera1.parameters.manual.BaseManualParameter;
import glasscam.settings.SettingKeys;
import glasscam.utils.Log;

/**
 * Created by Ingo on 06.03.2016.
 */
public class BaseCCTManual extends BaseManualParameter
{
    private final String TAG = BaseCCTManual.class.getSimpleName();

    private final String manual_WbMode;


    public BaseCCTManual(final Parameters parameters,final CameraWrapperInterface cameraUiWrapper,SettingKeys.Key settingMode) {
        super(parameters,cameraUiWrapper, settingMode);
        manual_WbMode = settingsManager.get(SettingKeys.M_WHITEBALANCE).getMode();
        setViewState(ViewState.Hidden);

        //wait 800ms to give awb a chance to set the ct value to the parameters
        if (TextUtils.isEmpty(settingsManager.get(SettingKeys.M_WHITEBALANCE).getCamera1ParameterKEY()))
            new Handler().postDelayed(() -> {
                try {
                    //get fresh parameters from camera
                    Parameters parameters1 = ((CameraHolder) cameraUiWrapper.getCameraHolder()).GetCameraParameters();
                    String wbcur = "";
                    //lookup if ct value is avail
                    if (parameters1.get(GlassCamApplication.getStringFromRessources(R.string.wb_current_cct)) != null)
                        wbcur = GlassCamApplication.getStringFromRessources(R.string.wb_current_cct);
                    else if (parameters1.get(GlassCamApplication.getStringFromRessources(R.string.wb_cct)) != null)
                        wbcur = GlassCamApplication.getStringFromRessources(R.string.wb_cct);
                    else if (parameters1.get(GlassCamApplication.getStringFromRessources(R.string.wb_ct)) != null)
                        wbcur = GlassCamApplication.getStringFromRessources(R.string.wb_ct);
                    else if (parameters1.get(GlassCamApplication.getStringFromRessources(R.string.wb_manual_cct)) != null)
                        wbcur = GlassCamApplication.getStringFromRessources(R.string.wb_manual_cct);
                    else if (parameters1.get(GlassCamApplication.getStringFromRessources(R.string.manual_wb_value)) != null)
                        wbcur = GlassCamApplication.getStringFromRessources(R.string.manual_wb_value);
                    if (!TextUtils.isEmpty(wbcur)) {
                        //update our stored parameters with ct
                        parameters.set(wbcur, parameters1.get(wbcur));
                        setViewState(ViewState.Visible);
                        key_value = wbcur;
                    }
                }
                catch (NullPointerException ex)
                {
                    setViewState(ViewState.Hidden);
                }
            }, 800);
        else
            key_value = settingsManager.get(SettingKeys.M_WHITEBALANCE).getCamera1ParameterKEY();
    }
    
    @Override
    public void setValue(int valueToSet, boolean setToCamera) {
        currentInt = valueToSet;
        //set to auto
        if (currentInt == 0) {
            set_to_auto();
        } else //set manual wb mode and key_value
        {
            set_manual();
        }
        try {
            ((ParametersHandler) cameraUiWrapper.getParameterHandler()).SetParametersToCamera(parameters);
        }
        catch (RuntimeException ex)
        {
            Log.WriteEx(ex);
            setViewState(ViewState.Hidden);
        }

    }

    protected void set_manual()
    {
        ParameterInterface wbm = cameraUiWrapper.getParameterHandler().get(SettingKeys.WHITE_BALANCE_MODE);
        Log.d(TAG, Arrays.toString(wbm.getStringValues()));
        try {
            if (parameters.get("whitebalance-values").contains("manual") && parameters.get("manual-wb-modes").contains("color-temperature")) {

                wbm.setStringValue(manual_WbMode, true);
                parameters.set(GlassCamApplication.getStringFromRessources(R.string.manual_wb_type), 0);
                parameters.set(GlassCamApplication.getStringFromRessources(R.string.manual_wb_value), stringvalues[currentInt]);
                Log.d(TAG, "NEW");

            } else {
                if (!wbm.getStringValue().equals(manual_WbMode) && !TextUtils.isEmpty(manual_WbMode))
                    wbm.setStringValue(manual_WbMode, true);
                parameters.set(key_value, stringvalues[currentInt]);
                Log.d(TAG, "OLD");
            }
        }
        catch (Exception err )
        {
            if (!wbm.getStringValue().equals(manual_WbMode) && !TextUtils.isEmpty(manual_WbMode))
                wbm.setStringValue(manual_WbMode, true);
            parameters.set(key_value, stringvalues[currentInt]);
            err.printStackTrace();
        }
        Log.d(TAG, "Set "+ key_value +" to : " + stringvalues[currentInt]);

    }

    protected void set_to_auto()
    {
        cameraUiWrapper.getParameterHandler().get(SettingKeys.WHITE_BALANCE_MODE).setStringValue("auto", true);
        Log.d(TAG, "Set  to : auto");
    }

    @Override
    protected String[] createStringArray(int min, int max, float step)
    {
        ArrayList<String> t = new ArrayList<>();
        t.add(GlassCamApplication.getStringFromRessources(R.string.auto_));
        for (int i = min; i<=max;i+=step)
        {
            t.add(i+"");
        }
        stringvalues = new String[t.size()];
        t.toArray(stringvalues);
        return stringvalues;
    }
}
