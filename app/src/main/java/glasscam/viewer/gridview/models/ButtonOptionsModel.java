package glasscam.viewer.gridview.models;

import android.view.View;
import android.widget.Button;

import glasscam.GlassCamApplication;
import com.hazia.glasscam.R;
import glasscam.viewer.gridview.modelview.GridViewFragmentModelView;

public class ButtonOptionsModel extends VisibilityModel implements  Popup, View.OnClickListener
{
    // Labels and listeners are parallel arrays matched by index in onClickListener below, so they
    // must line up. They did not: {Delete File, StackJpeg, Raw to Dng} was paired with
    // {delete, rawToDng, dngStack}, so "Raw to Dng" launched the DNG stacker and "StackJpeg" the
    // converter (critique 2026-09-16, #3). The stacker stays out until its native TIFFOpen path
    // accepts SAF/MediaStore files; the listener is still accepted so the caller does not change.
    private final String[] values = {
            GlassCamApplication.getStringFromRessources(R.string.gallery_option_delete),
            GlassCamApplication.getStringFromRessources(R.string.gallery_option_raw_to_dng)};
    private final View.OnClickListener[] clickListeners;
    private final GridViewFragmentModelView gridViewFragmentModelView;

    public ButtonOptionsModel(View.OnClickListener onDeleteButtonListner,
                              View.OnClickListener onRawToDngButtonListner,
                              View.OnClickListener onRawStackButtonListner, GridViewFragmentModelView gridViewFragmentModelView)
    {
        clickListeners = new View.OnClickListener[]{onDeleteButtonListner, onRawToDngButtonListner};
        this.gridViewFragmentModelView = gridViewFragmentModelView;
    }

    public String[] getValues() {
        return values;
    }

    public View.OnClickListener[] getClickListeners() {
        return clickListeners;
    }

    @Override
    public View.OnClickListener getOnPopupChildClickListner() {
        return onClickListener;
    }

    private final View.OnClickListener onClickListener = v -> {
        Button button = (Button)v;
        String txt = (String)button.getText();
        for (int i = 0; i < getValues().length; i++)
        {
            if (txt.equals(getValues()[i]))
                getClickListeners()[i].onClick(v);
        }
    };

    @Override
    public void onClick(View v) {
        if (!gridViewFragmentModelView.getPopupMenuModel().getVisibility()) {
            gridViewFragmentModelView.getPopupMenuModel().setButtonOptionsModel(this::getOnPopupChildClickListner);
            gridViewFragmentModelView.getPopupMenuModel().setStrings(values);
        }
        else
            gridViewFragmentModelView.getPopupMenuModel().setVisibility(false);
    }
}
