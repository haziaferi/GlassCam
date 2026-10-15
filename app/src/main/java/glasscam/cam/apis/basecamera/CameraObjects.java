package glasscam.cam.apis.basecamera;

import glasscam.cam.apis.basecamera.modules.ModuleHandlerInterface;
import glasscam.cam.apis.basecamera.parameters.ParameterHandler;

public interface CameraObjects<C extends CameraHolderInterface, P extends ParameterHandler, M extends ModuleHandlerInterface, F extends AbstractFocusHandler> {
    /**
     * Get the current active CameraHolder
     * @return
     */
    C getCameraHolder();

    /**
     * get the active parameterhandler
     * @return
     */
    P getParameterHandler();
    M getModuleHandler();
    F getFocusHandler();
}
