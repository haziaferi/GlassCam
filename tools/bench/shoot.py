"""Drive a bench run over adb: fix GlassCam's capture settings, show targets, fire, pull, record.

    python tools/bench/shoot.py configure CAM_SERIAL [--iso 100] [--shutter 1/30] [--focus 16cm|inf|auto]
                                          [--wb DAYLIGHT] [--quality fast|hq]
    python tools/bench/shoot.py show DISPLAY_SERIAL PAGE        # e.g. colour, flat_118, dark
    python tools/bench/shoot.py shot CAM_SERIAL RUN_DIR NAME    # fire GlassCam, pull NAME.jpg + NAME.dng
    python tools/bench/shoot.py record RUN_DIR CAM_SERIAL DISPLAY_SERIAL|monitor --target pair|shared
    python tools/bench/shoot.py restore CAM_SERIAL               # the phone's own GlassCam settings back

configure stops GlassCam (HOME first, so it saves its own state), edits camera 0 of the Camera2 section
of glasscam_config.xml, pushes it back and relaunches: manual ISO and shutter (AE off), manual focus,
a white-balance preset, JPEG + 16-bit DNG, and the processing modes (fast = as shipped, hq = list item 1).
The settings come from the config file, never from tapping menus. Photos stay on the phone: the bench
only pulls copies.
record writes run.json from the phones' live settings, so a dimmed or re-tinted screen is caught by
runinfo.check_run instead of trusted.
"""
import json
import os
import re
import subprocess
import sys
import time

APP = "com.hazia.glasscam"
CAMERA = f"{APP}/glasscam.cam.ActivityGlassCamMain"   # GlassCam has two launcher entries: camera and gallery
CONFIG = f"/sdcard/Android/data/{APP}/files/glasscam_config.xml"
PHOTOS = "/sdcard/DCIM/GlassCam"
PORT = 8765
BROWSER = "com.brave.browser/com.google.android.apps.chrome.Main"
MODELS = {"ONEPLUS A6013": "OnePlus 6T", "CPH2621": "OnePlus Nord CE4 Lite", "LE2123": "OnePlus 9 Pro"}
ENV = dict(os.environ, MSYS_NO_PATHCONV="1")
BACKUPS = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..", "bench_runs", "settings_backup")


def adb(serial, *args, binary=False):
    out = subprocess.run(["adb", "-s", serial, *args], capture_output=True, env=ENV)
    if out.returncode:
        raise RuntimeError(f"adb {' '.join(args)}: {out.stderr.decode(errors='replace').strip()}")
    return out.stdout if binary else out.stdout.decode("utf-8", errors="replace")


def sh(serial, cmd):
    return adb(serial, "shell", cmd).strip()


def foreground(serial):
    m = re.search(r"mCurrentFocus=Window\{\S+ \S+ ([^}]+)\}", sh(serial, "dumpsys window"))
    return m.group(1) if m else ""


def model(serial):
    name = sh(serial, "getprop ro.product.model")
    return MODELS.get(name, name)

# ---------------------------------------------------------------- GlassCam's config


def camera_block(xml):
    """(start, end) of Camera2's camera-0 settings."""
    api = xml.index('<api name = "camera2">')
    start = xml.index('<id name = "0">', api)
    return start, xml.index("</id>", start)


def setting(block, name):
    m = re.search(r'<setting name = "%s"[^>]*>.*?</setting>' % re.escape(name), block, re.S)
    if not m:
        raise KeyError(f"no setting {name!r} for camera 0")
    return m


def choices(block, name):
    return re.findall(r"<val>(.*?)</val>", setting(block, name).group(0), re.S)


def set_value(block, name, value):
    m = setting(block, name)
    new = re.sub(r"<value>.*?</value>", f"<value>{value}</value>", m.group(0), count=1, flags=re.S)
    return block[:m.start()] + new + block[m.end():]


def label_choice(block, name, label):
    """SettingMode stores the label (the part before ',n')."""
    labels = [v.rsplit(",", 1)[0] for v in choices(block, name)]
    if label not in labels:
        raise ValueError(f"{name}: {label!r} not offered, choices {labels}")
    return set_value(block, name, label)


def index_choice(block, name, wanted):
    """TypedSettingMode stores the index into its list."""
    vals = choices(block, name)
    if wanted not in vals:
        raise ValueError(f"{name}: {wanted!r} not offered, choices {vals}")
    return set_value(block, name, str(vals.index(wanted)))


def focus_choice(block, focus):
    vals = choices(block, "mf")
    if focus == "auto":
        return set_value(block, "mf", "0")
    if focus == "inf":
        return set_value(block, "mf", str(next(i for i, v in enumerate(vals) if v.startswith("∞"))))
    target = float(focus.rstrip("cm"))
    dist = []
    for i, v in enumerate(vals):
        m = re.match(r"([\d.]+)cm#", v)
        if m:
            dist.append((abs(float(m.group(1)) - target), i))
    if not dist:
        raise ValueError("manual focus offers no cm distances")
    return set_value(block, "mf", str(min(dist)[1]))


def configure(serial, iso="100", shutter="1/30", focus="auto", wb="DAYLIGHT", quality="fast"):
    sh(serial, "input keyevent KEYCODE_HOME")      # GlassCam saves its state in onPause
    time.sleep(2)
    sh(serial, f"am force-stop {APP}")
    xml = adb(serial, "exec-out", f"cat {CONFIG}", binary=True).decode("utf-8")
    backup = os.path.join(BACKUPS, f"glasscam_config_{model(serial).replace(' ', '_')}.xml")
    if not os.path.exists(backup):                 # the owner's own settings, kept from the first configure
        os.makedirs(BACKUPS, exist_ok=True)
        with open(backup, "w", encoding="utf-8", newline="") as fh:
            fh.write(xml)
        print(f"saved the phone's own GlassCam settings to {backup} (shoot.py restore puts them back)")
    a, b = camera_block(xml)
    blk = xml[a:b]
    blk = label_choice(blk, "pictureformat", "Jpg+16bitDng")
    blk = label_choice(blk, "exposuremode", "Off")
    blk = index_choice(blk, "iso", iso)
    blk = index_choice(blk, "expotime", shutter)
    blk = label_choice(blk, "focusmode", "Auto" if focus == "auto" else "Off")
    blk = focus_choice(blk, focus)
    blk = label_choice(blk, "whitebalancemode", wb)
    mode = {"fast": "Fast", "hq": "High Quality"}[quality]
    for name in ("denoisemode", "edgemode", "hotpixel"):
        blk = label_choice(blk, name, mode)
    new = xml[:a] + blk + xml[b:]
    tmp = os.path.join(os.environ.get("TEMP", "."), "bench_glasscam_config.xml")
    with open(tmp, "w", encoding="utf-8", newline="") as fh:
        fh.write(new)
    adb(serial, "push", tmp, CONFIG)
    sh(serial, f"am start -n {CAMERA}")
    time.sleep(4)
    if foreground(serial) != CAMERA:
        raise RuntimeError(f"GlassCam's camera did not open on {serial}: {foreground(serial)}")
    print(f"configured {model(serial)}: ISO {iso}, {shutter} s, focus {focus}, WB {wb}, {quality}; "
          f"foreground {foreground(serial)}")


def restore(serial):
    """Put back the GlassCam settings saved by the first configure, then delete nothing."""
    backup = os.path.join(BACKUPS, f"glasscam_config_{model(serial).replace(' ', '_')}.xml")
    sh(serial, "input keyevent KEYCODE_HOME")
    time.sleep(2)
    sh(serial, f"am force-stop {APP}")
    adb(serial, "push", backup, CONFIG)
    print(f"restored {backup}")

# ---------------------------------------------------------------- display and capture


def show(serial, page):
    adb(serial, "reverse", f"tcp:{PORT}", f"tcp:{PORT}")
    sh(serial, f"am start -a android.intent.action.VIEW -d http://127.0.0.1:{PORT}/{page}.html -n {BROWSER}")
    time.sleep(2.5)
    w, h = (int(v) for v in re.search(r"(\d+)x(\d+)", sh(serial, "wm size")).groups())
    sh(serial, f"input tap {w // 2} {h // 2}")     # a page tap requests full screen
    time.sleep(2.5)
    print(f"{model(serial)} shows {page}")


def listing(serial):
    out = sh(serial, f"ls -l {PHOTOS}")
    return {parts[-1]: int(parts[4]) for parts in (l.split() for l in out.splitlines()) if len(parts) >= 8}


def shot(serial, run, name, timeout=40):
    if foreground(serial) != CAMERA:          # the gallery is GlassCam too, and ignores the camera key
        raise RuntimeError(f"GlassCam's camera is not in front on {serial}: {foreground(serial)}")
    before = listing(serial)
    sh(serial, "input keyevent KEYCODE_CAMERA")
    deadline, last = time.time() + timeout, None
    while time.time() < deadline:
        time.sleep(1)
        now = listing(serial)
        new = {k: v for k, v in now.items() if k not in before}
        exts = {os.path.splitext(k)[1] for k in new}
        if {".jpg", ".dng"} <= exts and new == last:   # both written and no longer growing
            break
        last = new
    else:
        raise RuntimeError(f"no JPEG + DNG pair within {timeout} s (new: {sorted(last or {})})")
    os.makedirs(run, exist_ok=True)
    for remote in new:
        ext = os.path.splitext(remote)[1]
        adb(serial, "pull", f"{PHOTOS}/{remote}", os.path.join(run, name + ext))
    print(f"{name}: {', '.join(sorted(new))}")

# ---------------------------------------------------------------- run record


def live_display(serial):
    """The display phone's settings as runinfo compares them, read from the phone."""
    cam = model(serial)
    sett = lambda ns, k: sh(serial, f"settings get {ns} {k}")
    sf = re.search(r"Current color mode: (ColorMode::\w+)", sh(serial, "dumpsys SurfaceFlinger"))
    render = sf.group(1) if sf else None
    if cam == "OnePlus 6T":
        mode = (sett("system", "screen_color_mode_settings_value"),
                sett("system", "screen_color_mode_advanced_settings_value"))
        return "6T screen", {
            "screen_calibration": "Advanced / sRGB" if mode == ("3", "1") else f"codes {mode}",
            "render_mode": render,
            "oem_screen_better_value": int(sett("system", "oem_screen_better_value")),
            "night_light": "on" if sett("secure", "night_display_activated") == "1" else "off",
            "brightness": "max, auto-brightness off" if (sett("system", "screen_brightness_mode"), sett(
                "system", "screen_brightness")) == ("0", "255") else "not max or automatic",
        }
    if cam == "OnePlus Nord CE4 Lite":
        eye = sett("system", "oplus_customize_eye_protect_enable") == "1"
        return "Nord screen", {
            "display_color_mode": int(sett("system", "display_color_mode")),
            "render_mode": render,
            "eye_comfort": "on" if eye else "off, and its 22:00 schedule not reached during the session",
            "brightness": "max, auto-brightness off" if (sett("system", "screen_brightness_mode"), sett(
                "system", "screen_brightness")) == ("0", "2047") else "not max or automatic",
            "low_flicker": "not offered by this build",
        }
    raise ValueError(f"no display reader for {cam}")


def record(run, cam_serial, display, target):
    if display == "monitor":
        import runinfo
        name, settings = "monitor", runinfo.load_displays()["displays"]["monitor"]["settings"]
    else:
        name, settings = live_display(display)
    info = {"camera": model(cam_serial), "target": target, "display": name, "settings": settings,
            "time": time.strftime("%Y-%m-%d %H:%M")}
    os.makedirs(run, exist_ok=True)
    with open(os.path.join(run, "run.json"), "w", encoding="utf-8", newline="\n") as fh:
        json.dump(info, fh, indent=1, ensure_ascii=False)
    sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
    import runinfo
    problems, notes = runinfo.check_run(info)
    print(json.dumps(info, indent=1, ensure_ascii=False))
    for p in problems:
        print("DISPLAY:", p)
    return 1 if problems else 0


def main(argv):
    if len(argv) < 2:
        sys.exit(__doc__)
    cmd, args = argv[0], argv[1:]
    opts, pos, i = {}, [], 0
    while i < len(args):                    # "--name value" pairs anywhere; everything else positional
        if args[i].startswith("--"):
            if i + 1 >= len(args):
                sys.exit(f"{args[i]} needs a value")
            opts[args[i][2:]] = args[i + 1]
            i += 2
        else:
            pos.append(args[i])
            i += 1
    allowed = {"configure": {"iso", "shutter", "focus", "wb", "quality"}, "record": {"target"}}.get(cmd, set())
    if set(opts) - allowed:
        sys.exit(f"{cmd} does not take {sorted(set(opts) - allowed)}")
    if cmd == "configure":
        configure(pos[0], **opts)
    elif cmd == "show":
        show(*pos)
    elif cmd == "shot":
        shot(*pos)
    elif cmd == "restore":
        restore(pos[0])
    elif cmd == "record":
        return record(pos[0], pos[1], pos[2], opts.get("target", "pair"))
    else:
        sys.exit(__doc__)
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
