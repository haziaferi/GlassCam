package glasscam.cam.ui.themenextgen.adapter;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import glasscam.cam.apis.CameraApiManager;
import glasscam.cam.ui.themenextgen.layoutconfig.SettingItemConfig;
import glasscam.settings.SettingsManager;
import glasscam.utils.Log;

/**
 * Replaces the old GridView + ArrayAdapter pairing (NextGenSettingItemFragmentAdapter), which
 * broke Android's adapter contract by always returning each SettingItemConfig's pre-built View
 * from getView() and ignoring the convertView GridView offered for recycling. Every established
 * camera app (and Android app in general) moved this exact class of grid/list UI to RecyclerView
 * years ago specifically because GridView's auto_fit + variable-height-children + ignored-
 * recycling combination is a well-documented source of items rendering in the wrong cell or
 * doubled up in one cell - which is exactly the bug this replaces (two settings' text stacked on
 * top of each other in a single cell, confirmed via bounds dump, not a sizing/spacing issue).
 *
 * Each SettingItemConfig already owns one fully-built, self-contained View from
 * NextGenSettingItemAdapterTools (NextGenSettingItem/NextGenSettingBoolItem/etc. are bespoke per
 * config, not interchangeable templates) - so rather than fighting that with fabricated view-type
 * recycling, each position is given its own stable view type. What keeps a stale view out is that
 * NextGenSettingDummyFragment builds a NEW adapter for every rebuild and swaps it in with
 * setAdapter(), which clears the recycled-view pool for an incompatible adapter; notifyDataSetChanged()
 * on this same adapter would NOT do that (holders of the same view type are rebound, not rebuilt),
 * so call setKeyList() once per adapter. onCreateViewHolder also detaches a view still parented
 * from a previous generation.
 */
public class NextGenSettingItemRecyclerAdapter extends RecyclerView.Adapter<NextGenSettingItemRecyclerAdapter.ViewHolder>
{
    private static final String TAG = NextGenSettingItemRecyclerAdapter.class.getSimpleName();
    private SettingItemConfig[] keyList = new SettingItemConfig[0];
    private SettingsManager settingsManager;
    private CameraApiManager cameraApiManager;
    private OnItemClickListener onItemClickListener;
    private final Context context;

    public NextGenSettingItemRecyclerAdapter(Context context)
    {
        this.context = context;
    }

    public interface OnItemClickListener
    {
        void onItemClick(View view, int position);
    }

    public void setOnItemClickListener(OnItemClickListener listener)
    {
        this.onItemClickListener = listener;
    }

    public void setCameraApiManager(CameraApiManager cameraApiManager) {
        this.cameraApiManager = cameraApiManager;
    }

    public void setSettingsManager(SettingsManager settingsManager) {
        this.settingsManager = settingsManager;
    }

    public SettingItemConfig[] getKeyList() {
        return keyList;
    }

    public void setKeyList(java.util.List<SettingItemConfig> keyList)
    {
        NextGenSettingItemAdapterTools adapterTools = new NextGenSettingItemAdapterTools();
        java.util.List<SettingItemConfig> l = adapterTools.getValidSettingItemConfigList(keyList, cameraApiManager, settingsManager);
        this.keyList = l.toArray(new SettingItemConfig[0]);
        adapterTools.fillConfigsWithViews(this.keyList, cameraApiManager, settingsManager, context);
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position)
    {
        // One stable type per position: each config's view is bespoke, not a reusable template,
        // so there is nothing to gain (and real risk of mismatched rebinding) from pretending
        // two different settings share a recyclable view type.
        return position;
    }

    @Override
    public int getItemCount()
    {
        return keyList.length;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType)
    {
        View itemView = keyList[viewType].getView();
        if (itemView == null)
        {
            SettingItemConfig config = keyList[viewType];
            if (config.getKey() != null)
                Log.e(TAG, "error creating view for:" + config.getKey().toString());
            else
                Log.e(TAG, "error creating view for position " + viewType);
            itemView = new View(parent.getContext());
        }
        // itemView may still be attached to a ViewHolder from a previous adapter generation
        // (e.g. a fast resume/camera-reopen race) - detach it first so RecyclerView never tries
        // to add a view that already has a parent. ViewParent's only real-world implementation
        // here is ViewGroup, but that's not guaranteed by the type itself, so check rather than
        // assume before the cast.
        if (itemView.getParent() instanceof ViewGroup) {
            ((ViewGroup) itemView.getParent()).removeView(itemView);
        }
        return new ViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position)
    {
        if (keyList[position].getViewType() == SettingItemConfig.ViewType.Section) {
            holder.itemView.setOnClickListener(null);   // a label, not a target
            holder.itemView.setClickable(false);
        } else
            holder.itemView.setOnClickListener(v -> {
                if (onItemClickListener != null)
                    onItemClickListener.onItemClick(v, holder.getBindingAdapterPosition());
            });
    }

    public static class ViewHolder extends RecyclerView.ViewHolder
    {
        public ViewHolder(@NonNull View itemView)
        {
            super(itemView);
        }
    }
}
