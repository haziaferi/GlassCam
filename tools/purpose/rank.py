#!/usr/bin/env python3
"""Rank GlassCam's redundancy-candidate pairs by Pareto layers (no weights; owner's choice 2026-10-20).

Axes, all oriented "higher = more clearly redundant":
  same_job     2 high-confidence same job (same_job.json), 1 medium or a tagger note, 0 none
  same_param   1 when both send at least one Camera2 request key in common
  precedent    8 - the reference apps that have the less-backed side's job (0..8): a side few apps offer is the weaker keeper
  reach_gap    difference in taps to reach the two sides: one copy far easier to reach makes the other a spare
  confusion    1 when the pair is listed as confusable, or the two labels share their main word
Frequency of use is NOT here: the owner rates it for the candidates this produces.
Reach model (stated, not measured per row): camera-screen column/strip control = 1 tap; screen = 1;
gallery button = 2 (gallery cell, then button); settings row = gear + row + one scroll per 5 rows above it
(5 rows per screen, settings_compare.json 2026-09-28).
"""
import glob
import json
import os
import re
from collections import defaultdict

HERE = os.path.normpath(os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..", "docs", "purpose"))


def load(n):
    return json.load(open(os.path.join(HERE, n), encoding="utf-8"))


def main():
    census = {e["id"]: e for e in load("census_tagged.json")}
    names = load("labels.json")
    scores = load("scores.json")
    sj = load("same_job.json")
    conf = {}
    for f in sj:
        for a in f["ids"]:
            for b in f["ids"]:
                if a < b:
                    key = (a, b)
                    if f["type"] == "same_job":
                        conf[key] = max(conf.get(key, 0), {"high": 2, "medium": 1, "low": 0}[f["confidence"]])
                    if f["type"] == "confusable" and f["confidence"] != "low":
                        conf.setdefault(("confusable",) + key, f["why"])
    prec = defaultdict(set)
    for path in glob.glob(os.path.join(HERE, "refs", "*.json")):
        app = os.path.splitext(os.path.basename(path))[0]
        for f in json.load(open(path, encoding="utf-8"))["features"]:
            if f.get("glasscam") and f.get("match") in ("same", "partial"):
                prec[f["glasscam"]].add(app)

    # settings row positions, in drawer order
    pos, i = {}, 0
    for e in load("census_tagged.json"):
        if e["kind"] == "setting":
            pos[e["id"]] = i
            i += 1

    def reach(e):
        k = e["kind"]
        if k in ("column", "strip", "activity", "module"):
            return 1
        if k == "viewer":
            return 2
        if k == "setting":
            return 2 + pos[e["id"]] // 5
        return 1

    def label(e):
        return (names.get(e["id"]) or e["label"] or e["does"].split(".")[0]).rstrip(":")

    def main_word(s):
        w = [x for x in re.findall(r"[a-z]+", s.lower()) if x not in {"mode", "the", "of", "a", "on", "camera", "setting"}]
        return w[0] if w else ""

    rows = []
    for p in scores["pairs"]:
        a, b = sorted((p["a"], p["b"]))
        A, B = census[a], census[b]
        ka = set(re.findall(r"CaptureRequest\w*\.\w+", A.get("writes") or ""))
        kb = set(re.findall(r"CaptureRequest\w*\.\w+", B.get("writes") or ""))
        note = 1 if "noted" in p["why"] else 0
        confusable = ("confusable", a, b) in conf or main_word(label(A)) == main_word(label(B))
        r = {
            "a": a, "b": b, "a_name": label(A), "b_name": label(B),
            "same_job": conf.get((a, b), note),
            "same_param": 1 if ka & kb and len(ka) <= 3 and len(kb) <= 3 else 0,
            "precedent": 8 - min(len(prec[a]), len(prec[b])),
            "reach_gap": abs(reach(A) - reach(B)),
            "confusion": 1 if confusable else 0,
            "why": p["why"], "refs": {a: sorted(prec[a]), b: sorted(prec[b])},
            "reach": {a: reach(A), b: reach(B)},
        }
        rows.append(r)
    axes = ("same_job", "same_param", "precedent", "reach_gap", "confusion")
    use_path = os.path.join(HERE, "use.json")
    if os.path.exists(use_path):
        # the owner's ratings exist only for pairs whose two sides the 6T shows: rank those on their own,
        # with "rarely used" (2 - the less-used side's rating) as a sixth axis
        use = {k: v for k, v in load("use.json").items() if k != "about"}
        live = [r for r in rows if r["a"] in use and r["b"] in use]
        for r in live:
            r["rarely_used"] = 2 - min(use[r["a"]], use[r["b"]])
            r["use"] = {r["a"]: use[r["a"]], r["b"]: use[r["b"]]}
        axes6 = axes + ("rarely_used",)
        left, layer = list(live), 1
        while left:
            front = [r for r in left if not any(all(o[x] >= r[x] for x in axes6) and any(o[x] > r[x] for x in axes6) for o in left)]
            for r in front:
                r["live_layer"] = layer
            left = [r for r in left if r not in front]
            layer += 1
    left, layer = list(rows), 1
    while left:
        front = [r for r in left if not any(all(o[x] >= r[x] for x in axes) and any(o[x] > r[x] for x in axes) for o in left)]
        for r in front:
            r["layer"] = layer
        left = [r for r in left if r not in front]
        layer += 1
    rows.sort(key=lambda r: (r["layer"], -r["same_job"], -r["precedent"]))
    json.dump(rows, open(os.path.join(HERE, "candidates.json"), "w", encoding="utf-8", newline="\n"), ensure_ascii=False, indent=1)
    for r in rows:
        print("L%d  job=%d par=%d prec=%d reach=%d conf=%d  %s  <->  %s" % (r["layer"], r["same_job"], r["same_param"], r["precedent"],
              r["reach_gap"], r["confusion"], r["a_name"], r["b_name"]))
    print("\non the 6T (with the owner's use ratings):")
    for r in sorted([r for r in rows if "live_layer" in r], key=lambda r: r["live_layer"]):
        print("live L%d  rarely=%d job=%d par=%d prec=%d reach=%d conf=%d  %s  <->  %s" % (r["live_layer"], r["rarely_used"], r["same_job"],
              r["same_param"], r["precedent"], r["reach_gap"], r["confusion"], r["a_name"], r["b_name"]))


if __name__ == "__main__":
    main()
