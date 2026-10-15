#!/usr/bin/env python
"""measure.py -- the measured layer of the GlassCam critique, from the repo alone.

    python tools/critique/measure.py            # markdown tables to stdout, measured.json beside this file
    python tools/critique/measure.py --verify   # also cross-check 5 contrast rows with designkit

What it measures (every row is `measured`, never a judgement):
  * text/background contrast for every TextView-like element in every layout, with translucent
    colours composited over black AND over mid-grey (the preview behind the chrome is a scene)
  * every dp/sp literal per layout, spacing values (margin/padding) and whether they sit on 4/8 dp
  * declared touch targets under 48 dp (clickable elements, Buttons, Switches, onClick)
  * textSize authored in dp instead of sp
  * colours hard-coded in layouts instead of colors.xml
  * string resources: default vs values-it coverage, strings hard-coded in layouts
"""
import json
import os
import re
import subprocess
import sys
try:
    import defusedxml.ElementTree as ET
except ImportError:  # pragma: no cover
    import xml.etree.ElementTree as ET  # nosemgrep: python.lang.security.use-defused-xml-parse.use-defused-xml-parse

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HERE, "..", ".."))
RES = os.path.join(ROOT, "app", "src", "main", "res")
ANDROID = "{http://schemas.android.com/apk/res/android}"
APP = "{http://schemas.android.com/apk/res-auto}"
DESIGNKIT = os.path.expanduser(
    "~/.claude/plugins/cache/gold-standard-local/design-optimization/2.0.6/engine/designkit_cli.py")

TEXT_CLASSES = ("TextView", "Button", "EditText", "Switch", "CheckBox", "RadioButton",
                "StyledTextView", "NextGenSettingItem", "AutoCompleteTextView")
CLICK_CLASSES = ("Button", "ImageButton", "Switch", "CheckBox", "RadioButton", "SeekBar",
                 "ShutterButton", "NextGenManualButton", "NextGenCameraUiTextSwitch", "ManualButton")
# theme defaults from styles.xml (AppTheme -> Theme.AppCompat.DayNight.NoActionBar, dark on device)
THEME_TEXT = "#FFFFFFFF"      # textColorPrimary in the dark theme, and what the fixes set explicitly
PREVIEW_BLACK, PREVIEW_GREY = "#FF000000", "#FF808080"


# ------------------------------------------------------------------ resources

def load_values(name, only=None):
    """name -> raw string for <color>/<dimen>/<string> in values/*.xml (or one file)."""
    out = {}
    d = os.path.join(RES, "values")
    for fn in sorted(os.listdir(d)):
        if not fn.endswith(".xml") or (only and fn != only):
            continue
        root = ET.parse(os.path.join(d, fn)).getroot()
        for el in root:
            if el.tag == name and el.get("name"):
                out[el.get("name")] = (el.text or "").strip()
    return out


COLORS = load_values("color")
DIMENS = load_values("dimen")
STRINGS = load_values("string", only="strings.xml")


def resolve_color(v, depth=0):
    if v is None or depth > 5:
        return None
    v = v.strip()
    if v.startswith("#"):
        h = v[1:]
        if len(h) == 3:
            h = "".join(c * 2 for c in h)
        if len(h) == 4:
            h = "".join(c * 2 for c in h)
        if len(h) == 6:
            h = "FF" + h
        return "#" + h.upper() if len(h) == 8 else None
    if v.startswith("@color/"):
        return resolve_color(COLORS.get(v[7:]), depth + 1)
    if v.startswith("@android:color/"):
        return {"white": "#FFFFFFFF", "black": "#FF000000", "transparent": "#00000000",
                "darker_gray": "#FFAAAAAA"}.get(v[15:])
    return None  # drawables, selectors, ?attr -> not a flat colour


def resolve_dimen(v, depth=0):
    if v is None or depth > 5:
        return None
    v = v.strip()
    m = re.match(r"^(-?\d+(?:\.\d+)?)(dp|dip|sp|px)$", v)
    if m:
        return float(m.group(1)), ("dp" if m.group(2) == "dip" else m.group(2))
    if v.startswith("@dimen/"):
        return resolve_dimen(DIMENS.get(v[7:]), depth + 1)
    return None


# ------------------------------------------------------------------ colour maths

def rgba(c):
    return tuple(int(c[i:i + 2], 16) for i in (1, 3, 5, 7))


def composite(fg, bg):
    a, r, g, b = rgba(fg)
    _, br, bgc, bb = rgba(bg)
    t = a / 255
    return "#FF%02X%02X%02X" % (round(r * t + br * (1 - t)), round(g * t + bgc * (1 - t)),
                                round(b * t + bb * (1 - t)))


def luminance(c):
    def ch(x):
        x /= 255
        return x / 12.92 if x <= 0.03928 else ((x + 0.055) / 1.055) ** 2.4
    _, r, g, b = rgba(c)
    return 0.2126 * ch(r) + 0.7152 * ch(g) + 0.0722 * ch(b)


def contrast(fg, bg):
    l1, l2 = luminance(fg), luminance(bg)
    hi, lo = max(l1, l2), min(l1, l2)
    return round((hi + 0.05) / (lo + 0.05), 2)


def contrast_over(text, panel, scene):
    """text over (panel over scene); text and panel may be translucent."""
    base = composite(panel, scene) if panel else scene
    return contrast(composite(text, base), base)


# ------------------------------------------------------------------ layouts

def layout_files():
    files = []
    for d in sorted(os.listdir(RES)):
        if d.startswith("layout"):
            for fn in sorted(os.listdir(os.path.join(RES, d))):
                if fn.endswith(".xml"):
                    files.append(os.path.join(RES, d, fn))
    return files


def short(tag):
    return tag.split(".")[-1]


def walk(el, parents, out):
    out.append((el, list(parents)))
    for ch in el:
        walk(ch, parents + [el], out)


def analyse_layout(path):
    rel = os.path.relpath(path, RES).replace("\\", "/")
    root = ET.parse(path).getroot()
    if short(root.tag) == "layout":  # data-binding wrapper
        root = next((c for c in root if short(c.tag) != "data"), root)
    nodes = []
    walk(root, [], nodes)
    contrast_rows, literals, spacing, targets, dp_text, hard_colors, hard_strings = [], [], [], [], [], [], []
    for el, parents in nodes:
        tag = short(el.tag)
        a = {k.replace(ANDROID, "").replace(APP, "app:"): v for k, v in el.attrib.items()}
        ident = (a.get("id") or "").replace("@+id/", "").replace("@id/", "") or tag
        # literals + spacing
        for k, v in a.items():
            d = re.match(r"^(-?\d+(?:\.\d+)?)(dp|dip|sp)$", v.strip())
            if d:
                val, unit = float(d.group(1)), d.group(2)
                literals.append({"element": ident, "attr": k, "value": v})
                if "margin" in k.lower() or "padding" in k.lower():
                    spacing.append({"element": ident, "attr": k, "dp": val,
                                    "on4": val % 4 == 0, "on8": val % 8 == 0})
            if k == "textSize" and (v.strip().endswith("dp") or v.strip().endswith("dip")):
                dp_text.append({"element": ident, "value": v})
            elif k == "textSize" and v.startswith("@dimen/"):
                r = resolve_dimen(v)
                if r and r[1] == "dp":
                    dp_text.append({"element": ident, "value": f"{v} = {r[0]:g}dp"})
            if v.strip().startswith("#") and k not in ("tag",):
                hard_colors.append({"element": ident, "attr": k, "value": v})
            if k in ("text", "hint", "contentDescription") and v and not v.startswith("@") and not v.startswith("{"):
                hard_strings.append({"element": ident, "attr": k, "value": v})
        # touch targets
        clickable = a.get("clickable") == "true" or "onClick" in a or any(tag.endswith(c) for c in CLICK_CLASSES)
        if clickable:
            w, h = resolve_dimen(a.get("layout_width")), resolve_dimen(a.get("layout_height"))
            wd = w[0] if w and w[1] == "dp" else None
            hd = h[0] if h and h[1] == "dp" else None
            if (wd is not None and wd < 48) or (hd is not None and hd < 48):
                targets.append({"element": ident, "class": tag, "w": a.get("layout_width"),
                                "h": a.get("layout_height"), "min_dp": min(x for x in (wd, hd) if x is not None)})
        # contrast
        is_text = any(tag.endswith(c) for c in TEXT_CLASSES)
        if is_text:
            text = resolve_color(a.get("textColor")) or THEME_TEXT
            text_src = a.get("textColor") or "theme default"
            panel, panel_src = None, "none (preview)"
            for anc in [el] + parents[::-1]:
                bgv = anc.attrib.get(ANDROID + "background")
                c = resolve_color(bgv)
                if c and int(c[1:3], 16) >= 0x10:   # near-transparent "#02000000" is no panel
                    panel, panel_src = c, bgv
                    break
                if bgv and not c and not bgv.startswith("@drawable/roundcorners"):
                    panel_src = bgv + " (unresolved)"
                    break
            size = resolve_dimen(a.get("textSize"))
            sp = size[0] if size else 14.0
            bold = "bold" in (a.get("textStyle") or "")
            large = sp >= 18 or (bold and sp >= 14)
            need = 3.0 if large else 4.5
            c_black = contrast_over(text, panel, PREVIEW_BLACK)
            c_grey = contrast_over(text, panel, PREVIEW_GREY)
            contrast_rows.append({"element": ident, "text": text_src, "textColor": text,
                                  "panel": panel_src, "size_sp": sp, "large": large, "need": need,
                                  "over_black": c_black, "over_grey": c_grey,
                                  "worst": min(c_black, c_grey),
                                  "pass": min(c_black, c_grey) >= need})
    return {"layout": rel, "contrast": contrast_rows, "literals": literals, "spacing": spacing,
            "targets_under_48": targets, "textsize_in_dp": dp_text, "hardcoded_colors": hard_colors,
            "hardcoded_strings": hard_strings}


def strings_report():
    it = {}
    p = os.path.join(RES, "values-it", "strings.xml")
    if os.path.exists(p):
        for el in ET.parse(p).getroot():
            if el.tag == "string" and el.get("name"):
                it[el.get("name")] = (el.text or "").strip()
    translatable = {k: v for k, v in STRINGS.items()}
    missing = sorted(k for k in translatable if k not in it)
    empty = sorted(k for k, v in it.items() if not v)
    return {"default": len(translatable), "it_present": len(it), "it_missing": missing,
            "it_empty": empty, "it_coverage": round(100 * (len(it) - len(empty)) / max(1, len(translatable)), 1)}


def verify_with_designkit(rows):
    out = []
    for r in rows[:5]:
        # designkit takes opaque 6-digit hex and does not composite alpha, so composite first
        fg = composite(r["textColor"], PREVIEW_BLACK)
        res = subprocess.run([sys.executable, DESIGNKIT, "contrast", "#" + fg[3:], "#000000"],
                             capture_output=True, text=True)
        out.append({"element": r["element"], "ours_over_black": r["over_black"],
                    "designkit": res.stdout.strip().splitlines()[:2]})
    return out


def main():
    layouts = [analyse_layout(p) for p in layout_files()]
    report = {"layouts": layouts, "strings": strings_report(),
              "colors_defined": len(COLORS), "dimens_defined": len(DIMENS)}
    if "--verify" in sys.argv:
        allrows = [r for l in layouts for r in l["contrast"] if r["panel"] == "none (preview)"]
        report["designkit_verify"] = verify_with_designkit(allrows)
    with open(os.path.join(HERE, "measured.json"), "w", encoding="utf-8", newline="\n") as f:
        json.dump(report, f, indent=2)

    # --- markdown summary
    fails = [(l["layout"], r) for l in layouts for r in l["contrast"] if not r["pass"]]
    print("## Contrast (measured) -- failing rows, worst of over-black / over-grey scene\n")
    print("| layout | element | text | panel | sp | need | over black | over grey |")
    print("|---|---|---|---|---|---|---|---|")
    for lay, r in fails:
        print(f"| {lay} | {r['element']} | {r['text']} | {r['panel']} | {r['size_sp']:g} | {r['need']} | "
              f"{r['over_black']} | {r['over_grey']} |")
    total = sum(len(l["contrast"]) for l in layouts)
    print(f"\n{len(fails)} of {total} text elements fail on at least one scene background.\n")

    print("## Touch targets declared under 48 dp (measured)\n")
    print("| layout | element | class | width | height |")
    print("|---|---|---|---|---|")
    for l in layouts:
        for t in l["targets_under_48"]:
            print(f"| {l['layout']} | {t['element']} | {t['class']} | {t['w']} | {t['h']} |")

    print("\n## Spacing rhythm (measured)\n")
    sp_all = [s for l in layouts for s in l["spacing"]]
    vals = sorted({s["dp"] for s in sp_all})
    off8 = [s for s in sp_all if not s["on8"]]
    off4 = [s for s in sp_all if not s["on4"]]
    print(f"{len(sp_all)} margin/padding literals; distinct values: {[f'{v:g}' for v in vals]}")
    print(f"off the 8 dp grid: {len(off8)}; off the 4 dp grid: {len(off4)}")
    for s in off4[:40]:
        print(f"  - {s['element']}.{s['attr']} = {s['dp']:g}dp")

    print("\n## Text sized in dp instead of sp (measured)\n")
    for l in layouts:
        for t in l["textsize_in_dp"]:
            print(f"- {l['layout']} {t['element']}: {t['value']}")
    dim_dp_text = [k for k, v in DIMENS.items() if "text" in k.lower() and v.endswith(("dp", "dip"))]
    print(f"- dimens.xml text dimens in dp: {dim_dp_text}")

    print("\n## Hard-coded colours in layouts (measured)\n")
    for l in layouts:
        for c in l["hardcoded_colors"]:
            print(f"- {l['layout']} {c['element']}.{c['attr']} = {c['value']}")

    print("\n## Hard-coded strings in layouts (measured)\n")
    for l in layouts:
        for s in l["hardcoded_strings"]:
            print(f"- {l['layout']} {s['element']}.{s['attr']} = {s['value']!r}")

    lit = sum(len(l["literals"]) for l in layouts)
    print(f"\n## Literals: {lit} dp/sp literals across {len(layouts)} layouts; "
          f"{len(DIMENS)} dimens and {len(COLORS)} colours defined\n")
    s = report["strings"]
    print(f"## Strings: {s['default']} default, {s['it_present']} Italian entries "
          f"({len(s['it_empty'])} empty), coverage {s['it_coverage']}%, missing {len(s['it_missing'])}")
    if "designkit_verify" in report:
        print("\n## designkit cross-check\n")
        for v in report["designkit_verify"]:
            print(f"- {v['element']}: ours {v['ours_over_black']} / designkit {v['designkit']}")


if __name__ == "__main__":
    main()
