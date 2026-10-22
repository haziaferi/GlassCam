"""Compare two scored bench runs axis by axis: better / same / worse, never a weighted sum.

    python tools/bench/compare.py BASELINE_RUN CANDIDATE_RUN

A difference counts only when it exceeds the larger of the two runs' own spread between repeated shots
(and a small floor per axis, for axes measured once). The candidate is accepted when at least one axis
is better and none is worse, per output (JPEG and DNG separately). Runs are compared only when their
display records match (runinfo.comparable). Exit 2 = refused, 1 = rejected, 0 = accepted or no change.
"""
import json
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import runinfo  # noqa: E402

# axis: (scores.json key, higher is better, floor)
AXES = {
    "noise": ("snr18_db", True, 0.2),
    "colour": ("colour_mean_de", False, 0.3),
    "sharpness": ("mtf50", True, 0.005),
    "dynamic range": ("dynamic_range_stops", True, 0.1),
}


def value(entry):
    if isinstance(entry, dict):
        return entry.get("mean"), entry.get("spread")
    return entry, None


def verdicts(base, cand):
    out = {}
    for axis, (key, higher, floor) in AXES.items():
        (b, bs), (c, cs) = value(base.get(key)), value(cand.get(key))
        if b is None or c is None:
            out[axis] = ("missing", b, c, None)
            continue
        thr = max(floor, bs or 0, cs or 0)
        diff = (c - b) if higher else (b - c)
        out[axis] = ("better" if diff > thr else "worse" if diff < -thr else "same", b, c, thr)
    return out


def main(base_dir, cand_dir):
    load = lambda d: json.load(open(os.path.join(d, "scores.json"), encoding="utf-8"))
    base, cand = load(base_dir), load(cand_dir)
    ok, reason = runinfo.comparable(base, cand)
    if not ok:
        print(f"refused: {reason}")
        return 2
    accepted_all = True
    for key in ("jpeg", "dng"):
        v = verdicts(base[key], cand[key])
        print(f"\n{key.upper()}")
        for axis, (word, b, c, thr) in v.items():
            extra = "" if thr is None else f"  ({b:.4g} -> {c:.4g}, threshold {thr:.3g})"
            print(f"  {axis:14} {word}{extra}")
        over_b, over_c = value(base[key].get("overshoot_pct"))[0], value(cand[key].get("overshoot_pct"))[0]
        if over_b is not None and over_c is not None and over_c > over_b + 2:
            print(f"  warning: edge overshoot rose {over_b:.1f} % -> {over_c:.1f} % (halos)")
        wb_b, wb_c = value(base[key].get("colour_mean_de_wb"))[0], value(cand[key].get("colour_mean_de_wb"))[0]
        if wb_b is not None and wb_c is not None:      # information, not a verdict axis: rendering without the cast
            print(f"  colour after white balance: {wb_b:.2f} -> {wb_c:.2f} "
                  f"(cast a*b* {base[key].get('neutral_cast_ab')} -> {cand[key].get('neutral_cast_ab')})")
        words = [w for w, *_ in v.values()]
        accepted = "better" in words and "worse" not in words
        accepted_all &= accepted or "better" not in words and "worse" not in words
        print(f"  verdict: {'accept' if accepted else 'reject' if 'worse' in words else 'no measurable change'}")
    return 0 if accepted_all else 1


if __name__ == "__main__":
    if len(sys.argv) != 3:
        sys.exit(__doc__)
    sys.exit(main(*sys.argv[1:]))
