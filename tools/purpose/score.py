#!/usr/bin/env python3
"""Score candidate purpose sets and map granularities from facet-tagged features.

    python score.py            -> scores.json + a printed summary

Purpose-set axes (owner's choice, 2026-10-20), each per app and as the mean over the reference apps:
  coverage      share of features whose main facet falls in a purpose (not "outside")
  exclusivity   share of features whose facets all fall in ONE purpose (a feature spanning two
                purposes, or a purpose and "outside", is not exclusive)
  balance       normalised entropy of main-facet counts over the purposes (1 = even spread)
  glasscam_fit  mean of coverage and exclusivity on GlassCam's visible entries
  improvement   reference features GlassCam lacks ("none") or only partly has, whose main facet
                lands in a named purpose - a gap with an owner; reported per purpose

Granularity axes (owner's choice): redundancy it can see, review effort, upkeep, readability.
Stdlib only; every input is a JSON file beside this script.
"""
import glob
import itertools
import json
import math
import os
import re
from collections import Counter, defaultdict

HERE = os.path.normpath(os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..", "docs", "purpose"))


def load(name):
    with open(os.path.join(HERE, name), encoding="utf-8") as fh:
        return json.load(fh)


def purpose_of(facet, s):
    for p, fs in s["purposes"].items():
        if facet in fs:
            return p
    if facet in s.get("outside", []):
        return None
    raise SystemExit("facet %s not placed by a purpose set" % facet)


def set_axes(features, s):
    """features: list of facet lists (first = main)."""
    n = len(features)
    if not n:
        return None
    main = [purpose_of(f[0], s) for f in features]
    cov = sum(m is not None for m in main) / n
    excl = sum(len({purpose_of(x, s) for x in f}) == 1 and purpose_of(f[0], s) is not None for f in features) / n
    counts = Counter(m for m in main if m is not None)
    k = len(s["purposes"])
    tot = sum(counts.values())
    ent = -sum(c / tot * math.log(c / tot) for c in counts.values() if c) if tot else 0
    bal = ent / math.log(k) if k > 1 else 1.0
    return {"coverage": round(cov, 3), "exclusivity": round(excl, 3), "balance": round(bal, 3), "n": n,
            "per_purpose": dict(counts)}


def main():
    facets = load("facets.json")["facets"]
    sets = load("purpose_sets.json")["sets"]
    gc = [e for e in load("census_tagged.json") if e.get("visible", True)]
    refs = {}
    for path in sorted(glob.glob(os.path.join(HERE, "refs", "*.json"))):
        d = json.load(open(path, encoding="utf-8"))
        refs[os.path.splitext(os.path.basename(path))[0]] = d["features"]
    for f in [x for fs in refs.values() for x in fs] + gc:
        for fa in f["facets"]:
            if fa not in facets:
                raise SystemExit("unknown facet %r in %s" % (fa, f.get("name") or f.get("id")))

    out = {"refs": {a: len(v) for a, v in refs.items()}, "sets": {}, "granularity": {}}
    for sid, s in sets.items():
        per_app = {a: set_axes([f["facets"] for f in fs], s) for a, fs in refs.items()}
        mean = {ax: round(sum(v[ax] for v in per_app.values()) / len(per_app), 3)
                for ax in ("coverage", "exclusivity", "balance")} if per_app else {}
        g = set_axes([e["facets"] for e in gc], s)
        gaps = defaultdict(list)
        for a, fs in refs.items():
            for f in fs:
                if f.get("match") in ("none", "partial"):
                    p = purpose_of(f["facets"][0], s)
                    gaps[p or "(outside)"].append({"app": a, "name": f["name"], "match": f["match"],
                                                   "note": f.get("match_note", "")})
        named = sum(len(v) for p, v in gaps.items() if p != "(outside)")
        total = sum(len(v) for v in gaps.values())
        out["sets"][sid] = {
            "name": s["name"], "purposes": list(s["purposes"]), "per_app": per_app, "ref_mean": mean,
            "glasscam": g, "glasscam_fit": round((g["coverage"] + g["exclusivity"]) / 2, 3),
            "improvement": {"gaps_total": total, "gaps_in_named_purpose": named,
                            "share_named": round(named / total, 3) if total else None,
                            "per_purpose": {p: len(v) for p, v in gaps.items()}},
            "gaps": gaps,
        }

    # ---- granularity -------------------------------------------------------------------------
    allc = load("census_tagged.json")
    by_id = {e["id"]: e for e in allc}

    def keyset(e):
        w = e.get("writes") or ""
        ks = set(re.findall(r"\bCaptureRequest\w*\.\w+", w))
        return ks if 0 < len(ks) <= 3 else set()   # a whole AE manager's keys says nothing about one control

    pairs = {}
    for a, b in itertools.combinations([e for e in allc if e.get("visible", True)], 2):
        shared = keyset(a) & keyset(b)
        if shared:
            pairs[tuple(sorted((a["id"], b["id"])))] = "same parameter: " + ", ".join(sorted(shared))
    sj = os.path.join(HERE, "same_job.json")
    for f in (json.load(open(sj, encoding="utf-8")) if os.path.exists(sj) else []):
        if f["type"] in ("same_job", "confusable") and f.get("confidence") != "low":
            for a, b in itertools.combinations(sorted(f["ids"]), 2):
                if a in by_id and b in by_id:
                    pairs.setdefault((a, b), f["type"] + ": " + f["why"][:120])
    for e in allc:   # the tagger's "same job" notes that name another entry
        for other in re.findall(r"\b((?:setting|column|strip|module|viewer|activity):\w+)", e.get("notes", "")):
            if other in by_id and other != e["id"]:
                pairs.setdefault(tuple(sorted((e["id"], other))), "noted: " + e["notes"][:120])

    # one module per camera API/vendor is ever registered: a person never meets both, so not redundancy
    api_variants = []
    for p in list(pairs):
        a, b = by_id[p[0]], by_id[p[1]]
        if a["kind"] == b["kind"] == "module":   # each handler registers one per API/vendor
            api_variants.append({"a": p[0], "b": p[1], "why": pairs.pop(p)})

    vis = [e for e in allc if e.get("visible", True)]
    sections =sorted({e.get("section") for e in vis if e["kind"] == "setting"})

    def unit(e, g):
        if g == "G1_every_control":
            return e["id"]
        if g == "G2_settings_by_section":
            return "section:" + e["section"] if e["kind"] == "setting" else e["id"]
        return e["id"] if e["kind"] in ("activity", "module", "viewer") else None

    for g in ("G1_every_control", "G2_settings_by_section", "G3_screens_and_modules"):
        units = {unit(e, g) for e in vis} - {None}
        seen = [p for p in pairs if unit(by_id[p[0]], g) and unit(by_id[p[1]], g)
                and unit(by_id[p[0]], g) != unit(by_id[p[1]], g)]
        read = {}
        for sid, s in sets.items():
            # one count per map unit; a collapsed section goes under the purpose most of its rows serve
            votes = defaultdict(Counter)
            for e in vis:
                u = unit(e, g)
                if u:
                    votes[u][purpose_of(e["facets"][0], s) or "(outside)"] += 1
            cnt = Counter(v.most_common(1)[0][0] for v in votes.values())
            sizes = [c for p, c in cnt.items() if p != "(outside)"]
            read[sid] = round(sum(3 <= c <= 15 for c in sizes) / len(s["purposes"]), 3)
        out["granularity"][g] = {"entries": len(units), "pairs_seen": len(seen), "pairs_total": len(pairs),
                                 "redundancy_visible": round(len(seen) / len(pairs), 3) if pairs else None,
                                 "review_effort": len(units), "upkeep_tokens": len(units),
                                 "readability_by_set": read}
    out["pairs"] = [{"a": a, "b": b, "why": w} for (a, b), w in sorted(pairs.items())]
    out["api_variants"] = api_variants
    out["sections"] = sections
    with open(os.path.join(HERE, "scores.json"), "w", encoding="utf-8", newline="\n") as fh:
        json.dump(out, fh, ensure_ascii=False, indent=1)

    print("reference apps:", out["refs"])
    print("%-4s %-36s %5s %5s %5s | %5s | %s" % ("set", "name", "cov", "excl", "bal", "GCfit", "gaps in a named purpose"))
    for sid, r in out["sets"].items():
        m = r["ref_mean"]
        print("%-4s %-36s %5s %5s %5s | %5s | %s/%s" % (sid, r["name"], m.get("coverage"), m.get("exclusivity"),
              m.get("balance"), r["glasscam_fit"], r["improvement"]["gaps_in_named_purpose"], r["improvement"]["gaps_total"]))
    print("duplicate-candidate pairs:", len(pairs))
    for g, r in out["granularity"].items():
        print(g, {k: v for k, v in r.items()})


if __name__ == "__main__":
    main()
