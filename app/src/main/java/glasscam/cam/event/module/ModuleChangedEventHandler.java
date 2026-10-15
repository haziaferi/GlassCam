package glasscam.cam.event.module;

import glasscam.cam.event.BaseEventHandler;
import glasscam.utils.Log;

public class ModuleChangedEventHandler extends BaseEventHandler<ModuleChangedEvent> {

    private static final String TAG = ModuleChangedEventHandler.class.getSimpleName();

    public void fireOnModuleChanged(String module)
    {
        Log.d(TAG, "fireOnModuleChanged " + module);
        for (ModuleChangedEvent event : eventListners)
            event.onModuleChanged(module);
    }
}
