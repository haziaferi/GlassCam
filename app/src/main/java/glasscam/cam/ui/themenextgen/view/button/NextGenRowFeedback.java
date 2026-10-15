package glasscam.cam.ui.themenextgen.view.button;

import android.content.Context;
import android.util.TypedValue;
import android.view.View;

import androidx.core.content.ContextCompat;

/**
 * Applies a Material press ripple to a clickable settings row. The RecyclerView adapter attaches
 * the click listener to the bespoke row View itself (NextGenSettingItem / NextGenSettingButton),
 * so the ripple must live on that same View to react to its pressed state - putting it on an inner
 * layout would never ripple. Uses the theme's ?attr/selectableItemBackground (tinted by
 * colorControlHighlight), drawn as a foreground so it overlays the translucent row without making
 * it opaque. setForeground on an arbitrary View is API 23+, well below this app's minSdk 30.
 */
final class NextGenRowFeedback
{
    private NextGenRowFeedback() {}

    static void apply(View row)
    {
        Context context = row.getContext();
        TypedValue tv = new TypedValue();
        if (context.getTheme().resolveAttribute(android.R.attr.selectableItemBackground, tv, true))
        {
            row.setForeground(ContextCompat.getDrawable(context, tv.resourceId));
        }
        row.setClickable(true);
    }
}
