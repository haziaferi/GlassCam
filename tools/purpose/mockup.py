#!/usr/bin/env python3
"""Mock-up of GlassCam's purpose map under one purpose set and the G4 hybrid granularity.

    python mockup.py E6     -> mockup_E6.html

G4: settings rows are grouped by their settings section, except rows that are in a candidate
redundancy pair (scores.json), which keep their own entry. Every other kind keeps its own entry.
"""
import glob
import html
import json
import os
import sys
from collections import Counter, defaultdict

HERE = os.path.normpath(os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..", "docs", "purpose"))
JOBS = {
    "Frame and shoot": "see the scene, pick the lens and the mode, and take the picture",
    "Control exposure and focus": "get brightness and sharpness right - by hand, or by steering the automatics - and see whether they are right",
    "Shape the image": "colour, processing, and the file that comes out",
    "Sequences and video": "many frames for one result, frames over time, and video with its sound",
    "Review and keep": "look at what was taken, keep it where I want it, and what the file says about it",
    "Make it work on this phone": "the camera API, phone-specific workarounds, and how the app itself behaves",
}
KIND = {"activity": "screen", "module": "capture mode", "column": "camera screen, shutter column",
        "strip": "camera screen, manual strip", "setting": "settings", "viewer": "gallery viewer",
        "section": "settings section"}
APP = {"fossify": "Fossify", "freedcam": "FreeDcam", "librecamera": "Libre", "macamera": "MA Camera",
       "manualcamerapro": "Manual Camera", "oneplus": "OnePlus", "opencamera": "Open Camera", "proshot": "ProShot"}


def load(n):
    return json.load(open(os.path.join(HERE, n), encoding="utf-8"))


def main(sid):
    sets = load("purpose_sets.json")["sets"]
    s = sets[sid]
    scores = load("scores.json")
    census = [e for e in load("census_tagged.json") if e.get("visible", True)]
    by_id = {e["id"]: e for e in census}
    pairs = scores["pairs"]
    inpair = defaultdict(list)
    for p in pairs:
        inpair[p["a"]].append((p["b"], p["why"]))
        inpair[p["b"]].append((p["a"], p["why"]))

    def purpose(f):
        for p, fs in s["purposes"].items():
            if f in fs:
                return p
        return "(outside)"

    refs = {}
    for path in sorted(glob.glob(os.path.join(HERE, "refs", "*.json"))):
        refs[os.path.splitext(os.path.basename(path))[0]] = json.load(open(path, encoding="utf-8"))["features"]
    precedent = defaultdict(set)
    gaps = defaultdict(list)
    for app, fs in refs.items():
        for f in fs:
            if f.get("glasscam") and f.get("match") in ("same", "partial"):
                precedent[f["glasscam"]].add(app)
            if f.get("match") == "none":
                gaps[purpose(f["facets"][0])].append((app, f))

    # G4 units
    units = {}
    sections = defaultdict(list)
    for e in census:
        if e["kind"] == "setting" and e["id"] not in inpair:
            sections[e["section"]].append(e)
        else:
            units[e["id"]] = {"kind": e["kind"], "entries": [e]}
    for sec, rows in sections.items():
        units["section:" + sec] = {"kind": "section", "entries": rows, "name": sec}
    placed = defaultdict(list)
    for uid, u in units.items():
        votes = Counter(purpose(e["facets"][0]) for e in u["entries"])
        placed[votes.most_common(1)[0][0]].append((uid, u))

    names = load("labels.json")

    def label(e):
        return names.get(e["id"]) or e.get("label") or e["id"]

    out = []
    w = out.append
    w("<!doctype html><html lang='en'><head><meta charset='utf-8'><meta name='viewport' content='width=device-width,initial-scale=1'>")
    w("<title>GlassCam purpose map - mock-up</title><style>")
    w(""":root{--bg:#fbfaf7;--fg:#1d1d1b;--mut:#6a6a64;--line:#e3e0d8;--acc:#7a4b12;--pair:#a33a2a;--chip:#efece4}
@media (prefers-color-scheme:dark){:root{--bg:#161614;--fg:#ecebe6;--mut:#a3a29b;--line:#34332f;--acc:#e0a75e;--pair:#ef8a76;--chip:#25241f}}
body{background:var(--bg);color:var(--fg);font:15px/1.5 system-ui,sans-serif;margin:0;padding:24px 16px;max-width:1100px;margin:auto}
h1{font-size:22px;margin:0 0 4px}h2{font-size:18px;margin:32px 0 2px;color:var(--acc)}p.job{margin:0 0 10px;color:var(--mut);font-style:italic}
table{border-collapse:collapse;width:100%;margin:6px 0 4px}th,td{text-align:left;vertical-align:top;padding:6px 8px;border-bottom:1px solid var(--line)}
th{font-size:12px;text-transform:uppercase;letter-spacing:.04em;color:var(--mut)}td.n{white-space:nowrap}
.k{font-size:12px;color:var(--mut)}.pair{color:var(--pair);font-size:13px}.chip{display:inline-block;background:var(--chip);border-radius:10px;padding:0 7px;margin:1px 2px;font-size:12px}
details{margin:4px 0 18px}summary{cursor:pointer;color:var(--mut)}ul{margin:4px 0 0 18px;padding:0}.note{color:var(--mut);font-size:13px}
.wrap{overflow-x:auto}""")
    w("</style></head><body>")
    n_units = len(units)
    w("<h1>GlassCam purpose map &mdash; mock-up (%s, G4)</h1>" % html.escape(s["name"]))
    w("<p class='note'>Draft for the owner's decision, 2026-10-20. %d map entries: every screen, capture mode, camera-screen control and gallery button on its own; "
      "settings rows grouped by section, except the %d rows that are in a candidate redundancy pair, which stand alone. "
      "<b>Refs</b> = how many of the 8 reference apps have the same job (same or partial). "
      "<span class='pair'>&#9888; pair</span> = a redundancy candidate (scoring comes next, with your use ratings).</p>"
      % (n_units, sum(1 for e in census if e['kind'] == 'setting' and e['id'] in inpair)))
    for p in list(s["purposes"]) + (["(outside)"] if placed.get("(outside)") else []):
        items = sorted(placed.get(p, []), key=lambda x: (x[1]["kind"] != "activity", x[1]["kind"], x[0]))
        w("<h2>%s <span class='k'>%d entries</span></h2>" % (html.escape(p), len(items)))
        if p in JOBS:
            w("<p class='job'>%s</p>" % html.escape(JOBS[p]))
        w("<div class='wrap'><table><tr><th>Entry</th><th>Where</th><th>What it does</th><th>Refs</th></tr>")
        for uid, u in items:
            if u["kind"] == "section":
                rows = u["entries"]
                row_names = ", ".join(html.escape(label(e)) for e in rows)
                w("<tr><td><b>%s settings</b><br><span class='k'>%d rows</span></td><td class='n'>settings section</td><td class='k'>%s</td><td></td></tr>"
                  % (html.escape(u["name"]), len(rows), row_names))
                continue
            e = u["entries"][0]
            api = e.get("api")
            note = "" if api in (None, "app", "both", "camera2") else " <span class='k'>(%s only)</span>" % api
            pr = "".join("<div class='pair'>&#9888; pair with %s &mdash; %s</div>" % (html.escape(label(by_id[o])), html.escape(why.split(":", 1)[0]))
                         for o, why in inpair.get(uid, []) if o in by_id)
            refs_n = len(precedent.get(uid, ()))
            w("<tr><td><b>%s</b>%s<br><span class='k'>%s</span></td><td class='n'>%s%s</td><td>%s%s</td><td>%s</td></tr>"
              % (html.escape(label(e)), note, html.escape(uid), html.escape(KIND.get(u["kind"], u["kind"])),
                 " &rsaquo; " + html.escape(e["section"]) if e.get("section") else "", html.escape(e.get("does", "")), pr,
                 refs_n or "&ndash;"))
        w("</table></div>")
        g = gaps.get(p, [])
        if g:
            byname = defaultdict(set)
            for app, f in g:
                byname[f["name"].strip()].add(APP.get(app, app))
            top = sorted(byname.items(), key=lambda x: (-len(x[1]), x[0].lower()))
            w("<details><summary>What the reference apps do here that GlassCam has nothing for (%d features)</summary><ul>" % len(g))
            for name, apps in top[:40]:
                w("<li>%s %s</li>" % (html.escape(name), "".join("<span class='chip'>%s</span>" % html.escape(a) for a in sorted(apps))))
            if len(top) > 40:
                w("<li class='note'>... %d more</li>" % (len(top) - 40))
            w("</ul></details>")
    w("</body></html>")
    dst = os.path.join(HERE, "mockup_%s.html" % sid)
    with open(dst, "w", encoding="utf-8", newline="\n") as fh:
        fh.write("\n".join(out))
    print(dst, "units", n_units, {p: len(v) for p, v in placed.items()})


if __name__ == "__main__":
    main(sys.argv[1] if len(sys.argv) > 1 else "E6")
