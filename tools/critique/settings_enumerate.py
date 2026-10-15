"""settings_enumerate.py -- score EVERY settings-panel layout with settings_scorers.py.

    python tools/critique/settings_enumerate.py            -> ranked table + settings_enumerate.json

The space in settings_spec.json is small (5! section orders x 3 column counts x 2^4 categoricals
= 5,760 points), so it is enumerated exactly instead of searched: a heuristic search can stop
short on coupled categoricals, and an exact table also says how far the runner-up is. Weights
come from settings_spec.json. Both row models are scored, so the table shows whether a decision
holds for the panel that shipped ("uniform_76") and for the one the 2026-09-16 run modelled
("measured_4388").
"""
import itertools
import json
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import settings_scorers as S  # noqa: E402

spec = json.load(open(os.path.join(HERE, "settings_spec.json"), encoding="utf-8"))
weights = {c["name"]: c["weight"] for c in spec["fitness_components"]}
dims = {d["name"]: d for d in spec["dimensions"]}
scorers = {"reach": S.reach, "fill": S.fill, "legibility": S.legibility,
           "chips_reach": S.chips_reach, "stability": S.stability}


def score(v):
    parts = {k: f(v, None) for k, f in scorers.items()}
    return sum(weights[k] * parts[k] for k in parts), parts


results = {}
for model in ("uniform_76", "measured_4388"):
    rows = []
    for order in itertools.permutations(dims["section_order"]["items"]):
        for cols in (1, 2, 3):
            for packing, stepper, sort_by_use, chips in itertools.product(
                    dims["packing"]["values"], dims["stepper"]["values"],
                    dims["sort_by_use"]["values"], dims["chips_pos"]["values"]):
                v = {"section_order": list(order), "columns": cols, "packing": packing, "stepper": stepper,
                     "sort_by_use": sort_by_use, "chips_pos": chips, "row_model": model}
                total, parts = score(v)
                rows.append((round(total, 6), v, {k: round(x, 4) for k, x in parts.items()}))
    rows.sort(key=lambda r: -r[0])
    results[model] = rows


def best_where(rows, **fixed):
    for r in rows:
        if all(r[1][k] == val for k, val in fixed.items()):
            return r
    return None


shipped = dict(section_order=["video", "picture", "camera", "preview", "etc"], columns=2, packing="grid",
               stepper="compact", sort_by_use="declared", chips_pos="bottom")
out = {}
for model, rows in results.items():
    print(f"\n== row model {model}: {len(rows)} layouts")
    for total, v, parts in rows[:5]:
        print(f"  {total:.4f}  cols={v['columns']} {v['packing']:9s} {v['stepper']:7s} {v['sort_by_use']:8s} "
              f"chips={v['chips_pos']:6s} order={'/'.join(s[:3] for s in v['section_order'])}  {parts}")
    ship = next(r for r in rows if all(r[1][k] == shipped[k] for k in shipped))
    rank = rows.index(ship) + 1
    print(f"  shipped layout: {ship[0]:.4f}, rank {rank} of {len(rows)}")
    for c in (1, 2, 3):
        b = best_where(rows, columns=c)
        print(f"  best with {c} column(s): {b[0]:.4f}  {b[2]}")
    for chips in ("top", "bottom"):
        b = best_where(rows, chips_pos=chips)
        print(f"  best with chips {chips}: {b[0]:.4f}")
    orders = sorted({r[0] for r in rows if all(r[1][k] == shipped[k] for k in shipped if k != "section_order")})
    print(f"  section order spread at the shipped settings: {orders[0]:.4f} .. {orders[-1]:.4f}")
    out[model] = {"top": [{"score": t, "vector": v, "parts": p} for t, v, p in rows[:10]],
                  "shipped": {"score": ship[0], "rank": rank, "parts": ship[2]},
                  "section_order_spread": [orders[0], orders[-1]]}
json.dump(out, open(os.path.join(HERE, "settings_enumerate.json"), "w", encoding="utf-8", newline="\n"), indent=1)
