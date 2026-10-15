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

package glasscam.cam.apis.basecamera.modules;


import android.os.Handler;
import android.os.Looper;

import glasscam.GlassCamApplication;
import glasscam.cam.ActivityGlassCamMain;
import glasscam.cam.apis.basecamera.CameraWrapperInterface;
import glasscam.cam.event.capture.CaptureStateChangedEventHandler;
import glasscam.cam.event.capture.CaptureStates;
import glasscam.file.FileListController;
import glasscam.file.holder.BaseHolder;
import glasscam.settings.SettingsManager;
import glasscam.utils.LocationManager;
import glasscam.utils.Log;
import glasscam.utils.OrientationManager;
import glasscam.utils.SoundPlayer;

/**
 * Created by troop on 15.08.2014.
 */
public abstract class ModuleAbstract<CW extends CameraWrapperInterface> implements ModuleInterface
{
    protected boolean isWorking;
    protected boolean isLowStorage;
    public String name;
    private final String TAG = ModuleAbstract.class.getSimpleName();
    protected CaptureStates currentWorkState;
    protected CW cameraUiWrapper;
    protected Handler mBackgroundHandler;
    protected Handler mainHandler;
    protected SettingsManager settingsManager;
    protected FileListController fileListController;
    protected LocationManager locationManager;
    protected OrientationManager orientationManager;
    private CaptureStateChangedEventHandler captureStateChangedEventHandler;
    protected SoundPlayer soundPlayer;


    public ModuleAbstract(CW cameraUiWrapper, Handler mBackgroundHandler, Handler mainHandler)
    {
        this.cameraUiWrapper = cameraUiWrapper;
        this.mBackgroundHandler = mBackgroundHandler;
        this.mainHandler = new Handler(Looper.getMainLooper());
        settingsManager = GlassCamApplication.settingsManager();
        fileListController = GlassCamApplication.fileListController();
        locationManager = ActivityGlassCamMain.locationManager();
        orientationManager = ActivityGlassCamMain.orientationManager();
        this.soundPlayer = ActivityGlassCamMain.soundPlayer();
    }

    /**
     * throw this when camera starts working to notify ui
     */
    public void changeCaptureState(final CaptureStates captureStates)
    {
        Log.d(TAG, "work started");
        currentWorkState = captureStates;
        if (captureStateChangedEventHandler != null)
            captureStateChangedEventHandler.fireCaptureStateChangedEvent(captureStates);
    }

    @Override
    public void setCaptureStateEventHandler(CaptureStateChangedEventHandler eventHandler) {
        this.captureStateChangedEventHandler = eventHandler;
    }

    @Override
    public String ModuleName() {
        return name;
    }


    @Override
    public abstract void DoWork();

    @Override
    public boolean IsWorking() {
        return isWorking;
    }

    @Override
    public void IsLowStorage(Boolean x) {
        isLowStorage = x;
    }

    /**
     * this gets called when the module gets loaded. set here specific parameters that are needed by the module
     */
    @Override
    public void InitModule()
    {
        isWorking = false;
    }

    /**
     * this gets called when module gets unloaded reset the parameters that where set on InitModule
     */
    @Override
    public abstract void DestroyModule();

    @Override
    public abstract String LongName();

    @Override
    public abstract String ShortName();

    /**
     * its called when a saving task is done and the image/movie is rdy to get attached to the MainActivity/screenslideFragment
     * @param file that is new
     */
    @Override
    public void fireOnWorkFinish(BaseHolder file) {
        fileListController.addFromEventFile(file);
    }



    /**
     * gets called when a capture session with more pics is done
        and the images are rdy to get attached to the MainActivity/screenslideFragment
     * @param files that are new
     */
    @Override
    public void fireOnWorkFinish(BaseHolder[] files)
    {
        fileListController.addFromEventFiles(files);
    }

    @Override
    public CaptureStates getCurrentCaptureState() {
        return currentWorkState;
    }
}
