package glasscam.cam.ui.cameraui.childs;

import android.content.Context;

import glasscam.cam.apis.basecamera.parameters.ParameterInterface;
import glasscam.cam.ui.cameraui.ManualButton;

/**
 * Created by troop on 03.08.2017.
 */

public class ManualButtonToneCurve extends ManualButton
{

    private final String TAG = ManualButtonToneCurve.class.getSimpleName();

    public ManualButtonToneCurve(Context context, ParameterInterface parameter, int drawableImg) {
        super(context, parameter, drawableImg);
    }

}
