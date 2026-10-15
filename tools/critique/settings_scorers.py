"""Computable scorers for the settings-panel layout search (meta-optimizer, 2026-09-16).

The panel is full width and full height (804 x 360 dp landscape). As shipped (4.3.89) it has a
48 dp search row on top and a 48 dp chip row (at the top under search, or at the bottom), so the
list is 804 x 264 dp either way. Until the fresh-eyes audit (2026-09-28) the model used one 48 dp
header (list 312 dp) and the 4.3.88 row heights, i.e. it scored a panel that was never built;
`row_model` keeps that model selectable ("measured_4388") beside the shipped one ("uniform_76",
the default). Rows are the declared SettingGroupConfig rows
(tools/critique/settings_rows.json), laid out in `columns` columns under one Section row per
group, in `section_order`, optionally sorted by usage weight inside a section, packed either
as a grid (row tops aligned, so a tall cell leaves a gap beside a short one) or staggered
(each new cell goes to the shortest column). The judgements in here are the usage weights in USE
and their fallback DEFAULT_USE, plus the component weights in settings_spec.json; the rest is
arithmetic.

Every scorer returns [0, 1], higher is better.
"""
import json
import math
import os

W, H = 804.0, 360.0
SEARCH = 48.0                 # search row, always on top
CHIPS = 48.0                  # chip row, top (under search) or bottom
LIST_H = H - SEARCH - CHIPS   # 264 dp of list in both chip positions
THUMB = (W - 24.0, H * 0.55)
GUTTER = 12.0

# Row heights. "measured_4388": uiautomator on the 4.3.88 build (dp), the model the 2026-09-16
# run used. "uniform_76": the shipped 4.3.89 panel, every row at nextgen_settings_row_min_height
# with the compact stepper inside it (screenshots/set_glasscam.xml: 76 dp cells). Section rows
# are wrap_content (16 + 4 dp padding, 1 dp rule, label) = 44 dp in both.
HEIGHTS = {
    "measured_4388": {"Text": 66.0, "Boolean": 61.0, "Button": 56.0, "Custom": 56.0, "Section": 44.0, "Sect": 44.0,
                      "Stepper": 122.0, "StepperCompact": 66.0},
    "uniform_76": {"Text": 76.0, "Boolean": 76.0, "Button": 76.0, "Custom": 76.0, "Section": 44.0, "Sect": 44.0,
                   "Stepper": 122.0, "StepperCompact": 76.0},
}

# how often a row is visited per week of use; the judgement, declared once
USE = {
    "PICTURE_FORMAT": 4.0, "VIDEO_PROFILES": 3.0, "SD_SAVE_LOCATION": 1.5, "-api": 1.0,
    "PICTURE_SIZE": 1.5, "JPEG_QUALITY": 1.0, "FORCE_RAW_TO_DNG": 1.5, "RAW_SIZE": 0.8,
    "TIMELAPSE": 1.5, "INTERVAL_SHUTTER_SLEEP": 1.2, "INTERVAL_DURATION": 1.2,
    "FOCUSPEAK_COLOR": 1.5, "GUIDE_LIST": 1.5, "HORIZONT_LVL": 1.5, "PREVIEW_POST_PROCESSING_MODE": 1.0,
    "ZOOM_ON_MANUALFOCUS": 1.0, "NIGHT_OVERLAY": 1.0, "LOCATION_MODE": 0.8, "TOUCH_TO_CAPTURE": 0.8,
    "PLAY_SHUTTER_SOUND": 0.8, "VIDEO_STABILIZATION": 1.0, "VIDEO_AUDIO_SOURCE": 0.8, "VIDEO_HDR": 0.8,
    "WHITE_BALANCE_MODE": 1.5, "SCENE_MODE": 0.8, "HDR_MODE": 1.0, "ZSL": 0.8, "FACE_DETECTOR_MODE": 0.6,
    "OIS_MODE": 0.6, "AE_METERING": 0.8, "EXPOSURE_MODE": 0.8,
}
DEFAULT_USE = 0.3          # vendor tuning rows nobody touches twice
SECTIONS = ["video", "picture", "camera", "preview", "etc"]
_ROWS = None


def _rows():
    """Per section: list of (name, type, use). Picture carries Raw and Interval as sub-sections."""
    global _ROWS
    if _ROWS is not None:
        return _ROWS
    here = os.path.dirname(os.path.abspath(__file__))
    g = json.load(open(os.path.join(here, "settings_rows.json"), encoding="utf-8"))

    def rows_of(name):
        out = []
        for r in g[name]["rows"]:
            if r["type"] in ("Sect", "Section"):
                continue
            t = r["type"]
            key = r["key"] if r["key"] != "-" else "-" + r["header"].split("_")[1]
            out.append((key, t, USE.get(key, DEFAULT_USE)))
        return out

    video = rows_of("getVideoGroup") + [("TIMELAPSE", "Stepper", USE["TIMELAPSE"])]
    picture = rows_of("getPictureGroup") + [("Sect", "Sect", 0.0)] + rows_of("getRawGroup") \
        + [("Sect", "Sect", 0.0)] + rows_of("getIntervalGroup")
    _ROWS = {"video": video, "picture": picture, "camera": rows_of("getCameraGroup"),
             "preview": rows_of("getPreviewGroup"), "etc": rows_of("getEtcGroup")}
    return _ROWS


def _layout(vector):
    """Simulate the list: returns (cells, gap_area, total_height).
    cells = list of (name, use, x_centre, y_top, height, section_top_y)."""
    cols = int(vector["columns"])
    staggered = vector["packing"] == "staggered"
    compact = vector["stepper"] == "compact"
    sort_use = vector["sort_by_use"] == "by_use"
    HEIGHT = HEIGHTS[vector.get("row_model", "uniform_76")]
    col_w = W / cols
    rows = _rows()
    y_cols = [0.0] * cols
    cells, gap = [], 0.0

    def height(t):
        if t == "Stepper":
            return HEIGHT["StepperCompact"] if compact else HEIGHT["Stepper"]
        return HEIGHT[t]

    def flush(pending, y_cols, gap):
        """Place a partial grid row (fewer cells than columns) and account its gaps."""
        if not pending:
            return y_cols, gap
        row_h = max(p[2] for p in pending)
        y = max(y_cols)
        for i, (n, u, hh) in enumerate(pending):
            cells.append((n, u, i * col_w + col_w / 2, y, hh, sec_top))
            gap += (row_h - hh) * col_w
        gap += (cols - len(pending)) * row_h * col_w
        return [y + row_h] * cols, gap

    for sec in vector["section_order"]:
        # section row spans every column: flush columns to the tallest
        top = max(y_cols)
        gap += sum(top - y for y in y_cols) * col_w
        y_cols = [top + HEIGHT["Sect"]] * cols
        sec_top = top
        items = [r for r in rows[sec]]
        if sort_use:
            # keep sub-section labels in place, sort each run between them
            runs, cur = [], []
            for r in items:
                if r[1] == "Sect":
                    runs.append(cur); runs.append([r]); cur = []
                else:
                    cur.append(r)
            runs.append(cur)
            items = []
            for run in runs:
                if run and run[0][1] == "Sect":
                    items += run
                else:
                    items += sorted(run, key=lambda r: -r[2])
        pending = []
        for name, t, use in items:
            if t == "Sect":
                # a nested section (Raw, Interval) ends the grid row in progress: flush it first
                # (fresh-eyes 2026-09-28: the pending cells used to land after the divider)
                y_cols, gap = flush(pending, y_cols, gap)
                pending = []
                top = max(y_cols)
                gap += sum(top - y for y in y_cols) * col_w
                y_cols = [top + HEIGHT["Sect"]] * cols
                continue
            h = height(t)
            if staggered:
                c = min(range(cols), key=lambda i: y_cols[i])
                cells.append((name, use, c * col_w + col_w / 2, y_cols[c], h, sec_top))
                y_cols[c] += h
            else:
                pending.append((name, use, h))
                if len(pending) == cols:
                    row_h = max(p[2] for p in pending)
                    y = max(y_cols)
                    for i, (n, u, hh) in enumerate(pending):
                        cells.append((n, u, i * col_w + col_w / 2, y, hh, sec_top))
                        gap += (row_h - hh) * col_w
                    y_cols = [y + row_h] * cols
                    pending = []
        y_cols, gap = flush(pending, y_cols, gap)   # partial last grid row
    total = max(y_cols)
    if staggered:
        gap += sum(total - y for y in y_cols) * col_w
    return cells, gap, total


def _fitts(x, y, w):
    d = math.hypot(x - THUMB[0], y - THUMB[1])
    return math.log2(d / w + 1.0)


def reach(vector, context):
    """Use-weighted cost of getting to a row: the cheaper of scrolling from the top or one chip
    tap plus scrolling from the section top, then the Fitts distance to the row's cell."""
    cells, _, _ = _layout(vector)
    cols = int(vector["columns"])
    col_w = W / cols
    chips_y = _chips_y(vector)
    list_top = SEARCH + CHIPS if vector["chips_pos"] == "top" else SEARCH
    chip_cost = 1.0 + _fitts(W - 60.0, chips_y, 36.0) / 4.0
    worst = 6.0 + math.log2(math.hypot(W, H) / 48.0 + 1.0)
    num = den = 0.0
    for name, use, x, y, h, sec_top in cells:
        scroll_top = y / LIST_H
        scroll_sec = (y - sec_top) / LIST_H
        nav = min(scroll_top, chip_cost + scroll_sec)
        y_on_screen = list_top + (y - (sec_top if nav != scroll_top else 0.0)) % LIST_H
        cost = nav + _fitts(x, min(y_on_screen, H), min(col_w, 48.0 * 4)) / 4.0
        num += use * min(cost, worst)
        den += use * worst
    return 1.0 - num / den


def fill(vector, context):
    """1 - gap area / list area: gaps are grid row-top alignment and column imbalance."""
    _, gap, total = _layout(vector)
    # linear in the blank share b: 1 - 4b, so 4 % blank -> 0.84, 12.5 % -> 0.5, 25 % -> 0
    # (the comment used to say "about 6 % scores ~0.5", which this formula does not give: 0.76)
    return max(0.0, 1.0 - 4.0 * gap / (W * total))


def legibility(vector, context):
    """Descriptions wrap past ~300 dp columns and truncate past 240 dp: the row height model
    above assumes no wrapping, so a narrow column is penalised here instead."""
    col_w = W / int(vector["columns"]) - GUTTER
    if col_w >= 300.0:
        return 1.0
    if col_w >= 240.0:
        return 0.6
    return 0.3


def chips_reach(vector, context):
    """Fitts distance from the right thumb to the chip row's right end."""
    worst = math.log2(math.hypot(W, H) / 36.0 + 1.0)
    return 1.0 - _fitts(W - 60.0, _chips_y(vector), 36.0) / worst


def _chips_y(vector):
    """Centre of the chip row: under the search row at the top, or the last row at the bottom."""
    return SEARCH + CHIPS / 2 if vector["chips_pos"] == "top" else H - CHIPS / 2


def stability(vector, context):
    """Rows in their declared (upstream, documented) order are findable by memory."""
    return 1.0 if vector["sort_by_use"] == "declared" else 0.7


SCORERS = {
    "settings.reach": reach,
    "settings.fill": fill,
    "settings.legibility": legibility,
    "settings.chips_reach": chips_reach,
    "settings.stability": stability,
}
