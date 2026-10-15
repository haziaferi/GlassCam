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

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.databinding.Observable;

import com.hazia.glasscam.R.id;
import com.hazia.glasscam.R.layout;
import com.hazia.glasscam.R.string;

import glasscam.GlassCamApplication;
import glasscam.cam.apis.basecamera.CameraWrapperInterface;
import glasscam.cam.apis.basecamera.parameters.AbstractParameter;
import glasscam.cam.ui.AbstractFragment;
import glasscam.settings.SettingKeys;
import glasscam.settings.SettingsManager;
import glasscam.utils.Log;

/**
 * Created by Ar4eR on 15.01.16.
 */
public class HorizontLineFragment extends AbstractFragment {

    private View view;

    private SensorManager sensorManager;
    private Sensor accelerometer;
    private Sensor magnetometer;
    private float[] mGravity;
    private float[] mGeomagnetic;
    private float pitchdegree;
    private final Handler handler = new Handler();
    private Handler sensorHandler;
    private final MySensorListener msl =new MySensorListener();
    private LevelBarView levelBar;
    private final String TAG = HorizontLineFragment.class.getSimpleName();
    float[] orientation = new float[3];
    float yaw;
    float[] R = new float[9];
    float[] I = new float[9];

    SettingsManager settingsManager = GlassCamApplication.settingsManager();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        super.onCreateView(inflater,container,null);
        view = inflater.inflate(layout.cameraui_horizontline, container, false);
        levelBar = view.findViewById(id.levelbar);
        HandlerThread sensorThread = new HandlerThread("Sensor thread", Thread.MAX_PRIORITY);
        sensorThread.start();
        sensorHandler = new Handler(sensorThread.getLooper());
        sensorManager = (SensorManager) getActivity().getSystemService(Context.SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

        return view;
    }

    public void startStopListen(String value) {
        if (view == null)
            return;
        // reached from the camera thread on (re)start: visibility must change on the UI thread or
        // the bar never gets a layout pass (it measured 0x0 after a relaunch with the level on)
        final boolean on = value.equals(GlassCamApplication.getStringFromRessources(string.on));
        view.post(() -> {
            if (on)
            {
                startSensorListing();
                view.setVisibility(View.VISIBLE);
                view.bringToFront();
                if (levelBar != null) {
                    levelBar.setAlpha(1f);
                    levelBar.requestLayout();
                }
            }
            else
            {
                stopSensorListing();
                view.setVisibility(View.GONE);
            }
        });
    }

    public void setCameraUiWrapper(CameraWrapperInterface cameraUiWrapper)
    {
        ((AbstractParameter)cameraUiWrapper.getParameterHandler().get(SettingKeys.HORIZONT_LVL)).addOnPropertyChangedCallback(new Observable.OnPropertyChangedCallback() {
            @Override
            public void onPropertyChanged(Observable sender, int propertyId) {
                startStopListen(cameraUiWrapper.getParameterHandler().get(SettingKeys.HORIZONT_LVL).getStringValue());
            }
        });
        //cameraUiWrapper.getParameterHandler().get(SettingKeys.HorizontLvl).addEventListner(this);
        startStopListen(cameraUiWrapper.getParameterHandler().get(SettingKeys.HORIZONT_LVL).getStringValue());
    }

    private void startSensorListing()
    {
        if (settingsManager.getGlobal(SettingKeys.HORIZONT_LVL).get().equals(GlassCamApplication.getStringFromRessources(string.on))) {
            // SENSOR_DELAY_UI: the old SENSOR_STATUS_ACCURACY_LOW constant (=1) meant SENSOR_DELAY_GAME
            sensorManager.registerListener(msl, accelerometer, SensorManager.SENSOR_DELAY_UI, sensorHandler);
            sensorManager.registerListener(msl, magnetometer, SensorManager.SENSOR_DELAY_UI, sensorHandler);
        }
    }

    private void stopSensorListing()
    {
        if (sensorManager != null)
            sensorManager.unregisterListener(msl);

    }
    @Override
    public void onPause(){
        super.onPause();
        stopSensorListing();
    }
    @Override
    public void onResume(){
        super.onResume();
        try {
            if (settingsManager.getGlobal(SettingKeys.HORIZONT_LVL).get() != null && settingsManager.getGlobal(SettingKeys.HORIZONT_LVL).get().equals(GlassCamApplication.getStringFromRessources(string.off))
                    || TextUtils.isEmpty(settingsManager.getGlobal(SettingKeys.HORIZONT_LVL).get()))
                view.setVisibility(View.GONE);
            else
                startSensorListing();
        }
        catch (NullPointerException ex)
        {
            Log.WriteEx(ex);
        }

    }

    private class MySensorListener implements SensorEventListener {

        static final float ALPHA = 0.2f;

        float[] lowPass(float[] input, float[] output) {
            if ( output == null ) return input;

            for ( int i=0; i<input.length; i++ ) {
                output[i] = input[i] * ALPHA + output[i] * (1.0f - ALPHA);
            }
            return output;
        }

        public void onAccuracyChanged (Sensor sensor, int accuracy) {}

        public void onSensorChanged(final SensorEvent event) {
            if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER)
                mGravity =lowPass( event.values.clone(), mGravity);
            else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD)
                mGeomagnetic = lowPass(event.values.clone(), mGeomagnetic);
            if (mGravity != null && mGeomagnetic != null) {
                boolean success = SensorManager.getRotationMatrix(R, I, mGravity, mGeomagnetic);
                if (success) {
                    SensorManager.remapCoordinateSystem(R, SensorManager.AXIS_X, SensorManager.AXIS_MINUS_Y, I);
                    SensorManager.getOrientation(I, orientation);
                    float rolldegree = get360Degrees(orientation[1]);
                    pitchdegree = (float) Math.toDegrees(orientation[2]);
                    yaw = get360Degrees(orientation[0]);
                    updateUi(pitchdegree, rolldegree, -yaw);
                }
            }
        }
    }

    private float get360Degrees(float input)
    {
        return ((float) Math.toDegrees(input) +360) %360;
    }

    private void updateUi(final float pitch,final  float roll,final float yaw)
    {
        // roll arrives as 0..360 with 0 = level; fold it to a signed angle for the bar
        final float signed = roll > 180f ? roll - 360f : roll;
        handler.post(() -> {
            if (levelBar != null)
                levelBar.setRoll(signed);
        });
    }
}
