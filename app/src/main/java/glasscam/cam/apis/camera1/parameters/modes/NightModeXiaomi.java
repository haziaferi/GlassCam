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

import android.hardware.Camera;

import com.hazia.glasscam.R;

import glasscam.GlassCamApplication;
import glasscam.cam.apis.basecamera.CameraWrapperInterface;
import glasscam.cam.apis.camera1.parameters.ParametersHandler;
import glasscam.settings.SettingKeys;

/**
 * Created by troop on 10.06.2016.
 */
public class NightModeXiaomi extends BaseModeParameter
{
    final String TAG = NightModeZTE.class.getSimpleName();
    private boolean visible = true;
    private String state = "";
    private final String format = "";
    private String curmodule = "";

    public NightModeXiaomi(Camera.Parameters parameters, CameraWrapperInterface cameraUiWrapper) {
        super(parameters, cameraUiWrapper,SettingKeys.NIGHT_MODE);
        if(parameters.get(GlassCamApplication.getStringFromRessources(R.string.morpho_hht)) != null
                && parameters.get(GlassCamApplication.getStringFromRessources(R.string.ae_bracket_hdr)) != null) {
            setViewState(ViewState.Visible);
            //cameraUiWrapper.getParameterHandler().get(SettingKeys.PictureFormat).addEventListner(this);
        }
    }

    @Override
    public void setStringValue(String valueToSet, boolean setToCam)
    {
        if (valueToSet.equals(GlassCamApplication.getStringFromRessources(R.string.on_))) {
            parameters.set(GlassCamApplication.getStringFromRessources(R.string.morpho_hdr), GlassCamApplication.getStringFromRessources(R.string.false_));
            cameraUiWrapper.getParameterHandler().get(SettingKeys.HDR_MODE).fireStringValueChanged(GlassCamApplication.getStringFromRessources(R.string.off_));
            parameters.set("capture-burst-exposures","-10,0,10");
            parameters.set(GlassCamApplication.getStringFromRessources(R.string.ae_bracket_hdr), GlassCamApplication.getStringFromRessources(R.string.ae_bracket_hdr_values_aebracket));
            parameters.set(GlassCamApplication.getStringFromRessources(R.string.morpho_hht), GlassCamApplication.getStringFromRessources(R.string.true_));
        } else {
            parameters.set(GlassCamApplication.getStringFromRessources(R.string.ae_bracket_hdr), GlassCamApplication.getStringFromRessources(R.string.ae_bracket_hdr_values_aebracket));
            parameters.set(GlassCamApplication.getStringFromRessources(R.string.morpho_hht), GlassCamApplication.getStringFromRessources(R.string.false_));
        }
        ((ParametersHandler) cameraUiWrapper.getParameterHandler()).SetParametersToCamera(parameters);
        fireStringValueChanged(valueToSet);

    }

    @Override
    public String getStringValue()
    {
        if (parameters.get(GlassCamApplication.getStringFromRessources(R.string.morpho_hht)).equals(GlassCamApplication.getStringFromRessources(R.string.true_))
                && parameters.get(GlassCamApplication.getStringFromRessources(R.string.ae_bracket_hdr)).equals(GlassCamApplication.getStringFromRessources(R.string.ae_bracket_hdr_values_off)))
            return GlassCamApplication.getStringFromRessources(R.string.on_);
        else
            return GlassCamApplication.getStringFromRessources(R.string.off_);
    }

    @Override
    public String[] getStringValues()
    {
       return new String[] {GlassCamApplication.getStringFromRessources(R.string.off_),GlassCamApplication.getStringFromRessources(R.string.on_)};
    }

    @Override
    public void onModuleChanged(String module)
    {
        curmodule = module;
        if (curmodule.equals(GlassCamApplication.getStringFromRessources(R.string.module_video))|| curmodule.equals(GlassCamApplication.getStringFromRessources(R.string.module_hdr)))
            Hide();
        else
        {
            if (format.contains(GlassCamApplication.getStringFromRessources(R.string.jpeg_))) {
                Show();
                setViewState(ViewState.Visible);
            }
        }
    }

    /*@Override
    public void onViewStateChanged(ViewState value) {

    }

    @Override
    public void onIntValueChanged(int current) {

    }

    @Override
    public void onValuesChanged(String[] values) {

    }

    @Override
    public void onStringValueChanged(String val) {
        format = val;
        if (val.contains(GlassCamApplication.getStringFromRessources(R.string.jpeg_))&&!visible &&!curmodule.equals(GlassCamApplication.getStringFromRessources(R.string.module_hdr)))
            Show();

        else if (!val.contains(GlassCamApplication.getStringFromRessources(R.string.jpeg_))&& visible) {
            Hide();
        }
    }*/

    private void Hide()
    {
        state = getStringValue();
        visible = false;
        setStringValue(GlassCamApplication.getStringFromRessources(R.string.off_),true);
        fireStringValueChanged(GlassCamApplication.getStringFromRessources(R.string.off_));
        setViewState(ViewState.Hidden);
    }

    private void Show()
    {
        visible = true;
        setStringValue(state,true);
        fireStringValueChanged(state);
        setViewState(ViewState.Visible);
    }
}
