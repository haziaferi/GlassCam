"""The display record of a bench run, checked against the pairing in displays.json.

A run folder carries run.json:
    {"camera": "OnePlus 6T", "target": "pair" | "shared", "display": "Nord screen",
     "settings": {...the display's settings at the time of the run...}}

"pair" runs use the screen paired with that camera (within-phone verdicts); "shared" runs use the shared
monitor, the only target on which two different phones may be compared. Phone models only, never serials:
scores.json files can be committed as baselines.
"""
import json
import os

HERE = os.path.dirname(os.path.abspath(__file__))


def load_displays(path=os.path.join(HERE, "displays.json")):
    with open(path, encoding="utf-8") as fh:
        return json.load(fh)


def check_run(info, displays=None):
    """(problems, notes) for a run.json against the pairing. No problems = the record matches."""
    d = displays or load_displays()
    problems, notes = [], []
    if not info:
        return ["no run.json: the display is not recorded"], notes
    cam, target, disp = info.get("camera"), info.get("target"), info.get("display")
    if cam not in d["pairs"]:
        problems.append(f"camera {cam!r} is not in displays.json")
    if target == "pair":
        want = d["pairs"].get(cam)
        if disp != want:
            problems.append(f"{cam} is paired with {want!r}, the run used {disp!r}")
    elif target == "shared":
        if disp != d["shared"]:
            problems.append(f"shared runs use {d['shared']!r}, the run used {disp!r}")
    else:
        problems.append(f"target must be 'pair' or 'shared', not {target!r}")
    entry = d["displays"].get(disp)
    if entry is None:
        problems.append(f"display {disp!r} is not in displays.json")
        return problems, notes
    expected, actual = entry["settings"], info.get("settings") or {}
    if expected is None:
        problems.append(f"{disp!r} has no recorded settings in displays.json yet: record them first")
        return problems, notes
    for key, want in expected.items():
        if want is None:
            notes.append(f"{disp}: {key} not recorded yet (the run says {actual.get(key)!r})")
        elif actual.get(key) != want:
            problems.append(f"{disp}: {key} is {actual.get(key)!r}, displays.json says {want!r}")
    return problems, notes


def comparable(run_a, run_b):
    """(ok, reason) for comparing two scored runs: same display and settings, and two different phones
    only on the shared target."""
    a, b = run_a.get("run") or {}, run_b.get("run") or {}
    for name, r in (("baseline", a), ("candidate", b)):
        if r.get("problems") is None:
            return False, f"the {name} has no display record (score it with a run.json)"
        if r["problems"]:
            return False, f"the {name}'s display record does not match displays.json: {r['problems'][0]}"
    ia, ib = a["info"], b["info"]
    if ia["display"] != ib["display"]:
        return False, f"different displays: {ia['display']!r} and {ib['display']!r}"
    if ia.get("settings") != ib.get("settings"):
        return False, "same display, different settings"
    if ia["camera"] != ib["camera"] and not (ia["target"] == ib["target"] == "shared"):
        return False, f"{ia['camera']} and {ib['camera']} can only be compared on the shared target"
    return True, ""
