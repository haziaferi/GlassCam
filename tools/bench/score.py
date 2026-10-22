"""Score one bench run: the photos pulled from the phone, scored from their pixel values.

    python tools/bench/score.py RUN_DIR          # writes RUN_DIR/scores.json and prints a summary

RUN_DIR holds (any part may be missing; its scores are then absent):
    dark_1.dng, dark_2.dng                     lens covered
    flat_<level>_1.dng/.jpg, flat_<level>_2.*  each level of imaging.FLAT_LEVELS, screen defocused
    colour_<n>.dng/.jpg                        the colour page, n = 1..3
    edges_<n>.dng/.jpg                         the printed edge sheet, n = 1..3

    run.json                                   the display record (runinfo.py); without a matching
                                               one the run is scored but compare.py refuses it

Axes (docs/bench/PLAN.md): noise (SNR at 18 % grey, dB), colour (mean CIEDE2000), sharpness (MTF50,
cycles/pixel, with overshoot %), dynamic range (stops). Values from repeated shots carry their spread,
which compare.py uses as the threshold for "worse".
"""
import glob
import json
import math
import os
import re
import sys

import numpy as np

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import imaging as im  # noqa: E402
import runinfo  # noqa: E402


def stats(values):
    v = [x for x in values if x is not None and not (isinstance(x, float) and math.isnan(x))]
    if not v:
        return None
    return {"mean": float(np.mean(v)), "spread": float(np.ptp(v)) if len(v) > 1 else None, "n": len(v),
            "values": [round(float(x), 5) for x in v]}


def shots(run, kind, ext):
    pat = re.compile(rf"{kind}_(\d+)\.{ext}$", re.I)
    return sorted(p for p in glob.glob(os.path.join(run, f"{kind}_*.{ext}")) if pat.search(os.path.basename(p)))


def green_mask(d):
    return np.isin(d.colors, [i for i in range(len(d.desc)) if d.desc[i] == "G"])

# ---------------------------------------------------------------- DNG


def dng_noise_and_range(run, notes):
    """Raw noise and range, all from the frame's centre (a light leak at the 6T baseline's edge inflated
    the whole-frame read noise from 0.72 to 1.57 DN), against the black measured in the dark frames
    (the 6T's DNG tag says 63, its sensor sits at 63.9). Returns (scores, green signal per flat level)."""
    out, light = {}, {}
    darks = shots(run, "dark", "dng")
    black, sigma = None, None
    if len(darks) >= 2:
        a, b = im.Dng(darks[0]), im.Dng(darks[1])
        g = im.centre_crop(green_mask(a))
        ca, cb = im.centre_crop(a.raw), im.centre_crop(b.raw)
        black = float(np.median(ca))
        _, var = im.pair_stats(ca[g], cb[g])
        sigma = math.sqrt(var)
        out["black_measured"] = round(float(ca.mean()), 3)
        out["black_tag"] = a.black
        out["read_noise_dn"] = round(sigma, 4)
        out["hot_pixels_centre"] = int((ca > ca.mean() + 10 * max(sigma, 0.5)).sum())
        out["dynamic_range_stops"] = round(math.log2((a.white - ca.mean()) / sigma), 4)
    else:
        notes.append("DNG dynamic range: needs dark_1.dng and dark_2.dng")
    means, variances, profile = [], [], None
    for lv in im.FLAT_LEVELS:
        pair = [os.path.join(run, f"flat_{lv}_{n}.dng") for n in (1, 2)]
        if not all(os.path.exists(p) for p in pair):
            continue
        a, b = im.Dng(pair[0]), im.Dng(pair[1])
        ea, eb = (a.tags.get(33434), a.tags.get(34855)), (b.tags.get(33434), b.tags.get(34855))
        if ea != eb:                                  # a pair is only a noise sample at one exposure
            notes.append(f"flat {lv}: the two shots differ in exposure/ISO {ea} vs {eb}, left out")
            continue
        base = out.get("black_measured", a.black)
        ca, cb, cm = im.centre_crop(a.raw), im.centre_crop(b.raw), im.centre_crop(green_mask(a))
        m, v = im.pair_stats(ca[cm], cb[cm])
        light[lv] = m - base
        if (ca[cm] >= a.white - 1).mean() > 0.001:
            continue                                  # clipped: off the linear part of the curve
        means.append(m - base)
        variances.append(v)
        profile = profile or (a.tags.get(51041), a.white - base)
    if len(means) >= 3:
        a_fit, b_fit = im.fit_ptc(means, variances)
        span = profile[1] if profile else 1.0
        out["ptc_a"], out["ptc_b"] = round(a_fit, 5), round(b_fit, 4)
        out["snr18_db"] = round(im.snr_db(0.18 * span, a_fit, b_fit), 3)
        out["ptc_levels"] = len(means)
        if profile and profile[0]:
            s, o = profile[0][0], profile[0][1]
            out["noise_profile_tag"] = {"S": s, "O": o, "measured_S": a_fit / span, "measured_O": b_fit / span ** 2}
        else:
            out["noise_profile_tag"] = None
    else:
        notes.append("DNG noise: needs unclipped flat pairs at 3 or more levels")
    return out, light


def dng_colour(path):
    d = im.Dng(path)
    half = d.half_res_rgb()
    lum = half[::2, ::2, 1]
    corners = [(x * 2, y * 2) for x, y in im.locate_screen(lum)]
    M = d.camera_to_xyz_d50()
    to_xyz = lambda rgb: rgb @ M.T
    _, k, c = im.best_orientation(half, corners, to_xyz)
    return im.colour_report(to_xyz(im.sample_patches(half, c)))


def edges_of(lum):
    mtf, over = [], []
    for _, roi in im.edge_rois(lum, im.find_squares(lum)):
        m50, ov, slope = im.slanted_edge(roi)
        if 0.03 < slope < 0.2 and not math.isnan(m50):
            mtf.append(m50)
            over.append(ov)
    return mtf, over

# ---------------------------------------------------------------- JPEG


def jpeg_noise_and_range(run, notes, light=None):
    """light: the green raw signal the DNG measured at each flat level. A display's code value is not its
    light: the Nord's full-white screen gave less than its grey 180 (an OLED's power limiting), and it
    crushes levels 4 and 8. Without DNGs the nominal sRGB values are used, with a note."""
    out, per_level = {}, {}
    for lv in im.FLAT_LEVELS:
        pair = [os.path.join(run, f"flat_{lv}_{n}.jpg") for n in (1, 2)]
        if not all(os.path.exists(p) for p in pair):
            continue
        ya, yb = (im.centre_crop(im.luminance_from_srgb_linear(im.load_jpeg_linear(p))) for p in pair)
        m, v = im.pair_stats(ya, yb)
        per_level[lv] = (m, v)
    if not per_level:
        notes.append("JPEG noise: no flat pairs")
        return out
    snr = {lv: 20 * math.log10(m / math.sqrt(max(v, 1e-12))) for lv, (m, v) in per_level.items()}
    out["snr_db_by_level"] = {str(k): round(v, 3) for k, v in snr.items()}
    if 118 in snr:
        out["snr18_db"] = round(snr[118], 3)
    clip = float(im.srgb_to_linear(250 / 255))
    unclipped = [lv for lv, (m, _) in per_level.items() if m < clip]
    usable = [lv for lv in per_level if snr[lv] >= 0]          # SNR >= 1
    measured = [lv for lv in per_level if light and light.get(lv, 0) > 0]
    if measured:
        if len(measured) < len(per_level):
            notes.append(f"JPEG dynamic range: levels {sorted(set(per_level) - set(measured))} have no "
                         "measured light (no valid DNG pair) and are left out")
        per_level = {lv: per_level[lv] for lv in measured}
        unclipped = [lv for lv in unclipped if lv in per_level]
        usable = [lv for lv in usable if lv in per_level]
        level_light = {lv: light[lv] for lv in per_level}
        out["dynamic_range_light"] = "measured by the DNG flats"
    else:
        level_light = {lv: float(im.srgb_to_linear(lv / 255)) for lv in per_level}
        out["dynamic_range_light"] = "nominal sRGB (no DNG flats)"
        notes.append("JPEG dynamic range: no DNG flats, so the display's nominal sRGB light is assumed")
    if unclipped and usable:
        top = max(unclipped, key=lambda lv: level_light[lv])
        bottom = min(usable, key=lambda lv: level_light[lv])
        out["dynamic_range_stops"] = round(math.log2(level_light[top] / level_light[bottom]), 3)
        out["dynamic_range_capped"] = bottom == min(per_level, key=lambda lv: level_light[lv])
    return out


def jpeg_colour(path):
    img = im.load_jpeg_linear(path)
    lum = im.luminance_from_srgb_linear(img)
    corners = [(x * 4, y * 4) for x, y in im.locate_screen(lum[::4, ::4])]
    _, k, c = im.best_orientation(img, corners, im.srgb_linear_to_xyz_d50)
    return im.colour_report(im.srgb_linear_to_xyz_d50(im.sample_patches(img, c)))

# ---------------------------------------------------------------- run


def run_record(run):
    """run.json and its check against displays.json, kept in scores.json for compare.py."""
    path = os.path.join(run, "run.json")
    info = json.load(open(path, encoding="utf-8")) if os.path.exists(path) else None
    problems, notes = runinfo.check_run(info)
    return {"info": info, "problems": problems, "notes": notes}


def score(run):
    notes = []
    result = {"run": run_record(run), "dng": {}, "jpeg": {}, "notes": notes}
    for fmt, colour_fn in (("dng", dng_colour), ("jpg", jpeg_colour)):
        key = "dng" if fmt == "dng" else "jpeg"
        reports = [colour_fn(p) for p in shots(run, "colour", fmt)]
        if reports:
            des = [r[0] for r in reports]
            result[key]["colour_mean_de"] = stats([float(d.mean()) for d in des])
            result[key]["colour_max_de"] = stats([float(d.max()) for d in des])
            result[key]["colour_mean_de_wb"] = stats([float(r[1].mean()) for r in reports])
            result[key]["neutral_cast_ab"] = [round(float(np.mean([r[2][i] for r in reports])), 2) for i in (0, 1)]
            result[key]["colour_per_patch"] = {name: round(float(np.mean([d[i] for d in des])), 3)
                                               for i, (name, _) in enumerate(im.PATCHES)}
        mtfs, overs = [], []
        for p in shots(run, "edges", fmt):
            lum = im.Dng(p).wb_mosaic() if fmt == "dng" else im.luminance_from_srgb_linear(im.load_jpeg_linear(p))
            m, o = edges_of(lum)
            if m:
                mtfs.append(float(np.median(m)))
                overs.append(float(np.median(o)))
            else:
                notes.append(f"{os.path.basename(p)}: no usable edges found")
        if mtfs:
            result[key]["mtf50"] = stats(mtfs)
            result[key]["overshoot_pct"] = stats(overs)
    dng_scores, light = dng_noise_and_range(run, notes)
    result["dng"].update(dng_scores)
    result["jpeg"].update(jpeg_noise_and_range(run, notes, light))
    return result


def main(run):
    result = score(run)
    with open(os.path.join(run, "scores.json"), "w", encoding="utf-8", newline="\n") as fh:
        json.dump(result, fh, indent=1)
    for key in ("jpeg", "dng"):
        r = result[key]
        pick = lambda k: (r[k]["mean"] if isinstance(r.get(k), dict) else r.get(k))
        f = lambda v, d=2: "-" if v is None else f"{v:.{d}f}"
        print(f"{key:5} noise SNR18 {f(pick('snr18_db'))} dB | colour dE {f(pick('colour_mean_de'))} "
              f"(after white balance {f(pick('colour_mean_de_wb'))}, cast a*b* {r.get('neutral_cast_ab')}) | "
              f"MTF50 {f(pick('mtf50'), 3)} c/px, overshoot {f(pick('overshoot_pct'), 1)} % | "
              f"DR {f(pick('dynamic_range_stops'))} stops")
    for n in result["notes"] + result["run"]["notes"]:
        print("note:", n)
    for p in result["run"]["problems"]:
        print("DISPLAY:", p)


if __name__ == "__main__":
    if len(sys.argv) != 2:
        sys.exit(__doc__)
    main(sys.argv[1])
