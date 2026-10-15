package glasscam.cam.ui.themenextgen.fragment;

import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import com.hazia.glasscam.R;

/**
 * Settings panel host. Rebuilt 2026-09-16 (owner's decision after the drawer critique): one
 * page with every setting, a search field, a jump-chip row (one chip per section) and a sticky
 * section label over the list. The five-tab strip and its pager adapter are gone.
 */
public class NextGenCameraSettingFragment extends Fragment
{
    private static final String PAGE_TAG = "settings_page";

    private EditText searchField;
    private LinearLayout chipRow;
    private HorizontalScrollView chipScroll;
    private TextView stickySection;
    private String query = "";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.nextgen_setting_fragment, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // keep the panel's edges off the landscape cutout and the system bars (systemBars() includes
        // the bottom gesture handle; the side gesture strips, systemGestures(), are not inset)
        View settingHolder = view.findViewById(R.id.setting_holder);
        ViewCompat.setOnApplyWindowInsetsListener(settingHolder, (v, insets) -> {
            Insets safe = insets.getInsets(
                    WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
            v.setPadding(safe.left, safe.top, safe.right, safe.bottom);
            return insets;
        });
        ViewCompat.requestApplyInsets(settingHolder);

        searchField = view.findViewById(R.id.settings_search);
        chipRow = view.findViewById(R.id.settings_chips);
        chipScroll = view.findViewById(R.id.settings_chips_scroll);
        stickySection = view.findViewById(R.id.settings_sticky_section);

        if (getChildFragmentManager().findFragmentByTag(PAGE_TAG) == null)
            getChildFragmentManager().beginTransaction()
                    .replace(R.id.setting_fragment_holder, NextGenSettingDummyFragment.getInstance(NextGenSettingDummyFragment.GROUP_ALL), PAGE_TAG)
                    .commitNow();

        View clear = view.findViewById(R.id.settings_search_clear);
        searchField.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) {}
            @Override public void afterTextChanged(Editable e) {
                query = e.toString().trim();
                clear.setVisibility(query.isEmpty() ? View.GONE : View.VISIBLE);
                NextGenSettingDummyFragment page = page();
                if (page != null)
                    page.setFilter(query);
            }
        });
        searchField.setOnEditorActionListener((v, actionId, event) -> { hideKeyboard(); return true; });
        clear.setOnClickListener(v -> { searchField.setText(""); hideKeyboard(); });
    }

    @Nullable
    private NextGenSettingDummyFragment page() {
        Fragment f = getChildFragmentManager().findFragmentByTag(PAGE_TAG);
        return f instanceof NextGenSettingDummyFragment ? (NextGenSettingDummyFragment) f : null;
    }

    /** The current search text, "" when none - the page reads it when it (re)builds its list. */
    public String getQuery() { return query; }

    /**
     * The page reports its sections after every (re)build: label id, adapter position and how
     * many rows survived the filter. One chip per section; a section with no rows is dimmed.
     */
    public void onSections(java.util.List<NextGenSettingDummyFragment.SectionInfo> sections) {
        if (chipRow == null || getContext() == null) return;
        chipRow.removeAllViews();
        Context ctx = getContext();
        float d = ctx.getResources().getDisplayMetrics().density;
        for (NextGenSettingDummyFragment.SectionInfo s : sections) {
            TextView chip = new TextView(ctx);
            chip.setText(s.labelId);
            chip.setTextSize(13);
            chip.setTextColor(ContextCompat.getColor(ctx, R.color.nextgen_menu_secondary_text));
            // a 36 dp pill drawn inside a 48 dp target (the first cut was a 36 dp target)
            chip.setBackground(new android.graphics.drawable.InsetDrawable(
                    ContextCompat.getDrawable(ctx, R.drawable.nextgen_back_chip), 0, (int) (6 * d), 0, (int) (6 * d)));
            chip.setPadding((int) (14 * d), 0, (int) (14 * d), 0);
            chip.setMinHeight((int) (48 * d));
            chip.setMinWidth((int) (48 * d));
            chip.setGravity(android.view.Gravity.CENTER);
            chip.setEnabled(s.rows > 0);
            chip.setAlpha(s.rows > 0 ? 1f : 0.4f);
            chip.setTag(s.labelId);
            chip.setOnClickListener(v -> {
                hideKeyboard();
                NextGenSettingDummyFragment page = page();
                if (page != null) page.scrollToSection(s.position);
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, (int) (48 * d));
            lp.setMarginStart((int) (8 * d));
            chip.setLayoutParams(lp);
            chipRow.addView(chip);
        }
    }

    /** The page tells us which section the first visible row belongs to, and whether to pin it. */
    public void onCurrentSection(int labelId, boolean pin, int chipLabelId) {
        if (stickySection == null || getContext() == null) return;
        if (labelId == 0 || !pin) {
            stickySection.setVisibility(View.GONE);
        } else {
            stickySection.setText(labelId);
            stickySection.setVisibility(View.VISIBLE);
        }
        for (int i = 0; i < chipRow.getChildCount(); i++) {
            TextView chip = (TextView) chipRow.getChildAt(i);
            boolean active = chip.getTag() instanceof Integer && (Integer) chip.getTag() == chipLabelId;
            chip.setTextColor(ContextCompat.getColor(getContext(),
                    active ? R.color.manual_button_active : R.color.nextgen_menu_secondary_text));
            if (active && chipScroll != null)
                chipScroll.post(() -> chipScroll.smoothScrollTo(chip.getLeft() - chipScroll.getWidth() / 3, 0));
        }
    }

    public void hideKeyboard() {
        if (searchField == null) return;
        InputMethodManager imm = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) imm.hideSoftInputFromWindow(searchField.getWindowToken(), 0);
        searchField.clearFocus();
    }
}
