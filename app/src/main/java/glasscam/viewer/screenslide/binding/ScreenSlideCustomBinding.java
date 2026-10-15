package glasscam.viewer.screenslide.binding;

import android.widget.TextView;

import androidx.databinding.BindingAdapter;

import com.ortiz.touch.TouchImageView;
import com.hazia.glasscam.R;

import glasscam.GlassCamApplication;
import glasscam.utils.Log;
import glasscam.viewer.screenslide.BitmapLoader;
import glasscam.viewer.screenslide.models.ExifViewModel;
import glasscam.viewer.screenslide.models.ImageFragmentModel;

public class ScreenSlideCustomBinding {

    @BindingAdapter("setTypeFaceToTextView")
    public static void setTypeFaceToTextView(TextView textView, ExifViewModel gridImageViewModel)
    {
        if (gridImageViewModel != null)
            textView.setTypeface(gridImageViewModel.getTypeface());
    }

    @BindingAdapter("setImageModelModelToImageView")
    public static void setImageModelModelToImageView(TouchImageView gridImageView, ImageFragmentModel gridImageViewModel)
    {
        if (gridImageViewModel == null || gridImageViewModel.getBaseHolder() == null)
            return;
        if (gridImageViewModel.bitmapLoader != null) {
            gridImageViewModel.setProgressBarVisible(false);
            GlassCamApplication.imageManager().removeImageLoadTask(gridImageViewModel.bitmapLoader);
        }

        gridImageView.setImageBitmap(null);
        if (!gridImageViewModel.getBaseHolder().IsFolder())
        {
            gridImageView.setImageResource(R.drawable.noimage);
            gridImageViewModel.setProgressBarVisible(true);
            try {
                gridImageViewModel.bitmapLoader = new BitmapLoader(gridImageViewModel,gridImageView);
                GlassCamApplication.imageManager().putImageLoadTask(gridImageViewModel.bitmapLoader);
            }
            catch (NullPointerException ex)
            {
                Log.WriteEx(ex);
            }

        }
        else {
            gridImageViewModel.setProgressBarVisible(false);
            gridImageView.setImageResource(R.drawable.folder);
        }

        //invalidate();

    }
}
