package glasscam.cam.apis.basecamera;

import glasscam.GlassCamApplication;
import glasscam.cam.ActivityGlassCamMain;
import glasscam.cam.apis.basecamera.modules.ModuleHandlerInterface;
import glasscam.cam.apis.basecamera.parameters.ParameterHandler;
import glasscam.cam.event.camera.CameraHolderEventHandler;
import glasscam.cam.event.capture.CaptureStateChangedEventHandler;
import glasscam.cam.event.module.ModuleChangedEventHandler;
import glasscam.cam.previewpostprocessing.PreviewControllerInterface;
import glasscam.settings.SettingsManager;

public abstract class AbstractCamera<
        P extends ParameterHandler,
        C extends CameraHolderInterface,
        M extends ModuleHandlerInterface,
        F extends AbstractFocusHandler>
        implements CameraWrapperInterface
{
    private static final String TAG = AbstractCamera.class.getSimpleName();
    public M moduleHandler;
    /**
     * parameters for avail for the cameraHolder
     */
    protected P parametersHandler;
    /**
     * holds the current camera
     */
    protected C cameraHolder;
    /**
     * handels focus releated stuff for the current camera
     */
    public F focusHandler;

    protected PreviewControllerInterface preview;
    protected SettingsManager settingsManager;

    public AbstractCamera()
    {
        settingsManager = GlassCamApplication.settingsManager();
        preview = ActivityGlassCamMain.previewController();
    }

    @Override
    public void setCaptureStateChangedEventHandler(CaptureStateChangedEventHandler eventHandler)
    {
        moduleHandler.setCaptureStateChangedEventHandler(eventHandler);
    }

    @Override
    public void setCameraHolderEventHandler(CameraHolderEventHandler cameraHolderEventHandler) {

        cameraHolder.addEventListner(cameraHolderEventHandler);
    }

    @Override
    public void setModuleChangedEventHandler(ModuleChangedEventHandler moduleChangedEventHandler) {
        moduleHandler.setModuleChangedEventHandler(moduleChangedEventHandler);
    }

    @Override
    public boolean isAeMeteringSupported() {
        return focusHandler.isAeMeteringSupported();
    }

    @Override
    public F getFocusHandler() {
        return focusHandler;
    }

    @Override
    public C getCameraHolder() {
        return cameraHolder;
    }

    @Override
    public P getParameterHandler() {
        return parametersHandler;
    }

    @Override
    public M getModuleHandler() {
        return moduleHandler;
    }


}
