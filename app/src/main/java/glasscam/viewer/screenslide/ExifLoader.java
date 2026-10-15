package glasscam.viewer.screenslide;

import android.media.ExifInterface;
import android.os.Build;

import com.hazia.glasscam.R;

import java.io.IOException;
import java.io.InputStream;

import glasscam.GlassCamApplication;
import glasscam.file.holder.BaseHolder;
import glasscam.file.holder.FileHolder;
import glasscam.file.holder.UriHolder;
import glasscam.image.ImageTask;
import glasscam.utils.Log;
import glasscam.viewer.screenslide.models.ExifViewModel;

public class ExifLoader extends ImageTask {

    private final BaseHolder file;
    private final ExifViewModel exifViewModel;

    public ExifLoader(BaseHolder file, ExifViewModel exifViewModel)
    {
        this.file = file;
        this.exifViewModel = exifViewModel;
    }

    @Override
    public boolean process() {
        ExifInterface exifInterface = null;
        try {
            //Log.d(TAG, "File: " + file.getName() + " DateModded: " + file.lastModified());
            if (file instanceof FileHolder && file.exists())
                exifInterface = new ExifInterface(((FileHolder)file).getFile().getAbsolutePath());
            else if (file instanceof UriHolder)
            {

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && file.exists()) {
                    InputStream pfd = GlassCamApplication.getContext().getContentResolver().openInputStream(((UriHolder) file).getMediaStoreUri());
                    exifInterface = new ExifInterface(pfd);
                    pfd.close();
                }
            }

        } catch (IOException e) {
            Log.WriteEx(e);
        }
        catch (IllegalArgumentException ex)
        {
            Log.WriteEx(ex);
        }
        catch (ArrayIndexOutOfBoundsException ex)
        {
            Log.WriteEx(ex);
        }
        if (exifInterface == null)
            return false;
        try {
            String expostring = exifInterface.getAttribute(ExifInterface.TAG_EXPOSURE_TIME);
            if (expostring == null)
                exifViewModel.getShutter().setText("");
            else
            {
                exifViewModel.getShutter().setText(GlassCamApplication.getStringFromRessources(R.string.font_exposuretime) + getShutterStringSeconds(Double.parseDouble(expostring)));
            }
        }catch (NullPointerException e){
            Log.WriteEx(e);
            exifViewModel.getShutter().setText("");
        }
        try
        {
            double fnums = exifInterface.getAttributeDouble(ExifInterface.TAG_APERTURE,1.9d);
            exifViewModel.getFnumber().setText(GlassCamApplication.getStringFromRessources(R.string.font_aperture) + String.format("%.2f", fnums));
        }catch (NullPointerException e){
            exifViewModel.getFnumber().setText("");
            Log.WriteEx(e);
        }
        try {
            double focs = exifInterface.getAttributeDouble(ExifInterface.TAG_FOCAL_LENGTH,5.4d);
            exifViewModel.getFocal().setText(GlassCamApplication.getStringFromRessources(R.string.font_focallength)  + String.format("%.2f", focs) +"mm");
        }catch (NullPointerException e){
            exifViewModel.getFocal().setText("");
            Log.WriteEx(e);
        }
        try {
            String isos = exifInterface.getAttribute(ExifInterface.TAG_ISO_SPEED_RATINGS);
            if (isos != null)
                exifViewModel.getIso().setText(GlassCamApplication.getStringFromRessources(R.string.font_iso)  + isos);
            else
                exifViewModel.getIso().setText("");
        }catch (NullPointerException e){
            exifViewModel.getIso().setText("");
            Log.WriteEx(e);
        }
        try
        {
            int w = exifInterface.getAttributeInt(ExifInterface.TAG_IMAGE_WIDTH,0);
            int h = exifInterface.getAttributeInt(ExifInterface.TAG_IMAGE_LENGTH,0);
            exifViewModel.getImage_size().setText(GlassCamApplication.getStringFromRessources(R.string.font_image)  +w+"x"+h);
        }
        catch (NullPointerException e){
            exifViewModel.getImage_size().setText("");
            Log.WriteEx(e);
        }
        return true;
    }

    private String getShutterStringSeconds(double val)
    {
        if (val >= 1) {
            return "" + (int)val;
        }
        int i = (int)(1 / val);
        return "1/" + i;
    }
}
