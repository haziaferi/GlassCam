/*
 *
 *     Copyright (C) 2015 Ingo Fuchs
 *     This program is free software; you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation; either version 2 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License along
 *     with this program; if not, write to the Free Software Foundation, Inc.,
 *     51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 * /
 */

package glasscam.viewer.screenslide.views;


import android.app.AlertDialog;
import android.app.RecoverableSecurityException;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.core.content.FileProvider;
import androidx.databinding.DataBindingUtil;
import androidx.databinding.Observable;
import androidx.databinding.library.baseAdapters.BR;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.viewpager.widget.ViewPager;

import com.hazia.glasscam.R.dimen;
import com.hazia.glasscam.R.layout;
import com.hazia.glasscam.databinding.GlasscamviewerScreenslideFragmentBinding;

import glasscam.ActivityAbstract;
import glasscam.ActivityInterface;
import glasscam.ActivityInterface.I_OnActivityResultCallback;
import glasscam.GlassCamApplication;
import glasscam.ViewModelFactory;
import glasscam.file.FileListController;
import glasscam.file.holder.FileHolder;
import glasscam.file.holder.UriHolder;
import glasscam.utils.StringUtils.FileEnding;
import glasscam.viewer.screenslide.adapter.ScreenSlidePagerAdapter;
import glasscam.viewer.screenslide.modelview.ScreenSlideFragmentModelView;


/**
 * Created by troop on 18.09.2015.
 */
public class ScreenSlideFragment extends Fragment implements ViewPager.OnPageChangeListener, I_OnActivityResultCallback
{
    public final String TAG = ScreenSlideFragment.class.getSimpleName();
    public interface ButtonClick
    {
        void onButtonClick(int position, View view);
    }

    public interface FragmentClickClistner
    {
        void onFragmentClick(Fragment fragment);
    }

    /**
     * The pager adapter, which provides the pages to the view pager widget.
     */
    private ScreenSlidePagerAdapter mPagerAdapter;
    private ScreenSlideFragmentModelView screenSlideFragmentModelView;


    public int defitem = -1;
    public FileListController.FormatTypes filestoshow = FileListController.FormatTypes.all;
    private ButtonClick backClickListner;
    //hold the showed folder_to_show



    private ActivityInterface activityInterface;

    private final boolean showExifInfo = false;
    private int position;

    private GlasscamviewerScreenslideFragmentBinding screenslideFragmentBinding;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        super.onCreateView(inflater,container,savedInstanceState);
        screenslideFragmentBinding =  DataBindingUtil.inflate(inflater, layout.glasscamviewer_screenslide_fragment, container, false);

        int mImageThumbSize = getResources().getDimensionPixelSize(dimen.image_thumbnail_size);
        activityInterface = (ActivityInterface) getActivity();

        // Instantiate a ViewPager and a PagerAdapter.
        screenSlideFragmentModelView = new ViewModelProvider(this, new ViewModelFactory(() ->
                new ScreenSlideFragmentModelView(GlassCamApplication.fileListController(), GlassCamApplication.bitmapHelper(), GlassCamApplication.imageManager())))
                .get(ScreenSlideFragmentModelView.class);



        bind();
        return screenslideFragmentBinding.getRoot();
    }

    private void bind()
    {
        if (screenslideFragmentBinding != null && screenSlideFragmentModelView != null)
        {
            screenslideFragmentBinding.setScreenslideFragmentModel(screenSlideFragmentModelView);

            mPagerAdapter = new ScreenSlidePagerAdapter(getChildFragmentManager(),screenslideFragmentBinding.pager,screenSlideFragmentModelView.getFragmentclickListner());
            screenslideFragmentBinding.pager.setAdapter(mPagerAdapter);
            screenslideFragmentBinding.pager.setOffscreenPageLimit(2);
            screenslideFragmentBinding.pager.addOnPageChangeListener(this);
            screenslideFragmentBinding.pager.setCurrentItem(position, false);

            screenSlideFragmentModelView.getFilesHolderModel().addOnPropertyChangedCallback(new Observable.OnPropertyChangedCallback() {
                @Override
                public void onPropertyChanged(Observable sender, int propertyId) {
                    if (propertyId == BR.files)
                        mPagerAdapter.setImageFragmentModels(screenSlideFragmentModelView.getFilesHolderModel().getImageFragmentModels());
                }
            });

            screenslideFragmentBinding.buttonCloseView.setOnClickListener(v -> {
                if (backClickListner != null) {
                    backClickListner.onButtonClick(screenslideFragmentBinding.pager.getCurrentItem(), screenslideFragmentBinding.getRoot());
                    screenslideFragmentBinding.pager.setCurrentItem(0);
                } else
                    getActivity().finish();
            });

            screenslideFragmentBinding.buttonPlay.setOnClickListener(onplayClick);
            screenSlideFragmentModelView.getPlayButton().setVisibility(false);
            screenSlideFragmentModelView.getDeleteButton().setVisibility(false);
            screenslideFragmentBinding.buttonDelete.setOnClickListener(onDeleteButtonClick);
        }
    }

    private final DialogInterface.OnClickListener onDeleteAlertButtonClick = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            switch (which){
                case DialogInterface.BUTTON_POSITIVE:
                    try {
                        screenSlideFragmentModelView.getFilesHolderModel().getFileListController().DeleteFile(screenSlideFragmentModelView.getFolder_to_show());
                    }
                    catch (SecurityException ex)
                    {
                        if (ex instanceof RecoverableSecurityException)
                        {
                            RecoverableSecurityException rex = (RecoverableSecurityException)ex;
                            try {
                                startIntentSenderForResult(rex.getUserAction().getActionIntent().getIntentSender(), ActivityAbstract.DELETE_REQUEST_CODE,null,0,0,0,null);
                            } catch (IntentSender.SendIntentException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                    break;

                case DialogInterface.BUTTON_NEGATIVE:
                    //No button clicked
                    break;
            }
        }
    };

    private final View.OnClickListener onplayClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (screenSlideFragmentModelView.getFolder_to_show() == null)
                return;
            if (!screenSlideFragmentModelView.getFolder_to_show().getName().endsWith(FileEnding.RAW) || !screenSlideFragmentModelView.getFolder_to_show().getName().endsWith(FileEnding.BAYER)) {
                Uri uri = null;
                if (screenSlideFragmentModelView.getFolder_to_show() instanceof FileHolder) {
                    uri = FileProvider.getUriForFile(getContext(), getContext().getPackageName() + ".provider", ((FileHolder) screenSlideFragmentModelView.getFolder_to_show()).getFile());
                }
                else if (screenSlideFragmentModelView.getFolder_to_show() instanceof UriHolder)
                    uri = ((UriHolder)screenSlideFragmentModelView.getFolder_to_show()).getMediaStoreUri();

                Intent i;
                if (screenSlideFragmentModelView.getFolder_to_show().getName().endsWith(FileEnding.MP4))
                {
                    i = new Intent(Intent.ACTION_VIEW);
                    i.setDataAndType(uri, "video/*");
                }
                else {
                    i = new Intent(Intent.ACTION_EDIT);
                    i.setDataAndType(uri, "image/*");
                }
                i.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                Intent chooser = Intent.createChooser(i, "Choose App");
                chooser.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                //startActivity(i);
                if (i.resolveActivity(getActivity().getPackageManager()) != null) {
                    startActivity(chooser);
                }

            }
        }
    };

    private final View.OnClickListener onDeleteButtonClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            // Only writing to the external/SAF folder needs that folder to have been
            // picked first. MediaStore-backed files (the default until a folder is
            // ever picked, per the SAF migration notes) delete via
            // ContentResolver.delete() and have no dependency on it at all - this
            // check must not gate them, or a user who's never picked a folder can't
            // delete anything.
            if (GlassCamApplication.settingsManager().GetWriteExternal()
                    && FileListController.getExternalSdDocumentFile(getContext()) == null) {
                ActivityAbstract activity = (ActivityAbstract) getActivity();
                activity.ChooseSDCard(ScreenSlideFragment.this);
            } else {
                AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                builder.setMessage("Delete File?").setPositiveButton("Yes", onDeleteAlertButtonClick)
                        .setNegativeButton("No", onDeleteAlertButtonClick).show();
            }


        }
    };


    public void SetPostition(int position)
    {
        this.position = position;
        if (screenslideFragmentBinding.pager != null)
            screenslideFragmentBinding.pager.setCurrentItem(position, false);
    }

    public int getPosition()
    {
        if (screenslideFragmentBinding.pager != null)
            return screenslideFragmentBinding.pager.getCurrentItem();
        return 0;
    }


    public ScreenSlideFragment()
    {

    }

    public void setOnBackClickListner(ButtonClick thumbClick)
    {
        backClickListner = thumbClick;
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels)
    {
        screenSlideFragmentModelView.updateUi(mPagerAdapter.getCurrentFile());
        ImageFragment fragment = (ImageFragment) mPagerAdapter.getRegisteredFragment(position);
        screenslideFragmentBinding.setImagefragment(mPagerAdapter.getCurrentImageFragmentModel());

    }

    @Override
    public void onPageSelected(int position)
    {


    }

    @Override
    public void onPageScrollStateChanged(int state) {

    }

    @Override
    public void onActivityResultCallback(Uri uri) {

    }

}
