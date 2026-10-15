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

package glasscam.cam;


import android.os.Bundle;
import android.view.KeyEvent;

import com.hazia.glasscam.R;
import com.hazia.glasscam.R.layout;

import glasscam.ActivityAbstract;
import glasscam.GlassCamApplication;
import glasscam.cam.apis.CameraApiManager;
import glasscam.cam.histogram.HistogramController;
import glasscam.cam.previewpostprocessing.PreviewController;
import glasscam.cam.ui.KeyPressedController;
import glasscam.cam.ui.SecureCamera;
import glasscam.cam.ui.ThemeManager;
import glasscam.cam.ui.handler.UserMessageHandler;
import glasscam.settings.SettingKeys;
import glasscam.settings.mode.SettingMode;
import glasscam.utils.LocationManager;
import glasscam.utils.Log;
import glasscam.utils.OrientationManager;
import glasscam.utils.PermissionManager;
import glasscam.utils.SoundPlayer;

/**
 * Created by troop on 18.08.2014.
 */
public class ActivityGlassCamMain extends ActivityAbstract
        implements
            SecureCamera.SecureCameraActivity
{

    /*
        provide the current Activity's dependencies to non-UI classes
     */
    public static LocationManager locationManager()
    {
        return currentContainer().locationManager();
    }
    /*
        provide the current Activity's dependencies to non-UI classes
     */
    public static OrientationManager orientationManager()
    {
        return currentContainer().orientationManager();
    }

    public static PreviewController previewController()
    {
        return currentContainer().previewController();
    }

    public static CameraApiManager cameraApiManager()
    {
        return currentContainer().cameraApiManager();
    }

    public static UserMessageHandler userMessageHandler()
    {
        return currentContainer().userMessageHandler();
    }

    public static SoundPlayer soundPlayer()
    {
        return currentContainer().soundPlayer();
    }

    public static HistogramController histogramController()
    {
        return currentContainer().histogramController();
    }

    public static ThemeManager themeManager()
    {
        return currentContainer().themeManager();
    }


    private final String TAG =ActivityGlassCamMain.class.getSimpleName();
    //listen to orientation changes
    OrientationManager orientationManager;

    private final SecureCamera mSecureCamera = new SecureCamera(this);

    public CameraApiManager cameraApiManager;

    LocationManager locationManager;
    KeyPressedController keyPressedController;
    ThemeManager themeManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(null);
        orientationManager = container.orientationManager();
        cameraApiManager = container.cameraApiManager();
        locationManager = container.locationManager();
        keyPressedController = container.keyPressedController();
        themeManager = container.themeManager();
        Log.d(TAG,"onCreate: ");
        // Batch 3 (2026-09-16): a cold start opens in Picture with burst 1. The module and burst
        // count used to persist, so a relaunch after an Af-Bracket session fired a 10-frame
        // bracket on the first shutter tap.
        if (savedInstanceState == null) {
            settingsManager.SetCurrentModule(GlassCamApplication.getStringFromRessources(R.string.module_picture));
            try {
                if (settingsManager.get(SettingKeys.M_BURST) != null)
                    settingsManager.get(SettingKeys.M_BURST).set("1");
            } catch (Exception ex) {
                Log.WriteEx(ex);
            }
        }
        getLifecycle().addObserver(locationManager);
        mSecureCamera.onCreate();
        cameraApiManager.init();

        //listen to phone orientation changes
        getLifecycle().addObserver(orientationManager);
        themeManager.setLayoutholderAndFragmentManager(R.id.MainLayout,getSupportFragmentManager());

        // BACK on the settings page used to leave the app (the drawer is page 0 of the pager and
        // nothing intercepted it); now it closes the drawer first.
        getOnBackPressedDispatcher().addCallback(this, new androidx.activity.OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (closeSettingsIfOpen())
                    return;
                setEnabled(false);
                getOnBackPressedDispatcher().onBackPressed();
                setEnabled(true);
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG,"onDestroy");
        cameraApiManager.destroy();
        getLifecycle().removeObserver(locationManager);
        getLifecycle().removeObserver(orientationManager);
    }

    @Override
    protected void setContentToView() {
        setContentView(layout.glasscam_main_activity);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (permissionManager.isPermissionGranted(PermissionManager.Permissions.Camera)) {
            if (mSecureCamera !=  null)
                mSecureCamera.onResume();
        }
        else if (permissionManager.isPermissionPermanentlyDenied(PermissionManager.Permissions.Camera)) {
            // Don't keep re-showing a request the system will silently deny - explain
            // once per resume instead so the user knows why the camera never appears.
            userMessageHandler().sendMSG(getString(R.string.camera_permission_denied_settings), true);
        }
        else
            permissionManager.requestPermission(PermissionManager.Permissions.Camera);
    }

    @Override
    public void onResumeTasks() {
        Log.d(TAG, "onResumeTasks() ");
        if (!settingsManager.isInit())
            settingsManager.init();
        SettingMode mode = settingsManager.getGlobal(SettingKeys.THEME);
        String theme = ThemeManager.NEXTGEN;
        if (mode != null) {
            if (mode.get() != null && !mode.get().equals(""))
                theme = mode.get();
        }
        themeManager.changeTheme(theme);
    }

    @Override
    protected void onPause()
    {
        super.onPause();
        Log.d(TAG, "onPause() ");
        // forward to secure camera to handle resume bug
        if (mSecureCamera != null)
            mSecureCamera.onPause();
    }

    @Override
    public void onPauseTasks() {
        settingsManager.save();
        Log.d(TAG, "onPauseTasks() ");
        if(orientationManager != null)
            orientationManager.Stop();
    }




    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyPressedController.onKeyDown(keyCode,event))
            return true;
        return super.onKeyDown(keyCode,event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {

        if (keyPressedController.onKeyUp(keyCode,event))
            return true;
        if (keyCode == KeyEvent.KEYCODE_BACK || keyCode == KeyEvent.KEYCODE_HOME)
        {
            // the hardware/adb BACK key used to bypass the back dispatcher (gesture BACK never
            // did), so the settings page could only be closed by gesture; route both the same way
            if (keyCode == KeyEvent.KEYCODE_BACK && closeSettingsIfOpen())
                return true;
            closeActivity();
            return true;
        }
        return super.onKeyUp(keyCode, event);
    }

    @Override
    public boolean onKeyMultiple(int keyCode, int repeatCount, KeyEvent event) {
        keyPressedController.onKeyMultiple(keyCode,repeatCount,event);
        return super.onKeyMultiple(keyCode, repeatCount, event);
    }

    @Override
    public boolean onKeyLongPress(int keyCode, KeyEvent event) {
        if(keyPressedController.onKeyLongPressed(keyCode,event))
            return true;
        return super.onKeyLongPress(keyCode, event);
    }

    public void closeActivity()
    {
        moveTaskToBack(true);
    }

    /** True when the settings page was showing and has been sent away. */
    private boolean closeSettingsIfOpen() {
        androidx.fragment.app.Fragment f = getSupportFragmentManager().findFragmentById(R.id.MainLayout);
        return f instanceof glasscam.cam.ui.themenextgen.NextGenMainFragment
                && ((glasscam.cam.ui.themenextgen.NextGenMainFragment) f).closeSettingsIfOpen();
    }







}
