"""spec_check.py -- gate SPEC.md against the tree.

    python tools/spec_check.py          -> one line per check, exit 1 on any FAIL
    python tools/spec_check.py -v       -> also print what each check looked at

Three things are enforced, so the spec and the code cannot drift apart silently:
  1. the spec's revision line names the versionName/versionCode that app/build.gradle carries;
  2. every `[S-nn]` tag in SPEC.md has a check here, and every check here has a tag in SPEC.md
     (a claim cannot be marked as checked without a check, and a check cannot outlive its claim);
  3. every check passes against the source.

Adding a decision to the spec: write the row, put `[S-nn]` right after the one clause the check
proves (not at the end of the row - a tag certifies its clause only, and each tag appears once),
add `check_Snn` below with the same tag in its docstring, and prove the check can fail by
planting the defect in a scratch copy (tools/spec_check_mutants.py). Run this before the build
in every round.

Stdlib only; no device needed. Run from anywhere: paths resolve from this file.
"""
import os
import re
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
J = os.path.join(ROOT, "app", "src", "main", "java")
R = os.path.join(ROOT, "app", "src", "main", "res")
MANIFEST = os.path.join(ROOT, "app", "src", "main", "AndroidManifest.xml")
GRADLE = os.path.join(ROOT, "app", "build.gradle")
SPEC = os.path.join(ROOT, "SPEC.md")

VERBOSE = "-v" in sys.argv


def read(path):
    with open(path, encoding="utf-8") as f:
        return f.read()


def jfiles(name):
    """Every Java file with this basename."""
    return [os.path.join(d, name) for d, _, fs in os.walk(J) if name in fs]


def jfile(name):
    """The one Java file with this basename, or None when there is none. Two files with the same
    basename FAIL instead of reading as absent (fresh-eyes 2026-09-28: a removal check passed on
    None whether the file was gone or duplicated)."""
    hits = jfiles(name)
    need(len(hits) <= 1, f"{len(hits)} files named {name}: " + ", ".join(os.path.relpath(h, ROOT) for h in hits))
    return hits[0] if hits else None


def gone(name):
    """True when no Java file of this basename exists anywhere under the source tree."""
    return not jfiles(name)


def layout(name):
    return os.path.join(R, "layout", name)


def dimen(name):
    m = re.search(r'<dimen name="%s">([^<]+)</dimen>' % re.escape(name), read(os.path.join(R, "values", "dimens.xml")))
    return m.group(1) if m else None


def color(name):
    m = re.search(r'<color name="%s">([^<]+)</color>' % re.escape(name), read(os.path.join(R, "values", "colors.xml")))
    return m.group(1).upper() if m else None


def strings(qualifier=""):
    path = os.path.join(R, "values" + qualifier, "strings.xml")
    return read(path) if os.path.exists(path) else ""


def method_body(src, name):
    """Body of the first Java method called `name` (brace-matched from its signature)."""
    m = re.search(r'\b%s\s*\([^)]*\)\s*(?:throws [^{]+)?\{' % re.escape(name), src)
    if not m:
        return ""
    i, depth = m.end(), 1
    while i < len(src) and depth:
        depth += {"{": 1, "}": -1}.get(src[i], 0)
        i += 1
    return src[m.end():i - 1]


def xml_root(path):
    """Parsed root of one of this repo's own resource files. The input is the tree itself, not
    untrusted data; the gate is stdlib-only by design and expat resolves no external entities."""
    import xml.etree.ElementTree as ET
    return ET.parse(path).getroot()  # nosemgrep: python.lang.security.use-defused-xml-parse.use-defused-xml-parse


ANDROID = "{http://schemas.android.com/apk/res/android}"


def attrs(el):
    return {k.replace(ANDROID, "android:"): v for k, v in el.attrib.items()}


def by_id(root, id_):
    """The element whose android:id is @+id/<id_>, or None."""
    for el in root.iter():
        if el.attrib.get(ANDROID + "id") in (f"@+id/{id_}", f"@id/{id_}"):
            return el
    return None


class Fail(Exception):
    pass


def need(cond, why):
    if not cond:
        raise Fail(why)


# --------------------------------------------------------------------------------------------
# revision

def revision():
    g = read(GRADLE)
    code = int(re.search(r"versionCode (\d+)", g).group(1))
    name = re.search(r"versionName '([^']+)'", g).group(1)
    s = read(SPEC)
    m = re.search(r"\*\*Revision ([\d.]+) \(versionCode (\d+)\)", s)
    need(m, "SPEC.md has no '**Revision X (versionCode N)' line")
    need(m.group(1) == name and int(m.group(2)) == code,
         f"SPEC.md revision {m.group(1)}/{m.group(2)} != build.gradle {name}/{code}")
    return name, code


# --------------------------------------------------------------------------------------------
# checks -- one per [S-nn] tag; the tag is the first token of the docstring

def check_S01():
    """[S-01] manifest: no legacy storage permissions, allowBackup false"""
    # parsed, not substring-matched: a comment mentioning allowBackup="false" used to satisfy it
    root = xml_root(MANIFEST)
    perms = {el.attrib.get(ANDROID + "name", "") for el in root.iter() if el.tag.startswith("uses-permission")}
    need(not any(p.endswith(("WRITE_EXTERNAL_STORAGE", "READ_EXTERNAL_STORAGE")) for p in perms), "legacy storage permission in the manifest")
    app = root.find("application")
    need(app is not None and app.attrib.get(ANDROID + "allowBackup") == "false", "<application> allowBackup is not false")


def check_S02():
    """[S-02] build.gradle: minSdk 30, targetSdk 36, compileSdk 36"""
    g = read(GRADLE)
    for key, val in (("minSdkVersion", "30"), ("targetSdk", "36"), ("compileSdk", "36")):
        need(re.search(r"\b%s %s\b" % (key, val), g), f"{key} is not {val}")


def check_S03():
    """[S-03] versionName = 4.3.<versionCode-200>"""
    name, code = revision()
    need(name == f"4.3.{code - 200}", f"versionName {name} does not follow 4.3.<versionCode-200> for {code}")


def check_S04():
    """[S-04] every capture path writes into the GlassCam/ child of the SAF tree"""
    src = read(jfile("FileListController.java"))
    need("getGlassCamDocumentFolder" in method_body(src, "getNewImgFileHolder"), "getNewImgFileHolder does not use getGlassCamDocumentFolder()")
    body = method_body(src, "getGlassCamDocumentFolder")
    need('"GlassCam"' in body and "findFile" in body and "createDirectory" in body, "getGlassCamDocumentFolder does not find-or-create the child")
    need(re.search(r"\.createFile\([^)]*\)", src) and "getExternalSdDocumentFile(context).createFile" not in src,
         "a capture path still creates files in the tree root")


def check_S05():
    """[S-05] a vanished or revoked SAF tree reads as no folder"""
    body = method_body(read(jfile("FileListController.java")), "getExternalSdDocumentFile")
    # both guards must end in "no folder": a vanished tree returns null, a revoked grant is caught and returns null
    need(re.search(r"!\s*sdDir\.exists\(\)\)\s*return null;", body), "a tree that does not exist does not read as null")
    need(re.search(r"catch\s*\(\s*SecurityException[^)]*\)\s*\{[^}]*return null;", body), "a revoked grant does not read as null")


def check_S06():
    """[S-06] onCaptureSaved is raised from the capture path only"""
    src = read(jfile("FileListController.java"))
    need("void onCaptureSaved(BaseHolder" in src, "NotifyFilesChanged has no onCaptureSaved")
    need("fireCaptureSaved" in method_body(src, "addFromEventFile") and "fireCaptureSaved" in method_body(src, "addFromEventFiles"),
         "addFromEventFile(s) do not raise onCaptureSaved")
    # "only": the private raiser is called from nowhere but those two methods (its own definition aside)
    calls = len(re.findall(r"\bfireCaptureSaved\(", src)) - 1
    inside = method_body(src, "addFromEventFile").count("fireCaptureSaved(") + method_body(src, "addFromEventFiles").count("fireCaptureSaved(")
    need(calls == inside, f"fireCaptureSaved is called {calls} times, {inside} of them from addFromEventFile(s)")


def check_S07():
    """[S-07] shutter column: 56 dp cells, 72 dp shutter"""
    need(dimen("nextgen_cameraui_textswitch") == "56dp", f"cell is {dimen('nextgen_cameraui_textswitch')}")
    need(dimen("nextgen_shutterbutton_size") == "72dp", f"shutter is {dimen('nextgen_shutterbutton_size')}")


def check_S08():
    """[S-08] the gallery cell opens the viewer inside the newest folder and refreshes on a new capture"""
    path = jfile("NextGenGalleryButton.java")
    need(path, "NextGenGalleryButton.java missing")
    src = read(path)
    # fresh-eyes 2026-09-28: the old check passed on the word "viewer" in the import line
    need(re.search(r"new Intent\([^;]*ActivityGlassCamViewer\.class", src), "the gallery cell does not start ActivityGlassCamViewer")
    need(re.search(r"putExtra\(\s*ActivityGlassCamViewer\.EXTRA_OPEN_NEWEST_FOLDER\s*,\s*true\s*\)", src), "the cell does not ask for the newest folder")
    need("refresh()" in method_body(src, "onCaptureSaved") or "refresh()" in method_body(src, "onFilesChanged"),
         "the cell does not refresh when a capture is saved")


def check_S09():
    """[S-09] level bar: 0.5 deg tolerance, 0.2 deg redraw gate, 2 s hide, SENSOR_DELAY_UI"""
    src = read(jfile("LevelBarView.java"))
    need("LEVEL_TOLERANCE = 0.5f" in src and "MIN_CHANGE = 0.2f" in src and "HIDE_AFTER_MS = 2000" in src, "LevelBarView constants changed")
    need("SENSOR_DELAY_UI" in read(jfile("HorizontLineFragment.java")), "level sensor is not SENSOR_DELAY_UI")


def check_S10():
    """[S-10] BACK closes the drawer on both the dispatcher and the key path"""
    src = read(jfile("ActivityGlassCamMain.java"))
    need("OnBackPressedCallback" in src and "closeSettingsIfOpen" in method_body(src, "handleOnBackPressed"), "gesture BACK does not close the drawer")
    need("KEYCODE_BACK && closeSettingsIfOpen()" in method_body(src, "onKeyUp"), "the BACK key path bypasses closeSettingsIfOpen")


def check_S11():
    """[S-11] settings panel is full width and full height"""
    # fresh-eyes 2026-09-28: the old check fell back to the file's first 800 characters when
    # setting_holder was missing, and read the ROOT view's match_parent - it passed with no panel
    root = xml_root(layout("nextgen_setting_fragment.xml"))
    holder = by_id(root, "setting_holder")
    need(holder is not None, "setting_holder missing from nextgen_setting_fragment.xml")
    a = attrs(holder)
    need(a.get("android:layout_width") == "match_parent" and a.get("android:layout_height") == "match_parent",
         f"setting_holder is {a.get('android:layout_width')} x {a.get('android:layout_height')}")
    inside = {el.attrib.get(ANDROID + "id") for el in holder.iter()}
    for id_ in ("settings_search", "settings_sticky_section", "settings_chips_scroll", "setting_fragment_holder"):
        need(f"@+id/{id_}" in inside, f"{id_} is not inside setting_holder")


def check_S12():
    """[S-12] one list (GROUP_ALL), full-span unclickable sections, no tabs"""
    src = read(jfile("NextGenSettingDummyFragment.java"))
    need("GROUP_ALL = -1" in src and "SpanSizeLookup" in src, "GROUP_ALL / SpanSizeLookup missing")
    need(jfile("SettingTabPagerAdapter.java") is None, "SettingTabPagerAdapter.java still exists")
    ad = read(jfile("NextGenSettingItemRecyclerAdapter.java"))
    need("setClickable(false)" in ad, "section rows are clickable")


def check_S13():
    """[S-13] uniform 76 dp rows, compact stepper with 48 dp steps"""
    need(dimen("nextgen_settings_row_min_height") == "76dp", f"row min is {dimen('nextgen_settings_row_min_height')}")
    for lay in ("nextgen_setting_item.xml", "nextgen_setting_boolitem.xml", "settings_expandable_childs_number.xml"):
        need("@dimen/nextgen_settings_row_min_height" in read(layout(lay)), f"{lay} does not use the row minimum")
    st = read(layout("settings_expandable_childs_number.xml"))
    need(st.count('android:layout_width="48dp"') >= 2 and 'android:maxLines="1"' in st, "stepper is not the compact one-line form")


def check_S14():
    """[S-14] jump chips in their own bottom row, 48 dp targets"""
    x = read(layout("nextgen_setting_fragment.xml"))
    lst, chips = x.find('@+id/setting_fragment_holder"'), x.find('@+id/settings_chips_scroll"')
    need(lst >= 0 and chips >= 0, "list or chips row missing")   # find() is -1 when absent: -1 < n passed before
    need(lst < chips, "chips row is not below the list")
    src = read(jfile("NextGenCameraSettingFragment.java"))
    need("InsetDrawable" in src and "setMinHeight((int) (48 * d))" in src and "setMinWidth((int) (48 * d))" in src, "chips are not 48 dp targets")


def check_S15():
    """[S-15] drawer search: strings in EN and IT, filter in the list"""
    for key in ("settings_search_hint", "settings_search_clear", "settings_search_empty"):
        need(f'name="{key}"' in strings(), f"{key} missing (EN)")
        need(f'name="{key}"' in strings("-it"), f"{key} missing (IT)")
    need("void setFilter(String" in read(jfile("NextGenSettingDummyFragment.java")), "setFilter missing")


def check_S16():
    """[S-16] picker title row: 48 dp, not clickable, a11y heading, no click listener"""
    t = read(layout("nextgen_setting_picker_title.xml"))
    need('android:minHeight="48dp"' in t and 'android:clickable="false"' in t and 'android:accessibilityHeading="true"' in t, "picker title row attributes")
    ad = read(jfile("NextGenSettingValuePickerAdapter.java"))
    need(re.search(r"if \(getItemViewType\(position\) != ROW_TITLE\)[^\n]*\n\s*holder\.itemView\.setOnClickListener", ad),
         "the adapter does not skip the title row's listener")


def check_S17():
    """[S-17] no update row, no update URL, checker hard-off"""
    need("CHECKFORUPDATES" not in read(jfile("SettingGroupConfig.java")), "the update row is back in the drawer")
    need("isGithubRelease = false" in read(jfile("ReleaseChecker.java")), "ReleaseChecker is not hard-off")
    # Java AND resources: an update link can live in strings.xml as easily as in code
    for top in (J, R):
        for d, _, fs in os.walk(top):
            # res/raw/supported_devices.xml is upstream's device database: its opcode URLs are parsed and
            # rewritten to the config file but never fetched (SettingsManager.loadOpCodes reads local
            # opc*.bin only; fresh-eyes 2026-09-28), so they are data, not an update link
            if os.path.basename(d) == "raw":
                continue
            for f in fs:
                # a URL only: "Created by KillerInk" headers are the GPL author attribution and stay
                if f.endswith((".java", ".xml")) and re.search(r"https?://[^\s\"'<>]*(github\.com|KillerInk)", read(os.path.join(d, f))):
                    raise Fail(f"a github/KillerInk URL is still in {os.path.relpath(os.path.join(d, f), ROOT)}")


def check_S18():
    """[S-18] app colours: dark ground and lime, not stock indigo/pink"""
    need(color("colorPrimary") == "#FF121212", f"colorPrimary {color('colorPrimary')}")
    need(color("colorAccent") == "#64DD17", f"colorAccent {color('colorAccent')}")
    need(color("manual_button_active") == "#64DD17", "the active lime changed")


def check_S19():
    """[S-19] share activity: registered, configChanges, waits for onCaptureSaved"""
    root = xml_root(MANIFEST)
    acts = {el.attrib.get(ANDROID + "name"): el for el in root.iter("activity")}
    share, main = acts.get("glasscam.cam.ActivityGlassCamShare"), acts.get("glasscam.cam.ActivityGlassCamMain")
    need(share is not None and main is not None, "ActivityGlassCamShare or ActivityGlassCamMain not in the manifest")
    # "the same configChanges as the main activity": compare the attribute itself, not a substring of the tag
    cc = lambda el: set((el.attrib.get(ANDROID + "configChanges") or "").split("|")) - {""}
    need(cc(share) and cc(share) == cc(main), f"share configChanges {sorted(cc(share))} != main {sorted(cc(main))}")
    actions = {a.attrib.get(ANDROID + "name") for a in share.iter("action")}
    need("android.media.action.IMAGE_CAPTURE" in actions, "share activity has no IMAGE_CAPTURE filter")
    need("public void onCaptureSaved(BaseHolder" in read(jfile("ActivityGlassCamShare.java")), "share activity does not listen to onCaptureSaved")


def check_S20():
    """[S-20] the Batch 1 typos stay fixed"""
    s = strings()
    for bad in ("Continouse", "Dectection", "Potrait", "reseted", " wich "):
        need(bad not in s, f"'{bad.strip()}' is back in strings.xml")


def check_S21():
    """[S-21] removed code stays removed: harness, tab adapter, no orphan layout (the count SPEC states), no themesample, no Hilt"""
    for f in ("PresetGridHarnessActivity.java", "PresetGridLayout.java", "SettingTabPagerAdapter.java"):
        need(gone(f), f"{f} is back")
    need("PresetGridHarnessActivity" not in read(MANIFEST), "the harness is in the manifest")
    # the five orphan cameraui_* layouts were never listed by name, so the claim is gated as what it
    # means: no layout is an orphan, and the count SPEC states is the count on disk
    layouts = sorted(f[:-4] for f in os.listdir(os.path.join(R, "layout")) if f.endswith(".xml"))
    stated = re.search(r"\b(\d+) layouts remain\b", read(SPEC))
    need(stated and int(stated.group(1)) == len(layouts), f"SPEC says {stated and stated.group(1)} layouts, the tree has {len(layouts)}")
    corpus = []
    for top in (J, R, MANIFEST):
        for d, _, fs in (os.walk(top) if os.path.isdir(top) else [(os.path.dirname(top), None, [os.path.basename(top)])]):
            corpus += [read(os.path.join(d, f)) for f in fs if f.endswith((".java", ".xml"))]
    text = "\n".join(corpus)
    # R.layout.x, a static-imported layout.x, @layout/x, or the data-binding class XBinding
    orphans = [n for n in layouts if not re.search(r"\blayout\.%s\b|@layout/%s\b|\b%sBinding\b" % (
        n, n, "".join(p.capitalize() for p in n.split("_"))), text)]
    need(not orphans, "orphan layouts: " + ", ".join(orphans))
    need(not any("themesample" in d for d, _, _ in os.walk(J)), "a themesample package is back")
    gradle = [os.path.join(ROOT, p) for p in ("build.gradle", "app/build.gradle", "settings.gradle", "gradle/libs.versions.toml")]
    need(not any("hilt" in read(p).lower() for p in gradle if os.path.exists(p)), "Hilt is back in the build files")


CLICK_CLASSES = ("Button", "ImageButton", "Switch", "SwitchCompat", "CheckBox", "RadioButton", "ToggleButton", "Chip")


def dimen_dp(value):
    """A declared size in dp, following @dimen/ references; None for match_parent/wrap_content/other units."""
    hops = 0
    while value and value.startswith("@dimen/") and hops < 5:
        value, hops = dimen(value[len("@dimen/"):]) or "", hops + 1
    m = re.fullmatch(r"(\d+(?:\.\d+)?)(dp|dip)", (value or "").strip())
    return float(m.group(1)) if m else None


def layout_elements():
    """(layout file, element) for every element of every file in res/layout - no hand-picked list:
    a literal list cannot prove an 'every' claim (fresh-eyes seed, plant G8)."""
    d = os.path.join(R, "layout")
    for f in sorted(os.listdir(d)):
        if f.endswith(".xml"):
            for el in xml_root(os.path.join(d, f)).iter():
                yield f, el


def t7_exceptions():
    """`layout.xml#id` entries that SPEC.md's T7 row names as stated exceptions to the floor."""
    row = next((l for l in read(SPEC).splitlines() if l.startswith("| T7 |")), "")
    return set(re.findall(r"`([\w.]+\.xml#\w+)`", row))


def check_S22():
    """[S-22] no layout declares a tappable view under 48 dp on either side, except the exceptions T7 names"""
    allowed, bad, used = t7_exceptions(), [], set()
    for f, el in layout_elements():
        a = attrs(el)
        tag = el.tag.split(".")[-1]
        if a.get("android:clickable") == "false":
            continue
        if not (a.get("android:clickable") == "true" or "android:onClick" in a or tag.endswith(CLICK_CLASSES)):
            continue
        ident = f"{f}#{a.get('android:id', '?').split('/')[-1]}"
        # per axis: the fixed size if one is declared, else the declared minimum (wrap_content + minHeight)
        w = dimen_dp(a.get("android:layout_width"))
        h = dimen_dp(a.get("android:layout_height"))
        w = w if w is not None else dimen_dp(a.get("android:minWidth"))
        h = h if h is not None else dimen_dp(a.get("android:minHeight"))
        small = [s for s in (w, h) if s is not None and s < 48]
        if small:
            if ident in allowed:
                used.add(ident)
            else:
                bad.append(f"{ident} {w}x{h} dp")
    need(not bad, f"{len(bad)} tappable views declared under 48 dp: " + "; ".join(bad[:8]))
    need(used == allowed, "T7 names exceptions that no longer exist: " + ", ".join(sorted(allowed - used)))


def check_S23():
    """[S-23] every textSize in a layout is sp (directly or through the dimen it names), never dp/dip/px"""
    bad = []
    for f, el in layout_elements():
        ts = el.attrib.get(ANDROID + "textSize")
        if ts is None:
            continue
        v, hops = ts, 0
        while v.startswith("@dimen/") and hops < 5:
            v, hops = dimen(v[len("@dimen/"):]) or "", hops + 1
        if not re.fullmatch(r"\d+(?:\.\d+)?sp", v.strip()):
            bad.append(f"{f}: textSize {ts} = {v}")
    need(not bad, f"{len(bad)} text sizes not in sp: " + "; ".join(bad[:6]))


def check_S24():
    """[S-24] every activity in the manifest is locked to landscape"""
    acts = list(xml_root(MANIFEST).iter("activity"))
    need(acts, "no activities found in the manifest")
    loose = [a.attrib.get(ANDROID + "name", "?").split(".")[-1] for a in acts
             if a.attrib.get(ANDROID + "screenOrientation") not in ("landscape", "sensorLandscape")]
    need(not loose, "not landscape-locked: " + ", ".join(loose))


def check_S25():
    """[S-25] the value scale opens beside the shutter column: anchored to the end, clear of two column cells"""
    root = xml_root(layout("nextgen_cameraui_manual_fragment_rotatingseekbar.xml"))
    frame = by_id(root, "frameLayout")
    need(frame is not None and by_id(frame, "seekbar") is not None, "the value scale's frame or seekbar is missing")
    a = attrs(frame)
    need(a.get("android:layout_alignParentEnd") == "true" or a.get("android:layout_alignParentRight") == "true",
         "the value scale is not anchored to the end (thumb) side")
    need(a.get("android:layout_alignParentStart") != "true" and a.get("android:layout_alignParentLeft") != "true",
         "the value scale is still anchored to the start (left band)")
    margin = dimen_dp(a.get("android:layout_marginEnd") or a.get("android:layout_marginRight"))
    cell = dimen_dp("@dimen/nextgen_cameraui_textswitch")
    need(margin is not None and cell is not None and margin >= 2 * cell,
         f"end margin {margin} dp does not clear the two {cell} dp column cells")


def check_S26():
    """[S-26] no Play button on stills: the button is bound to its model, and the model hides it for JPG, JPS, DNG and raw"""
    x = xml_root(layout("glasscamviewer_screenslide_fragment.xml"))
    play = by_id(x, "button_play")
    need(play is not None, "button_play missing from the viewer")
    # the Batch 2 hide never reached the screen: the model was set, the view was never bound (6T walk 2026-09-28)
    need(play.attrib.get("setVisibility", "").replace(" ", "") == "@{screenslideFragmentModel.playButton.visibility}",
         "button_play is not bound to playButton.visibility")
    src = read(jfile("ScreenSlideFragmentModelView.java"))
    body = method_body(src, "updateUi")
    for ending in ("JPG", "DNG", "RAW"):
        branch = re.search(r"FileEnding\.%s\)[^{]*\{(.*?)\n            \}" % ending, body, re.S)
        need(branch and "playButton.setVisibility(false)" in branch.group(1), f"the {ending} branch does not hide Play")


def check_S27():
    """[S-27] Camera2 mode defaults do not depend on the phone's language: picked by the English label of the mode, every caller passes the array id"""
    src = read(jfile("Camera2Util.java"))
    body = method_body(src, "detectIntMode")
    # the Italian 6T never found "Fast" ("Veloce") and fell through to noise reduction Off (2026-09-28)
    need("getDefaultLocaleStringArray" in body and "defaultLabel(" in body,
         "detectIntMode does not pick its default through the English labels")
    need(not re.search(r'map\.get\("', src), 'Camera2Util looks a mode up by a literal (translated) label again')
    calls = []
    for root, _, files in os.walk(J):
        for f in files:
            if f.endswith(".java"):
                calls += re.findall(r"Camera2Util\.detectIntMode\(([^;]*)\);", read(os.path.join(root, f)), re.S)
    need(len(calls) >= 14, f"only {len(calls)} detectIntMode callers found")
    for c in calls:
        need(re.search(r",\s*R\.array\.\w+\s*,\s*settingsManager\s*$", c),
             f"a detectIntMode caller does not pass an array id: {c[-80:]!r}")


def check_S28():
    """[S-28] noise reduction Off never reaches the Camera2 preview request: every preview write goes through previewValue, which maps Off to Fast"""
    src = read(jfile("CaptureSessionHandler.java"))
    # an Off preview keeps delivering frames on the 6T but shows a frozen, smeared image (owner, 2026-09-28)
    raw = re.findall(r"mPreviewRequestBuilder\.set\(key\s*,\s*value\)", src)
    need(not raw, f"{len(raw)} preview write(s) bypass previewValue")
    body = method_body(src, "previewValue")
    need("NOISE_REDUCTION_MODE_OFF" in body and "NOISE_REDUCTION_MODE_FAST" in body,
         "previewValue no longer maps noise reduction Off to Fast")


# --------------------------------------------------------------------------------------------

def main():
    checks = {re.match(r"\[(S-\d\d)\]", fn.__doc__).group(1): fn for name, fn in globals().items() if name.startswith("check_S")}
    all_tags = re.findall(r"\[(S-\d\d)\]", read(SPEC))
    tagged = set(all_tags)
    fails = 0

    def report(tag, ok, text):
        nonlocal fails
        print(f"{'ok  ' if ok else 'FAIL'} {tag}  {text}")
        if not ok:
            fails += 1

    try:
        name, code = revision()
        report("rev ", True, f"SPEC.md revision {name} / {code} = build.gradle")
    except Fail as e:
        report("rev ", False, str(e))
    # a tag certifies the one clause it follows (SPEC.md header): the same tag on two clauses would
    # certify a second claim its check never looked at (T7 and D4 both carried S-13 until 4.3.90)
    for tag in sorted(t for t in tagged if all_tags.count(t) > 1):
        report(tag, False, f"tagged {all_tags.count(tag)} times in SPEC.md; a tag certifies one clause")
    for tag in sorted(tagged - set(checks)):
        report(tag, False, "tagged in SPEC.md but has no check")
    for tag in sorted(set(checks) - tagged):
        report(tag, False, "has a check but no tag in SPEC.md")
    for tag in sorted(checks):
        fn = checks[tag]
        title = fn.__doc__.split("]", 1)[1].strip()
        try:
            fn()
            report(tag, True, title)
        except Fail as e:
            report(tag, False, f"{title} -- {e}")
        except Exception as e:  # a missing file or a moved anchor is a failure too, not a crash
            report(tag, False, f"{title} -- {type(e).__name__}: {e}")
    print(f"{len(checks)} checks, {fails} failing")
    sys.exit(1 if fails else 0)


if __name__ == "__main__":
    main()
