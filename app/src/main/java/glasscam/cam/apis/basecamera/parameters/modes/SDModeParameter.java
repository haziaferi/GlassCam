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

import android.content.Context;
import android.net.Uri;

import androidx.documentfile.provider.DocumentFile;

import glasscam.ActivityAbstract;
import glasscam.ActivityInterface;
import glasscam.cam.ActivityGlassCamMain;
import glasscam.cam.apis.basecamera.parameters.AbstractParameter;
import glasscam.settings.SettingKeys;
import glasscam.utils.Log;

/**
 * Created by troop on 21.07.2015.
 *
 * The pre-KitKat "can we actually write to this path" probe and the pre-Lollipop
 * visibility check are gone - both branches were unreachable once minSdk is 30.
 * Choosing "External" always goes through the SAF folder picker now.
 */
public class SDModeParameter extends AbstractParameter  implements ActivityInterface.I_OnActivityResultCallback
{
    private static final String TAG = SDModeParameter.class.getSimpleName();
    public static final String internal = "Internal";
    public static final String external ="External";
    private String lastval;
    private Context context;

    public SDModeParameter() {
        super(SettingKeys.SD_SAVE_LOCATION);
    }

    public void setContext(Context context)
    {
        this.context = context;
    }


    @Override
    public ViewState getViewState() {
        return ViewState.Visible;
    }

    @Override
    public void setStringValue(String valueToSet, boolean setToCamera)
    {
        setValue(valueToSet,setToCamera);
    }

    @Override
    protected void setValue(String value, boolean setToCamera) {
        if (value.equals(SDModeParameter.external))
        {
            lastval = value;
            ActivityGlassCamMain activity = (ActivityGlassCamMain) ActivityAbstract.from(context);
            // the picker answers asynchronously: announce the result from the callback, not here
            // (listeners were told "External" even when the grant then failed; fresh-eyes 2026-09-28)
            activity.ChooseSDCard(this::onActivityResultCallback);
        } else {
            settingsManager.SetWriteExternal(false);
            fireStringValueChanged(value);
        }
    }

    @Override
    public String getStringValue()
    {
        if (settingsManager.GetWriteExternal())
            return external;
        else
            return internal;
    }

    @Override
    public String[] getStringValues() {
        return new String[] {internal, external};
    }

    //content://com.android.externalstorage.documents/document/59EC-12E2%3ADCIM%2FGlassCam%2Ftest.txt
    @Override
    public void onActivityResultCallback(Uri uri)
    {
        DocumentFile f = uri == null ? null : DocumentFile.fromTreeUri(context, uri);   // null: picker cancelled
        settingsManager.SetWriteExternal(f != null && f.canWrite() && SDModeParameter.external.equals(lastval));
        lastval = "";
        fireStringValueChanged(getStringValue());   // the state the grant actually produced
    }

}
