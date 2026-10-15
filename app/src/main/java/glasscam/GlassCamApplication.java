package glasscam;

import android.app.Application;
import android.content.Context;

import com.hazia.glasscam.R;

import glasscam.file.FileListController;
import glasscam.image.ImageManager;
import glasscam.settings.SettingsManager;
import glasscam.viewer.helper.BitmapHelper;

/**
 * App-wide singletons, manually constructed (previously Hilt's SingletonComponent).
 * Every call site already went through the static accessors below rather than field
 * injection, so removing Hilt here needed no changes anywhere else in the app.
 */
public class GlassCamApplication extends Application {

    private static Context context;

    private static SettingsManager settingsManager;
    private static FileListController fileListController;
    private static ImageManager imageManager;
    private static BitmapHelper bitmapHelper;

    public static Context getContext()
    {
        return context;
    }

    public static String getStringFromRessources(int id)
    {
        return context.getResources().getString(id);
    }

    public static String[] getStringArrayFromRessource(int id)
    {
        return context.getResources().getStringArray(id);
    }

    /** The same array in the default (English) resources, whatever the phone's language: for
     *  decisions that must not depend on the locale, like picking a default by its label. */
    public static String[] getDefaultLocaleStringArray(int id)
    {
        android.content.res.Configuration conf = new android.content.res.Configuration(context.getResources().getConfiguration());
        conf.setLocale(java.util.Locale.ROOT);
        return context.createConfigurationContext(conf).getResources().getStringArray(id);
    }

    public static synchronized SettingsManager settingsManager()
    {
        if (settingsManager == null)
            settingsManager = new SettingsManager();
        return settingsManager;
    }

    public static synchronized FileListController fileListController()
    {
        if (fileListController == null)
            fileListController = new FileListController(context);
        return fileListController;
    }

    public static synchronized BitmapHelper bitmapHelper()
    {
        if (bitmapHelper == null)
            bitmapHelper = new BitmapHelper(context, context.getResources().getDimensionPixelSize(R.dimen.image_thumbnails_size));
        return bitmapHelper;
    }

    public static synchronized ImageManager imageManager()
    {
        if (imageManager == null)
            imageManager = new ImageManager();
        return imageManager;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        context = getApplicationContext();
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        context = null;
        settingsManager = null;
        fileListController = null;
        imageManager = null;
        bitmapHelper = null;
    }
}
