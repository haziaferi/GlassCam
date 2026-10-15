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

package glasscam.cam.ui.themenextgen.fragment;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import com.hazia.glasscam.R.id;
import com.hazia.glasscam.R.layout;
import com.hazia.glasscam.R.string;

import glasscam.GlassCamApplication;
import glasscam.settings.SettingsManager;

/**
 * Created by troop on 29.02.2016.
 * Ported from upstream FreeDcam (freed.cam.ui.themesample.cameraui.HelpFragment).
 * Adapted for this fork: uses GlassCamApplication.settingsManager() instead of
 * Hilt field injection, and references the standalone i_HelpFragment interface
 * instead of CameraUiFragment's nested one.
 */
public class HelpFragment extends Fragment
{
    private ImageView finger;
    private TextView description;
    private Button nextButton;
    private int helpState;
    private i_HelpFragment closer;
    private CheckBox dontshowagain;
    private SettingsManager settingsManager;

    public static HelpFragment getFragment(i_HelpFragment closer)
    {
        HelpFragment h = new HelpFragment();
        h.closer = closer;
        return h;
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        super.onCreateView(inflater,container,savedInstanceState);
        settingsManager = GlassCamApplication.settingsManager();
        View view = inflater.inflate(layout.help_fragment,container,false);
        finger = view.findViewById(id.imageView_finger);
        description = view.findViewById(id.textView_description);
        dontshowagain = view.findViewById(id.checkBox_dontShowAgain);
        dontshowagain.setVisibility(View.GONE);
        nextButton = view.findViewById(id.button_nextHelp);
        nextButton.setOnClickListener(v -> {
            helpState++;
            switch (helpState)
            {
                case 1: //close settings
                    showCloseSettingsMenu();
                    break;
                case 2: //open manual
                    showOpenManualMenu();
                    break;
                case 3: //close manual
                    showCloseManualMenu();
                    break;
                case 4:
                    // Batch 3: the walkthrough is a first-run thing - closing it is enough; the
                    // checkbox (kept for discoverability) can only confirm that.
                    settingsManager.setshowHelpOverlay(false);
                    closer.Close(HelpFragment.this);
            }
        });
        showOpenSettingsMenu();
        return view;
    }


    private void showOpenSettingsMenu()
    {
        TranslateAnimation animation = new TranslateAnimation(0.0f, 400.0f,
                0.0f, 0.0f);
        animation.setDuration(1000);
        animation.setRepeatCount(Animation.INFINITE);
        animation.setRepeatMode(Animation.INFINITE);
        animation.setFillAfter(true);
        finger.startAnimation(animation);
        description.setText(string.help_open_settings);
        description.setTextColor(Color.WHITE);
    }

    private void showCloseSettingsMenu()
    {
        TranslateAnimation animation = new TranslateAnimation(400.0f, 0.0f,
                0.0f, 0.0f);
        animation.setDuration(1000);
        animation.setRepeatCount(Animation.INFINITE);
        animation.setRepeatMode(Animation.INFINITE);
        animation.setFillAfter(true);
        finger.startAnimation(animation);
        description.setText(string.help_close_settings);
        description.setTextColor(Color.WHITE);
    }

    private void showOpenManualMenu()
    {
        TranslateAnimation animation = new TranslateAnimation(0.0f, 0.0f,
                400.0f, 0.0f);
        animation.setDuration(1000);
        animation.setRepeatCount(Animation.INFINITE);
        animation.setRepeatMode(Animation.INFINITE);
        animation.setFillAfter(true);
        finger.startAnimation(animation);
        description.setText(string.help_open_manuals);
        description.setTextColor(Color.WHITE);
    }

    private void showCloseManualMenu()
    {
        TranslateAnimation animation = new TranslateAnimation(0.0f, 0.0f,
                0.0f, 400.0f);
        animation.setDuration(1000);
        animation.setRepeatCount(Animation.INFINITE);
        animation.setRepeatMode(Animation.INFINITE);
        animation.setFillAfter(true);
        finger.startAnimation(animation);
        description.setText(string.help_close_manuals);
        description.setTextColor(Color.WHITE);
        nextButton.setText(string.help_close);
        dontshowagain.setVisibility(View.VISIBLE);
        dontshowagain.setTextColor(Color.WHITE);
    }
}
