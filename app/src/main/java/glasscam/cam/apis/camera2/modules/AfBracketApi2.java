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

package glasscam.cam.apis.camera2.modules;

import android.annotation.TargetApi;
import android.hardware.camera2.CaptureRequest;
import android.os.Build;
import android.os.Handler;

import com.hazia.glasscam.R;

import glasscam.GlassCamApplication;
import glasscam.cam.ActivityGlassCamMain;
import glasscam.cam.apis.camera2.Camera2;
import glasscam.cam.apis.camera2.parameters.manual.ManualFocus;
import glasscam.cam.event.capture.CaptureStates;
import glasscam.file.holder.BaseHolder;
import glasscam.imaging.FocusStackProcessor;
import glasscam.settings.SettingKeys;
import glasscam.utils.Log;

/**
 * Created by troop on 18.08.2016.
 */
@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class AfBracketApi2 extends PictureModuleApi2
{
    private final String TAG = AfBracketApi2.class.getSimpleName();
    private ManualFocus manualFocus;

    public AfBracketApi2(Camera2 cameraUiWrapper, Handler mBackgroundHandler, Handler mainHandler) {
        super(cameraUiWrapper,mBackgroundHandler,mainHandler);
        name = GlassCamApplication.getStringFromRessources(R.string.module_afbracket);
    }

    private int PICSTOTAKE = 10;

    private int focusStep;
    private int currentFocusPos;
    private int focusCaptureRange;

    @Override
    public String ShortName() {
        return "AfBracket";
    }

    @Override
    public String LongName() {
        return "Af-Bracket";
    }

    @Override
    public void InitModule() {
        super.InitModule();
        manualFocus = (ManualFocus) cameraUiWrapper.getParameterHandler().get(SettingKeys.M_FOCUS);
        cameraUiWrapper.getParameterHandler().get(SettingKeys.M_BURST).setIntValue(PICSTOTAKE-1, true);
        focusCaptureRange = parameterHandler.get(SettingKeys.M_FOCUS).getStringValues().length -1;
        focusStep =  focusCaptureRange /PICSTOTAKE;
        currentFocusPos = 1;
        changeCaptureState(CaptureStates.image_capture_stop);

    }

    @Override
    public void DestroyModule() {
        super.DestroyModule();
    }

    @Override
    protected void onStartTakePicture() {
        super.onStartTakePicture();
        PICSTOTAKE = cameraUiWrapper.getParameterHandler().get(SettingKeys.M_BURST).getIntValue();
        cameraUiWrapper.captureSessionHandler.SetCaptureParameter(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_OFF);
        cameraUiWrapper.captureSessionHandler.SetPreviewParameter(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_OFF,true);
        int max  = 0;
        int min = 0;
        try {
            min = Integer.parseInt(settingsManager.get(SettingKeys.AF_BRACKET_MIN).get());
            max = Integer.parseInt(settingsManager.get(SettingKeys.AF_BRACKET_MAX).get());
        }
        catch (NumberFormatException ex)
        {
            min = 0;
            max = 0;
        }

        if (min == 0 && max == 0)
        {
            focusCaptureRange = settingsManager.get(SettingKeys.M_FOCUS).getValues().length -1;
            focusStep = focusCaptureRange /PICSTOTAKE;
            currentFocusPos = 1;
        }
        else {
            if (max > min){
                focusCaptureRange = max - min;
                currentFocusPos = min;
            }
            else {
                focusCaptureRange = min - max;
                currentFocusPos = max;
            }
            focusStep = focusCaptureRange /PICSTOTAKE;

        }
        Log.d(TAG,"onStartTakePicture() min:" + min + " max:" + max +" focusCaptureRange:" + focusCaptureRange + " focusStep:" + focusStep + " currentFocusPos:" + currentFocusPos);
    }

    @Override
    protected void prepareCaptureBuilder(int captureNum) {
        cameraUiWrapper.captureSessionHandler.SetCaptureParameter(CaptureRequest.LENS_FOCUS_DISTANCE, manualFocus.getFloatValue(currentFocusPos));
        cameraUiWrapper.captureSessionHandler.SetPreviewParameter(CaptureRequest.LENS_FOCUS_DISTANCE, manualFocus.getFloatValue(currentFocusPos),true);
        Log.d(TAG,"prepareCaptureBuilder() focusCaptureRange:" + focusCaptureRange + " focusStep:" + focusStep + " currentFocusPos:" + currentFocusPos + " :" +  manualFocus.getFloatValue(currentFocusPos) + " :" + manualFocus.getStringValue(currentFocusPos));
        currentFocusPos +=focusStep;
        if (currentFocusPos >= parameterHandler.get(SettingKeys.M_FOCUS).getStringValues().length)
            currentFocusPos = parameterHandler.get(SettingKeys.M_FOCUS).getStringValues().length-1;
        cameraUiWrapper.captureSessionHandler.capture();
    }


    @Override
    protected void finishCapture() {
        super.finishCapture();
    }

    @Override
    public void fireOnWorkFinish(BaseHolder[] files) {
        super.fireOnWorkFinish(files);
        maybeStackFocusBracket(files);
    }

    /** If the person has turned on "Merge Af-Bracket into Focus Stack", kick off a
     *  background merge of the frames this bracket just saved. Runs after (not
     *  instead of) the normal save, so the individual bracket frames are always kept.
     *  Only works for JPEG-producing formats - BitmapRegionDecoder (what
     *  FocusStackProcessor uses to stay memory-safe) doesn't understand RAW/DNG. */
    private void maybeStackFocusBracket(BaseHolder[] files) {
        if (!settingsManager.getGlobal(SettingKeys.FOCUS_STACK_MERGE).get())
            return;
        if (files == null || files.length < 2)
            return;
        String format = settingsManager.get(SettingKeys.PICTURE_FORMAT).get();
        boolean isJpeg = format != null && (
                format.equals(GlassCamApplication.getStringFromRessources(R.string.pictureformat_jpeg))
                        || format.equals(GlassCamApplication.getStringFromRessources(R.string.pictureformat_jpg_p_dng)));
        if (!isJpeg) {
            Log.d(TAG, "FOCUS_STACK_MERGE is on but picture format isn't JPEG-producing - skipping, bracket frames are still saved normally");
            return;
        }
        Log.d(TAG, "starting focus stack merge of " + files.length + " frames");
        ActivityGlassCamMain.userMessageHandler().sendMSG(
                GlassCamApplication.getStringFromRessources(R.string.focusstack_started), false);
        FocusStackProcessor.stackAsync(files, new FocusStackProcessor.Callback() {
            @Override
            public void onFinished(BaseHolder result) {
                Log.d(TAG, "focus stack merge finished: " + result.getName());
                ActivityGlassCamMain.userMessageHandler().sendMSG(
                        GlassCamApplication.getStringFromRessources(R.string.focusstack_done) + " " + result.getName(), false);
            }

            @Override
            public void onFailed(Exception ex) {
                Log.WriteEx(ex);
                ActivityGlassCamMain.userMessageHandler().sendMSG(
                        GlassCamApplication.getStringFromRessources(R.string.focusstack_failed) + " " + ex.getMessage(), false);
            }

            @Override
            public void onProgress(int percent) {
                // stack() only calls this every ~10%, so this isn't spamming the
                // message view - it's throttled at the source.
                ActivityGlassCamMain.userMessageHandler().sendMSG(
                        GlassCamApplication.getStringFromRessources(R.string.focusstack_started) + " " + percent + "%", false);
            }
        });
    }

}
