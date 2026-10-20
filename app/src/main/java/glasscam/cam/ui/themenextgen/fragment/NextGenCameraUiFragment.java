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

package glasscam.cam.ui.themenextgen.fragment;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.PointF;
import android.hardware.camera2.params.Face;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProvider;

import com.hazia.glasscam.R;
import com.hazia.glasscam.R.anim;
import com.hazia.glasscam.R.id;
import com.hazia.glasscam.databinding.NextgenCamerauiFragmentBinding;

import glasscam.ActivityAbstract;
import glasscam.ActivityContainer;
import glasscam.GlassCamApplication;
import glasscam.ViewModelFactory;
import glasscam.cam.apis.CameraApiManager;
import glasscam.cam.apis.basecamera.CameraWrapperInterface;
import glasscam.cam.apis.basecamera.Size;
import glasscam.cam.apis.basecamera.parameters.AbstractParameter;
import glasscam.cam.apis.basecamera.parameters.ParameterHandler;
import glasscam.cam.apis.camera2.Camera2;
import glasscam.cam.apis.camera2.CameraValuesChangedCaptureCallback;
import glasscam.cam.apis.camera2.parameters.manual.ManualToneMapCurveApi2;
import glasscam.cam.event.camera.CameraHolderEvent;
import glasscam.cam.histogram.HistogramController;
import glasscam.cam.histogram.MyHistogram;
import glasscam.cam.previewpostprocessing.PreviewController;
import glasscam.cam.ui.I_swipe;
import glasscam.cam.ui.SwipeMenuListner;
import glasscam.cam.ui.guide.GuideHandler;
import glasscam.cam.ui.infooverlay.modelview.InfoOverlayModelView;
import glasscam.cam.ui.themenextgen.view.NextGenCameraUiTextSwitch;
import glasscam.cam.ui.themenextgen.view.NextGenGalleryButton;
import glasscam.cam.ui.SettingsChildAbstract;
import glasscam.cam.ui.cameraui.HorizontLineFragment;
import glasscam.cam.ui.handler.FocusImageHandler;
import glasscam.cam.ui.handler.UserMessageHandler;
import glasscam.settings.SettingKeys;
import glasscam.settings.SettingsManager;
import glasscam.settings.mode.BooleanSettingModeInterface;
import glasscam.update.ReleaseChecker;
import glasscam.update.VersionView;
import glasscam.utils.LocationManager;
import glasscam.utils.Log;
import glasscam.views.CurveView;
import glasscam.views.CurveViewControl;
import glasscam.views.FaceRectDrawer;
import glasscam.views.pagingview.PagingViewTouchState;
import glasscam.views.shutter.ShutterButton;

/**
 * Created by troop on 14.06.2015.
 */
public class NextGenCameraUiFragment extends Fragment implements
        SettingsChildAbstract.CloseChildClick,
        I_swipe,
        OnClickListener,
        CameraHolderEvent,
        CurveView.CurveChangedEvent,
        CameraValuesChangedCaptureCallback.FaceEvent
{
    final String TAG = NextGenCameraUiFragment.class.getSimpleName();

    private NextgenCamerauiFragmentBinding binding;

    //hold the button wich opened the horizontalValuesFragment
    private NextGenCameraUiTextSwitch currentOpendChild;
    //Shows the values when a uibutton got clicked
    private NextGenHorizontalValuesFragment horizontalValuesFragment;
    //there the horizontalValuesFragment gets inflated
    private View camerauiValuesFragmentHolder;
    //handels the touch events that happen on screen
    private SwipeMenuListner touchHandler;
    //well that get clicked when we want to start an action
    private ShutterButton shutterButton;
    //holds the manualButtons
    private NextGenManualsFragment manualModesFragment;
    //used to inflate the manualsModesFragment
    private FrameLayout manualModes_holder;
    //open state from manualModesFragment
    private boolean manualsettingsIsOpen;
    //Handel the animation/visibility for focus trigger, meteringarea, manualwbarea
    private FocusImageHandler focusImageHandler;
    //show the time,sdspace, pic/video size
    private InfoOverlayModelView infoOverlayModelView;
    //private SampleInfoOverlayHandler infoOverlayHandler;
    //holds guide
    private GuideHandler guideHandler;

    private HorizontLineFragment horizontLineFragment;

    private boolean rightbuttonvisible = true;
    private CurveViewControl curveView;

    private FaceRectDrawer faceRectDrawer;
    private FrameLayout faceDrawerHolder;

    public SettingsManager settingsManager;
    UserMessageHandler userMessageHandler;
    CameraApiManager cameraApiManager;
    PagingViewTouchState pagingViewTouchState;
    HistogramController histogramController;
    PreviewController preview;
    LocationManager locationManager;

    @Override
    public void onAttach(@androidx.annotation.NonNull android.content.Context context) {
        super.onAttach(context);
        ActivityContainer c = ((ActivityAbstract) getActivity()).container;
        settingsManager = GlassCamApplication.settingsManager();
        userMessageHandler = c.userMessageHandler();
        cameraApiManager = c.cameraApiManager();
        pagingViewTouchState = c.pagingViewTouchState();
        histogramController = c.histogramController();
        preview = c.previewController();
        locationManager = c.locationManager();
    }

    private final Handler handler = new Handler();

    private final i_HelpFragment helpfragmentCloser = fragment -> {
        FragmentTransaction transaction = getChildFragmentManager().beginTransaction();
        transaction.remove(fragment);
        transaction.addToBackStack(null);
        transaction.commit();
    };

    public NextGenCameraUiFragment()
    {
    }

    private void setCameraToUi(CameraWrapperInterface wrapper) {

            binding.leftUiHolder.removeAllViews();
            binding.rightUiItemsBottom.removeAllViews();
            binding.linearlayoutPreviewpostprocessingmodes.removeAllViews();
            binding.rightTopHolder.removeAllViews();
        if (wrapper == null) {
            if (focusImageHandler != null) {
                focusImageHandler.AEMeteringSupported(false);
                focusImageHandler.TouchToFocusSupported(false);
                shutterButton.setVisibility(View.GONE);
                if (faceDrawerHolder != null)
                    faceDrawerHolder.removeAllViews();
                if (isAdded())
                    hide_ManualSettings();
            }
        }
        else {

            ParameterHandler parameterHandler = wrapper.getParameterHandler();
            if (parameterHandler == null)
                return;
            // Column arrangement from the 2026-09-16 layout search (tools/critique/opt_run, #1648).
            // The four holders are the cells of a 2-column grid around the shutter:
            //   above:  linearlayoutPreviewpostprocessingmodes (left) | leftUiHolder (right)
            //   below:  rightTopHolder (left)                         | rightUiItemsBottom (right)
            // Above the shutter: the left holder stacks camera switch, self-timer; the right holder
            // stacks every toggle the camera exposes - histogram, focus peak, clipping, QCOM
            // histogram, tone curve, focus mode. On the target phone in the default preview mode
            // that is histogram, focus mode, so the rows pair camera switch | histogram, self-timer |
            // focus mode; with more toggles (OpenGL preview) the right side grows past two rows and
            // the shutter is no longer centred (SPEC C1/T6, open). Below it: gallery | module, then AE-lock | flash - the two
            // most-used items sit directly under the thumb and pairs that change together share a
            // row. (Search runner-up, 0.883 vs 0.884: the winner's third row below the shutter
            // clipped on a 360dp screen, and the owner wants the shutter centred.)
            LinearLayout aboveLeft = binding.linearlayoutPreviewpostprocessingmodes;
            LinearLayout aboveRight = binding.leftUiHolder;
            LinearLayout belowLeft = binding.rightTopHolder;
            LinearLayout belowRight = binding.rightUiItemsBottom;

            addUiTextSwitchWithValue(aboveLeft, (AbstractParameter) parameterHandler.get(SettingKeys.CAMERA_SWITCH),GlassCamApplication.getStringFromRessources(R.string.font_camera), onNextGenButtonClick);
            addUiTextSwitchWithValue(aboveLeft, (AbstractParameter) parameterHandler.get(SettingKeys.SELF_TIMER),GlassCamApplication.getStringFromRessources(R.string.font_exposuretime),onNextGenButtonClick);

            // one histogram button (owner, purpose map 2026-10-20): the hardware histogram where the
            // phone has one, the app's own (OpenGL preview only) where it does not
            if (parameterHandler.get(SettingKeys.HISTOGRAM) != null && parameterHandler.get(SettingKeys.HISTOGRAM_STATS_QCOM) == null)
                addUiTextSwitch(aboveRight, (BooleanSettingModeInterface) parameterHandler.get(SettingKeys.HISTOGRAM), GlassCamApplication.getStringFromRessources(R.string.font_flash_histogram));
            if (parameterHandler.get(SettingKeys.FOCUSPEAK) != null)
                addUiTextSwitch(aboveRight, (BooleanSettingModeInterface) parameterHandler.get(SettingKeys.FOCUSPEAK), GlassCamApplication.getStringFromRessources(R.string.font_focuspeak_on));
            if (parameterHandler.get(SettingKeys.CLIPPING) != null)
                addUiTextSwitch(aboveRight, (BooleanSettingModeInterface) parameterHandler.get(SettingKeys.CLIPPING),GlassCamApplication.getStringFromRessources(R.string.font_clipping));
            if (parameterHandler.get(SettingKeys.HISTOGRAM_STATS_QCOM) != null)
                addUiTextSwitch(aboveRight, (BooleanSettingModeInterface) parameterHandler.get(SettingKeys.HISTOGRAM_STATS_QCOM), GlassCamApplication.getStringFromRessources(R.string.font_flash_histogram));
            if (parameterHandler.get(SettingKeys.TONE_CURVE_PARAMETER)!= null)
                addUiTextSwitchWithValue(aboveRight, (AbstractParameter) parameterHandler.get(SettingKeys.TONE_CURVE_PARAMETER),GlassCamApplication.getStringFromRessources(R.string.font_tonecurve),onNextGenToneCurveButtonClick);
            if (parameterHandler.get(SettingKeys.FOCUS_MODE) != null)
                addUiTextSwitchWithValue(aboveRight, (AbstractParameter) parameterHandler.get(SettingKeys.FOCUS_MODE),GlassCamApplication.getStringFromRessources(R.string.font_manual_focus),onNextGenButtonClick);

            // row 1 below the shutter: gallery | module
            belowLeft.addView(new NextGenGalleryButton(getContext()), new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, getResources().getDimensionPixelSize(R.dimen.nextgen_cameraui_textswitch)));
            addUiTextSwitchWithValue(belowRight, (AbstractParameter) parameterHandler.get(SettingKeys.MODULE),GlassCamApplication.getStringFromRessources(R.string.font_image), onNextGenButtonClick);
            // row 2: AE lock | flash
            if (parameterHandler.get(SettingKeys.EXPOSURE_LOCK) != null)
                addUiTextSwitch(belowLeft, (BooleanSettingModeInterface) parameterHandler.get(SettingKeys.EXPOSURE_LOCK),GlassCamApplication.getStringFromRessources(R.string.font_exposurelock));
            if (parameterHandler.get(SettingKeys.FLASH_MODE) != null)
                addUiTextSwitch(belowRight, (AbstractParameter) parameterHandler.get(SettingKeys.FLASH_MODE), onNextGenButtonClick);


            if (isAdded()) {
                focusImageHandler.SetCamerUIWrapper(wrapper);

                shutterButton.setVisibility(View.VISIBLE);
                shutterButton.SetCameraUIWrapper(wrapper);

                guideHandler.setCameraUiWrapper(wrapper);

                horizontLineFragment.setCameraUiWrapper(wrapper);

                //restore view state for the manuals
                if (settingsManager.getGlobal(SettingKeys.SHOWMANUALSETTINGS).get())
                    showManualSettings();
                //remove the values fragment from ui when a new api gets loaded and it was open.
                if (horizontalValuesFragment != null && horizontalValuesFragment.isAdded())
                    removeHorizontalFragment();

                if (wrapper instanceof Camera2)
                {
                    faceDrawerHolder = binding.framelyoutFacedrawer;
                    if (faceRectDrawer == null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                        faceRectDrawer = new FaceRectDrawer(getContext());
                    faceDrawerHolder.removeAllViews();
                    faceDrawerHolder.addView(faceRectDrawer);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        ((Camera2) wrapper).cameraBackroundValuesChangedListner.setFaceEventListner(this);
                    }
                }
            }
        }
    }

    private void cyclePictureFormat()
    {
        if (cameraApiManager.getCamera() == null || cameraApiManager.getCamera().getParameterHandler() == null)
            return;
        if (cameraApiManager.getCamera().getModuleHandler().getCurrentModuleName().equals(GlassCamApplication.getStringFromRessources(R.string.module_video)))
            return;
        AbstractParameter format = (AbstractParameter) cameraApiManager.getCamera().getParameterHandler().get(SettingKeys.PICTURE_FORMAT);
        if (format == null || format.getStringValues() == null || format.getStringValues().length == 0)
            return;
        String[] values = format.getStringValues();
        String current = format.getStringValue();
        int next = 0;
        for (int i = 0; i < values.length; i++)
            if (values[i].equals(current)) { next = (i + 1) % values.length; break; }
        format.setStringValue(values[next], true);
    }

    private final int iconsize = 31;
    private final int frontsize = 10;
    private void addUiTextSwitch(LinearLayout root, AbstractParameter parameter, OnClickListener onClickListener) {
        NextGenCameraUiTextSwitch nextgenCamerauiTextSwitchBinding = new NextGenCameraUiTextSwitch(getContext());
        nextgenCamerauiTextSwitchBinding.setParameter(parameter,iconsize);
        nextgenCamerauiTextSwitchBinding.setOnClickListener(onClickListener);
        nextgenCamerauiTextSwitchBinding.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.END);
        root.addView(nextgenCamerauiTextSwitchBinding);
    }

    private void addUiTextSwitchWithValue(LinearLayout root, AbstractParameter parameter,String background, OnClickListener onClickListener) {
        NextGenCameraUiTextSwitch nextgenCamerauiTextSwitchBinding = new NextGenCameraUiTextSwitch(getContext());
        nextgenCamerauiTextSwitchBinding.setParameter(parameter,true,background,iconsize,frontsize);
        nextgenCamerauiTextSwitchBinding.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.END);
        nextgenCamerauiTextSwitchBinding.setOnClickListener(onClickListener);
        root.addView(nextgenCamerauiTextSwitchBinding);
    }

    private void addUiTextSwitch(LinearLayout root, BooleanSettingModeInterface parameter, String icon) {
        NextGenCameraUiTextSwitch nextgenCamerauiTextSwitchBinding = new NextGenCameraUiTextSwitch(getContext());
        nextgenCamerauiTextSwitchBinding.setBooleanSettingModeInterface(parameter, icon,iconsize);
        nextgenCamerauiTextSwitchBinding.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.END);
        root.addView(nextgenCamerauiTextSwitchBinding);
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        super.onCreateView(inflater,container,savedInstanceState);
        binding = NextgenCamerauiFragmentBinding.inflate(inflater);
        Log.d(TAG, "####################ONCREATEDVIEW####################");
        touchHandler = new SwipeMenuListner(this);
        manualsettingsIsOpen = settingsManager.getGlobal(SettingKeys.SHOWMANUALSETTINGS).get();
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        userMessageHandler.setMessageTextView(view.findViewById(id.textView_usermessage));
        manualModes_holder = binding.manualModesHolder;
        cameraApiManager.addEventListner(this);
        infoOverlayModelView = new ViewModelProvider(this, new ViewModelFactory(() ->
                new InfoOverlayModelView(GlassCamApplication.getContext()))).get(InfoOverlayModelView.class);
        getLifecycle().addObserver(infoOverlayModelView);
        infoOverlayModelView.setCameraApiManager(cameraApiManager);
        infoOverlayModelView.setSettingsManager(settingsManager);
        infoOverlayModelView.setLocationManager(locationManager);
        binding.infoOverlay.setInfoOverlayModel(infoOverlayModelView.getInfoOverlayModel());
        // Batch 2: the overlay already shows the picture format; tapping it now cycles Jpeg / DNG /
        // Jpeg+DNG instead of a four-tap trip through the drawer (critique workflow table, task f).
        binding.infoOverlay.textViewFormat.setOnClickListener(v -> cyclePictureFormat());
        binding.buttonSettings.setOnClickListener(v -> {
            Log.d(TAG, "settings gear tapped; parent=" + getParentFragment());
            if (getParentFragment() instanceof glasscam.cam.ui.themenextgen.NextGenMainFragment)
                ((glasscam.cam.ui.themenextgen.NextGenMainFragment) getParentFragment()).openSettings();
        });
        curveView = view.findViewById(id.curveView);
        curveView.setCurveChangedListner(this);
        curveView.setVisibility(View.GONE);

        focusImageHandler = new FocusImageHandler(view, (ActivityAbstract) getActivity(), pagingViewTouchState);

        shutterButton = binding.shutterButton;

        view.setOnTouchListener(onTouchListener);

        MyHistogram histogram = binding.hisotview;
        histogramController.setMyHistogram(histogram);
        histogramController.setCameraHistogram(binding.hisotviewcamera);
        ImageView waveform = binding.imageViewWaveform;
        waveform.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                preview.setColorWaveForm(!preview.isColorWaveForm());
            }
        });
        histogramController.setWaveFormView(waveform);


        manualModesFragment = new NextGenManualsFragment();

        horizontLineFragment = new HorizontLineFragment();

        guideHandler =GuideHandler.getInstance();

        manualModes_holder.setVisibility(View.GONE);
        camerauiValuesFragmentHolder = binding.camerauiValuesFragmentHolder;

        FragmentTransaction transaction = getChildFragmentManager().beginTransaction();
        transaction.replace(id.guideHolder, guideHandler, "Guide");
        transaction.commit();

        transaction = getChildFragmentManager().beginTransaction();
        transaction.setCustomAnimations(anim.bottom_to_top_enter, anim.empty);
        transaction.replace(id.manualModesHolder, manualModesFragment);
        transaction.commit();

        transaction = getChildFragmentManager().beginTransaction();
        transaction.setCustomAnimations(anim.empty, anim.empty);
        transaction.replace(id.horHolder, horizontLineFragment);
        transaction.addToBackStack(null);
        transaction.commit();

        if (settingsManager.getShowHelpOverlay()) {
            transaction = getChildFragmentManager().beginTransaction();
            transaction.setCustomAnimations(anim.empty, anim.empty);
            transaction.replace(id.helpfragment_container, HelpFragment.getFragment(helpfragmentCloser));
            transaction.addToBackStack(null);
            transaction.commit();
        }

        setCameraToUi(cameraApiManager.getCamera());
        checkForUpdate();
        faceDrawerHolder = view.findViewById(R.id.framelyout_facedrawer);
    }

    private void checkForUpdate() {
        if (ReleaseChecker.isGithubRelease && settingsManager.getGlobal(SettingKeys.CHECKFORUPDATES).get()) {
            new ReleaseChecker(new ReleaseChecker.UpdateEvent() {
                @Override
                public void onUpdateAvailable() {
                    binding.framelayoutVersion.post(new Runnable() {
                        @Override
                        public void run() {
                            binding.framelayoutVersion.addView(new VersionView(getContext(), new VersionView.ButtonEvents() {
                                @Override
                                public void onDownloadClick() {
                                    startActivity(Intent.createChooser(new Intent(Intent.ACTION_VIEW, Uri.parse(ReleaseChecker.RELEASES_PAGE)), "Choose browser"));
                                    binding.framelayoutVersion.removeAllViews();
                                }

                                @Override
                                public void onCloseClick() {
                                    try {
                                        if (binding.framelayoutVersion != null)
                                            binding.framelayoutVersion.removeAllViews();
                                    }
                                    catch (ActivityNotFoundException ex)
                                    {
                                        Log.WriteEx(ex);
                                    }

                                }
                            }));
                        }
                    });
                }
            }).isUpdateAvailable();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG,"onDestroy");
        userMessageHandler.setMessageTextView(null);
        cameraApiManager.removeEventListner(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG,"onResume");
    }

    @Override
    public void onPause()
    {
        Log.d(TAG,"onPause");
        settingsManager.getGlobal(SettingKeys.SHOWMANUALSETTINGS).set(manualsettingsIsOpen);
        super.onPause();

    }

    private void hide_ManualSettings()
    {
        manualsettingsIsOpen = false;
        Log.d(TAG, "HideSettings");
        manualModes_holder.animate().translationY(manualModes_holder.getHeight()).setDuration(300);
    }

    private void showManualSettings()
    {
        Log.d(TAG, "ShowSettings");
        manualsettingsIsOpen = true;
        manualModes_holder.animate().translationY(0).setDuration(300);
        manualModes_holder.setVisibility(View.VISIBLE);
    }

    private final OnClickListener onNextGenButtonClick = new OnClickListener() {
        @Override
        public void onClick(View v) {
            if (currentOpendChild == v)
            {
                removeHorizontalFragment();
                currentOpendChild = null;
                return;
            }
            if (currentOpendChild != null)
            {
                removeHorizontalFragment();
                currentOpendChild = null;
            }
            if (horizontalValuesFragment != null)
                horizontalValuesFragment.Clear();

            currentOpendChild = (NextGenCameraUiTextSwitch) v;
            horizontalValuesFragment = new NextGenHorizontalValuesFragment();
            String[] tmo = currentOpendChild.getParameter().getStringValues();
            if (tmo != null && tmo.length >0)
                horizontalValuesFragment.SetStringValues(tmo, NextGenCameraUiFragment.this);
            inflateIntoHolder(id.cameraui_values_fragment_holder, horizontalValuesFragment);
        }
    };


    private final OnClickListener onNextGenToneCurveButtonClick = new OnClickListener() {

        @Override
        public void onClick(View v) {
            if (curveView.getVisibility() == View.GONE) {
                curveView.setVisibility(View.VISIBLE);
                curveView.bringToFront();
            }
            else {
                curveView.setVisibility(View.GONE);
            }
        }
    };



    private void inflateIntoHolder(int id, Fragment fragment)
    {
        FragmentTransaction transaction = getChildFragmentManager().beginTransaction();
        transaction.setCustomAnimations(anim.left_to_right_enter, 0);
        transaction.replace(id, fragment);
        transaction.commit();
    }

    private void removeHorizontalFragment()
    {
        getChildFragmentManager().beginTransaction().remove(horizontalValuesFragment).setCustomAnimations(0, anim.right_to_left_exit).commit();
    }

    @Override
    public void onCloseClicked(String value) {
        currentOpendChild.getParameter().setStringValue(value,true);
        currentOpendChild.setText(value);
        removeHorizontalFragment();
        currentOpendChild = null;
    }



    @Override
    public void  doRightToLeftSwipe() {

    }

    @Override
    public void doLeftToRightSwipe(){
    }

    @Override
    public void doTopToBottomSwipe(){
        hide_ManualSettings();
    }

    @Override
    public void doBottomToTopSwipe(){
        showManualSettings();
    }

    @Override
    public void onClick(int x, int y) {
        if (focusImageHandler != null)
            focusImageHandler.OnClick(x,y);
    }

    @Override
    public void onMotionEvent(MotionEvent event) {
        if (focusImageHandler != null)
            focusImageHandler.onTouchEvent(event);
    }

    OnTouchListener onTouchListener = new OnTouchListener()
    {
        public boolean onTouch(View v, MotionEvent event)
        {
            return touchHandler.onTouchEvent(event);
        }

    };

    //On Settings Menu Click
    @Override
    public void onClick(View v) {

    }

    @Override
    public void onCameraOpen() {

    }

    @Override
    public void onCameraOpenFinished() {
        // posted from the camera thread; skip if this instance was detached by a recreation
        handler.post(() -> { if (isAdded() && getContext() != null) setCameraToUi(cameraApiManager.getCamera()); });
    }

    @Override
    public void onCameraClose() {
        // same thread and same race as onCameraOpenFinished: guard the same way
        handler.post(() -> { if (isAdded() && getContext() != null) setCameraToUi(null); });
    }

    @Override
    public void onCameraError(String error) {

    }

    @Override
    public void onCameraChangedAspectRatioEvent(Size size) {

    }


    public static float[] pointFtoFloatArray(PointF[] pointFs)
    {
        float[] ar = new float[pointFs.length*2];
        int count = 0;
        for (int i = 0; i< pointFs.length; i++)
        {
            ar[count++] = pointFs[i].x;
            ar[count++] = pointFs[i].y;
        }
        return ar;
    }

    @Override
    public void onCurveChanged(PointF[] pointFs) {
        float[] ar = new float[pointFs.length*2];
        int count = 0;
        for (int i = 0; i< pointFs.length; i++)
        {
            ar[count++] = pointFs[i].x;
            ar[count++] = pointFs[i].y;
        }
        ((ManualToneMapCurveApi2.ToneCurveParameter) cameraApiManager.getCamera().getParameterHandler().get(SettingKeys.TONE_CURVE_PARAMETER)).setCurveToCamera(ar);
    }

    @Override
    public void onCurveChanged(PointF[] r, PointF[] g, PointF[] b) {
        ((ManualToneMapCurveApi2.ToneCurveParameter) cameraApiManager.getCamera().getParameterHandler().get(SettingKeys.TONE_CURVE_PARAMETER)).setCurveToCamera(pointFtoFloatArray(r),pointFtoFloatArray(g),pointFtoFloatArray(b));
    }

    @Override
    public void onTouchStart() {
        pagingViewTouchState.setTouchEnable(false);
    }

    @Override
    public void onTouchEnd() {
        pagingViewTouchState.setTouchEnable(true);
    }

    @Override
    public void onClick(PointF pointF) {

    }

    @Override
    public void onFacesDetected(Face[] faces) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                if (faceRectDrawer != null)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        faceRectDrawer.setFaces(faces);
                    }
            }
        });
    }

}
