"""Bench self-test: every measurement must recover a value planted in a synthetic image.

    python tools/bench/selftest.py          # exit 0 = every check passed

Needs numpy only (no phone, no rawpy): it is the bench's proof that its numbers mean what they say, and it
runs in CI. Each check also runs once with a planted error and must then FAIL, so a check that cannot
fail is caught too.
"""
import math
import os
import sys

import numpy as np

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import imaging as im  # noqa: E402

RESULTS = []


def check(name, ok, detail):
    RESULTS.append((name, bool(ok), detail))


# ---------------------------------------------------------------- colour


def test_de2000(plant=0.0):
    # Sharma, Wu, Dalal (2005) test pairs 1, 7 and 17
    pairs = [((50, 2.6772, -79.7751), (50, 0, -82.7485), 2.0425),
             ((50, 0, 0), (50, -1, 2), 2.3669),
             ((50, 2.5, 0), (73, 25, -18), 27.1492)]
    got = [float(im.delta_e2000(a, b)[0]) + plant for a, b, _ in pairs]
    return all(abs(g - e) < 1e-3 for g, (_, _, e) in zip(got, pairs)), f"{[round(g, 4) for g in got]}"


def test_white_lab(plant=0.0):
    lab = im.xyz_to_lab(im.srgb_linear_to_xyz_d50(np.ones(3)) * (1 + plant))
    return np.allclose(lab, [100, 0, 0], atol=0.02), f"{np.round(lab, 3).tolist()}"


def warped_chart(size=(900, 1200), corners=((180, 130), (1030, 170), (1000, 800), (150, 760))):
    """A rendered chart (linear sRGB) seen in perspective on a dark background."""
    h, w = size
    H = im.homography(corners, [(0, 0), (1, 0), (1, 1), (0, 1)])
    yy, xx = np.mgrid[0:h, 0:w]
    uv = im.apply_h(H, np.c_[xx.ravel(), yy.ravel()]).reshape(h, w, 2)
    u, v = uv[..., 0], uv[..., 1]
    img = np.zeros((h, w, 3))
    inside = (u >= 0) & (u <= 1) & (v >= 0) & (v <= 1)
    img[inside] = im.srgb_to_linear(im.CHART_BACKGROUND / 255.0)
    for i, (_, rgb) in enumerate(im.PATCHES):
        x0, y0, x1, y1 = im.patch_rect(i)
        m = (u >= x0) & (u <= x1) & (v >= y0) & (v <= y1)
        img[m] = im.srgb_to_linear(np.array(rgb) / 255.0)
    return img


def test_chart_sampling(plant=0):
    img = warped_chart()
    img = np.rot90(img, plant)        # plant = 0: as rendered; the orientation search must undo any turn
    lum = im.luminance_from_srgb_linear(img)
    de, k, _ = im.best_orientation(img, im.locate_screen(lum), im.srgb_linear_to_xyz_d50)
    ok = de.max() < 0.5 and abs(k - 1) < 0.02
    return ok, f"mean dE {de.mean():.3f}, max {de.max():.3f}, scale {k:.3f}"


def test_chart_filling_frame(plant=0.0):
    """The screen fills almost the whole frame: the bright patches are then more than 1 % of the pixels."""
    img = warped_chart(size=(900, 1200), corners=((12, 10), (1190, 6), (1188, 893), (8, 890)))
    if plant:
        img = img[..., [1, 0, 2]]
    lum = im.luminance_from_srgb_linear(img)
    de, k, _ = im.best_orientation(img, im.locate_screen(lum), im.srgb_linear_to_xyz_d50)
    return de.max() < 0.5, f"mean dE {de.mean():.3f}, max {de.max():.3f}"


def test_chart_rotated(plant=0.0):
    img = np.rot90(warped_chart(), 1) * 0.6          # rotated a quarter turn and underexposed
    if plant:
        img = img[..., ::-1]                         # planted: red and blue swapped
    lum = im.luminance_from_srgb_linear(img)
    de, k, _ = im.best_orientation(img, im.locate_screen(lum), im.srgb_linear_to_xyz_d50)
    return de.max() < 0.5, f"mean dE {de.mean():.3f}, max {de.max():.3f}, scale {k:.3f}"


def test_dng_matrices(plant=0.0):
    """Camera RGB made from the chart through a known XYZ->camera matrix comes back with dE ~ 0,
    both through ColorMatrix alone and through ForwardMatrix."""
    rng = np.random.default_rng(1)
    cm = np.array([[0.9, 0.3, -0.2], [-0.4, 1.3, 0.1], [0.05, -0.2, 0.8]]) + rng.normal(0, 0.02, (3, 3))
    xyz_d65 = im.srgb_to_linear(np.array([p[1] for p in im.PATCHES]) / 255.0) @ im.SRGB_TO_XYZ_D65.T
    cam = xyz_d65 @ cm.T
    neutral = cm @ im.D65
    neutral = neutral / neutral[1]
    fm = im.bradford(im.D65, im.D50) @ np.linalg.inv(cm) @ np.diag(neutral)
    fm = fm * (1 + plant)
    fm[0, 0] += plant
    out = []
    for tags in ({50721: cm.ravel(), 50728: neutral, 50778: (21,)},
                 {50721: cm.ravel(), 50964: fm.ravel(), 50728: neutral, 50778: (21,)}):
        d = im.Dng.__new__(im.Dng)
        d.tags, d.neutral = tags, np.array(tags[50728], float)
        de, _ = im.colour_errors(cam @ d.camera_to_xyz_d50().T)
        out.append(de.max())
    return max(out) < 0.05, f"max dE ColorMatrix {out[0]:.4f}, ForwardMatrix {out[1]:.4f}"


def test_interpolation(plant=0.0):
    cm_a = np.eye(3) * [1.0, 1.0, 0.4]
    cm_d = np.eye(3)
    neutral = cm_d @ im.D65
    g = im.interpolation_weight([cm_a, cm_d], [2856, 6504], neutral / neutral[1]) + plant
    return g < 0.1, f"weight of illuminant A under D65 light: {g:.3f}"

# ---------------------------------------------------------------- noise and dynamic range


def test_ptc(plant=0.0):
    rng = np.random.default_rng(2)
    a_true, b_true = 0.8, 4.0
    vignette = 1 - 0.3 * np.linspace(-1, 1, 400)[None, :] ** 2      # fixed pattern: cancels in a - b
    means, variances = [], []
    for s in (20, 60, 150, 300, 600, 850):
        mean_img = s * vignette * np.ones((300, 1))
        frames = [mean_img + rng.normal(0, 1, mean_img.shape) * np.sqrt(a_true * mean_img + b_true)
                  for _ in range(2)]
        m, v = im.pair_stats(*frames)
        means.append(m)
        variances.append(v * (1 + plant))
    a, b = im.fit_ptc(means, variances)
    ok = abs(a / a_true - 1) < 0.03 and abs(b / b_true - 1) < 0.25
    return ok, f"a {a:.3f} (0.8), b {b:.2f} (4.0)"


def test_read_noise_dr(plant=0.0):
    rng = np.random.default_rng(3)
    sigma, black, white = 2.0, 64.0, 1023.0
    d1, d2 = (black + rng.normal(0, sigma * (1 + plant), (500, 500)) for _ in range(2))
    _, var = im.pair_stats(d1, d2)
    dr = math.log2((white - black) / math.sqrt(var))
    expected = math.log2((white - black) / sigma)
    return abs(dr - expected) < 0.03, f"{dr:.3f} stops (expected {expected:.3f})"

# ---------------------------------------------------------------- sharpness


def synthetic_edge(sigma, rows=120, cols=40, angle_deg=5.0, sharpen=0.0):
    """Area-sampled image of a Gaussian-blurred edge at a slant (1 = paper, 0.05 = ink)."""
    from math import erf
    G = lambda u: u * 0.5 * (1 + np.vectorize(erf)(u / (sigma * math.sqrt(2)))) + \
        sigma / math.sqrt(2 * math.pi) * np.exp(-u * u / (2 * sigma * sigma))
    t = math.tan(math.radians(angle_deg))
    img = np.zeros((rows, cols))
    x = np.arange(cols)
    for r in range(rows):
        acc = 0
        for sub in np.linspace(0, 1, 8, endpoint=False) + 1 / 16:
            e = cols / 2 + (r + sub - rows / 2) * t
            acc = acc + (G(x + 1 - e) - G(x - e))
        img[r] = acc / 8
    img = 0.05 + 0.95 * img
    if sharpen:
        blur = (np.roll(img, 1, 1) + np.roll(img, -1, 1) + img) / 3
        img = img + sharpen * (img - blur)
        img = img[:, 2:-2]
    return img


def expected_mtf50(sigma):
    lo, hi = 0.0, 1.0
    for _ in range(60):
        f = (lo + hi) / 2
        v = math.exp(-2 * math.pi ** 2 * sigma ** 2 * f ** 2) * np.sinc(f)
        lo, hi = (f, hi) if v > 0.5 else (lo, f)
    return lo


def test_mtf(plant=0.0):
    out = []
    for sigma in (0.7, 1.0, 1.6):
        got, _, _ = im.slanted_edge(synthetic_edge(sigma + plant))
        out.append((sigma, got, expected_mtf50(sigma)))
    ok = all(abs(g / e - 1) < 0.04 for _, g, e in out)
    return ok, "; ".join(f"sigma {s}: {g:.4f} vs {e:.4f}" for s, g, e in out)


def test_overshoot(plant=0.0):
    """A plain edge has none; a sharpened one reads what its own pixels show (max over the far plateau)."""
    _, plain, _ = im.slanted_edge(synthetic_edge(1.0))
    out = []
    for amount in (1.5, 3.0):
        img = synthetic_edge(1.0, sharpen=amount)
        lo, hi = img[:, :4].mean(), img[:, -4:].mean()
        truth = 100 * (img.max() - hi) / (hi - lo)
        _, got, _ = im.slanted_edge(synthetic_edge(1.0, sharpen=amount * (1 - plant)))
        out.append((truth, got))
    ok = plain < 0.5 and all(abs(g - t) < 0.5 for t, g in out)
    return ok, f"plain {plain:.2f} %; sharpened " + ", ".join(f"{g:.2f} % (pixels {t:.2f} %)" for t, g in out)


def test_find_squares(plant=0.0):
    """Four rotated dark squares on a sheet are found, and their sides give edges with the planted blur."""
    h, w = 1200, 1700
    yy, xx = np.mgrid[0:h, 0:w].astype(float)
    img = np.ones((h, w))
    th = math.radians(im.EDGE_ANGLE_DEG)
    centres = [(450, 330), (1250, 330), (450, 870), (1250, 870)]
    side = 360
    for cx, cy in centres:
        u = (xx - cx) * math.cos(th) + (yy - cy) * math.sin(th)
        v = -(xx - cx) * math.sin(th) + (yy - cy) * math.cos(th)
        img[(np.abs(u) < side / 2) & (np.abs(v) < side / 2)] = 0.05
    if plant:
        img[:] = 1.0
    sq = im.find_squares(img)
    ok = len(sq) == 4 and all(min(math.hypot(c[0] - x, c[1] - y) for x, y in centres) < 6 for c in sq) \
        and all(abs(c[2] / side - 1) < 0.05 for c in sq)
    rois = im.edge_rois(img, sq) if sq else []
    ok = ok and len(rois) == 16
    return ok, f"{len(sq)} squares, {len(rois)} edges"


def test_score_jpeg_run(plant=0.0):
    """A whole run of real JPEG files through score.py: chart, edge sheet and flat pairs with known noise."""
    import tempfile
    from PIL import Image
    import score
    rng = np.random.default_rng(4)
    to_jpeg = lambda lin, path: Image.fromarray(np.clip(np.round(
        np.where(lin <= 0.0031308, lin * 12.92, 1.055 * np.clip(lin, 0, None) ** (1 / 2.4) - 0.055) * 255), 0, 255)
        .astype(np.uint8)).save(path, quality=100, subsampling=0)
    sigma = 1.2
    with tempfile.TemporaryDirectory() as run:
        chart = warped_chart()
        if plant:
            chart = chart[..., [1, 0, 2]]                       # planted: red and green swapped
        for n in (1, 2):
            to_jpeg(chart, os.path.join(run, f"colour_{n}.jpg"))
        h, w = 1000, 1400
        yy, xx = np.mgrid[0:h, 0:w].astype(float)
        sheet = np.ones((h, w))
        th = math.radians(im.EDGE_ANGLE_DEG)
        for cx, cy in ((400, 280), (1000, 280), (400, 720), (1000, 720)):
            u = (xx - cx) * math.cos(th) + (yy - cy) * math.sin(th)
            v = -(xx - cx) * math.sin(th) + (yy - cy) * math.cos(th)
            d = np.maximum(np.abs(u), np.abs(v)) - 150                  # signed distance-ish to the square
            from math import erf
            sheet = np.minimum(sheet, 0.05 + 0.95 * 0.5 * (1 + np.vectorize(erf)(d / (sigma * math.sqrt(2)))))
        for n in (1, 2):
            to_jpeg(np.repeat(sheet[..., None], 3, 2), os.path.join(run, f"edges_{n}.jpg"))
        for lv in im.FLAT_LEVELS:
            lin = float(im.srgb_to_linear(lv / 255))
            for n in (1, 2):
                noisy = np.clip(lin + rng.normal(0, 0.004, (300, 400)), 0, 1)
                to_jpeg(np.repeat(noisy[..., None], 3, 2), os.path.join(run, f"flat_{lv}_{n}.jpg"))
        r = score.score(run)["jpeg"]
    de = r["colour_mean_de"]["mean"]
    mtf = r["mtf50"]["mean"]
    snr = r.get("snr18_db")
    want_mtf = 0.1874 / sigma                                    # the Gaussian alone, point-sampled
    want_snr = 20 * math.log10(float(im.srgb_to_linear(118 / 255)) / 0.004)
    ok = de < 1.0 and abs(mtf / want_mtf - 1) < 0.08 and snr is not None and abs(snr - want_snr) < 1.5
    return ok, f"dE {de:.3f}, MTF50 {mtf:.4f} (~{want_mtf:.4f}), SNR18 {snr} dB (~{want_snr:.1f})"


TESTS = [test_de2000, test_white_lab, test_chart_sampling, test_chart_filling_frame, test_chart_rotated,
         test_dng_matrices, test_interpolation, test_ptc, test_read_noise_dr, test_mtf, test_overshoot, test_find_squares,
         test_score_jpeg_run]
PLANTS = {test_de2000: 0.01, test_white_lab: 0.05, test_chart_sampling: None, test_chart_filling_frame: 1, test_chart_rotated: 1,
          test_dng_matrices: 0.05, test_interpolation: 0.5, test_ptc: 0.2, test_read_noise_dr: 0.1,
          test_mtf: 0.3, test_overshoot: 1.0, test_find_squares: 1, test_score_jpeg_run: 1}


def main():
    failed = 0
    for t in TESTS:
        ok, detail = t()
        print(f"{'PASS' if ok else 'FAIL'}  {t.__name__:24} {detail}")
        failed += not ok
        plant = PLANTS[t]
        if plant is not None:
            caught, detail = t(plant)
            caught = not caught
            print(f"{'ok  ' if caught else 'MISS'}  {t.__name__ + ' (planted)':24} {detail}")
            failed += not caught
    print(f"\n{len(TESTS)} checks, {failed} failures")
    return 1 if failed else 0


if __name__ == "__main__":
    sys.exit(main())
