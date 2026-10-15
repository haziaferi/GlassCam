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

package glasscam.cam.ui.cameraui;

import android.graphics.PointF;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;

import androidx.databinding.Observable;

import com.hazia.glasscam.BR;
import com.hazia.glasscam.R;
import com.hazia.glasscam.R.id;
import com.hazia.glasscam.R.layout;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import glasscam.ActivityAbstract;
import glasscam.ActivityContainer;
import glasscam.GlassCamApplication;
import glasscam.cam.apis.CameraApiManager;
import glasscam.cam.apis.basecamera.CameraWrapperInterface;
import glasscam.cam.apis.basecamera.Size;
import glasscam.cam.apis.basecamera.parameters.AbstractParameter;
import glasscam.cam.apis.basecamera.parameters.ParameterHandler;
import glasscam.cam.apis.camera2.parameters.manual.ManualToneMapCurveApi2;
import glasscam.cam.event.camera.CameraHolderEvent;
import glasscam.cam.event.module.ModuleChangedEvent;
import glasscam.cam.ui.KeyPressedController;
import glasscam.cam.ui.AbstractFragment;
import glasscam.cam.ui.cameraui.childs.ManualButtonMF;
import glasscam.cam.ui.cameraui.childs.ManualButtonToneCurve;
import glasscam.settings.SettingKeys;
import glasscam.utils.Log;
import glasscam.views.CurveView;
import glasscam.views.CurveViewControl;
import glasscam.views.RotatingSeekbar;
import glasscam.views.pagingview.PagingViewTouchState;

/**
 * Created by troop on 08.12.2015.
 */
public class ManualFragment extends AbstractFragment implements OnSeekBarChangeListener, ModuleChangedEvent, CurveView.CurveChangedEvent, CameraHolderEvent
{
    private int currentValuePos;

    private RotatingSeekbar seekbar;

    private ManualButton currentButton;

    private CurveViewControl curveView;

    private AfBracketSettingsView afBracketSettingsView;

    private LinearLayout manualItemsHolder;

    private final Handler handler = new Handler();

    private final String TAG = ManualFragment.class.getSimpleName();
    CameraApiManager cameraApiManager;
    PagingViewTouchState pagingViewTouchState;
    KeyPressedController keyPressedController;

    @Override
    public void onAttach(@androidx.annotation.NonNull android.content.Context context) {
        super.onAttach(context);
        ActivityContainer c = ((ActivityAbstract) getActivity()).container;
        cameraApiManager = c.cameraApiManager();
        pagingViewTouchState = c.pagingViewTouchState();
        keyPressedController = c.keyPressedController();
    }

    private HashMap<SettingKeys.Key, ManualButton> buttonHashMap;
    private List<SettingKeys.Key> supportedManuals;

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        return inflater.inflate(layout.cameraui_manual_fragment_rotatingseekbar, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        buttonHashMap = new HashMap<>();
        supportedManuals = new ArrayList<>();
        keyPressedController.setManualModeChangedEventListner(manualModeChangedEvent);
        seekbar = view.findViewById(id.seekbar);
        seekbar.setOnSeekBarChangeListener(this);
        seekbar.setVisibility(View.GONE);

        curveView = view.findViewById(id.curveView);
        curveView.setVisibility(View.GONE);

        manualItemsHolder = view.findViewById(id.manualItemsHolder);

        afBracketSettingsView = view.findViewById(id.manualFragment_afbsettings);
        afBracketSettingsView.setVisibility(View.GONE);
        cameraApiManager.addEventListner(this);
        cameraApiManager.addModuleChangedEventListner(this);
        onCameraOpenFinished();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        cameraApiManager.removeEventListner(this);
        cameraApiManager.removeModuleChangedEventListner(this);
    }

    private void setCameraToUi(CameraWrapperInterface cameraUiWrapper)
    {
        if (manualItemsHolder == null)
            return;
        //rest views to init state
        manualItemsHolder.removeAllViews();
        buttonHashMap.clear();
        supportedManuals.clear();
        seekbar.setVisibility(View.GONE);
        if (currentButton != null) {
            currentButton.SetActive(false);
            currentButton = null;
        }
        afBracketSettingsView.setVisibility(View.GONE);


        if (cameraUiWrapper != null)
        {
            ParameterHandler parms = cameraUiWrapper.getParameterHandler();
            addManualButton(parms,SettingKeys.M_ZOOM,R.drawable.manual_zoom);
            //addManualButton(parms,SettingKeys.M_Focus,R.drawable.manual_focus);
            //used to simple check if its mf and then activate near/far limits in afbracket module
            //need a rework that modules can have their own specific settings
            if (parms.get(SettingKeys.M_FOCUS) != null) {
                ManualButtonMF btn = new ManualButtonMF(getContext(), parms.get(SettingKeys.M_FOCUS), R.drawable.manual_focus);
                btn.setOnClickListener(manualButtonClickListner);
                manualItemsHolder.addView(btn);
                buttonHashMap.put(SettingKeys.M_FOCUS,btn);
                supportedManuals.add(SettingKeys.M_FOCUS);
            }
            addManualButton(parms,SettingKeys.M_MANUAL_ISO,R.drawable.manual_iso);
            addManualButton(parms,SettingKeys.M_EXPOSURE_TIME,R.drawable.manual_shutter);
            addManualButton(parms,SettingKeys.M_FNUMBER,R.drawable.manual_fnum);
            addManualButton(parms,SettingKeys.M_APERTURE,R.drawable.manual_fnum);
            addManualButton(parms,SettingKeys.M_EXPOSURE_COMPENSATION,R.drawable.manual_exposure);
            addManualButton(parms,SettingKeys.M_WHITEBALANCE,R.drawable.manual_wb);
            addManualButton(parms,SettingKeys.M_BURST,R.drawable.manual_burst);
            addManualButton(parms,SettingKeys.M_CONTRAST,R.drawable.manual_contrast);
            addManualButton(parms,SettingKeys.M_Brightness,R.drawable.brightness);
            addManualButton(parms,SettingKeys.M_SATURATION,R.drawable.manual_saturation);
            addManualButton(parms,SettingKeys.M_SHARPNESS,R.drawable.manual_sharpness);
            addManualButton(parms,SettingKeys.M_FX,R.drawable.manual_fx);
            addManualButton(parms,SettingKeys.M_PROGRAM_SHIFT,R.drawable.manual_shift);

            /*if (parms.black != null) {
                ManualButton btn = new ManualButton(getContext(), null, parms.black, R.drawable.manual_black);
                btn.setOnClickListener(manualButtonClickListner);
                manualItemsHolder.addView(btn);
            }
            if (parms.shadows != null) {
                ManualButton btn = new ManualButton(getContext(), null, parms.shadows, R.drawable.manual_shadows);
                btn.setOnClickListener(manualButtonClickListner);
                manualItemsHolder.addView(btn);
            }
            if (parms.midtones != null) {
                ManualButton btn = new ManualButton(getContext(), null, parms.midtones, R.drawable.manual_midtones);
                btn.setOnClickListener(manualButtonClickListner);
                manualItemsHolder.addView(btn);
            }
            if (parms.highlights != null) {
                ManualButton btn = new ManualButton(getContext(), null, parms.highlights, R.drawable.manual_highlights);
                btn.setOnClickListener(manualButtonClickListner);
                manualItemsHolder.addView(btn);
            }
            if (parms.white != null) {
                ManualButton btn = new ManualButton(getContext(), null, parms.white, R.drawable.manual_white);
                btn.setOnClickListener(manualButtonClickListner);
                manualItemsHolder.addView(btn);
            }*/
            if (parms.get(SettingKeys.TONE_CURVE_PARAMETER) != null)
            {
                ManualButtonToneCurve btn = new ManualButtonToneCurve(getContext(), parms.get(SettingKeys.TONE_CURVE_PARAMETER), R.drawable.manual_midtones);
                btn.setOnClickListener(manualButtonClickListner);
                //btn.onStringValueChanged("");
                manualItemsHolder.addView(btn);
            }
            curveView.setVisibility(View.GONE);
            curveView.setCurveChangedListner(this);

            if (parms.get(SettingKeys.M_ZEBRA_HIGH) != null) {
                ManualButton btn = new ManualButton(getContext(), parms.get(SettingKeys.M_ZEBRA_HIGH), R.drawable.clipping);
                btn.setOnClickListener(manualButtonClickListner);
                manualItemsHolder.addView(btn);
            }
            if (parms.get(SettingKeys.M_ZEBRA_LOW) != null) {
                ManualButton btn = new ManualButton(getContext(), parms.get(SettingKeys.M_ZEBRA_LOW), R.drawable.clipping);
                btn.setOnClickListener(manualButtonClickListner);
                manualItemsHolder.addView(btn);
            }

            seekbar.setVisibility(View.GONE);
            if (cameraUiWrapper.getModuleHandler().getCurrentModuleName().equals(GlassCamApplication.getStringFromRessources(R.string.module_afbracket))
                    && currentButton instanceof ManualButtonMF
                    && seekbar.getVisibility() == View.VISIBLE)
                afBracketSettingsView.setVisibility(View.VISIBLE);
            else
                afBracketSettingsView.setVisibility(View.GONE);
            keyPressedController.setSupportedManualsModes(supportedManuals);
            if (currentButton != null)
                keyPressedController.setActiveKey(getKeyFromButton(currentButton));
            else if(supportedManuals.size() > 0)
                keyPressedController.setActiveKey(supportedManuals.get(0));
        }
    }

    private void addManualButton(ParameterHandler parms,SettingKeys.Key key, int drawable)
    {
        if (parms.get(key) != null) {
            ManualButton btn = new ManualButton(getContext(), parms.get(key), drawable);
            btn.setOnClickListener(manualButtonClickListner);
            manualItemsHolder.addView(btn);
            buttonHashMap.put(key,btn);
            supportedManuals.add(key);
        }
    }

    //######## ManualButton Stuff#####
    private final OnClickListener manualButtonClickListner = new OnClickListener() {
        @Override
        public void onClick(View v)
        {
            if (v instanceof ManualButton && ((ManualButton)v).parameter.getViewState() == AbstractParameter.ViewState.Disabled)
                return;
            //when same button gets clicked second time
            if(v == currentButton && seekbar.getVisibility() == View.VISIBLE)
            {
                //hideseekbar and set color back from button
                seekbar.setVisibility(View.GONE);
                currentButton.SetActive(false);
                afBracketSettingsView.setVisibility(View.GONE);
            }
            //if no button was active or a different was clicked
            else
            {
                if (seekbar.getVisibility() == View.GONE)
                    seekbar.setVisibility(View.VISIBLE);
                //when already a button is active disable it
                if (currentButton != null) {
                    ((AbstractParameter)currentButton.parameter).removeOnPropertyChangedCallback(selectedParameterObserver);
                    currentButton.SetActive(false);
                }
                //set the returned view as active and fill seekbar
                currentButton = (ManualButton) v;
                currentButton.SetActive(true);
                keyPressedController.setActiveKey(getKeyFromButton(currentButton));
                ((AbstractParameter)currentButton.parameter).addOnPropertyChangedCallback(selectedParameterObserver);

                if (currentButton instanceof ManualButtonMF && cameraApiManager.getCamera().getModuleHandler().getCurrentModuleName().equals(GlassCamApplication.getStringFromRessources(R.string.module_afbracket)))
                    afBracketSettingsView.setVisibility(View.VISIBLE);
                else
                    afBracketSettingsView.setVisibility(View.GONE);

                if (currentButton instanceof ManualButtonToneCurve)
                {
                    seekbar.setVisibility(View.GONE);
                    if (curveView.getVisibility() == View.GONE) {
                        curveView.setVisibility(View.VISIBLE);
                        curveView.bringToFront();
                    }
                    else {
                        curveView.setVisibility(View.GONE);
                        currentButton.SetActive(false);
                    }
                }
                else {
                    curveView.setVisibility(View.GONE);
                    String[] vals = currentButton.getStringValues();
                    if (vals == null || vals.length == 0) {
                        currentButton.SetActive(false);
                        seekbar.setVisibility(View.GONE);
                        Log.e(TAG, "Values returned from currentButton are NULL!");
                        return;
                    }
                    seekbar.SetStringValues(vals);
                    seekbar.setProgress(currentButton.getCurrentItem(), false);
                    currentValuePos = currentButton.getCurrentItem();
                    Log.d(TAG, "CurrentvaluePos " + currentValuePos);
                }
            }

        }
    };

    //#########################SEEKBAR STUFF#############################

    private SettingKeys.Key getKeyFromButton(ManualButton button)
    {
        for (Map.Entry entry : buttonHashMap.entrySet())
        {
            if (entry.getValue() == button)
                return (SettingKeys.Key) entry.getKey();
        }
        return SettingKeys.M_ZOOM;
    }


    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser)
    {
        Log.d(TAG, "onProgressChanged:" + progress);
        currentValuePos = progress;
        try {
            currentButton.setValueToParameters(progress);
        }
        catch (NullPointerException ex)
        {}

    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        currentButton.setValueToParameters(currentValuePos);
    }

    Observable.OnPropertyChangedCallback selectedParameterObserver = new Observable.OnPropertyChangedCallback() {
        @Override
        public void onPropertyChanged(Observable sender, int propertyId) {
            if (propertyId == BR.viewState)
                onViewStateChanged(((AbstractParameter)sender).getViewState());
            if (propertyId == BR.intValue)
                onIntValueChanged(((AbstractParameter)sender).getIntValue());
            if (propertyId == BR.stringValues)
                onValuesChanged(((AbstractParameter)sender).getStringValues());
        }
    };

    private void onViewStateChanged(AbstractParameter.ViewState value) {
        switch (value)
        {
            case Visible:
                break;
            case Hidden:
                seekbar.setVisibility(View.GONE);
                currentButton.SetActive(false);
                break;
            case Disabled:
                seekbar.post(() -> {
                        seekbar.setVisibility(View.GONE);
                });
                break;
            case Enabled:
                seekbar.post(() -> {
                    seekbar.setVisibility(View.VISIBLE);
                });
                break;
        }
    }

    private void onIntValueChanged(int current)
    {
        if(!seekbar.IsAutoScrolling()&& !seekbar.IsMoving())
        {
            seekbar.setProgress(current, false);
        }
    }

    private void onValuesChanged(String[] values)
    {
        seekbar.SetStringValues(values);
    }


    /**
     * Gets called when the module has changed
     *
     * @param module
     */
    @Override
    public void onModuleChanged(String module)
    {
        if (cameraApiManager.getCamera() == null || GlassCamApplication.getContext() == null)
            return;
        if (module.equals(GlassCamApplication.getStringFromRessources(R.string.module_afbracket)) && seekbar.getVisibility() == View.VISIBLE)
            afBracketSettingsView.setVisibility(View.VISIBLE);
        else
            afBracketSettingsView.setVisibility(View.GONE);
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
    public void onCameraOpen() {

    }

    @Override
    public void onCameraOpenFinished() {
        handler.post(() -> setCameraToUi(cameraApiManager.getCamera()));
    }

    @Override
    public void onCameraClose() {
        handler.post(() -> setCameraToUi(null));

    }

    @Override
    public void onCameraError(String error) {

    }

    @Override
    public void onCameraChangedAspectRatioEvent(Size size) {

    }

    private final KeyPressedController.ManualModeChangedEvent manualModeChangedEvent = new KeyPressedController.ManualModeChangedEvent() {
        @Override
        public void onManualModeChanged(SettingKeys.Key key) {
            ManualButton button = buttonHashMap.get(key);
            if (button != null)
                manualButtonClickListner.onClick(button);
        }
    };
}
