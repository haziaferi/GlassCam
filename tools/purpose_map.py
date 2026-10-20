#!/usr/bin/env python3
"""The purpose map: GlassCam's screens, controls and settings by what they are for.

    python tools/purpose_map.py --census       print every screen-like thing the code can show, as JSON
    python tools/purpose_map.py                writes docs/purpose-map.md from docs/purpose-map.json; exit 1 if a check fails
    python tools/purpose_map.py --check        checks only, and fails if the page is stale

The same format as Emivita's and Factotum's purpose maps: edit docs/purpose-map.json, never the
page. The census reads the code, so a renamed or removed control fails the check here:

  activity:<Name>    every activity in the manifest
  module:<Name>      every capture module the two module handlers register (Camera2 and Camera1)
  column:<KEY>       every toggle the camera screen's shutter column adds
  strip:<KEY>        every item of the manual strip (ManualGroupConfig)
  setting:<KEY>      every settings row (SettingGroupConfig), keyed by SettingKeys name, or
  setting:@<string>  by its header string when the row has no key (action rows)
  viewer:<id>        the gallery viewer's buttons
  fragment:<Name>    every fragment class (most are parts of a screen; the map lists them under "parts")

Each entry carries what it writes underneath (`writes`: a CaptureRequest key, a vendor key, a Camera1
parameter, or app state) and which camera API offers it, so "two controls, one parameter" is countable.
Stdlib only.
"""
import glob
import json
import os
import re
from collections import Counter
import sys

sys.stdout.reconfigure(encoding="utf-8")

ROOT = os.path.normpath(os.path.join(os.path.dirname(os.path.abspath(__file__)), ".."))
MAIN = os.path.join(ROOT, "app", "src", "main")
J = os.path.join(MAIN, "java", "glasscam")
RES = os.path.join(MAIN, "res")


def read(path):
    with open(path, encoding="utf-8") as fh:
        return fh.read()


def jfile(name):
    hits = glob.glob(os.path.join(J, "**", name), recursive=True)
    if len(hits) != 1:
        raise SystemExit("expected one %s, found %d" % (name, len(hits)))
    return read(hits[0])


def strings(qualifier=""):
    x = read(os.path.join(RES, "values" + qualifier, "strings.xml"))
    out = {}
    for name, val in re.findall(r'<string name="([^"]+)"[^>]*>(.*?)</string>', x, re.S):
        out[name] = re.sub(r"\\(['\"])", r"\1", val.strip())
    return out


REQUEST = r"\b(CaptureRequest\w*)\.([A-Za-z_][A-Za-z0-9_]+)\b(?!\()"
SENDS = re.compile(r"(?:SetParameterRepeating|SetPreviewParameterRepeating|SetPreviewParameter|SetParameter|SetCaptureParameter"
                   r"|parameterKey\s*=|\.set)\(?\s*" + REQUEST)


def class_bodies(src):
    """(class name, body) for every class in a Java file, nested ones split out by brace matching."""
    out = []
    for m in re.finditer(r"\bclass (\w+)[^{]*\{", src):
        i, depth = m.end(), 1
        while i < len(src) and depth:
            depth += {"{": 1, "}": -1}.get(src[i], 0)
            i += 1
        out.append((m.group(1), src[m.end():i - 1]))
    return out


def camera2_writes():
    """SettingKeys name -> the Camera2 request keys it sends, read from the parameter classes themselves.

    A key registered in ParameterHandlerApi2 with an explicit CaptureRequest uses that; otherwise the
    class that registers itself for the key (super(..., SettingKeys.K)) is read and every request key
    it sends is listed. Keys handled outside ParameterHandlerApi2 (WbHandler's colour correction) are
    found the same way."""
    files = glob.glob(os.path.join(J, "cam", "apis", "camera2", "**", "*.java"), recursive=True)
    by_class, by_key = {}, {}
    for path in files:
        for name, body in class_bodies(read(path)):
            sent = sorted({"%s.%s" % m.groups() for m in re.finditer(SENDS, body)})
            by_class[name] = sent
            own = re.findall(r"super\([^;]*SettingKeys\.(\w+)", body)
            if len(set(own)) == 1 and sent:
                by_key.setdefault(own[0], set()).update(sent)
    out = {}
    src = jfile("ParameterHandlerApi2.java")
    local = dict(re.findall(r"\b(\w+)\s*=\s*(new \w+\([^;]*)\);", src))   # tonemapmode = new BaseModeApi2(..., CaptureRequest.TONEMAP_MODE)
    for key, rest in re.findall(r"add\(SettingKeys\.(\w+),\s*([^;]*)\);", src):
        if re.match(r"\w+$", rest.strip()) and rest.strip() in local:
            rest = local[rest.strip()]
        m = re.search(REQUEST, rest)
        cls = re.search(r"new (\w+)\(", rest)
        if m:
            out[key] = "%s.%s" % m.groups()
        elif key in by_key:
            out[key] = ", ".join(sorted(by_key[key]))
        elif cls and by_class.get(cls.group(1)):
            out[key] = ", ".join(by_class[cls.group(1)])
        else:
            out[key] = "class " + cls.group(1) if cls else rest.strip()
    for key, sent in by_key.items():
        out.setdefault(key, ", ".join(sorted(sent)))
    return out


def camera1_keys():
    src = jfile("ParametersHandler.java")
    return set(re.findall(r"add\(SettingKeys\.(\w+)", src))


def census():
    en, it = strings(), strings("-it")
    c2, c1 = camera2_writes(), camera1_keys()

    def api(key):
        if key is None:
            return "app"
        return "both" if key in c2 and key in c1 else "camera2" if key in c2 else "camera1" if key in c1 else "app"

    out = []

    def add(id_, kind, label_res=None, label=None, key=None, section=None, **extra):
        e = {"id": id_, "kind": kind, "label": label or en.get(label_res or "", ""), "label_it": it.get(label_res or "", "")}
        if key:
            e["key"] = key
            e["writes"] = c2.get(key) or ("Camera1 parameter" if key in c1 else "app setting")
            e["api"] = api(key)
        if section:
            e["section"] = section
        e.update(extra)
        out.append(e)

    manifest = read(os.path.join(MAIN, "AndroidManifest.xml"))
    for name in re.findall(r'<activity[^>]*?android:name="([^"]+)"', manifest, re.S):
        add("activity:" + name.rsplit(".", 1)[-1], "activity", label=name.rsplit(".", 1)[-1])

    for handler in ("ModuleHandlerApi2.java", "ModuleHandler.java"):
        src = jfile(handler)
        for cls in re.findall(r"\w+\s+\w+\s*=\s*new (\w+)\(", src):
            add("module:" + cls, "module", label=cls, api="camera2" if handler.endswith("Api2.java") else "camera1")

    ui = jfile("NextGenCameraUiFragment.java")
    for key in re.findall(r"addUiTextSwitch\w*\(\w+,\s*\([^)]*\)\s*parameterHandler\.get\(SettingKeys\.(\w+)\)", ui):
        add("column:" + key, "column", label=key.lower(), key=key)

    strip = jfile("ManualGroupConfig.java")
    for key, font in re.findall(r"ManualItemConfig<String>\(SettingKeys\.(\w+),\s*GlassCamApplication\.getStringFromRessources\(R\.string\.(\w+)\)", strip):
        add("strip:" + key, "strip", label=key[2:].lower() if key.startswith("M_") else key.lower(), key=key, glyph=font)

    cfg = jfile("SettingGroupConfig.java")
    for group, body in re.findall(r"public List<SettingItemConfig> (get\w+Group)\(\)\s*\{(.*?)\n    \}", cfg, re.S):
        section = group[3:-5]
        for key, header, _desc, vtype in re.findall(
                r"new SettingItemConfig\((?:SettingKeys\.(\w+)|null)\s*,\s*(?:R\.string\.(\w+)|0)\s*,\s*(?:R\.string\.(\w+)|0)\s*,\s*\w+\s*,\s*SettingItemConfig\.ViewType\.(\w+)\)", body):
            if vtype == "Section":
                continue
            id_ = "setting:" + (key or "@" + header)
            add(id_, "setting", label_res=header, key=key or None, section=section, view=vtype)

    slide = read(os.path.join(RES, "layout", "glasscamviewer_screenslide_fragment.xml"))
    for bid in re.findall(r'android:id="@\+id/(button_\w+)"', slide):
        add("viewer:" + bid, "viewer", label=bid[7:])

    for path in glob.glob(os.path.join(J, "**", "*Fragment.java"), recursive=True):
        name = os.path.basename(path)[:-5]
        if re.search(r"\bclass %s\b" % name, read(path)) and "abstract class" not in read(path):
            add("fragment:" + name, "fragment", label=name)

    seen, dup = set(), []
    for e in out:
        if e["id"] in seen:
            dup.append(e["id"])
        seen.add(e["id"])
    if dup:
        raise SystemExit("duplicate census ids: %s" % ", ".join(dup))
    return out


SRC = os.path.join(ROOT, "docs", "purpose-map.json")
OUT = os.path.join(ROOT, "docs", "purpose-map.md")
REFS = {"fossify": "Fossify", "freedcam": "FreeDcam", "librecamera": "Libre Camera", "macamera": "MA Camera",
        "manualcamerapro": "Manual Camera", "oneplus": "OnePlus Camera", "opencamera": "Open Camera", "proshot": "ProShot"}


def checks(m, code):
    """Every census id placed exactly once, every placed id still in the code, every reference known."""
    bad = []
    ids = {e["id"] for e in code}
    placed = Counter(c for s in m["screens"] for c in s["code"]) + Counter(m["parts"])
    for c in sorted(ids - set(placed)):
        bad.append("in the code, not on the map: %s" % c)
    for c in sorted(set(placed) - ids):
        bad.append("on the map, gone from the code: %s" % c)
    for c, n in sorted(placed.items()):
        if n > 1:
            bad.append("placed %d times: %s" % (n, c))
    purposes = {p["id"] for p in m["purposes"]}
    entry_ids = Counter(s["id"] for s in m["screens"])
    for i, n in entry_ids.items():
        if n > 1:
            bad.append("two entries share the id %s" % i)
    for s in m["screens"]:
        if s["purpose"] not in purposes:
            bad.append("%s: unknown purpose %s" % (s["id"], s["purpose"]))
        for o in s.get("overlaps", []):
            if o not in placed:
                bad.append("%s: overlaps an unknown entry %s" % (s["id"], o))
        for r in s.get("refs", []):
            if r not in REFS:
                bad.append("%s: unknown reference app %s" % (s["id"], r))
    return bad


def render(m):
    by_code = {c: s for s in m["screens"] for c in s["code"]}
    out = ["# " + m["title"], "", m["intro"], "",
           "Generated from `purpose-map.json` by `tools/purpose_map.py`; edit the JSON, not this page.", "",
           "## Purposes", "", "| | Purpose | Its job | Entries |", "|---|---|---|---|"]
    for p in m["purposes"]:
        n = sum(1 for s in m["screens"] if s["purpose"] == p["id"])
        out.append("| %s | **%s** | %s | %d |" % (p["id"], p["name"], p["job"], n))
    for p in m["purposes"]:
        out += ["", "## %s %s" % (p["id"], p["name"]), "", "_%s_" % p["job"], "",
                "| Entry | In the code | Where | API | Reference apps | Overlaps |", "|---|---|---|---|---|---|"]
        for s in [s for s in m["screens"] if s["purpose"] == p["id"]]:
            code = ", ".join("`%s`" % c for c in s["code"]) if len(s["code"]) <= 3 else "%d rows: %s" % (
                len(s["code"]), ", ".join("`%s`" % c.split(":", 1)[1] for c in s["code"]))
            name = s["name"] if s["id"].startswith("section:") else "**%s**<br>%s" % (s["name"], s["does"])
            refs = ", ".join(REFS[r] for r in s.get("refs", [])) or "-"
            ov = ", ".join(by_code[o]["name"] for o in s.get("overlaps", []) if o in by_code) or ""
            out.append("| %s | %s | %s | %s | %s | %s |" % (name, code, s["where"], s.get("api", ""), refs, ov))
    out += ["", "## Parts", "", "Fragments that are only part of a screen: " + ", ".join("`%s`" % x for x in m["parts"]) + "."]
    return "\n".join(out) + "\n"


def main(argv):
    if "--census" in argv:
        print(json.dumps(census(), ensure_ascii=False, indent=1))
        return 0
    m = json.load(open(SRC, encoding="utf-8"))
    bad = checks(m, census())
    page = render(m)
    if "--check" in argv:
        stale = not os.path.exists(OUT) or read(OUT) != page
        if stale:
            bad.append("docs/purpose-map.md is stale: run python tools/purpose_map.py")
    else:
        with open(OUT, "w", encoding="utf-8", newline="\n") as fh:
            fh.write(page)
    for b in bad:
        print("FAIL", b)
    print("purpose map: %d entries, %d purposes, %d problem(s)" % (len(m["screens"]), len(m["purposes"]), len(bad)))
    return 1 if bad else 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
