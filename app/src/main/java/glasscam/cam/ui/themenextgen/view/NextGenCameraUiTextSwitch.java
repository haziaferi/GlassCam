package glasscam.cam.ui.themenextgen.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.hazia.glasscam.R;
import com.hazia.glasscam.databinding.NextgenCamerauiTextSwitchBinding;

import glasscam.cam.apis.basecamera.parameters.AbstractParameter;
import glasscam.settings.mode.BooleanSettingModeInterface;

public class NextGenCameraUiTextSwitch extends LinearLayout {

    NextgenCamerauiTextSwitchBinding binding;
    private final boolean isExpanded = false;
    int vcount = 0;
    private boolean isSwitch = false;
    private boolean showvaluetxt = false;
    private BooleanSettingModeInterface booleanSettingModeInterface;
    private OnClickListener onClickListener;
    public NextGenCameraUiTextSwitch(Context context) {
        super(context);
        bind(context);
    }

    public NextGenCameraUiTextSwitch(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        bind(context);
    }

    public NextGenCameraUiTextSwitch(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        bind(context);
    }

    private void bind(Context context)
    {
        LayoutInflater inflater = LayoutInflater.from(context);
        binding = NextgenCamerauiTextSwitchBinding.inflate(inflater,this,true);
        // the whole 56dp cell is the target, not just the 30dp glyph (critique 2026-09-16)
        binding.layoutHolder.setOnClickListener(onNormalButtonClickListner);
        binding.textViewValueHolder.setOnClickListener(onNormalButtonClickListner);
    }

    /** Short caption for the column: the full value still shows in the popup chips. */
    public static String shortCaption(String value)
    {
        if (value == null)
            return "";
        switch (value) {
            case "Continuous Picture": return "C-AF";
            case "Continuous Video": return "C-Video";
            case "Extended Deep of Field": return "EDOF";
            case "Ae-Bracket": return "AEB";
            case "Af-Bracket": return "AFB";
            case "Interval": return "Int";
            case "Picture": return "Pic";
            case "Video": return "Vid";
            case "Always Flash": return "On";
            case "Auto Flash": return "Auto";
            case "Red Eye": return "Red-eye";
            default: return value;
        }
    }

    public void setParameter(AbstractParameter parameter, float size)
    {
        binding.setParameter(parameter);
        binding.textViewValueHolder.setText(glyph(parameter.getStringValue()));
        binding.textViewValueHolder.setTextSize((int) size);
        binding.textViewFront.setVisibility(GONE);
    }

    /**
     * The glyph slot is set in the icon font. Camera2 parameters already carry the font glyph as
     * their value; Camera1's BaseModeParameter carries the driver's plain word ("off", "auto",
     * "on", "torch", "red-eye"), which the font renders as tofu (walk 2026-09-16, shot 149).
     * The first four map to the Camera2 flash glyphs. "red-eye" has no glyph in the font and
     * still falls through as text - an open owner's call (fresh-eyes 2026-09-28), not handled.
     */
    private String glyph(String value)
    {
        if (value == null)
            return "";
        Context c = getContext();
        switch (value.toLowerCase(java.util.Locale.ROOT)) {
            case "off":      return c.getString(R.string.font_flash_off);
            case "auto":     return c.getString(R.string.font_flash_auto);
            case "on":       return c.getString(R.string.font_flash_always);
            case "torch":    return c.getString(R.string.font_flash_torch);
            default:         return value;
        }
    }

    @Override
    public void setOnClickListener(OnClickListener onClickListener) {
        this.onClickListener = onClickListener;
    }

    public void setParameter(AbstractParameter parameter, boolean showvaluetxt, String backgroundText, float valuesize, float frontsize)
    {
        this.showvaluetxt = showvaluetxt;

        binding.textViewValueHolder.setText(backgroundText);
        binding.textViewFront.setTextSize((int) frontsize);
        binding.textViewValueHolder.setTextSize((int) valuesize);
        if(parameter != null) {
            binding.setParameter(parameter);
            binding.textViewFront.setText(shortCaption(parameter.getStringValue()));
        }
        if (!showvaluetxt)
            binding.textViewFront.setVisibility(GONE);
    }

    public void setBooleanSettingModeInterface(BooleanSettingModeInterface booleanSettingModeInterface,String valueText, float valuesize)
    {
        this.booleanSettingModeInterface = booleanSettingModeInterface;
        binding.textViewValueHolder.setText(valueText);
        binding.textViewValueHolder.setTextSize((int) valuesize);
        binding.textViewFront.setVisibility(GONE);
        if (booleanSettingModeInterface instanceof AbstractParameter)
            binding.setParameter((AbstractParameter)booleanSettingModeInterface);
        isSwitch = true;
    }

    public void setText(String s)
    {
        if (!showvaluetxt)
            binding.textViewValueHolder.setText(glyph(s));
        else
            binding.textViewFront.setText(shortCaption(s));
    }

    public AbstractParameter getParameter()
    {
        return binding.getParameter();
    }

    public OnClickListener onNormalButtonClickListner =new OnClickListener() {
        @Override
        public void onClick(View v) {
            if (!isSwitch) {
                if (onClickListener != null)
                    onClickListener.onClick(NextGenCameraUiTextSwitch.this);
            }
            else
            {
                booleanSettingModeInterface.set(!booleanSettingModeInterface.get());
                setChecked();
            }
        }
    };

    private void setChecked()
    {
        if (booleanSettingModeInterface.get())
            binding.textViewValueHolder.setTextColor(ContextCompat.getColor(getContext(), R.color.manual_button_active));
        else
            binding.textViewValueHolder.setTextColor(ContextCompat.getColor(getContext(), R.color.nextgen_menu_right_text));
    }

    public void setValueTextSize(float size)
    {
        binding.textViewValueHolder.setTextSize((int) size);
    }

    public void setFrontTextSize(float size)
    {
        binding.textViewFront.setTextSize((int) size);
    }

}
