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

package glasscam.viewer;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Bundle;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;

import androidx.databinding.Observable;
import androidx.databinding.library.baseAdapters.BR;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProvider;

import com.hazia.glasscam.R;

import java.util.List;

import glasscam.ActivityAbstract;
import glasscam.GlassCamApplication;
import glasscam.ViewModelFactory;
import glasscam.file.FileListController;
import glasscam.file.holder.BaseHolder;
import glasscam.utils.GlassCamPool;
import glasscam.utils.Log;
import glasscam.utils.PermissionManager;
import glasscam.viewer.gridview.modelview.GridViewFragmentModelView;
import glasscam.viewer.gridview.views.GridViewFragment;
import glasscam.viewer.screenslide.views.ScreenSlideFragment;

/**
 * Created by troop on 11.12.2015.
 */
public class ActivityGlassCamViewer extends ActivityAbstract
{
    private final String TAG = ActivityGlassCamViewer.class.getSimpleName();
    private GridViewFragment gridViewFragment;
    private ScreenSlideFragment screenSlideFragment;
    private FrameLayout gridholder;
    private FrameLayout slideholder;
    private AnimatorSet mCurrentAnimator;
    private int mShortAnimationDuration;
    private GridViewFragmentModelView gridViewFragmentModelView;
    FileListController fileListController = GlassCamApplication.fileListController();


    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        init();
    }


    @Override
    protected void setContentToView() {
        Log.d(TAG, "Set Content to view");
        setContentView(R.layout.glasscamviewer_activity);
    }

    private void init()
    {
        Log.d(TAG,"init");
        gridViewFragmentModelView = new ViewModelProvider(this, new ViewModelFactory(() ->
                new GridViewFragmentModelView(GlassCamApplication.fileListController(), GlassCamApplication.imageManager())))
                .get(GridViewFragmentModelView.class);
        mShortAnimationDuration = getResources().getInteger(
                android.R.integer.config_shortAnimTime);
        gridViewFragment = new GridViewFragment();
        gridViewFragment.setGridViewFragmentModelView(gridViewFragmentModelView);
        gridViewFragment.SetOnGridItemClick(onGridItemClick);



        screenSlideFragment = new ScreenSlideFragment();
        screenSlideFragment.setOnBackClickListner(onScreenSlideBackClick);
        slideholder =  findViewById(R.id.glasscamviewer_screenslideholder);
        gridholder = findViewById(R.id.glasscamviewer_gridviewholder);
        slideholder.setVisibility(View.GONE);
        // Batch 2 (2026-09-16): system Back used to finish the activity from the full-screen image;
        // now it returns to the grid first, like the on-screen chevron.
        getOnBackPressedDispatcher().addCallback(this, new androidx.activity.OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                // no "animator idle" condition: loadGridView cancels a running zoom itself, and a
                // Back during one used to fall through and finish the gallery (fresh-eyes 2026-09-28)
                if (slideholder.getVisibility() == View.VISIBLE && openedOnPhoto) {
                    finish();   // opened from the camera's thumbnail: Back returns to the camera (owner, 4.3.97)
                } else if (slideholder.getVisibility() == View.VISIBLE) {
                    loadGridView(screenSlideFragment.getPosition(), slideholder);
                } else {
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                    setEnabled(true);
                }
            }
        });
        replaceCameraFragment(gridViewFragment,"Gridview", R.id.glasscamviewer_gridviewholder);
        replaceCameraFragment(screenSlideFragment,"Gridview", R.id.glasscamviewer_screenslideholder);


        gridViewFragmentModelView.getFilesHolderModel().addOnPropertyChangedCallback(new Observable.OnPropertyChangedCallback() {
            @Override
            public void onPropertyChanged(Observable sender, int propertyId) {
                if (BR.formatType == propertyId)
                {
                    //TODO FIX this
                    //screenSlideFragmentModelView.getFilesHolderModel().setFormatTypes(gridViewFragmentModelView.getFilesHolderModel().getFormatType());
                }
            }
        });

        openedOnPhoto = getIntent() != null && getIntent().getBooleanExtra(EXTRA_OPEN_NEWEST_PHOTO, false);
        GlassCamPool.Execute(() -> {
            fileListController.loadDefaultFiles();
            if (openedOnPhoto)
                // the grid fragment's adapter must exist or the loaded list is dropped, so open
                // the folder a beat later on the UI thread - the same path a folder tap takes -
                // then show its newest photo full screen
                runOnUiThread(() -> gridholder.postDelayed(() -> {
                    gridViewFragmentModelView.openNewestFolder();
                    showNewestPhoto();
                }, 300));
        });
    }

    /** Set by the camera's gallery cell: open on the newest capture, full screen, and swipe through
     *  GlassCam's pictures newest first; Back returns to the camera, the grid button opens the grid
     *  (owner, 4.3.97 - it used to land on the newest folder's grid, Batch 3). */
    public static final String EXTRA_OPEN_NEWEST_PHOTO = "glasscam.viewer.OPEN_NEWEST_PHOTO";

    /** True while the person is browsing photos they opened from the camera's thumbnail. */
    private boolean openedOnPhoto;

    private void showNewestPhoto()
    {
        List<BaseHolder> files = fileListController.getFiles();
        if (files == null || files.isEmpty() || files.get(0).IsFolder()) {
            openedOnPhoto = false;   // nothing to show: stay on the grid
            return;
        }
        if (mCurrentAnimator != null)
            mCurrentAnimator.cancel();
        screenSlideFragment.SetPostition(0);
        slideholder.setScaleX(1f);
        slideholder.setScaleY(1f);
        slideholder.setTranslationX(0f);
        slideholder.setTranslationY(0f);
        slideholder.setVisibility(View.VISIBLE);
    }

    private void replaceCameraFragment(Fragment fragment, String id, int layout)
    {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        //transaction.setCustomAnimations(R.anim.left_to_right_enter, R.anim.left_to_right_exit);
        transaction.replace(layout, fragment, id);
        transaction.commit();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (permissionManager().isPermissionGranted(PermissionManager.Permissions.SdCard) && (fileListController.getFiles() == null || fileListController.getFiles().size() == 0))
            GlassCamPool.Execute(() -> fileListController.loadDefaultFiles());
    }

    // the full-screen photo's grid button: from here on Back walks the grid as before
    private final ScreenSlideFragment.ButtonClick onScreenSlideBackClick = (position, view) -> {
        openedOnPhoto = false;
        loadGridView(position, view);
    };

    private final ScreenSlideFragment.ButtonClick onGridItemClick = this::loadScreenSlide;

    private void loadGridView(int position, View view)
    {
        //replaceCameraFragment(gridViewFragment,"Gridview");
        if (mCurrentAnimator != null) {
            mCurrentAnimator.cancel();
        }

        View griditem = gridViewFragment.GetGridItem(screenSlideFragment.getPosition());
        if (griditem == null)
            gridViewFragment.smoothScrollToPos(screenSlideFragment.getPosition());
            //griditem = gridViewFragment.GetGridItem(0);
        griditem = gridViewFragment.GetGridItem(screenSlideFragment.getPosition());
        if (griditem == null)
            griditem = gridViewFragment.GetGridItem(0);
        // Calculate the starting and ending bounds for the zoomed-in image.
        // This step involves lots of math. Yay, math.
        final Rect startBounds = new Rect();
        final Rect finalBounds = new Rect();
        final Point globalOffset = new Point();

        // The start bounds are the global visible rectangle of the thumbnail,
        // and the final bounds are the global visible rectangle of the container
        // view. Also set the container view's offset as the origin for the
        // bounds, since that's the origin for the positioning animation
        // properties (X, Y).
        if (griditem != null)
            griditem.getGlobalVisibleRect(startBounds);
        gridholder.getGlobalVisibleRect(finalBounds, globalOffset);
        startBounds.offset(-globalOffset.x, -globalOffset.y);
        finalBounds.offset(-globalOffset.x, -globalOffset.y);

        // Adjust the start bounds to be the same aspect ratio as the final
        // bounds using the "center crop" technique. This prevents undesirable
        // stretching during the animation. Also calculate the start scaling
        // factor (the end scaling factor is always 1.0).
        float startScale;
        if ((float) finalBounds.width() / finalBounds.height()
                > (float) startBounds.width() / startBounds.height()) {
            // Extend start bounds horizontally
            startScale = (float) startBounds.height() / finalBounds.height();
            float startWidth = startScale * finalBounds.width();
            float deltaWidth = (startWidth - startBounds.width()) / 2;
            startBounds.left -= deltaWidth;
            startBounds.right += deltaWidth;
        } else {
            // Extend start bounds vertically
            startScale = (float) startBounds.width() / finalBounds.width();
            float startHeight = startScale * finalBounds.height();
            float deltaHeight = (startHeight - startBounds.height()) / 2;
            startBounds.top -= deltaHeight;
            startBounds.bottom += deltaHeight;
        }

        final float startScaleFinal = startScale;

        // Animate the four positioning/sizing properties in parallel,
        // back to their original values.
        AnimatorSet set = new AnimatorSet();
        set.play(ObjectAnimator
                .ofFloat(slideholder, View.X, startBounds.left))
                .with(ObjectAnimator
                        .ofFloat(slideholder,
                                View.Y,startBounds.top))
                .with(ObjectAnimator
                        .ofFloat(slideholder,
                                View.SCALE_X, startScaleFinal))
                .with(ObjectAnimator
                        .ofFloat(slideholder,
                                View.SCALE_Y, startScaleFinal));
        set.setDuration(mShortAnimationDuration);
        set.setInterpolator(new DecelerateInterpolator());
        set.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                //gridholder.setAlpha(1f);
                slideholder.setVisibility(View.GONE);
                mCurrentAnimator = null;
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                gridholder.setAlpha(1f);
                slideholder.setVisibility(View.GONE);
                mCurrentAnimator = null;
            }
        });
        set.start();
        mCurrentAnimator = set;
    }

    private void loadScreenSlide(int position, View view)
    {
        // If there's an animation in progress, cancel it
        // immediately and proceed with this one.
        if (mCurrentAnimator != null) {
            mCurrentAnimator.cancel();
        }

        screenSlideFragment.SetPostition(position);

        // Calculate the starting and ending bounds for the zoomed-in image.
        // This step involves lots of math. Yay, math.
        final Rect startBounds = new Rect();
        final Rect finalBounds = new Rect();
        final Point globalOffset = new Point();

        // The start bounds are the global visible rectangle of the thumbnail,
        // and the final bounds are the global visible rectangle of the container
        // view. Also set the container view's offset as the origin for the
        // bounds, since that's the origin for the positioning animation
        // properties (X, Y).
        view.getGlobalVisibleRect(startBounds);
        gridholder.getGlobalVisibleRect(finalBounds, globalOffset);
        startBounds.offset(-globalOffset.x, -globalOffset.y);
        finalBounds.offset(-globalOffset.x, -globalOffset.y);

        // Adjust the start bounds to be the same aspect ratio as the final
        // bounds using the "center crop" technique. This prevents undesirable
        // stretching during the animation. Also calculate the start scaling
        // factor (the end scaling factor is always 1.0).
        float startScale;
        if ((float) finalBounds.width() / finalBounds.height()
                > (float) startBounds.width() / startBounds.height()) {
            // Extend start bounds horizontally
            startScale = (float) startBounds.height() / finalBounds.height();
            float startWidth = startScale * finalBounds.width();
            float deltaWidth = (startWidth - startBounds.width()) / 2;
            startBounds.left -= deltaWidth;
            startBounds.right += deltaWidth;
        } else {
            // Extend start bounds vertically
            startScale = (float) startBounds.width() / finalBounds.width();
            float startHeight = startScale * finalBounds.height();
            float deltaHeight = (startHeight - startBounds.height()) / 2;
            startBounds.top -= deltaHeight;
            startBounds.bottom += deltaHeight;
        }

        // Hide the thumbnail and show the zoomed-in view. When the animation
        // begins, it will position the zoomed-in view in the place of the
        // thumbnail.
        //slideholder.setAlpha(0f);
        slideholder.setVisibility(View.VISIBLE);

        // Set the pivot point for SCALE_X and SCALE_Y transformations
        // to the top-left corner of the zoomed-in view (the default
        // is the center of the view).
        slideholder.setPivotX(0f);
        slideholder.setPivotY(0f);

        // Construct and run the parallel animation of the four translation and
        // scale properties (X, Y, SCALE_X, and SCALE_Y).
        AnimatorSet set = new AnimatorSet();
        set
                .play(ObjectAnimator.ofFloat(slideholder, View.X,
                        startBounds.left, finalBounds.left))
                .with(ObjectAnimator.ofFloat(slideholder, View.Y,
                        startBounds.top, finalBounds.top))
                .with(ObjectAnimator.ofFloat(slideholder, View.SCALE_X,
                        startScale, 1f)).with(ObjectAnimator.ofFloat(slideholder,
                View.SCALE_Y, startScale, 1f));
        set.setDuration(mShortAnimationDuration);
        set.setInterpolator(new DecelerateInterpolator());
        set.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mCurrentAnimator = null;
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                mCurrentAnimator = null;
            }
        });
        set.start();
        mCurrentAnimator = set;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == gridViewFragment.STACK_REQUEST || requestCode == gridViewFragment.DNGCONVERT_REQUEST || requestCode == ActivityAbstract.DELETE_REQUEST_CODE)
        {
            List<BaseHolder> files = fileListController.getFiles();
            if(files.size() > 0 && files.get(0).IsFolder()) {

                BaseHolder f =  files.get(0);
                fileListController.LoadFolder(f, gridViewFragmentModelView.formatsToShow);
            }
            else
                fileListController.loadDefaultFiles();
        }
    }

}
