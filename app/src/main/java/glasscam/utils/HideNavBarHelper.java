package glasscam.utils;

import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;

public class HideNavBarHelper
{
    // WindowInsetsController (API 30+, guaranteed at this app's minSdk) replaces the legacy
    // View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY approach this used to use. hide() is documented to be a
    // no-op when the bars are already hidden, so calling it on every onWindowFocusChanged(true) (as
    // ActivityAbstract does) doesn't re-enter sticky mode each time - no need to pre-check visibility
    // (a getRootWindowInsets() snapshot can be stale right at focus-change time, which caused the
    // system bars to incorrectly stay visible after backgrounding via the recents/overview screen).
    public void HIDENAVBAR(Window window)
    {
        WindowInsetsController controller = window.getInsetsController();
        if (controller != null) {
            controller.hide(WindowInsets.Type.systemBars());
            controller.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
        }
    }

    public void showNavbar(Window window)
    {
        WindowInsetsController controller = window.getInsetsController();
        if (controller != null) {
            controller.show(WindowInsets.Type.systemBars());
        }
    }
}
