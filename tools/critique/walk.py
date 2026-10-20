#!/usr/bin/env python
"""walk.py -- adb driver for the GlassCam design-critique walk.

One command per step so a critique round is reproducible:

    python tools/critique/walk.py build            # arm64 debug APK, log + exit code in logs/build.log
                                                   # runs tools/spec_check.py and purpose_map.py --check first (--no-spec to skip)
    python tools/critique/walk.py install          # install -r -g, per-app locale en-US, long screen timeout
    python tools/critique/walk.py launch [main|gallery]
    python tools/critique/walk.py shot NAME        # screencap + uiautomator dump -> screenshots/NN_NAME.{png,xml}
    python tools/critique/walk.py tap X Y | swipe X1 Y1 X2 Y2 [ms] | key KEYCODE | text STRING
    python tools/critique/walk.py tapt TEXT        # tap the node showing TEXT, on a settled tree (two equal dumps, 3 tries)
    python tools/critique/walk.py nodes            # print clickable nodes of the current screen (text, bounds, dp size)
    python tools/critique/walk.py restore          # clear locale override, restore timeout, remove device temp files,
                                                   # HOME if our app is in front
    python tools/critique/walk.py free [SECONDS]   # block until the launcher has been in front that long (default 120)
    python tools/critique/walk.py log PATTERN      # the app's own logcat lines since the last `log -c`

Run from Git Bash with MSYS_NO_PATHCONV=1 (or from PowerShell): MSYS rewrites `/sdcard/...`
and `http://host/...` arguments into Windows paths before adb sees them.

stdlib only. Every adb call goes through `adb()`; every UI action but `key` goes through
`guard()` first, which refuses to act if another package owns the foreground (a second session
may be driving this phone). `key` is the exception on purpose: BACK/HOME are how a walk leaves
the system folder picker and other packages it opened.
"""
import io
import json
import os
import re
import subprocess
import sys
import time
try:  # the XML is uiautomator output from our own phone, but defused costs nothing
    import defusedxml.ElementTree as ET
except ImportError:  # pragma: no cover
    import xml.etree.ElementTree as ET  # nosemgrep: python.lang.security.use-defused-xml-parse.use-defused-xml-parse

# Windows consoles default to cp1252; the app's glyphs are not in it
sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding="utf-8", errors="replace")

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HERE, "..", ".."))
SHOTS = os.path.join(HERE, "screenshots")
LOGS = os.path.join(HERE, "logs")
STATE = os.path.join(LOGS, "walk_state.json")
PKG = "com.hazia.glasscam"
MAIN = PKG + "/glasscam.cam.ActivityGlassCamMain"
GALLERY = PKG + "/glasscam.viewer.ActivityGlassCamViewer"
OWN = {PKG, "com.android.documentsui", "com.google.android.documentsui",
       "com.android.permissioncontroller", "com.google.android.permissioncontroller",
       "com.android.systemui", "com.oplus.camera"}  # packages the walk itself may put in front
DENSITY = None


def adb(*args, check=True, capture=True):
    cmd = ["adb", *args]
    r = subprocess.run(cmd, capture_output=capture, text=True, encoding="utf-8", errors="replace")
    if check and r.returncode != 0:
        sys.exit(f"adb {' '.join(args)} failed ({r.returncode}): {r.stderr.strip()}")
    # uiautomator reports "could not get idle state" on stderr with exit 0, so keep both streams
    return (r.stdout + r.stderr) if capture else ""


def sh(*args, **kw):
    return adb("shell", *args, **kw)


def density():
    global DENSITY
    if DENSITY is None:
        m = re.search(r"Physical density: (\d+)", sh("wm", "density"))
        DENSITY = int(m.group(1)) if m else 480
    return DENSITY


def px_to_dp(px):
    return round(px * 160 / density(), 1)


def foreground():
    out = sh("dumpsys", "window")
    m = re.search(r"mCurrentFocus=Window\{\S+ u0 ([\w.]+)/", out)
    return m.group(1) if m else None


def guard(allow_any=False):
    fg = foreground()
    if allow_any or fg is None or fg in OWN or fg in state().get("extra_allowed", []):
        return fg
    sys.exit(f"STOP: foreground is {fg}, not ours. Another session may be driving the phone. "
             f"Add it with `walk.py allow {fg}` if that is intended.")


def state():
    if os.path.exists(STATE):
        with open(STATE, encoding="utf-8") as f:
            return json.load(f)
    return {}


def save_state(s):
    os.makedirs(LOGS, exist_ok=True)
    with open(STATE, "w", encoding="utf-8", newline="\n") as f:
        json.dump(s, f, indent=2)


def settle(ms=600):
    time.sleep(ms / 1000)


# ---------------------------------------------------------------- commands

def cmd_build(*flags):   # accepts --no-spec (read from sys.argv below); it used to crash the dispatcher
    # the spec gate runs first: a build whose SPEC.md revision or checked claims are stale is not
    # worth installing (tools/spec_check.py; `walk.py build --no-spec` skips it for a scratch build)
    if "--no-spec" not in sys.argv:
        gate = subprocess.run([sys.executable, os.path.join(ROOT, "tools", "spec_check.py")], cwd=ROOT)
        if gate.returncode:
            sys.exit("spec_check failed; update SPEC.md (revision + tagged claims) or pass --no-spec")
        # the purpose map places every screen, control and settings row; a code change that adds or
        # removes one must update docs/purpose-map.json in the same round (purpose map 2026-10-20)
        pmap = subprocess.run([sys.executable, os.path.join(ROOT, "tools", "purpose_map.py"), "--check"], cwd=ROOT)
        if pmap.returncode:
            sys.exit("purpose_map --check failed; update docs/purpose-map.json, run tools/purpose_map.py, or pass --no-spec")
    os.makedirs(LOGS, exist_ok=True)
    log = os.path.join(LOGS, "build.log")
    with open(log, "w", encoding="utf-8", newline="\n") as f:
        r = subprocess.run([os.path.join(ROOT, "gradlew.bat"), "assembleDebug",
                            "-Pandroid.injected.build.abi=arm64-v8a", "--console=plain"],
                           cwd=ROOT, stdout=f, stderr=subprocess.STDOUT)
        f.write(f"\nEXIT={r.returncode}\n")
    print(f"build exit={r.returncode}; log {log}")
    sys.exit(r.returncode)


def apk_path():
    # -Pandroid.injected.build.abi routes the APK to intermediates/ (IDE deploy path), not outputs/
    apks = []
    for d in (os.path.join(ROOT, "app", "build", "outputs", "apk", "debug"),
              os.path.join(ROOT, "app", "build", "intermediates", "apk", "debug")):
        if os.path.isdir(d):
            apks += [os.path.join(d, x) for x in os.listdir(d) if x.endswith(".apk")]
    if not apks:
        sys.exit("no debug APK found; run `walk.py build` first")
    return max(apks, key=os.path.getmtime)


def cmd_install():
    s = state()
    # the saved timeout belongs to one phone: re-read it when the attached phone is another one
    # (2026-09-28: a 9 Pro value would otherwise be "restored" onto the 6T)
    serial = adb("get-serialno").strip()
    if s.get("device") != serial:
        s["screen_off_timeout"] = sh("settings", "get", "system", "screen_off_timeout").strip()
        s["device"] = serial
    s.setdefault("screen_off_timeout", sh("settings", "get", "system", "screen_off_timeout").strip())
    apk = apk_path()
    print(adb("install", "-r", "-g", "-t", apk).strip())  # -t: injected-ABI builds are marked testOnly
    # per-app locales exist from API 33; on older phones (the 6T, API 30) the app follows the system language
    if int(sh("getprop", "ro.build.version.sdk").strip() or 0) >= 33:
        sh("cmd", "locale", "set-app-locales", PKG, "--locales", "en-US")
    else:
        print("per-app locale unavailable below API 33; the app runs in the system language")
    sh("settings", "put", "system", "screen_off_timeout", "600000")
    sh("mkdir", "-p", "/sdcard/critique")
    s["installed_apk"] = apk
    save_state(s)
    print("locale:", sh("cmd", "locale", "get-app-locales", PKG, check=False).strip() or "system")
    print("timeout was", s["screen_off_timeout"], "-> 600000")


def cmd_launch(which="main"):
    guard(allow_any=True)
    comp = GALLERY if which == "gallery" else MAIN
    sh("am", "start", "-n", comp)
    settle(1500)
    print("foreground:", foreground())


def next_index():
    os.makedirs(SHOTS, exist_ok=True)
    nums = [int(m.group(1)) for x in os.listdir(SHOTS) if (m := re.match(r"(\d+)_", x))]
    return (max(nums) + 1) if nums else 1


def cmd_shot(name):
    guard()
    n = next_index()
    base = f"{n:02d}_{name}"
    sh("screencap", "-p", f"/sdcard/critique/{base}.png")
    adb("pull", f"/sdcard/critique/{base}.png", os.path.join(SHOTS, base + ".png"))
    # a continuously-invalidating view (the level overlay) keeps the a11y tree from idling;
    # the PNG is still evidence, so record the miss instead of failing the walk
    out = sh("uiautomator", "dump", f"/sdcard/critique/{base}.xml", check=False)
    if "could not get idle state" in out or "ERROR" in out:
        with open(os.path.join(SHOTS, base + ".noxml.txt"), "w", encoding="utf-8") as f:
            f.write(out)
        print(base, "(no uiautomator dump: tree never idled)")
    else:
        adb("pull", f"/sdcard/critique/{base}.xml", os.path.join(SHOTS, base + ".xml"))
        print(base)
    sh("rm", "-f", f"/sdcard/critique/{base}.png", f"/sdcard/critique/{base}.xml")


def parse_bounds(b):
    m = re.match(r"\[(\d+),(\d+)\]\[(\d+),(\d+)\]", b)
    return tuple(int(x) for x in m.groups()) if m else None


def cmd_nodes(path=None):
    """Clickable nodes with their dp size -- the tap-target measurement."""
    if path is None:
        guard()
        sh("uiautomator", "dump", "/sdcard/critique/_now.xml")
        xml = sh("cat", "/sdcard/critique/_now.xml")
        root = ET.fromstring(xml)
    else:
        root = ET.parse(path).getroot()
    rows = []
    for node in root.iter("node"):
        if node.get("clickable") != "true" and node.get("checkable") != "true":
            continue
        b = parse_bounds(node.get("bounds", ""))
        if not b:
            continue
        w, h = b[2] - b[0], b[3] - b[1]
        rows.append({
            "id": (node.get("resource-id") or "").split("/")[-1],
            "text": node.get("text") or node.get("content-desc") or "",
            "class": node.get("class", "").split(".")[-1],
            "bounds": b, "w_dp": px_to_dp(w), "h_dp": px_to_dp(h),
            "under48": min(w, h) < 48 * density() / 160,
        })
    for r in rows:
        flag = "  <48dp" if r["under48"] else ""
        print(f"{r['id'] or '-':38} {r['class']:14} {r['w_dp']:6}x{r['h_dp']:<6} {r['text'][:30]!r}{flag}")
    return rows


def cmd_tap(x, y):
    guard(); sh("input", "tap", x, y); settle()


def cmd_swipe(x1, y1, x2, y2, ms="300"):
    guard(); sh("input", "swipe", x1, y1, x2, y2, ms); settle()


def cmd_key(code):
    guard(allow_any=True); sh("input", "keyevent", code); settle()


def cmd_text(s):
    guard(); sh("input", "text", s.replace(" ", "%s")); settle()


def settled_tree(tries=3):
    """Two identical consecutive dumps: a tree captured mid-layout gave stale row positions once
    (an "External" tap landed on Back and looked like a dead control)."""
    prev, settled = None, False
    for _ in range(tries):
        sh("uiautomator", "dump", "/sdcard/critique/_now.xml", check=False)
        xml = sh("cat", "/sdcard/critique/_now.xml", check=False)
        if xml == prev:
            settled = True
            break
        prev = xml
        time.sleep(0.3)
    if not settled:   # the tap still goes ahead on the last dump, but not silently
        print(f"warning: tree still changing after {tries} dumps; positions may be stale", file=sys.stderr)
    return ET.fromstring(prev)


def cmd_tapt(text, nth="0"):
    """Tap the nth node whose text equals TEXT (uiautomator lookup, so the level must be off)."""
    guard()
    root = settled_tree()
    hits = [n for n in root.iter("node") if (n.get("text") or "").strip() == text]
    if len(hits) <= int(nth):
        sys.exit(f"no node with text {text!r} (found {len(hits)})")
    b = parse_bounds(hits[int(nth)].get("bounds"))
    sh("input", "tap", str((b[0] + b[2]) // 2), str((b[1] + b[3]) // 2)); settle()
    print("tapped", text, b)


def cmd_free(seconds="120"):
    """Wait until the launcher (or our app) has owned the foreground for SECONDS - another
    session may be driving the phone; its walk ends with HOME."""
    need = int(seconds)
    quiet = 0
    while quiet < need:
        fg = foreground()
        # an undetectable foreground is not evidence of a free phone (it counted as free before)
        quiet = quiet + 10 if (fg is not None and (fg in OWN or "launcher" in fg)) else 0
        time.sleep(10)
    print("free; foreground", foreground())


def cmd_log(*args):
    """`log -c` clears; `log PATTERN` prints the app's own lines matching PATTERN (by pid, so the
    buffer another session floods does not hide them)."""
    if args and args[0] == "-c":
        adb("logcat", "-c"); print("cleared"); return
    pid = sh("pidof", PKG, check=False).strip()
    if not pid:
        sys.exit("app not running")
    out = adb("logcat", "-d", "--pid=" + pid)
    pat = re.compile(args[0]) if args else None
    for line in out.splitlines():
        if pat is None or pat.search(line):
            print(line)


def cmd_allow(pkg):
    s = state(); s.setdefault("extra_allowed", []).append(pkg); save_state(s); print("allowed", pkg)


def cmd_restore():
    s = state()
    sh("cmd", "locale", "set-app-locales", PKG, "--locales", "", check=False)   # API 33+ only
    if s.get("screen_off_timeout"):
        sh("settings", "put", "system", "screen_off_timeout", s["screen_off_timeout"])
    sh("rm", "-rf", "/sdcard/critique")
    # SPEC §12 says restore leaves the launcher in front; press HOME only over our own app, never
    # over a package another session may be driving
    if foreground() in OWN:
        sh("input", "keyevent", "KEYCODE_HOME")
        time.sleep(1)
    print("locale:", sh("cmd", "locale", "get-app-locales", PKG, check=False).strip() or "system")
    print("timeout:", sh("settings", "get", "system", "screen_off_timeout").strip())
    print("foreground:", foreground())


COMMANDS = {
    "build": cmd_build, "install": cmd_install, "launch": cmd_launch, "shot": cmd_shot,
    "nodes": cmd_nodes, "tapt": cmd_tapt, "tap": cmd_tap, "swipe": cmd_swipe, "key": cmd_key, "text": cmd_text,
    "allow": cmd_allow, "restore": cmd_restore, "free": cmd_free, "log": cmd_log,
    "fg": lambda: print(foreground()),
}

if __name__ == "__main__":
    if len(sys.argv) < 2 or sys.argv[1] not in COMMANDS:
        sys.exit(__doc__)
    COMMANDS[sys.argv[1]](*sys.argv[2:])
