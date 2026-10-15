package glasscam.utils;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.UriPermission;
import android.content.pm.PackageManager;
import android.net.Uri;

import androidx.core.app.ActivityCompat;

import java.util.List;

import glasscam.ActivityAbstract;
import glasscam.GlassCamApplication;

/**
 * Created by troop on 09.03.2017.
 *
 * SdCard / SdCard_Camera no longer request the legacy READ/WRITE_EXTERNAL_STORAGE
 * permissions (removed from the manifest entirely - they'd be a no-op under scoped
 * storage anyway, and minSdk 30 guarantees every supported device enforces it).
 * They're satisfied instead by a persisted SAF tree-Uri, picked via
 * ActivityAbstract#ChooseSDCard(). requestPermission(SdCard[_Camera]) launches that
 * SAF folder picker instead of a runtime permission dialog.
 */
public class PermissionManager
{

    public enum Permissions
    {
        SdCard,
        Camera,
        Location,
        Wifi,
        RecordAudio,
        SdCard_Camera,
    }

    private final String TAG = PermissionManager.class.getSimpleName();

    private final Context activity;

    public PermissionManager(Context activity)
    {
        this.activity = activity;
    }

    public boolean isPermissionGranted(Permissions permissions)
    {
        switch (permissions) {
            case SdCard:
                return hasSafFolderAccess();
            case Camera:
                return isPermissionGranted(new String[]{Manifest.permission.CAMERA});
            case Location:
                return isPermissionGranted(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION,Manifest.permission.ACCESS_FINE_LOCATION});
            case Wifi:
                return isPermissionGranted(new String[]{Manifest.permission.ACCESS_WIFI_STATE, Manifest.permission.CHANGE_WIFI_STATE});
            case RecordAudio:
                return isPermissionGranted(new String[]{Manifest.permission.RECORD_AUDIO});
            case SdCard_Camera:
                return hasSafFolderAccess() && isPermissionGranted(new String[]{Manifest.permission.CAMERA});
        }
        return false;
    }

    /** True once the user has picked a folder via SAF and we still hold a persisted grant
     *  for that specific folder - not just any persisted URI permission. A stale grant for
     *  a since-revoked or previously-chosen folder shouldn't count as access. */
    private boolean hasSafFolderAccess()
    {
        String baseFolder = GlassCamApplication.settingsManager().GetBaseFolder();
        if (baseFolder == null || baseFolder.isEmpty())
            return false;
        Uri baseUri = Uri.parse(baseFolder);
        List<UriPermission> perms = activity.getContentResolver().getPersistedUriPermissions();
        if (perms == null)
            return false;
        for (UriPermission perm : perms) {
            if (perm.getUri().equals(baseUri))
                return true;
        }
        return false;
    }

    private boolean isPermissionGranted(String[] permission)
    {
        for (String s : permission) {
            if (activity.checkSelfPermission(s) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    private static final String REQUESTED_PERMISSIONS_PREFS = "glasscam_requested_permissions";

    private SharedPreferences requestedPermissionsPrefs()
    {
        return activity.getSharedPreferences(REQUESTED_PERMISSIONS_PREFS, Context.MODE_PRIVATE);
    }

    /**
     * True once a permission has been denied and the system will no longer show the
     * request dialog for it (either "don't ask again" was chosen, or it's been denied
     * twice on API 30+). Once this is true, calling requestPermission() again is a
     * silent no-op - the caller should stop looping the request and tell the user to
     * grant it from Settings instead. Only meaningful for the normal runtime
     * permissions above (Camera, Location, Wifi, RecordAudio) - SdCard is handled via
     * the SAF picker, not this flow.
     */
    public boolean isPermissionPermanentlyDenied(Permissions permission)
    {
        String[] names;
        switch (permission) {
            case Camera:
                names = new String[]{Manifest.permission.CAMERA};
                break;
            case Location:
                names = new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION};
                break;
            case Wifi:
                names = new String[]{Manifest.permission.ACCESS_WIFI_STATE, Manifest.permission.CHANGE_WIFI_STATE};
                break;
            case RecordAudio:
                names = new String[]{Manifest.permission.RECORD_AUDIO};
                break;
            default:
                return false;
        }
        if (!(activity instanceof Activity))
            return false;
        SharedPreferences prefs = requestedPermissionsPrefs();
        for (String name : names) {
            boolean granted = activity.checkSelfPermission(name) == PackageManager.PERMISSION_GRANTED;
            if (granted)
                continue;
            boolean requestedBefore = prefs.getBoolean(name, false);
            boolean shouldShowRationale = ActivityCompat.shouldShowRequestPermissionRationale((Activity) activity, name);
            if (requestedBefore && !shouldShowRationale)
                return true;
        }
        return false;
    }

    public void requestPermission(Permissions permission)
    {
        switch (permission) {
            case SdCard:
                requestSafFolder();
                break;
            case Camera:
                requestPermission(new String[]{Manifest.permission.CAMERA});
                break;
            case Location:
                requestPermission(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION,Manifest.permission.ACCESS_FINE_LOCATION});
                break;
            case Wifi:
                requestPermission(new String[]{Manifest.permission.ACCESS_WIFI_STATE, Manifest.permission.CHANGE_WIFI_STATE});
                break;
            case RecordAudio:
                requestPermission(new String[]{Manifest.permission.RECORD_AUDIO});
                break;
            case SdCard_Camera:
                requestSafFolder();
                requestPermission(new String[]{Manifest.permission.CAMERA});
                break;
        }
    }

    /** Launches the SAF ACTION_OPEN_DOCUMENT_TREE picker if this PermissionManager's
     *  context is an app Activity (it always is - PermissionManager is Activity-scoped). */
    private void requestSafFolder()
    {
        if (activity instanceof ActivityAbstract) {
            ((ActivityAbstract) activity).ChooseSDCard(null);
        }
    }

    private void requestPermission(String[] permissions)
    {
        SharedPreferences.Editor editor = requestedPermissionsPrefs().edit();
        for (String permission : permissions)
            editor.putBoolean(permission, true);
        editor.apply();
        ((Activity)activity).requestPermissions(permissions, 1);
    }

}
