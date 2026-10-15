package glasscam.update;

import com.hazia.glasscam.BuildConfig;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import glasscam.cam.apis.sonyremote.sonystuff.SimpleHttpClient;

public class ReleaseChecker {

    public static final boolean isGithubRelease = false;

    public interface UpdateEvent
    {
        void onUpdateAvailable();
    }

    private static final String TAG = ReleaseChecker.class.getSimpleName();
    // Batch 3 (2026-09-16): the fork has no release feed; isGithubRelease stays false and the
    // upstream KillerInk URL is gone so a future feed cannot point users at the wrong project.
    public static final String RELEASES_PAGE = "";
    private final String githubrepo = "";

    private final UpdateEvent event;

    public ReleaseChecker(UpdateEvent eventListner)
    {
        this.event = eventListner;
    }

    public void isUpdateAvailable()  {

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    checkupdate();
                } catch (IOException | JSONException | NumberFormatException e) {
                    e.printStackTrace();
                }
            }
        }).start();


    }

    private void checkupdate() throws IOException, JSONException, NumberFormatException
    {
        String ret = SimpleHttpClient.httpGet(githubrepo);
        JSONObject jsonObject = new JSONObject(ret);
        String version = jsonObject.getString("tag_name");
        String curVersion = BuildConfig.VERSION_NAME;
        int v = Integer.parseInt(version.replace(".",""));
        int cv = Integer.parseInt(curVersion.replace(".",""));
        if (v > cv)
        {
            if (event != null)
                event.onUpdateAvailable();
        }
    }
}
