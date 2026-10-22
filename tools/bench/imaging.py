"""Image-quality bench: the measurements (numpy only; rawpy is imported lazily by load_dng).

Every score is computed from the pixel values in the files, never from how a screen shows them. The
methods follow docs/bench/PLAN.md:
- noise: photon-transfer fit `variance = a * signal + b` from pairs of flat fields (the difference of two
  frames removes vignetting and fixed pattern; Stanford CS348K L2 / UNC lecture 4 noise model);
- dynamic range: log2((white - black) / read-noise sigma) for raw, from dark frames;
- colour: CIEDE2000 between the shot's patches and the chart's own sRGB values, after one exposure scale
  fitted on the mid neutrals;
- sharpness: slanted-edge MTF50 (ISO 12233 style: 4x oversampled edge spread, Hamming window), and the
  edge's overshoot, which is where oversharpening halos show.
"""
import math
import os
import sys

import numpy as np

# ---------------------------------------------------------------- colour science

SRGB_TO_XYZ_D65 = np.array([[0.4124564, 0.3575761, 0.1804375],
                            [0.2126729, 0.7151522, 0.0721750],
                            [0.0193339, 0.1191920, 0.9503041]])
BRADFORD = np.array([[0.8951, 0.2664, -0.1614],
                     [-0.7502, 1.7135, 0.0367],
                     [0.0389, -0.0685, 1.0296]])
D65 = np.array([0.95047, 1.0, 1.08883])
D50 = np.array([0.96422, 1.0, 0.82521])

# The classic 24-patch chart, as the sRGB values published for it; the bench shows exactly these values,
# so they are the target whatever the real chart's spectra are.
PATCHES = [
    ("dark skin", (115, 82, 68)), ("light skin", (194, 150, 130)), ("blue sky", (98, 122, 157)),
    ("foliage", (87, 108, 67)), ("blue flower", (133, 128, 177)), ("bluish green", (103, 189, 170)),
    ("orange", (214, 126, 44)), ("purplish blue", (80, 91, 166)), ("moderate red", (193, 90, 99)),
    ("purple", (94, 60, 108)), ("yellow green", (157, 188, 64)), ("orange yellow", (224, 163, 46)),
    ("blue", (56, 61, 150)), ("green", (70, 148, 73)), ("red", (175, 54, 60)),
    ("yellow", (231, 199, 31)), ("magenta", (187, 86, 149)), ("cyan", (8, 133, 161)),
    ("white 9.5", (243, 243, 242)), ("neutral 8", (200, 200, 200)), ("neutral 6.5", (160, 160, 160)),
    ("neutral 5", (122, 122, 121)), ("neutral 3.5", (85, 85, 85)), ("black 2", (52, 52, 52)),
]
SCALE_PATCHES = (19, 20, 21, 22)          # mid neutrals: the exposure scale is fitted on these
CHART_BACKGROUND = 160                    # page background code value around the patches
GRID = dict(cols=6, rows=4, x0=0.08, x1=0.92, y0=0.12, y1=0.88, fill=0.8)
FLAT_LEVELS = (4, 8, 16, 32, 64, 118, 180, 245)   # 118 is 18 % grey in linear light
EDGE_ANGLE_DEG = 5.0


def srgb_to_linear(c):
    c = np.asarray(c, dtype=float)
    return np.where(c <= 0.04045, c / 12.92, ((c + 0.055) / 1.055) ** 2.4)


def bradford(src_white, dst_white):
    s, d = BRADFORD @ src_white, BRADFORD @ dst_white
    return np.linalg.inv(BRADFORD) @ np.diag(d / s) @ BRADFORD


def xyz_to_lab(xyz, white=D50):
    t = np.asarray(xyz, dtype=float) / white
    e = 216 / 24389
    f = np.where(t > e, np.cbrt(t), (24389 / 27 * t + 16) / 116)
    return np.stack([116 * f[..., 1] - 16, 500 * (f[..., 0] - f[..., 1]), 200 * (f[..., 1] - f[..., 2])], -1)


def srgb_linear_to_xyz_d50(rgb_lin):
    return np.asarray(rgb_lin) @ (bradford(D65, D50) @ SRGB_TO_XYZ_D65).T


def target_xyz_d50():
    return srgb_linear_to_xyz_d50(srgb_to_linear(np.array([p[1] for p in PATCHES]) / 255.0))


def delta_e2000(lab1, lab2):
    """CIEDE2000 (Sharma, Wu, Dalal 2005), row-wise."""
    lab1, lab2 = np.atleast_2d(lab1).astype(float), np.atleast_2d(lab2).astype(float)
    L1, a1, b1 = lab1.T
    L2, a2, b2 = lab2.T
    C1, C2 = np.hypot(a1, b1), np.hypot(a2, b2)
    Cm7 = ((C1 + C2) / 2) ** 7
    G = 0.5 * (1 - np.sqrt(Cm7 / (Cm7 + 25.0 ** 7)))
    a1p, a2p = (1 + G) * a1, (1 + G) * a2
    C1p, C2p = np.hypot(a1p, b1), np.hypot(a2p, b2)
    h1p = np.degrees(np.arctan2(b1, a1p)) % 360
    h2p = np.degrees(np.arctan2(b2, a2p)) % 360
    dLp, dCp = L2 - L1, C2p - C1p
    dh = h2p - h1p
    dh = np.where(C1p * C2p == 0, 0, np.where(dh > 180, dh - 360, np.where(dh < -180, dh + 360, dh)))
    dHp = 2 * np.sqrt(C1p * C2p) * np.sin(np.radians(dh / 2))
    Lpm, Cpm = (L1 + L2) / 2, (C1p + C2p) / 2
    hs = h1p + h2p
    hpm = np.where(C1p * C2p == 0, hs,
                   np.where(np.abs(h1p - h2p) <= 180, hs / 2, np.where(hs < 360, (hs + 360) / 2, (hs - 360) / 2)))
    T = (1 - 0.17 * np.cos(np.radians(hpm - 30)) + 0.24 * np.cos(np.radians(2 * hpm))
         + 0.32 * np.cos(np.radians(3 * hpm + 6)) - 0.20 * np.cos(np.radians(4 * hpm - 63)))
    dtheta = 30 * np.exp(-(((hpm - 275) / 25) ** 2))
    Rc = 2 * np.sqrt(Cpm ** 7 / (Cpm ** 7 + 25.0 ** 7))
    Sl = 1 + 0.015 * (Lpm - 50) ** 2 / np.sqrt(20 + (Lpm - 50) ** 2)
    Sc, Sh = 1 + 0.045 * Cpm, 1 + 0.015 * Cpm * T
    Rt = -np.sin(np.radians(2 * dtheta)) * Rc
    return np.sqrt((dLp / Sl) ** 2 + (dCp / Sc) ** 2 + (dHp / Sh) ** 2 + Rt * (dCp / Sc) * (dHp / Sh))


def colour_errors(xyz_d50):
    """ΔE2000 of 24 measured patches (XYZ, D50, any exposure) against the chart, after one scale factor."""
    target = target_xyz_d50()
    idx = list(SCALE_PATCHES)
    m, t = xyz_d50[idx, 1], target[idx, 1]
    k = float(m @ t / (m @ m))
    de = delta_e2000(xyz_to_lab(xyz_d50 * k), xyz_to_lab(target))
    return de, k


def white_balance_on_neutrals(xyz_d50):
    """The patches adapted (Bradford) so the mid neutrals' average white becomes D50: what remains is
    colour rendering, without the white-balance cast."""
    n = xyz_d50[list(SCALE_PATCHES)].sum(0)
    return xyz_d50 @ bradford(n / n[1], D50).T


def neutral_cast(xyz_d50, k):
    """Average a*, b* of the mid neutrals at the fitted exposure: the white-balance cast."""
    lab = xyz_to_lab(xyz_d50[list(SCALE_PATCHES)] * k)
    return float(lab[:, 1].mean()), float(lab[:, 2].mean())


def colour_report(xyz_d50):
    """ΔE as shot, ΔE after white balance on the neutrals, and the cast."""
    de, k = colour_errors(xyz_d50)
    de_wb, _ = colour_errors(white_balance_on_neutrals(xyz_d50))
    return de, de_wb, neutral_cast(xyz_d50, k)

# ---------------------------------------------------------------- chart geometry


def patch_rect(i, inset=1.0):
    """Normalized (x0, y0, x1, y1) of patch i on the page; inset < 1 shrinks it about its centre."""
    g = GRID
    cw, ch = (g["x1"] - g["x0"]) / g["cols"], (g["y1"] - g["y0"]) / g["rows"]
    cx = g["x0"] + (i % g["cols"] + 0.5) * cw
    cy = g["y0"] + (i // g["cols"] + 0.5) * ch
    hw, hh = cw * g["fill"] * inset / 2, ch * g["fill"] * inset / 2
    return cx - hw, cy - hh, cx + hw, cy + hh


def homography(src, dst):
    """3x3 H with dst ~ H @ src for four point pairs."""
    A, b = [], []
    for (x, y), (u, v) in zip(src, dst):
        A.append([x, y, 1, 0, 0, 0, -u * x, -u * y]); b.append(u)
        A.append([0, 0, 0, x, y, 1, -v * x, -v * y]); b.append(v)
    h = np.linalg.solve(np.array(A, float), np.array(b, float))
    return np.append(h, 1).reshape(3, 3)


def apply_h(H, pts):
    p = np.c_[pts, np.ones(len(pts))] @ H.T
    return p[:, :2] / p[:, 2:3]


def locate_screen(lum):
    """Corners (tl, tr, br, bl) of the lit screen in a dark room, as (x, y) pixel coordinates.

    The threshold must sit between the room and the page background (code 160, about 0.35 of white),
    not between the background and the bright patches: at 0.5 x the brightest pixels, a screen filling the
    frame lost its background and only the bright patches were "the screen"."""
    thr = 0.2 * np.percentile(lum, 99)
    k = max(1, int(max(lum.shape) / 600))
    mask = downsample(lum, k) > thr
    blob = max(components(mask), key=len)          # the screen; a lamp or an app overlay is a smaller blob
    keep = np.zeros_like(mask)
    keep[blob[:, 0], blob[:, 1]] = True
    full = np.repeat(np.repeat(keep, k, 0), k, 1)
    full = np.pad(full, ((0, lum.shape[0] - full.shape[0]), (0, lum.shape[1] - full.shape[1])))
    ys, xs = np.nonzero((lum > thr) & full)
    s, d = xs + ys, xs - ys
    pick = lambda k: (float(xs[k]), float(ys[k]))
    return [pick(np.argmin(s)), pick(np.argmax(d)), pick(np.argmax(s)), pick(np.argmin(d))]


def sample_patches(img, corners, inset=0.5):
    """Mean of each patch's central area. img: HxWxC; returns 24xC."""
    H = homography([(0, 0), (1, 0), (1, 1), (0, 1)], corners)
    out = []
    for i in range(len(PATCHES)):
        x0, y0, x1, y1 = patch_rect(i, inset)
        u = np.linspace(x0, x1, 12)
        gx, gy = np.meshgrid(u, np.linspace(y0, y1, 12))
        pts = apply_h(H, np.c_[gx.ravel(), gy.ravel()])
        xi = np.clip(np.round(pts[:, 0]).astype(int), 0, img.shape[1] - 1)
        yi = np.clip(np.round(pts[:, 1]).astype(int), 0, img.shape[0] - 1)
        out.append(img[yi, xi].mean(0))
    return np.array(out)


def rotations(corners):
    """The four cyclic orders of the corners: the chart may be photographed in any orientation."""
    return [corners[k:] + corners[:k] for k in range(4)]


def best_orientation(img, corners, to_xyz):
    """(ΔE array, scale, corners) for the orientation with the lowest mean ΔE."""
    best = None
    for c in rotations(corners):
        de, k = colour_errors(to_xyz(sample_patches(img, c)))
        if best is None or de.mean() < best[0].mean():
            best = (de, k, c)
    return best

# ---------------------------------------------------------------- noise and dynamic range


def centre_crop(a, frac=0.2):
    h, w = a.shape[:2]
    dh, dw = int(h * frac / 2), int(w * frac / 2)
    return a[h // 2 - dh:h // 2 + dh, w // 2 - dw:w // 2 + dw]


def pair_stats(a, b):
    """(mean, temporal variance) of two frames of the same flat field."""
    a, b = a.astype(float), b.astype(float)
    return float((a.mean() + b.mean()) / 2), float(np.var(a - b) / 2)


def fit_ptc(means, variances):
    """variance = a * mean + b, each point weighted by 1/variance: a variance estimate scatters in
    proportion to itself, so unweighted the bright levels decide b, the read-noise floor (6T baseline,
    2026-10-22: unweighted b = 0.20 DN^2 against the dark frames' 0.52; weighted 0.48)."""
    y = np.asarray(variances, float)
    w = 1 / np.sqrt(np.maximum(y, 1e-9))
    A = np.c_[means, np.ones(len(means))]
    (a, b), *_ = np.linalg.lstsq(A * w[:, None], y * w, rcond=None)
    return float(a), float(b)


def snr_db(signal, a, b):
    return 20 * math.log10(signal / math.sqrt(max(a * signal + b, 1e-12)))

# ---------------------------------------------------------------- sharpness


def slanted_edge(roi, bin_width=0.25):
    """MTF50 (cycles/pixel) and overshoot (%) of a near-vertical edge in a 2-D linear-light ROI."""
    roi = np.asarray(roi, float)
    rows, cols = roi.shape
    d = np.abs(np.diff(roi, axis=1))
    xs = np.arange(cols - 1) + 0.5
    # first guess per row: where the smoothed row crosses halfway between ink and paper, nearest the window's
    # centre. The gradient centroid over the whole row was pulled off the edge by a laser print's mottled
    # toner (6T edge sheet, 2026-10-23: a visibly sharp edge read MTF50 0.046)
    lo, hi = np.percentile(roi, 10), np.percentile(roi, 90)
    k = np.ones(5) / 5
    pos = np.empty(rows)
    for r in range(rows):
        sm = np.convolve(roi[r], k, mode="same")
        above = sm > (lo + hi) / 2
        flips = np.nonzero(above[1:] != above[:-1])[0]
        pos[r] = flips[np.argmin(np.abs(flips - cols / 2))] + 0.5 if len(flips) else cols / 2
    m, c = np.polyfit(np.arange(rows), pos, 1)
    for _ in range(3):                                   # refine on a window around the fit
        win = np.abs(xs[None, :] - (m * np.arange(rows)[:, None] + c)) < 8
        dd = d * win
        pos = (dd * xs).sum(1) / np.maximum(dd.sum(1), 1e-12)
        m, c = np.polyfit(np.arange(rows), pos, 1)
    yy, xx = np.mgrid[0:rows, 0:cols]
    dist = (xx - (m * yy + c)) * math.cos(math.atan(m))
    half = min(cols / 2 - 2, 16)
    keep = np.abs(dist) < half
    nb = int(2 * half / bin_width)
    idx = np.clip(((dist[keep] + half) / bin_width).astype(int), 0, nb - 1)
    sums = np.bincount(idx, roi[keep], nb)
    cnt = np.bincount(idx, minlength=nb)
    good = cnt > 0
    esf = np.interp(np.arange(nb), np.nonzero(good)[0], sums[good] / cnt[good])
    if esf[: nb // 5].mean() > esf[-nb // 5:].mean():
        esf = esf[::-1]
    lo, hi = esf[: nb // 5].mean(), esf[-nb // 5:].mean()
    overshoot = 100 * max(esf.max() - hi, lo - esf.min(), 0) / max(hi - lo, 1e-12)
    lsf = np.gradient(esf)
    centre = int(np.argmax(lsf))
    n = np.arange(nb)
    lsf = lsf * (0.54 + 0.46 * np.cos(np.pi * np.clip((n - centre) / (nb / 2), -1, 1)))
    spec = np.abs(np.fft.rfft(lsf))
    f = np.fft.rfftfreq(nb, d=bin_width)
    mtf = spec / spec[0]
    mtf = mtf / np.maximum(np.sinc(2 * f * bin_width) * np.sinc(f * bin_width), 1e-6)
    below = np.nonzero((mtf < 0.5) & (f <= 1.0))[0]
    if not len(below) or below[0] == 0:
        return float("nan"), overshoot, abs(m)
    k = below[0]
    mtf50 = f[k - 1] + (mtf[k - 1] - 0.5) * (f[k] - f[k - 1]) / (mtf[k - 1] - mtf[k])
    return float(mtf50), float(overshoot), abs(m)


def downsample(a, k):
    """Block mean by k x k (the edges that don't fill a block are dropped)."""
    a = a[: a.shape[0] // k * k, : a.shape[1] // k * k]
    return a.reshape(a.shape[0] // k, k, a.shape[1] // k, k).mean((1, 3))


def components(mask):
    """4-connected regions of a boolean array, each as an (n, 2) array of (row, col)."""
    seen = np.zeros_like(mask)
    h, w = mask.shape
    out = []
    for y0, x0 in zip(*np.nonzero(mask)):
        if seen[y0, x0]:
            continue
        stack, pix = [(y0, x0)], []
        seen[y0, x0] = True
        while stack:
            y, x = stack.pop()
            pix.append((y, x))
            for yy, xx in ((y + 1, x), (y - 1, x), (y, x + 1), (y, x - 1)):
                if 0 <= yy < h and 0 <= xx < w and mask[yy, xx] and not seen[yy, xx]:
                    seen[yy, xx] = True
                    stack.append((yy, xx))
        out.append(np.array(pix))
    return out


def find_squares(lum, target_count=4):
    """Dark squares on a lit sheet: list of (cx, cy, side) in pixels, largest first."""
    k = max(1, int(max(lum.shape) / 600))
    small = downsample(lum, k)
    # threshold at the geometric middle of ink and paper: a fixed half of the paper merged a square with
    # the lamp-dimmed paper at the sheet's edge (6T, linear raw, faded print: 3 of 4 squares found)
    paper, ink = np.percentile(small, 90), np.percentile(small, 5)
    dark = small < np.sqrt(max(ink, 1e-6) * paper)
    h, w = dark.shape
    found = []
    for p in components(dark):
        bh, bw = np.ptp(p[:, 0]) + 1, np.ptp(p[:, 1]) + 1
        if len(p) < 0.002 * h * w or not 0.7 < bh / bw < 1.4 or len(p) / (bh * bw) < 0.7:
            continue
        theta = math.radians(EDGE_ANGLE_DEG)
        side = max(bh, bw) / (math.cos(theta) + math.sin(theta))
        found.append(((p[:, 1].mean() + 0.5) * k, (p[:, 0].mean() + 0.5) * k, side * k, len(p)))
    found.sort(key=lambda s: -s[3])
    return [s[:3] for s in found[:target_count]]


def edge_rois(lum, squares):
    """For each square, its four sides as near-vertical ROIs (horizontal sides are transposed)."""
    out = []
    for cx, cy, s in squares:
        r_along, r_across = int(0.25 * s), int(0.12 * s)
        cx, cy = int(round(cx)), int(round(cy))
        off = int(round(s / 2))
        for name, (x, y, vertical) in {"left": (cx - off, cy, True), "right": (cx + off, cy, True),
                                       "top": (cx, cy - off, False), "bottom": (cx, cy + off, False)}.items():
            if vertical:
                roi = lum[y - r_along:y + r_along, x - r_across:x + r_across]
            else:
                roi = lum[y - r_across:y + r_across, x - r_along:x + r_along].T
            if roi.size and min(roi.shape) > 8:
                out.append((name, roi))
    return out

# ---------------------------------------------------------------- files


def load_jpeg_linear(path):
    from PIL import Image, ImageOps
    with Image.open(path) as im:
        rgb = np.asarray(ImageOps.exif_transpose(im).convert("RGB"), float) / 255.0
    return srgb_to_linear(rgb)


def luminance_from_srgb_linear(rgb_lin):
    return rgb_lin @ SRGB_TO_XYZ_D65[1]


def dng_tags(path):
    """IFD0 colour tags of a DNG, read with tools/dng_dump.py's reader."""
    sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), ".."))
    from dng_dump import read_ifds
    with open(path, "rb") as fh:
        ifds = read_ifds(fh.read())
    tags = {}
    for name, t in ifds:
        if name == "IFD0" or name.startswith("SubIFD"):
            for k, (_typ, _count, vals) in t.items():
                tags.setdefault(k, vals)
    return tags


def gain_maps(opcodes):
    """The GainMap opcodes (DNG opcode 9) of an OpcodeList as dicts; other opcodes are skipped.
    Opcode lists are big-endian whatever the file's byte order (DNG spec 1.4, chapter 7)."""
    import struct
    if opcodes is None:
        return []
    b = bytes(opcodes)
    count, off, out = struct.unpack(">I", b[:4])[0], 4, []
    for _ in range(count):
        oid, _ver, _flags, size = struct.unpack(">IIII", b[off:off + 16])
        p = b[off + 16:off + 16 + size]
        if oid == 9:
            top, left, bottom, right, plane, planes, rp, cp, mv, mh = struct.unpack(">10I", p[:40])
            sv, sh, ov, oh = struct.unpack(">4d", p[40:72])
            mp = struct.unpack(">I", p[72:76])[0]
            gains = np.array(struct.unpack(">%df" % (mv * mh * mp), p[76:76 + 4 * mv * mh * mp]), float)
            out.append(dict(area=(top, left, bottom, right), plane=plane, pitch=(rp, cp), spacing=(sv, sh),
                            origin=(ov, oh), gains=gains.reshape(mv, mh, mp)[:, :, 0]))
        off += 16 + size
    return out


def apply_gain_maps(img, maps):
    """Multiply a single-plane raw (normalized) by its GainMaps, bilinear between map points and clamped
    at the edges. Map coordinates are relative to the whole image, as in the DNG SDK."""
    out = np.array(img, dtype=np.float64)
    h, w = out.shape
    for m in maps:
        top, left, bottom, right = m["area"]
        rp, cp = m["pitch"]
        rows, cols = np.arange(top, min(bottom, h), rp), np.arange(left, min(right, w), cp)
        g = m["gains"]
        fv = np.clip((rows / h - m["origin"][0]) / m["spacing"][0], 0, g.shape[0] - 1)
        fh = np.clip((cols / w - m["origin"][1]) / m["spacing"][1], 0, g.shape[1] - 1)
        v0, h0 = np.minimum(fv.astype(int), g.shape[0] - 2), np.minimum(fh.astype(int), g.shape[1] - 2)
        wv, wh = (fv - v0)[:, None], (fh - h0)[None, :]
        along = g[:, h0] * (1 - wh) + g[:, h0 + 1] * wh               # map rows x image columns
        grid = along[v0] * (1 - wv) + along[v0 + 1] * wv
        out[top:min(bottom, h):rp, left:min(right, w):cp] *= grid
    return out


class Dng:
    """Raw planes of a DNG plus the DNG-spec camera-to-XYZ(D50) matrix built from its own tags."""

    def __init__(self, path):
        import rawpy
        with rawpy.imread(path) as r:
            self.raw = r.raw_image_visible.astype(np.float32)
            self.colors = r.raw_colors_visible.copy()
            self.desc = r.color_desc.decode()
            self.black = float(np.mean(r.black_level_per_channel))
            self.white = float(r.white_level)
        self.tags = dng_tags(path)
        self.neutral = np.array(self.tags.get(50728, (1.0, 1.0, 1.0)), float)

    def channel_of(self, index):
        return "RGB".index(self.desc[index]) if self.desc[index] in "RGB" else None

    def norm(self, shading=True):
        """Linear 0..1 raw; with shading, the DNG's own lens-shading GainMaps (OpcodeList2) applied, as a
        raw developer must (phone raw is not shading-corrected; the correction travels in the file).
        Noise and range use self.raw directly: the gains would rescale the noise."""
        n = (self.raw - self.black) / (self.white - self.black)
        return apply_gain_maps(n, gain_maps(self.tags.get(51009))) if shading else n

    def plane_means(self, region):
        """Mean R, G, B (normalized) of a boolean pixel region."""
        n = self.norm()
        out = np.zeros(3)
        for ch in range(3):
            m = region & np.isin(self.colors, [i for i in range(len(self.desc)) if self.channel_of(i) == ch])
            out[ch] = n[m].mean()
        return out

    def half_res_rgb(self):
        """Each 2x2 quad -> one camera RGB pixel (normalized, no white balance)."""
        n = self.norm()
        h, w = n.shape[0] // 2 * 2, n.shape[1] // 2 * 2
        out = np.zeros((h // 2, w // 2, 3), np.float32)
        cnt = np.zeros(3)
        for dy in (0, 1):
            for dx in (0, 1):
                ch = self.channel_of(int(self.colors[dy, dx]))
                out[..., ch] += n[dy:h:2, dx:w:2]
                cnt[ch] += 1
        return out / cnt

    def wb_mosaic(self, from_scene=False):
        """Full-resolution mosaic divided by a neutral: for a grey target this is a monochrome image.
        from_scene: each CFA site divided by its own paper level (90th percentile), for a black-and-white
        target under any light; the camera's neutral left a red/blue checkerboard under a warm lamp with
        the Daylight preset (6T edge sheet, 2026-10-23: 1 usable edge of 16)."""
        n = self.norm()
        if from_scene:
            lut = np.array([np.percentile(n[self.colors == i], 90) for i in range(len(self.desc))])
        else:
            lut = np.array([self.neutral[self.channel_of(i)] for i in range(len(self.desc))])
        return n / lut[self.colors]

    def camera_to_xyz_d50(self):
        t = self.tags
        cm = [np.array(t[k], float).reshape(3, 3) for k in (50721, 50722) if k in t]
        fm = [np.array(t[k], float).reshape(3, 3) for k in (50964, 50965) if k in t]
        temps = [illuminant_cct(t.get(k, (21,))[0]) for k in (50778, 50779)]
        g = interpolation_weight(cm, temps, self.neutral)
        mix = lambda ms: ms[0] if len(ms) == 1 else g * ms[0] + (1 - g) * ms[1]
        if fm:
            return mix(fm) @ np.diag(1 / self.neutral)
        cam_to_xyz = np.linalg.inv(mix(cm))
        white = cam_to_xyz @ self.neutral
        return bradford(white / white[1], D50) @ cam_to_xyz


LIGHT_SOURCE_CCT = {1: 5500, 2: 4200, 3: 2850, 4: 5500, 9: 5500, 10: 6500, 11: 7500, 12: 6430, 13: 5000,
                    14: 4150, 15: 3525, 17: 2856, 18: 4874, 19: 6774, 20: 5503, 21: 6504, 22: 7504,
                    23: 5003, 24: 3200}


def illuminant_cct(code):
    return LIGHT_SOURCE_CCT.get(int(code), 6504)


def xy_to_cct(x, y):
    n = (x - 0.3320) / (0.1858 - y)
    return 449 * n ** 3 + 3525 * n ** 2 + 6823.3 * n + 5520.33


def interpolation_weight(cms, temps, neutral):
    """Weight of matrix 1 (DNG spec: linear in 1/CCT between the two calibration illuminants)."""
    if len(cms) < 2 or temps[0] == temps[1]:
        return 1.0
    g = 0.5
    for _ in range(8):
        xyz = np.linalg.inv(g * cms[0] + (1 - g) * cms[1]) @ neutral
        x, y = xyz[0] / xyz.sum(), xyz[1] / xyz.sum()
        cct = min(max(xy_to_cct(x, y), 2000), 12000)
        g = (1 / cct - 1 / temps[1]) / (1 / temps[0] - 1 / temps[1])
        g = min(max(g, 0.0), 1.0)
    return g
