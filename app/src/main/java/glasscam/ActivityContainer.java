package glasscam;

import android.app.Activity;

import glasscam.cam.apis.CameraApiManager;
import glasscam.cam.apis.basecamera.parameters.modes.ApiParameter;
import glasscam.cam.histogram.HistogramController;
import glasscam.cam.previewpostprocessing.PreviewController;
import glasscam.cam.ui.KeyPressedController;
import glasscam.cam.ui.ThemeManager;
import glasscam.cam.ui.handler.UserMessageHandler;
import glasscam.utils.LocationManager;
import glasscam.utils.OrientationManager;
import glasscam.utils.PermissionManager;
import glasscam.utils.SoundPlayer;
import glasscam.views.pagingview.PagingViewTouchState;

/**
 * Manual per-Activity dependency container, replacing Hilt's ActivityComponent.
 *
 * One instance lives for the lifetime of an ActivityAbstract (created in onCreate,
 * dropped in onDestroy - see ActivityAbstract). Everything below is built lazily and
 * cached for the life of that instance, matching the old @ActivityScoped behaviour.
 * Fragments/Views reach it via ((ActivityAbstract) activity).container().
 */
public class ActivityContainer {

    private final Activity activity;

    public ActivityContainer(Activity activity) {
        this.activity = activity;
    }

    private PermissionManager permissionManager;
    public synchronized PermissionManager permissionManager() {
        if (permissionManager == null)
            permissionManager = new PermissionManager(activity);
        return permissionManager;
    }

    private PreviewController previewController;
    public synchronized PreviewController previewController() {
        if (previewController == null)
            previewController = new PreviewController();
        return previewController;
    }

    private CameraApiManager cameraApiManager;
    public synchronized CameraApiManager cameraApiManager() {
        if (cameraApiManager == null)
            cameraApiManager = new CameraApiManager(GlassCamApplication.settingsManager(), previewController());
        return cameraApiManager;
    }

    private OrientationManager orientationManager;
    public synchronized OrientationManager orientationManager() {
        if (orientationManager == null)
            orientationManager = new OrientationManager(activity);
        return orientationManager;
    }

    private SoundPlayer soundPlayer;
    public synchronized SoundPlayer soundPlayer() {
        if (soundPlayer == null)
            soundPlayer = new SoundPlayer(activity);
        return soundPlayer;
    }

    private UserMessageHandler userMessageHandler;
    public synchronized UserMessageHandler userMessageHandler() {
        if (userMessageHandler == null)
            userMessageHandler = new UserMessageHandler(activity);
        return userMessageHandler;
    }

    private LocationManager locationManager;
    public synchronized LocationManager locationManager() {
        if (locationManager == null)
            locationManager = new LocationManager(activity, GlassCamApplication.settingsManager(), userMessageHandler());
        return locationManager;
    }

    private KeyPressedController keyPressedController;
    public synchronized KeyPressedController keyPressedController() {
        if (keyPressedController == null)
            keyPressedController = new KeyPressedController(cameraApiManager(), userMessageHandler());
        return keyPressedController;
    }

    private PagingViewTouchState pagingViewTouchState;
    public synchronized PagingViewTouchState pagingViewTouchState() {
        if (pagingViewTouchState == null)
            pagingViewTouchState = new PagingViewTouchState();
        return pagingViewTouchState;
    }

    private ThemeManager themeManager;
    public synchronized ThemeManager themeManager() {
        if (themeManager == null)
            themeManager = new ThemeManager(cameraApiManager());
        return themeManager;
    }

    private HistogramController histogramController;
    public synchronized HistogramController histogramController() {
        if (histogramController == null)
            histogramController = new HistogramController(GlassCamApplication.settingsManager());
        return histogramController;
    }

    private ApiParameter apiParameter;
    public synchronized ApiParameter apiParameter() {
        if (apiParameter == null)
            apiParameter = new ApiParameter(GlassCamApplication.settingsManager(), cameraApiManager());
        return apiParameter;
    }

}
