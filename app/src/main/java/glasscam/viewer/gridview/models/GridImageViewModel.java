package glasscam.viewer.gridview.models;

import androidx.databinding.BaseObservable;
import androidx.databinding.Bindable;
import androidx.databinding.library.baseAdapters.BR;

import glasscam.file.holder.BaseHolder;
import glasscam.viewer.gridview.BitmapLoadRunnable;
import glasscam.viewer.gridview.enums.ViewStates;
import glasscam.viewer.helper.BitmapHelper;

public class GridImageViewModel extends BaseObservable
{
    private static final String TAG = GridImageViewModel.class.getSimpleName();
    private final BitmapHelper bitmapHelper;
    private final BaseHolder imagePath;
    private String filending;
    private String foldername;
    private boolean isExternalSD;
    private boolean isChecked = false;
    private boolean isCheckVisible = false;
    private boolean isProgressBarVisible = false;
    public BitmapLoadRunnable bitmapLoadRunnable;

    public GridImageViewModel(BitmapHelper bitmapHelper, BaseHolder imagePath)
    {
        this.bitmapHelper = bitmapHelper;
        this.imagePath = imagePath;
        if (!imagePath.IsFolder()) {
            // the whole extension: the last three characters read "YER" for a .bayer dump (9 Pro walk 2026-10-21)
            String name = imagePath.getName();
            int dot = name.lastIndexOf('.');
            setFilending(dot >= 0 ? name.substring(dot + 1) : "");
            setFoldername("");
        }
        else {
            setFilending("");
            setFoldername(imagePath.getName());
        }
        setExternalSD(imagePath.isExternalSD());
        setProgressBarVisible(false);
    }

    public void setFilending(String filending)
    {
        this.filending = filending;
        notifyPropertyChanged(BR.filending);
    }

    @Bindable
    public String getFilending() {
        return filending;
    }

    @Bindable
    public String getFoldername() {
        //Log.d(TAG,"getFoldername:" + foldername);
        return foldername;
    }

    public void setFoldername(String foldername) {
        this.foldername = foldername;
        notifyPropertyChanged(BR.foldername);
    }

    @Bindable
    public boolean getExternalSD() {
        return isExternalSD;
    }

    public void setExternalSD(boolean externalSD) {
        isExternalSD = externalSD;
        notifyPropertyChanged(BR.externalSD);
    }

    @Bindable
    public boolean getChecked() {
        return isChecked;
    }

    public void setChecked(boolean checked) {
        isChecked = checked;
        notifyPropertyChanged(BR.checked);
    }

    @Bindable
    public boolean getCheckVisible() {
        return isCheckVisible;
    }

    public void setCheckVisible(boolean checkVisible) {
        isCheckVisible = checkVisible;
        notifyPropertyChanged(BR.checkVisible);
    }

    @Bindable
    public boolean getProgressBarVisible() {
        return isProgressBarVisible;
    }

    public void setProgressBarVisible(boolean progressBarVisible) {
        isProgressBarVisible = progressBarVisible;
        notifyPropertyChanged(BR.progressBarVisible);
    }

    public BaseHolder getImagePath() {
        return imagePath;
    }

    public BitmapHelper getBitmapHelper() {
        return bitmapHelper;
    }

    public void setViewState(ViewStates state)
    {
        switch (state)
        {
            case normal:
                setCheckVisible(false);
                setChecked(false);
                break;
            case selection:
                setCheckVisible(true);
                setChecked(getChecked());
        }
    }

}
