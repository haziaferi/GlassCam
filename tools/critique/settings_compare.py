"""settings_compare.py -- open each camera app's settings screen, dump the a11y tree, measure it.

    python tools/critique/settings_compare.py            -> screenshots/set_<app>.png/.xml + settings_compare.json

Measured per app: entry-point position (where the settings control sits, in dp from the bottom-
right screen corner), taps to reach the settings screen, every clickable node on the first
settings screen (nested ones included: a row's own +/- buttons count), their median/min height
in dp, how many are under 48 dp, and whether any EditText is on screen (`search`, a proxy: an
unrelated text field would count too). Added after the drawer critique (2026-09-16): `fill` =
fraction of the screen covered by top-level tappables, `reach_dp` = mean distance from the thumb
anchor (screen_w - 72 px, 0.55 * screen_h) to them, and `rows_first_screen` = how many top-level
tappables one screen shows.

Read these as TAPPABLES, not settings: on GlassCam the search field and the five jump chips are
top-level tappables too, so its "11 rows" are 5 setting rows + search + 5 chips, and the 48 dp
median is the chips' and search's height, not a row's (fresh-eyes 2026-09-28). Whether the live
preview stays visible behind the settings is NOT measured here; that column is judgement, as
are the other judgement columns in the report. A page with fewer than 5 titled top-level
tappables is reported as "not exposed to accessibility" (Open Camera's list has none); a
genuinely short settings page would be misread the same way. The a11y dump is kept as set_<app>.xml.
"""
import json
import os
import re
import subprocess
import sys
import time

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import walk  # noqa: E402

try:
    import defusedxml.ElementTree as ET
except ImportError:  # pragma: no cover
    import xml.etree.ElementTree as ET  # nosemgrep: python.lang.security.use-defused-xml-parse.use-defused-xml-parse

APPS = [
    ("glasscam", "com.hazia.glasscam", "text:⚙"),
    ("freedcam", "troop.com.freedcam", "swipe"),
    ("opencamera", "net.sourceforge.opencamera", "desc:Impostazioni|Settings"),
    ("proshot", "com.riseupgames.proshot2", "xy:1030,170"),
    ("manualcamerapro", "com.lensesdev.manual.camera.pro", "desc:Impostazioni|Settings"),
    ("macamera", "com.vayunmathur.camera", "desc:Settings|Impostazioni"),
    ("fossify", "org.fossify.camera", "desc:Settings|Impostazioni"),
    ("librecamera", "com.iakmds.librecamera", "desc:Settings|Impostazioni"),
    ("oneplus", "com.oplus.camera", "text:ALTRO|Altro|More"),
]
DENSITY = 480.0


def dump():
    walk.sh("mkdir", "-p", "/sdcard/critique")   # `walk.py restore` removes the folder
    walk.sh("rm", "-f", "/sdcard/critique/_s.xml", check=False)
    walk.sh("uiautomator", "dump", "/sdcard/critique/_s.xml", check=False)
    x = walk.sh("cat", "/sdcard/critique/_s.xml", check=False)   # an animating viewfinder never idles
    try:
        return ET.fromstring(x)
    except Exception:
        return None


def bounds(n):
    b = walk.parse_bounds(n.get("bounds", ""))
    return b


def find(root, kind, pat):
    rx = re.compile(pat, re.I)
    for n in root.iter("node"):
        v = n.get("content-desc" if kind == "desc" else "text") or ""
        if rx.fullmatch(v.strip()):
            return n
    return None


def measure(root, screen_w=2412, screen_h=1080):
    rows = []
    tops = []          # top-level tappables: no tappable ancestor (a row, not the switch inside it)
    search = False

    def walk_nodes(n, under_tappable):
        nonlocal search
        cls = n.get("class", "")
        if "EditText" in cls or (n.get("content-desc") or "").lower().startswith(("search", "cerca")):
            search = True
        tappable = n.get("clickable") == "true" or n.get("checkable") == "true"
        b = bounds(n) if tappable else None
        ok = b is not None and b[2] > b[0] and b[3] > b[1]
        if ok:
            rows.append(((b[2] - b[0]) * 160 / DENSITY, (b[3] - b[1]) * 160 / DENSITY))
            if not under_tappable:
                tops.append(b)
        for c in n:
            walk_nodes(c, under_tappable or ok)

    walk_nodes(root, False)
    hs = sorted(h for _, h in rows)
    med = hs[len(hs) // 2] if hs else 0
    # a settings row carries a title; when the tappables have none the page does not expose its
    # rows to accessibility (Open Camera's preference list) and fill/reach would measure the
    # icon buttons of the window behind it instead
    def has_text(n):
        return any((x.get("text") or "").strip() for x in n.iter("node"))
    titled = 0
    for n in root.iter("node"):
        b = bounds(n) if (n.get("clickable") == "true" or n.get("checkable") == "true") else None
        if b is not None and b in tops and has_text(n):
            titled += 1
    if titled < 5:
        return {"clickable": len(rows), "median_h_dp": round(med, 1), "min_h_dp": round(hs[0], 1) if hs else 0,
                "under_48": sum(1 for h in hs if h < 48), "search": search,
                "rows_first_screen": titled, "fill": None, "reach_dp": None,
                "note": "rows not exposed as tappable to accessibility; fill/reach not measurable"}
    # fill: union of the top-level tappable rows over the screen, on a 4 dp raster
    cell = 4 * DENSITY / 160
    gw, gh = int(screen_w / cell) + 1, int(screen_h / cell) + 1
    covered = bytearray(gw * gh)
    for b in tops:
        x0, y0 = max(0, int(b[0] / cell)), max(0, int(b[1] / cell))
        x1, y1 = min(gw, int(b[2] / cell) + 1), min(gh, int(b[3] / cell) + 1)
        for y in range(y0, y1):
            covered[y * gw + x0:y * gw + x1] = bytes([1]) * (x1 - x0)
    fill = sum(covered) / float(gw * gh)
    # reach: mean distance from the right-thumb anchor to each top-level row's centre, in dp
    tx, ty = screen_w - 24 * DENSITY / 160, screen_h * 0.55
    dists = [(((b[0] + b[2]) / 2 - tx) ** 2 + ((b[1] + b[3]) / 2 - ty) ** 2) ** 0.5 * 160 / DENSITY for b in tops]
    reach = sum(dists) / len(dists) if dists else 0
    return {"clickable": len(rows), "median_h_dp": round(med, 1), "min_h_dp": round(hs[0], 1) if hs else 0,
            "under_48": sum(1 for h in hs if h < 48), "search": search,
            "rows_first_screen": len(tops), "fill": round(fill, 3), "reach_dp": round(reach)}


def run(only=None):
    here = os.path.dirname(os.path.abspath(__file__))
    out_path = os.path.join(here, "settings_compare.json")
    out = {}
    if only and os.path.exists(out_path):
        out = json.load(open(out_path, encoding="utf-8"))   # re-measure a subset, keep the rest
    for name, pkg, how in APPS:
        if only and name not in only:
            continue
        walk.state().setdefault("extra_allowed", [])
        subprocess.run(["adb", "shell", "am", "force-stop", pkg], capture_output=True)
        subprocess.run(["adb", "shell", "monkey", "-p", pkg, "-c", "android.intent.category.LAUNCHER", "1"], capture_output=True)
        # wait for the app itself, not whatever was in front before (ProShot once measured
        # Open Camera's page because Open Camera had not gone away yet)
        for _ in range(20):
            time.sleep(0.5)
            if walk.foreground() == pkg:
                break
        time.sleep(3)
        if walk.foreground() != pkg:
            out[name] = {"package": pkg, "note": f"never came to the front (foreground {walk.foreground()})"}
            print(name, out[name])
            continue
        root = dump()
        entry = None
        taps = 0
        w, h = 2412, 1080
        if root is not None and root.find("node") is not None:
            b = bounds(root.find("node"))
            if b:
                w, h = b[2], b[3]
        if how == "swipe":
            walk.sh("input", "swipe", "300", str(h // 2), "1500", str(h // 2), "300"); taps = 1
            entry = ("swipe L->R", None)
        elif how.startswith("desc:") or how.startswith("text:"):
            kind, pat = how.split(":", 1)
            n = find(root, kind, pat) if root is not None else None
            if n is not None:
                b = bounds(n)
                walk.sh("input", "tap", str((b[0] + b[2]) // 2), str((b[1] + b[3]) // 2)); taps = 1
                entry = ("button", b)
            else:
                entry = ("not found", None)
        elif how.startswith("xy:"):
            x, y = how[3:].split(",")
            walk.sh("input", "tap", x, y); taps = 1
            entry = ("button", (int(x) - 60, int(y) - 60, int(x) + 60, int(y) + 60))
        if entry and entry[0] == "not found":
            out[name] = {"package": pkg, "entry": "not found", "note": "entry control not on screen; nothing measured"}
            print(name, out[name]); subprocess.run(["adb", "shell", "am", "force-stop", pkg], capture_output=True); continue
        time.sleep(2)
        walk.sh("screencap", "-p", f"/sdcard/critique/set_{name}.png")
        walk.adb("pull", f"/sdcard/critique/set_{name}.png", os.path.join(walk.SHOTS, f"set_{name}.png"))
        root2 = dump()
        if root2 is not None and {x.get("package") for x in root2.iter("node") if x.get("package")} - {pkg, "android", "com.android.systemui"}:
            out[name] = {"package": pkg, "note": "settings screen belongs to another package: " + str({x.get("package") for x in root2.iter("node")})}
            print(name, out[name]); subprocess.run(["adb", "shell", "am", "force-stop", pkg], capture_output=True); continue
        if root2 is not None:
            with open(os.path.join(walk.SHOTS, f"set_{name}.xml"), "w", encoding="utf-8") as f:
                f.write(ET.tostring(root2, encoding="unicode"))
        m = measure(root2, w, h) if root2 is not None else {"clickable": 0, "median_h_dp": 0, "min_h_dp": 0, "under_48": 0, "search": False, "note": "tree never idled"}
        # entry position relative to the bottom-right corner (right thumb) in dp
        pos = None
        if entry and entry[1]:
            b = entry[1]
            cx, cy = (b[0] + b[2]) / 2, (b[1] + b[3]) / 2
            pos = {"x_dp": round(cx * 160 / DENSITY), "y_dp": round(cy * 160 / DENSITY),
                   "screen_w_dp": round(w * 160 / DENSITY), "screen_h_dp": round(h * 160 / DENSITY),
                   "from_right_thumb_dp": round(((w - cx) ** 2 + (h - cy) ** 2) ** 0.5 * 160 / DENSITY)}
        out[name] = {"package": pkg, "entry": entry[0] if entry else None, "entry_pos": pos, "taps_to_settings": taps, **m}
        print(name, out[name])
        subprocess.run(["adb", "shell", "am", "force-stop", pkg], capture_output=True)
    with open(out_path, "w", encoding="utf-8", newline="\n") as f:
        json.dump(out, f, indent=2)


if __name__ == "__main__":
    run(set(sys.argv[1:]) or None)   # `settings_compare.py proshot oneplus` re-measures those two
