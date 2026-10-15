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

package glasscam.cam.ui.videoprofileeditor.views;

import android.R.id;
import android.os.Bundle;
import android.os.PersistableBundle;

import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentTransaction;

/**
 * Created by troop on 15.02.2016.
 */
public class VideoProfileEditorActivity extends FragmentActivity
{
    private final String TAG = VideoProfileEditorActivity.class.getSimpleName();

    public void onCreate(Bundle savedInstanceState, PersistableBundle persistentState) {

    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        if (getSupportFragmentManager().findFragmentByTag(TAG) == null) {
            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            ft.replace(id.content, new VideoProfileEditorFragment(), TAG);
            ft.commit();
        }
    }

    // Batch 3: immersive like the camera - this activity does not extend ActivityAbstract, so the
    // navigation bar stayed on screen (critique 2026-09-16, #12 leftover).
    private final glasscam.utils.HideNavBarHelper hideNavBarHelper = new glasscam.utils.HideNavBarHelper();

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus)
            hideNavBarHelper.HIDENAVBAR(getWindow());
        else
            hideNavBarHelper.showNavbar(getWindow());
    }
}
