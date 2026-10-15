package glasscam.settings;

import android.content.Context;

import com.hazia.glasscam.R;

import glasscam.cam.apis.basecamera.parameters.modes.SDModeParameter;

/**
 * What the settings screen shows for a parameter value. Some values are compared by their text
 * (the storage location in SDModeParameter, the audio source in VideoRecorder) and the value picker
 * hands the shown row's value back to setStringValue, so those values stay English and only the
 * label follows the locale. Scoped by setting, so a "Default" elsewhere is left alone.
 */
public final class ValueLabels
{
    private ValueLabels() {}

    /** Audio-source {value resource (untranslated), label resource} pairs. */
    private static final int[][] AUDIO_SOURCE = {
            {R.string.video_audio_source_default, R.string.audio_source_label_default},
            {R.string.video_audio_source_mic, R.string.audio_source_label_mic},
            {R.string.video_audio_source_camcorder, R.string.audio_source_label_camcorder},
            {R.string.video_audio_source_voice_recognition, R.string.audio_source_label_voice_recognition},
            {R.string.video_audio_source_voice_communication, R.string.audio_source_label_voice_communication},
            {R.string.video_audio_source_unprocessed, R.string.audio_source_label_unprocessed},
    };

    public static String label(Context context, SettingKeys.Key key, String value)
    {
        if (context == null || key == null || value == null)
            return value;
        if (key == SettingKeys.SD_SAVE_LOCATION) {
            if (SDModeParameter.internal.equals(value))
                return context.getString(R.string.sdmode_internal);
            if (SDModeParameter.external.equals(value))
                return context.getString(R.string.sdmode_external);
        }
        else if (key == SettingKeys.VIDEO_AUDIO_SOURCE) {
            for (int[] p : AUDIO_SOURCE)
                if (value.equals(context.getString(p[0])))
                    return context.getString(p[1]);
        }
        return value;
    }
}
