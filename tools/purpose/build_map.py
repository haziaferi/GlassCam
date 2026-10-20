#!/usr/bin/env python3
"""One-time: write GlassCam/docs/purpose-map.json from the E6 + G4 mock-up data.

After this, docs/purpose-map.json is the source of truth and is edited by hand; this script is
kept only as the record of how the first draft was made.
"""
import glob
import json
import os
from collections import Counter, defaultdict

HERE = os.path.normpath(os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..", "docs", "purpose"))
DST = os.path.join(HERE, "..", "purpose-map.json")
PURPOSES = [
    ("P1", "Frame and shoot", "see the scene, pick the lens and the mode, and take the picture"),
    ("P2", "Control exposure and focus", "get brightness and sharpness right - by hand, or by steering the automatics - and see whether they are right"),
    ("P3", "Shape the image", "colour, processing, and the file that comes out"),
    ("P4", "Sequences and video", "many frames for one result, frames over time, and video with its sound"),
    ("P5", "Review and keep", "look at what was taken, keep it where I want it, and what the file says about it"),
    ("P6", "Make it work on this phone", "the camera API, phone-specific workarounds, and how the app itself behaves"),
]
WHERE = {"activity": "screen", "module": "capture mode", "column": "camera screen, shutter column",
         "strip": "camera screen, manual strip", "setting": "settings", "viewer": "gallery viewer"}


def load(n):
    return json.load(open(os.path.join(HERE, n), encoding="utf-8"))


def main():
    s = load("purpose_sets.json")["sets"]["E6"]
    pid = {name: p for p, name, _ in PURPOSES}
    census = load("census_tagged.json")
    names = load("labels.json")
    scores = load("scores.json")
    inpair = defaultdict(set)
    for p in scores["pairs"]:
        inpair[p["a"]].add(p["b"])
        inpair[p["b"]].add(p["a"])
    precedent = defaultdict(set)
    for path in glob.glob(os.path.join(HERE, "refs", "*.json")):
        app = os.path.splitext(os.path.basename(path))[0]
        for f in json.load(open(path, encoding="utf-8"))["features"]:
            if f.get("glasscam") and f.get("match") in ("same", "partial"):
                precedent[f["glasscam"]].add(app)

    def purpose(f):
        return next(pid[p] for p, fs in s["purposes"].items() if f in fs)

    screens, sections = [], defaultdict(list)
    for e in census:
        if not e.get("visible", True):
            continue
        if e["kind"] == "setting" and e["id"] not in inpair:
            sections[e["section"]].append(e)
            continue
        screens.append({
            "id": e["id"], "name": names.get(e["id"]) or e["label"].rstrip(":") or e["id"],
            "purpose": purpose(e["facets"][0]), "code": [e["id"]],
            "where": WHERE[e["kind"]] + (" > " + e["section"] if e.get("section") else ""),
            "does": e["does"], "api": e.get("api") or "app",
            "refs": sorted(precedent.get(e["id"], ())),
            "overlaps": sorted(inpair.get(e["id"], ())),
        })
    for sec, rows in sorted(sections.items()):
        votes = Counter(purpose(e["facets"][0]) for e in rows)
        screens.append({
            "id": "section:" + sec, "name": sec + " settings (the other rows)",
            "purpose": votes.most_common(1)[0][0], "code": [e["id"] for e in rows],
            "where": "settings > " + sec,
            "does": "; ".join(e["label"].rstrip(":") for e in rows),
            "api": "mixed", "refs": [], "overlaps": [],
        })
    m = {
        "title": "Purpose map",
        "intro": ("GlassCam's screens, capture modes, camera-screen controls, gallery buttons and settings, by what they are for. "
                  "Drafted 2026-10-20 to find redundancy: the six purposes (set E6) were scored against eight reference camera apps "
                  "(coverage, exclusivity, balance, GlassCam fit, potential improvement) and chosen by the owner after a mock-up; "
                  "the granularity (G4) lists every control, and settings rows by section except the rows in a redundancy-candidate pair. "
                  "`refs` names the reference apps that do the same job; `overlaps` names the map entries it is a redundancy candidate with."),
        "purposes": [{"id": p, "name": n, "job": j} for p, n, j in PURPOSES],
        "screens": screens,
        "parts": sorted(e["id"] for e in census if not e.get("visible", True)),
    }
    with open(DST, "w", encoding="utf-8", newline="\n") as fh:
        json.dump(m, fh, ensure_ascii=False, indent=1)
        fh.write("\n")
    print(DST, len(screens), "entries", Counter(x["purpose"] for x in screens))


if __name__ == "__main__":
    main()
