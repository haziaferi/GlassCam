package glasscam.cam.ui.themenextgen.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.hazia.glasscam.R;

import java.util.List;

import glasscam.ActivityAbstract;
import glasscam.ActivityContainer;
import glasscam.GlassCamApplication;
import glasscam.cam.apis.CameraApiManager;
import glasscam.cam.apis.basecamera.Size;
import glasscam.cam.event.camera.CameraHolderEvent;
import glasscam.cam.ui.themenextgen.adapter.NextGenSettingItemRecyclerAdapter;
import glasscam.cam.ui.themenextgen.adapter.NextGenSettingValuePickerAdapter;
import glasscam.cam.ui.themenextgen.layoutconfig.SettingGroupConfig;
import glasscam.cam.ui.themenextgen.layoutconfig.SettingItemConfig;
import glasscam.cam.ui.themenextgen.view.button.NextGenSettingBoolItem;
import glasscam.cam.ui.themenextgen.view.button.NextGenSettingButton;
import glasscam.cam.ui.themenextgen.view.button.NextGenSettingItem;
import glasscam.settings.SettingsManager;


public class NextGenSettingDummyFragment extends Fragment implements CameraHolderEvent
{
    CameraApiManager cameraApiManager;
    SettingsManager settingsManager;

    // Column count = measured list width / TARGET_COLUMN_WIDTH_DP, recomputed once layout happens,
    // so it adapts to whatever width the panel has. The old GridView used a 200dp auto_fit target:
    // on the then 60% phone panel (~460dp) that made two 224dp columns and every long header
    // ellipsized ("Optical Image Stabilis...", critique 2026-09-16 #5), hence 300dp and the
    // one-column floor below SINGLE_COLUMN_BELOW_DP. The full-width panel (Batch 4) is ~800dp on
    // this phone: two columns. Wider panels get up to 3.
    private static final int TARGET_COLUMN_WIDTH_DP = 300;
    private static final int SINGLE_COLUMN_BELOW_DP = 600;

    @Override
    public void onAttach(@androidx.annotation.NonNull android.content.Context context) {
        super.onAttach(context);
        settingsManager = GlassCamApplication.settingsManager();
        // Use the guaranteed-non-null context parameter, not getActivity() (which can be null at
        // attach time); guard the container so a restored/reattached fragment can't NPE here.
        if (context instanceof ActivityAbstract) {
            ActivityContainer c = ((ActivityAbstract) context).container;
            if (c != null)
                cameraApiManager = c.cameraApiManager();
        }
    }

    private List<SettingItemConfig> keyList;
    private int groupIndex;
    private String filter = "";
    private android.widget.TextView emptyLabel;
    private RecyclerView itemHolder;
    private GridLayoutManager gridLayoutManager;
    NextGenSettingItemRecyclerAdapter adapter;
    NextGenSettingValuePickerAdapter valuePickerAdapter;

    private static final String ARG_GROUP = "group_index";
    /** Every group in one list, each under a Section row (owner's decision, 2026-09-16). */
    public static final int GROUP_ALL = -1;

    /** What the host needs for one jump chip: the section label, where it sits, how many rows follow. */
    public static final class SectionInfo {
        public final int labelId, position, rows;
        SectionInfo(int labelId, int position, int rows) { this.labelId = labelId; this.position = position; this.rows = rows; }
    }

    public static NextGenSettingDummyFragment getInstance(int groupIndex)
    {
        NextGenSettingDummyFragment f = new NextGenSettingDummyFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_GROUP, groupIndex);
        f.setArguments(args);
        return f;
    }

    // Public no-arg constructor is REQUIRED: after process death the framework recreates each tab
    // via reflection using this constructor plus the saved arguments. A private/parameterized
    // constructor threw Fragment$InstantiationException, and a field-passed keyList came back null.
    public NextGenSettingDummyFragment()
    {

    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        groupIndex = getArguments() != null ? getArguments().getInt(ARG_GROUP, 0) : 0;
        this.keyList = buildKeyList(groupIndex);
    }

    // Rebuilds this tab's item list from its group index. Runs in onCreate (not the constructor),
    // so a framework-recreated fragment restores its content from arguments. No camera access here
    // - SettingGroupConfig only assembles static SettingItemConfig descriptors.
    private static List<SettingItemConfig> buildKeyList(int groupIndex)
    {
        SettingGroupConfig g = new SettingGroupConfig();
        switch (groupIndex)
        {
            case 1: return g.getPictureGroup();   // includes the Raw and Interval sections
            case 2: return g.getCameraGroup();
            case 3: return g.getPreviewGroup();
            case 4: return g.getEtcGroup();
            case GROUP_ALL: {
                List<SettingItemConfig> all = new java.util.ArrayList<>();
                all.add(section(R.string.settings_tab_video));    all.addAll(g.getVideoGroup());
                all.add(section(R.string.settings_tab_picture));  all.addAll(g.getPictureGroup());
                all.add(section(R.string.settings_tab_camera));   all.addAll(g.getCameraGroup());
                all.add(section(R.string.settings_tab_preview));  all.addAll(g.getPreviewGroup());
                all.add(section(R.string.settings_tab_etc));      all.addAll(g.getEtcGroup());
                return all;
            }
            default: return g.getVideoGroup();
        }
    }

    private static SettingItemConfig section(int labelId) {
        return new SettingItemConfig(null, labelId, 0, false, SettingItemConfig.ViewType.Section);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.nextgen_setting_dummyfragment, container, false);
    }

    private final ViewTreeObserver.OnGlobalLayoutListener spanCountListener = new ViewTreeObserver.OnGlobalLayoutListener() {
        @Override
        public void onGlobalLayout() {
            // This can still fire after the fragment's view is torn down (the listener removal
            // in onDestroyView races the layout pass that queued this callback), so guard on
            // isAdded()/context rather than relying on removal alone - getResources() otherwise
            // throws IllegalStateException("Fragment ... not attached to a context").
            if (itemHolder == null || !isAdded() || getContext() == null || pickerRestore != null)
                return;   // a value picker is open and owns the span count
            int widthPx = itemHolder.getWidth();
            if (widthPx <= 0)
                return;
            float density = getResources().getDisplayMetrics().density;
            float targetPx = TARGET_COLUMN_WIDTH_DP * density;
            int spanCount = widthPx / density < SINGLE_COLUMN_BELOW_DP ? 1 : Math.max(1, (int) (widthPx / targetPx));
            if (gridLayoutManager.getSpanCount() != spanCount)
                gridLayoutManager.setSpanCount(spanCount);
        }
    };

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        this.itemHolder = view.findViewById(R.id.nextgen_itemholder);
        this.emptyLabel = view.findViewById(R.id.nextgen_empty);
        if (getParentFragment() instanceof NextGenCameraSettingFragment)
            filter = ((NextGenCameraSettingFragment) getParentFragment()).getQuery();
        gridLayoutManager = new GridLayoutManager(getContext(), 1);
        // a section label always spans the full width, whatever the column count
        gridLayoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                if (adapter != null && itemHolder.getAdapter() == adapter && position < adapter.getKeyList().length
                        && adapter.getKeyList()[position].getViewType() == SettingItemConfig.ViewType.Section)
                    return gridLayoutManager.getSpanCount();
                return 1;
            }
        });
        itemHolder.setLayoutManager(gridLayoutManager);
        itemHolder.getViewTreeObserver().addOnGlobalLayoutListener(spanCountListener);
        itemHolder.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView rv, int dx, int dy) {
                reportCurrentSection();
            }
        });
        cameraApiManager.addEventListner(this);
    }

    /** Which section owns the first visible row - the host pins its label over the list. */
    private void reportCurrentSection() {
        if (!(getParentFragment() instanceof NextGenCameraSettingFragment) || adapter == null)
            return;
        int label = 0, chipLabel = 0;
        boolean pin = false;
        if (itemHolder.getAdapter() == adapter) {
            int first = gridLayoutManager.findFirstVisibleItemPosition();
            SettingItemConfig[] keys = adapter.getKeyList();
            for (int i = Math.min(first, keys.length - 1); i >= 0; i--)
                if (keys[i].getViewType() == SettingItemConfig.ViewType.Section) {
                    if (label == 0) label = keys[i].getHeader();          // nearest section: the pinned text
                    if (isTop(keys[i].getHeader(), TOPS)) { chipLabel = keys[i].getHeader(); break; }   // its chip
                }
            // the pinned copy is redundant while the section row itself sits at the top
            View firstView = first >= 0 ? gridLayoutManager.findViewByPosition(first) : null;
            pin = label != 0 && !(first < keys.length && keys[first].getViewType() == SettingItemConfig.ViewType.Section
                    && firstView != null && firstView.getTop() >= itemHolder.getPaddingTop());
        }
        ((NextGenCameraSettingFragment) getParentFragment()).onCurrentSection(label, pin, chipLabel);
    }

    /** Jump chip: put the section row at the top of the list. */
    public void scrollToSection(int position) {
        if (itemHolder == null || adapter == null) return;
        if (itemHolder.getAdapter() != adapter) {   // a picker is open: close it first
            if (pickerRestore != null) { pickerRestore.run(); pickerRestore = null; }
            activeItem = null;
            itemHolder.setAdapter(adapter);
        }
        gridLayoutManager.scrollToPositionWithOffset(position, 0);
        itemHolder.post(this::reportCurrentSection);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        cameraApiManager.removeEventListner(this);
        if (itemHolder != null) {
            itemHolder.removeCallbacks(fillUiRunnable);
            itemHolder.getViewTreeObserver().removeOnGlobalLayoutListener(spanCountListener);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        fillUiWithParameters();
    }

    @Override
    public void onCameraOpen() {

    }

    @Override
    public void onCameraOpenFinished() {
        fillUiWithParameters();
    }

    @Override
    public void onCameraClose() {

    }

    @Override
    public void onCameraError(String error) {

    }

    @Override
    public void onCameraChangedAspectRatioEvent(Size size) {

    }

    private final Runnable fillUiRunnable = new Runnable() {
        @Override
        public void run() {
            // Build the grid only when we have a Context, the fragment is still attached, and the
            // camera is actually open - setKeyList()->NextGenSettingItemAdapterTools dereferences
            // cameraApiManager.getCamera().getParameterHandler(). Opening Settings before the
            // camera finishes opening, or a teardown between post() and run(), would otherwise NPE.
            // onCameraOpenFinished() re-posts this once the camera exists.
            if (!isAdded() || getContext() == null || itemHolder == null)
                return;
            if (cameraApiManager == null || cameraApiManager.getCamera() == null)
                return;

            adapter = new NextGenSettingItemRecyclerAdapter(getContext());
            adapter.setCameraApiManager(cameraApiManager);
            adapter.setSettingsManager(settingsManager);
            adapter.setKeyList(filteredKeyList());
            adapter.setOnItemClickListener(itemClickListener);
            itemHolder.setAdapter(adapter);
            int rows = 0;
            for (SettingItemConfig c : adapter.getKeyList())
                if (c.getViewType() != SettingItemConfig.ViewType.Section) rows++;
            if (emptyLabel != null) {
                boolean empty = rows == 0 && !filter.isEmpty();
                emptyLabel.setText(empty ? getString(R.string.settings_search_empty, filter) : "");
                emptyLabel.setVisibility(empty ? View.VISIBLE : View.GONE);
            }
            if (getParentFragment() instanceof NextGenCameraSettingFragment) {
                ((NextGenCameraSettingFragment) getParentFragment()).onSections(topSections());
                itemHolder.post(NextGenSettingDummyFragment.this::reportCurrentSection);
            }
        }
    };

    /**
     * The jump chips: one per top-level section (the ones GROUP_ALL inserted), with the row count
     * under it after the filter. When a search empties a whole section the adapter has already
     * pruned its label; it is reported with 0 rows so its chip dims instead of vanishing.
     */
    private List<SectionInfo> topSections() {
        List<SectionInfo> out = new java.util.ArrayList<>();
        if (adapter == null) return out;
        SettingItemConfig[] keys = adapter.getKeyList();
        int[] tops = TOPS;
        for (int labelId : tops) {
            int position = -1, rows = 0;
            for (int i = 0; i < keys.length; i++) {
                if (keys[i].getViewType() == SettingItemConfig.ViewType.Section) {
                    if (keys[i].getHeader() == labelId) position = i;
                    else if (position >= 0 && isTop(keys[i].getHeader(), tops)) break;
                } else if (position >= 0)
                    rows++;
            }
            out.add(new SectionInfo(labelId, Math.max(position, 0), position < 0 ? 0 : rows));
        }
        return out;
    }

    private static final int[] TOPS = {R.string.settings_tab_video, R.string.settings_tab_picture, R.string.settings_tab_camera,
            R.string.settings_tab_preview, R.string.settings_tab_etc};

    private static boolean isTop(int id, int[] tops) {
        for (int t : tops) if (t == id) return true;
        return false;
    }

    /** Search (2026-09-16): the host pushes the query; an open value picker is dropped first. */
    public void setFilter(String query) {
        String q = query == null ? "" : query.trim();
        if (q.equals(filter)) return;
        filter = q;
        if (pickerRestore != null) { pickerRestore.run(); pickerRestore = null; }
        activeItem = null;
        if (itemHolder != null)
            fillUiWithParameters();
    }

    /**
     * Rows whose header or description contains the query (case-insensitive); a section whose
     * own label matches keeps all its rows; orphaned section labels are pruned by the adapter.
     */
    private List<SettingItemConfig> filteredKeyList() {
        if (filter.isEmpty() || getContext() == null)
            return keyList;
        String q = filter.toLowerCase(java.util.Locale.getDefault());
        List<SettingItemConfig> out = new java.util.ArrayList<>();
        // A top-level section (Video, Picture...) and a nested one (Raw, Interval inside Picture)
        // match separately: "picture" must keep Picture's Raw and Interval rows too. One flag,
        // reset by every section row, dropped them at the nested label (fresh-eyes 2026-09-28).
        boolean topMatches = false, nestedMatches = false;
        for (SettingItemConfig c : keyList) {
            if (c.getViewType() == SettingItemConfig.ViewType.Section) {
                if (isTop(c.getHeader(), TOPS)) {
                    topMatches = contains(c.getHeader(), q);
                    nestedMatches = false;
                } else
                    nestedMatches = contains(c.getHeader(), q);
                out.add(c);
                continue;
            }
            if (topMatches || nestedMatches || contains(c.getHeader(), q) || contains(c.getDescription(), q))
                out.add(c);
        }
        return out;
    }

    private boolean contains(int stringId, String q) {
        if (stringId == 0) return false;
        try {
            return getString(stringId).toLowerCase(java.util.Locale.getDefault()).contains(q);
        } catch (android.content.res.Resources.NotFoundException ex) {
            return false;
        }
    }

    private void fillUiWithParameters()
    {
        // onResume() and the onCameraOpenFinished() camera event can both fire in close
        // succession (e.g. on first launch, while this tab's fragment is being eagerly created
        // by offscreenPageLimit at the same time the camera finishes opening). Removing any
        // not-yet-run previous post before queuing a new one ensures only the latest call
        // actually rebuilds the grid, avoiding redundant back-to-back adapter swaps.
        itemHolder.removeCallbacks(fillUiRunnable);
        itemHolder.post(fillUiRunnable);
    }

    private final NextGenSettingItemRecyclerAdapter.OnItemClickListener itemClickListener = new NextGenSettingItemRecyclerAdapter.OnItemClickListener() {
        @Override
        public void onItemClick(View view, int position) {
            // getBindingAdapterPosition() returns NO_POSITION (-1) for a holder mid-rebuild/detach
            // (e.g. a tap landing during a notifyDataSetChanged or the item<->value-picker adapter
            // swap); guard before indexing so it can't throw ArrayIndexOutOfBounds.
            if (adapter == null || position == RecyclerView.NO_POSITION || position >= adapter.getKeyList().length)
                return;
            SettingItemConfig config = adapter.getKeyList()[position];
            if (getParentFragment() instanceof NextGenCameraSettingFragment)
                ((NextGenCameraSettingFragment) getParentFragment()).hideKeyboard();
            if (view instanceof NextGenSettingItem && config.getViewType() != SettingItemConfig.ViewType.Custom) {
                fillUiWithValuesFromParameter((NextGenSettingItem) view);
            }
            else if (view instanceof NextGenSettingBoolItem)
            {

            }
            else if (view instanceof NextGenSettingButton && config.getViewType() == SettingItemConfig.ViewType.Custom)
            {
                ((NextGenSettingButton) view).onClick(view);


            }
        }
    };

    private NextGenSettingItem activeItem;
    private Runnable pickerRestore;
    private void fillUiWithValuesFromParameter(NextGenSettingItem nextGenSettingItem)
    {
        activeItem = nextGenSettingItem;
        String[] values = nextGenSettingItem.getParameter().getStringValues();
        String value  = nextGenSettingItem.getParameter().getStringValue();
        // row 0 = title, row 1 = Back, then the values (see NextGenSettingValuePickerAdapter)
        String[] valuesWithBack = new String[values.length+2];
        valuesWithBack[0] = nextGenSettingItem.getHeaderText().toString();
        valuesWithBack[1] = getString(R.string.settings_value_back);
        final android.os.Parcelable listScroll = gridLayoutManager.onSaveInstanceState();
        int i =2;
        int selected =2;
        for (String s : values) {
            if (s.equals(value))
                selected = i;
            valuesWithBack[i++] = s;
        }

        valuePickerAdapter = new NextGenSettingValuePickerAdapter(nextGenSettingItem.getParameter().getKey(), valuesWithBack);
        valuePickerAdapter.setSelected(selected);
        valuePickerAdapter.setOnValueClickListener(position -> {
            if (position == RecyclerView.NO_POSITION || position >= valuesWithBack.length)
                return;
            // Rows are told apart by position, not by string (ROW_TITLE = 0, ROW_BACK = 1, values
            // from ROW_VALUE on), so a real parameter value equal to the Back label (e.g. a
            // "Back"/"Front" camera selector) is not swallowed, and the label localizes.
            if (position >= NextGenSettingValuePickerAdapter.ROW_VALUE && activeItem instanceof NextGenSettingItem) {
                activeItem.getParameter().setStringValue(valuesWithBack[position], true);
            }
            else if (position == NextGenSettingValuePickerAdapter.ROW_TITLE)
                return;
            if (pickerRestore != null) { pickerRestore.run(); pickerRestore = null; }
            itemHolder.setAdapter(adapter);
            // the list used to snap back to the top after every pick, so the same spot on screen
            // hit a different row (critique 2026-09-16, interactivity)
            if (listScroll != null)
                gridLayoutManager.onRestoreInstanceState(listScroll);
            activeItem = null;
        });
        // values are short, so the picker keeps two columns even when the settings list is one;
        // the title and Back rows span the full width
        final int listSpan = gridLayoutManager.getSpanCount();
        final GridLayoutManager.SpanSizeLookup listLookup = gridLayoutManager.getSpanSizeLookup();
        gridLayoutManager.setSpanCount(Math.max(2, listSpan));
        gridLayoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                return position < NextGenSettingValuePickerAdapter.ROW_VALUE ? gridLayoutManager.getSpanCount() : 1;
            }
        });
        pickerRestore = () -> {
            gridLayoutManager.setSpanSizeLookup(listLookup);
            gridLayoutManager.setSpanCount(listSpan);
        };
        itemHolder.setAdapter(valuePickerAdapter);
        itemHolder.scrollToPosition(selected);
    }
}
