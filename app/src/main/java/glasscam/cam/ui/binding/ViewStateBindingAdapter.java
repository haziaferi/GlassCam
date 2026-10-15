package glasscam.cam.ui.binding;

import android.view.View;

import androidx.databinding.BindingAdapter;

import glasscam.cam.apis.basecamera.parameters.AbstractParameter;

/**
 * Restores the view-state handling that used to live in the (now commented out)
 * EventBus based "applyViewState" methods (see ManualButton, SettingsChildAbstract, etc).
 *
 * The layouts bind to it with:
 *     setViewState="@{parameter.viewState}"
 *
 * Data binding needs a matching @BindingAdapter for that exact attribute name,
 * which never existed - that is why compileDebugJavaWithJavac failed with
 * "Cannot find a setter for <... setViewState> that accepts parameter type
 * AbstractParameter.ViewState".
 */
public class ViewStateBindingAdapter {

    @BindingAdapter("setViewState")
    public static void setViewState(View view, AbstractParameter.ViewState state) {
        if (state == null)
            return;

        switch (state) {
            case Enabled:
                view.setVisibility(View.VISIBLE);
                view.setEnabled(true);
                break;
            case Disabled:
                view.setVisibility(View.VISIBLE);
                view.setEnabled(false);
                break;
            case Visible:
                view.setVisibility(View.VISIBLE);
                view.setEnabled(true);
                break;
            case Hidden:
                view.setVisibility(View.GONE);
                break;
        }
    }
}
