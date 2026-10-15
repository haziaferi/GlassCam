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

package glasscam;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import java.io.File;

import glasscam.image.ImageManager;
import glasscam.settings.SettingsManager;
import glasscam.utils.HideNavBarHelper;
import glasscam.utils.Log;
import glasscam.utils.PermissionManager;

/**
 * Created by troop on 28.03.2016.
 */
public abstract class ActivityAbstract extends AppCompatActivity implements ActivityInterface {


    private static Activity context;

    /** Live ActivityAbstract instances (onCreate..onDestroy), counted on the main thread. The
     *  image queues and the settings store are process-wide singletons, so only the last activity
     *  to go may cancel or release them (fresh-eyes 2026-09-28: the camera's onDestroy used to
     *  reset the settings under a still-open GlassGallery, and a closing gallery cancelled the
     *  camera's pending saves). */
    private static int liveActivities;

    /** Custom Views get handed whatever Context inflated them, which may be a
     *  ContextThemeWrapper rather than the Activity itself. Walk the wrapper chain
     *  to find it - same thing Hilt's generated View injection used to do. */
    public static ActivityAbstract from(android.content.Context context)
    {
        while (context instanceof android.content.ContextWrapper) {
            if (context instanceof ActivityAbstract)
                return (ActivityAbstract) context;
            context = ((android.content.ContextWrapper) context).getBaseContext();
        }
        if (context instanceof ActivityAbstract)
            return (ActivityAbstract) context;
        throw new IllegalStateException("Context does not wrap an ActivityAbstract: " + context);
    }

    /** The current foreground Activity's dependency container - used by static
     *  accessors (in this class and subclasses) that hand a dependency to non-UI
     *  classes that can't hold a reference to the Activity themselves. */
    protected static ActivityContainer currentContainer()
    {
        return ((ActivityAbstract) context).container;
    }

    public static PermissionManager permissionManager()
    {
        return currentContainer().permissionManager();
    }

    private final boolean forceLogging = false;

    private final String TAG = ActivityAbstract.class.getSimpleName();

    private I_OnActivityResultCallback resultCallback;
    private HideNavBarHelper hideNavBarHelper;

    /** Manual per-Activity dependency container - see ActivityContainer. Replaces Hilt's ActivityComponent. */
    public ActivityContainer container;
    protected PermissionManager permissionManager;
    protected SettingsManager settingsManager;
    protected ImageManager imageManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        liveActivities++;
        context = this;
        container = new ActivityContainer(this);
        permissionManager = container.permissionManager();
        settingsManager = GlassCamApplication.settingsManager();
        imageManager = GlassCamApplication.imageManager();
        setContentToView();
        Log.d(TAG,"onCreate");
        hideNavBarHelper = new HideNavBarHelper();
        if (!settingsManager.isInit()) {
            settingsManager.init();
        }
        Log.d(TAG,"onCreatePermissionGranted");
        File log = new File(getApplicationContext().getExternalFilesDir(null)+ "/log.txt");
        if (!forceLogging) {
            if (!Log.isLogToFileEnable() && log.exists()) {
                new Log();
            }
        }
        else
        {
            new Log();
        }
        Log.d(TAG, "initOnCreate()");

    }

    protected abstract void setContentToView();

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
    }

    @Override
    protected void onDestroy() {
        if (--liveActivities <= 0) {   // shared singletons: torn down by the last activity only
            liveActivities = 0;
            imageManager.cancelImageSaveTasks();
            imageManager.cancelImageLoadTasks();
            settingsManager.release();
        }
        if (context == this)      // never clear another activity's claim
            context = null;
        container = null;
        super.onDestroy();
        /*if (Log.isLogToFileEnable())
            Log.destroy();*/
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Batch 3 (2026-09-16): `context` is static and every ActivityAbstract's onCreate overwrote
        // it, so after a GlassGallery visit currentContainer() returned the *viewer's* container -
        // whose PreviewController has no preview. A Camera1 built after that got a null preview
        // (NPE in PictureModule.createPreview, no camera UI). The foreground activity is current.
        context = this;
    }

    @Override
    protected void onPause()
    {
        super.onPause();
        Log.flush();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus)
            hideNavBarHelper.HIDENAVBAR(getWindow());
        else
            hideNavBarHelper.showNavbar(getWindow());
    }

    public void ChooseSDCard(I_OnActivityResultCallback callback)
    {
        try {
            resultCallback = callback;
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
            startActivityForResult(intent, READ_REQUEST_CODE);
        }
        catch(ActivityNotFoundException activityNotFoundException)
        {
            Log.WriteEx(activityNotFoundException);

        }
    }

    private final int READ_REQUEST_CODE = 42;
    public static final int DELETE_REQUEST_CODE = 433;

    @TargetApi(VERSION_CODES.KITKAT)
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == READ_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            // The document selected by the user won't be returned in the intent.
            // Instead, a URI to that document will be contained in the return intent
            // provided to this method as a parameter.
            // Pull that URI using resultData.getData().
            Uri uri = null;
            if (data != null) {
                uri = data.getData();
                int takeFlags = data.getFlags()
                        & (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                // Check for the freshest data.


                getContentResolver().takePersistableUriPermission(uri, takeFlags);
                settingsManager.SetBaseFolder(uri.toString());
                if (resultCallback != null) {
                    resultCallback.onActivityResultCallback(uri);
                    resultCallback = null;
                }
            }
        }
        else if (requestCode == READ_REQUEST_CODE && resultCallback != null) {
            // cancelled (or no data): the caller still has to hear back, with no folder
            resultCallback.onActivityResultCallback(null);
            resultCallback = null;
        }
    }

}
