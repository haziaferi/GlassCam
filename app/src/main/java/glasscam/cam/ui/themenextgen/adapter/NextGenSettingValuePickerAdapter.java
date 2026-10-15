package glasscam.cam.ui.themenextgen.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.hazia.glasscam.R;

import glasscam.settings.SettingKeys;
import glasscam.settings.ValueLabels;

/**
 * RecyclerView replacement for NextGenSettingSubItemAdapter (the value-picker list shown when
 * tapping a setting) - not because that ArrayAdapter was broken (it correctly reused convertView
 * via the standard super.getView() pattern), but because it shared the same GridView widget as
 * the buggy item grid; moving the widget to RecyclerView means both adapters that drive it need
 * to speak RecyclerView.Adapter.
 */
public class NextGenSettingValuePickerAdapter extends RecyclerView.Adapter<NextGenSettingValuePickerAdapter.ViewHolder>
{
    public interface OnValueClickListener
    {
        void onValueClick(int position);
    }

    private final String[] values;
    private final SettingKeys.Key key;
    private int selected;
    private OnValueClickListener onValueClickListener;
    // Batch 2 (2026-09-16): row 0 is the title of the setting being edited (not clickable),
    // row 1 the Back chip, rows 2.. the values. Before, the picker had no title and "Back" was
    // styled like a value.
    public static final int ROW_TITLE = 0, ROW_BACK = 1, ROW_VALUE = 2;

    public NextGenSettingValuePickerAdapter(SettingKeys.Key key, String[] values)
    {
        this.key = key;
        this.values = values;
    }

    @Override
    public int getItemViewType(int position)
    {
        return position == 0 ? ROW_TITLE : position == 1 ? ROW_BACK : ROW_VALUE;
    }

    public void setSelected(int selected)
    {
        this.selected = selected;
        notifyDataSetChanged();
    }

    public void setOnValueClickListener(OnValueClickListener listener)
    {
        this.onValueClickListener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType)
    {
        int layout = viewType == ROW_TITLE ? R.layout.nextgen_setting_picker_title
                : viewType == ROW_BACK ? R.layout.nextgen_setting_picker_back
                : R.layout.nextgen_setting_adapterlayout_simpeltext;
        View v = LayoutInflater.from(parent.getContext()).inflate(layout, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position)
    {
        // rows from ROW_VALUE on hold parameter values; ValueLabels translates the ones compared by text
        holder.textView.setText(position >= ROW_VALUE
                ? ValueLabels.label(holder.itemView.getContext(), key, values[position]) : values[position]);
        // Selected value uses the app's "active control" lime (manual_button_active), matching the
        // rest of the camera UI, instead of a raw pure-green; unselected uses the shared white.
        Context ctx = holder.itemView.getContext();
        holder.textView.setTextColor(ContextCompat.getColor(ctx,
                position == selected ? R.color.manual_button_active : R.color.nextgen_menu_right_text));
        if (getItemViewType(position) != ROW_TITLE)   // the title is a heading, not a target
            holder.itemView.setOnClickListener(v -> {
                if (onValueClickListener != null)
                    onValueClickListener.onValueClick(holder.getBindingAdapterPosition());
            });
    }

    @Override
    public int getItemCount()
    {
        return values.length;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder
    {
        final TextView textView;

        public ViewHolder(@NonNull View itemView)
        {
            super(itemView);
            textView = itemView.findViewById(R.id.listviewlayout_textview);
        }
    }
}
