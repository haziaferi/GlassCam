package glasscam.cam.ui.themenextgen;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.hazia.glasscam.R;

import glasscam.ActivityAbstract;
import glasscam.ActivityContainer;
import glasscam.GlassCamApplication;
import glasscam.cam.apis.CameraApiManager;
import glasscam.cam.apis.basecamera.Size;
import glasscam.cam.apis.camera2.Camera2;
import glasscam.cam.event.camera.CameraHolderEvent;
import glasscam.cam.histogram.HistogramController;
import glasscam.cam.previewpostprocessing.PreviewController;
import glasscam.cam.previewpostprocessing.PreviewPostProcessingModes;
import glasscam.cam.ui.themenextgen.adapter.NextGenCameraUiSlidePagerAdapter;
import glasscam.file.FileListController;
import glasscam.image.ImageManager;
import glasscam.image.ImageTask;
import glasscam.settings.SettingKeys;
import glasscam.settings.SettingsManager;
import glasscam.utils.Log;
import glasscam.utils.PermissionManager;
import glasscam.viewer.screenslide.views.ScreenSlideFragment;
import glasscam.views.FaceRectDrawer;
import glasscam.views.pagingview.PagingView;

public class NextGenMainFragment extends Fragment implements CameraHolderEvent, PreviewController.PreviewPostProcessingChangedEvent
{
    private static final String TAG = NextGenMainFragment.class.getSimpleName();
    PermissionManager permissionManager;
    ImageManager imageManager = GlassCamApplication.imageManager();
    SettingsManager settingsManager = GlassCamApplication.settingsManager();
    FileListController fileListController = GlassCamApplication.fileListController();
    CameraApiManager cameraApiManager;
    PreviewController previewController;
    HistogramController histogramController;

    @Override
    public void onAttach(@NonNull android.content.Context context) {
        super.onAttach(context);
        ActivityContainer c = ((ActivityAbstract) getActivity()).container;
        permissionManager = c.permissionManager();
        cameraApiManager = c.cameraApiManager();
        previewController = c.previewController();
        histogramController = c.histogramController();
    }
    private PagingView uiViewPager;
    private NextGenCameraUiSlidePagerAdapter uiViewPagerAdapter;
    private LinearLayout nightoverlay;
    private View view;
    private FrameLayout cameraPreview;


    @Override
    public void onCameraOpen() {

    }

    @Override
    public void onCameraOpenFinished() {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                //in case the featuredetector runned bevor, uiViewPagerAdapter is null.
                //thats the correct behavior because we dont want that the helpview overlay the featuredetector on first start
                if (uiViewPagerAdapter == null)
                    initScreenSlide();
                SetNightOverlay();
                imageManager.putImageLoadTask(new LoadGlassCamDcimDirsFilesRunner());
            }
        });
    }

    @Override
    public void onCameraClose() {

    }

    @Override
    public void onCameraError(String error) {

    }

    @Override
    public void onCameraChangedAspectRatioEvent(Size size) {

    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.nextgen_main_fragment, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        this.view = view;
        cameraPreview = view.findViewById(R.id.nextgen_camera_preview);
        cameraApiManager.addEventListner(this);
        previewController.previewPostProcessingChangedEventHandler.setEventListner(this);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        previewController.previewPostProcessingChangedEventHandler.removeEventListner(this);
        cameraApiManager.removeEventListner(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG,"onResume");
        cameraApiManager.onResume();
        if (!settingsManager.appVersionHasChanged() && uiViewPagerAdapter == null)
            initScreenSlide();
    }

    @Override
    public void onPause() {
        super.onPause();
        cameraApiManager.onPause();
    }

    private void initScreenSlide() {

        // Use this fragment's OWN child FragmentManager, not the parent/Activity-level one.
        // NextGenCameraUiSlidePagerAdapter.saveState() intentionally returns null (no state restore),
        // so if the pager's fragments live in the parent manager they can outlive this fragment's
        // view and keep sitting around when a new adapter/page set is created later, causing
        // stray duplicate children on a fresh page set. This used to also manifest as a stray,
        // already-dismissed HelpFragment/onboarding overlay stacking with a fresh one; that was
        // the original symptom that got the overlay removed outright rather than patched. The
        // overlay (glasscam.cam.ui.themenextgen.fragment.HelpFragment) is back as of this change,
        // relying on this child-FragmentManager fix so the same duplication can't recur - if this
        // line ever goes back to getFragmentManager()/getChildFragmentManager() from a different
        // scope, re-check HelpFragment for the stray-overlay symptom before assuming it's unrelated.
        uiViewPagerAdapter = new NextGenCameraUiSlidePagerAdapter(getChildFragmentManager(),onThumbBackClick);
        if (uiViewPager == null)
            uiViewPager = view.findViewById(R.id.viewPager_fragmentHolder);
        uiViewPager.setOffscreenPageLimit(2);
        uiViewPager.setAdapter(uiViewPagerAdapter);
        uiViewPager.setCurrentItem(1);
        uiViewPager.addOnPageChangeListener(new androidx.viewpager.widget.ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                // leaving the settings page: drop the search field's keyboard and focus
                if (position != 0 && getView() != null) {
                    View focus = getView().findFocus();
                    if (focus != null) {
                        android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager)
                                requireContext().getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
                        if (imm != null) imm.hideSoftInputFromWindow(focus.getWindowToken(), 0);
                        focus.clearFocus();
                    }
                }
            }
        });
    }

    /** BACK while the settings page is showing closes it instead of leaving the app (2026-09-16). */
    public boolean closeSettingsIfOpen() {
        if (uiViewPager != null && uiViewPager.getCurrentItem() == 0) {
            uiViewPager.setCurrentItem(1, true);
            return true;
        }
        return false;
    }

    /** Batch 3: the settings page had no visible entry (swipe only) - the gear cell calls this. */
    public void openSettings() {
        if (uiViewPager != null)
            uiViewPager.setCurrentItem(0, true);
    }

    //get called when the back button from screenslidefragment gets clicked
    private final ScreenSlideFragment.ButtonClick onThumbBackClick = new ScreenSlideFragment.ButtonClick() {
        @Override
        public void onButtonClick(int position, View view)
        {
            //show cameraui
            if (uiViewPager != null)
                uiViewPager.setCurrentItem(1);
        }

    };

    public void SetNightOverlay() {
        if (nightoverlay == null)
            nightoverlay = view.findViewById(R.id.nightoverlay);
        Log.d(TAG, "NightOverlay:" + settingsManager.getGlobal(SettingKeys.NIGHT_OVERLAY).get());
        if (settingsManager.getGlobal(SettingKeys.NIGHT_OVERLAY).get())
            nightoverlay.setVisibility(View.VISIBLE);
        else
            nightoverlay.setVisibility(View.GONE);
    }

    @Override
    public void onPreviewPostProcessingChanged() {
        changePreviewPostProcessing();
    }

    private class LoadGlassCamDcimDirsFilesRunner extends ImageTask
    {
        @Override
        public boolean process() {
            fileListController.LoadGlassCamDCIMDirsFiles();
            return false;
        }
    }

    private void changePreviewPostProcessing()
    {
        Log.d(TAG,"changePreviewPostProcessing");
        cameraPreview.removeAllViews();
        if (settingsManager.getGlobal(SettingKeys.PREVIEW_POST_PROCESSING_MODE).get() == null)
            previewController.initPreview(PreviewPostProcessingModes.off,getContext(),histogramController);
        else if (settingsManager.getGlobal(SettingKeys.PREVIEW_POST_PROCESSING_MODE).get().equals(PreviewPostProcessingModes.OpenGL.name()))
            previewController.initPreview(PreviewPostProcessingModes.OpenGL, getContext(), histogramController);
        else
            previewController.initPreview(PreviewPostProcessingModes.off,getContext(),histogramController);
        cameraPreview.addView(previewController.getPreviewView());
    }
}
