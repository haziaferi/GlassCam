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

import com.hazia.glasscam.R;

import java.util.AbstractMap;
import java.util.HashMap;

import glasscam.GlassCamApplication;
import glasscam.cam.apis.basecamera.CameraWrapperInterface;
import glasscam.cam.event.capture.CaptureStateChangedEventHandler;
import glasscam.cam.event.module.ModuleChangedEventHandler;
import glasscam.settings.SettingsManager;
import glasscam.utils.BackgroundHandlerThread;
import glasscam.utils.Log;

/**
 * Created by troop on 09.12.2014.
 */
public abstract class ModuleHandlerAbstract<CW extends CameraWrapperInterface> implements ModuleHandlerInterface
{
    private final String TAG = ModuleHandlerAbstract.class.getSimpleName();
    protected AbstractMap<String, ModuleInterface> moduleList;
    protected ModuleInterface currentModule;
    protected CW cameraUiWrapper;

    private final BackgroundHandlerThread backgroundHandlerThread;

    protected Handler mBackgroundHandler;
    protected Handler mainHandler;
    protected SettingsManager settingsManager;
    private CaptureStateChangedEventHandler captureStateChangedEventHandler;
    private ModuleChangedEventHandler moduleChangedEventHandler;

    public ModuleHandlerAbstract(CW cameraUiWrapper)
    {
        this.cameraUiWrapper = cameraUiWrapper;
        settingsManager = GlassCamApplication.settingsManager();
        // LinkedHashMap keeps registration order, so the module popup lists Picture, Interval,
        // Video, AE bracket, AF bracket top to bottom instead of HashMap's hash order (Batch 3).
        moduleList = new java.util.LinkedHashMap<>();
        backgroundHandlerThread = new BackgroundHandlerThread(TAG);
        backgroundHandlerThread.create();
        mBackgroundHandler = backgroundHandlerThread.getBackgroundHandler();
    }

    @Override
    public void setCaptureStateChangedEventHandler(CaptureStateChangedEventHandler captureStateChangedEventHandler) {
        this.captureStateChangedEventHandler = captureStateChangedEventHandler;
    }

    @Override
    public void setModuleChangedEventHandler(ModuleChangedEventHandler eventHandler) {
        this.moduleChangedEventHandler = eventHandler;
    }

    /**
     * Load the new module
     * @param name of the module to load
     */
    @Override
    public void setModule(String name) {
        if (currentModule !=null) {
            currentModule.DestroyModule();
            currentModule.setCaptureStateEventHandler(null);
            currentModule = null;
        }
        currentModule = moduleList.get(name);
        if(currentModule == null)
            currentModule = moduleList.get(GlassCamApplication.getStringFromRessources(R.string.module_picture));
        currentModule.setCaptureStateEventHandler(captureStateChangedEventHandler);
        currentModule.InitModule();
        fireModuleHasChanged(currentModule.ModuleName());
        Log.d(TAG, "Set Module to " + name);
    }

    @Override
    public String getCurrentModuleName() {
        if (currentModule != null)
            return currentModule.ModuleName();
        else return GlassCamApplication.getStringFromRessources(R.string.module_picture);
    }

    @Override
    public ModuleInterface getCurrentModule() {
        if (currentModule != null)
            return currentModule;
        return null;
    }

    @Override
    public boolean startWork() {
        if (currentModule != null) {
            currentModule.DoWork();
            return true;
        }
        else
            return false;
    }

    @Override
    public void SetIsLowStorage(Boolean x) {
        if( currentModule != null )
            currentModule.IsLowStorage(x);
    }

    /**
     * Gets thrown when the module has changed
     * @param module the new module that gets loaded
     */
    @Override
    public void fireModuleHasChanged(final String module)
    {
        moduleChangedEventHandler.fireOnModuleChanged(module);
    }

    @Override
    public AbstractMap<String, ModuleInterface> getModuleList() {
        return moduleList;
    }

}
