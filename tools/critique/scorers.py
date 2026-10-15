"""Computable scorers for the GlassCam shutter-column layout search (meta-optimizer).

Geometry: OnePlus 9 Pro landscape, 804 x 360 dp. The 4:3 preview is centred, 480 dp wide,
leaving a 162 dp band each side; the left band loses 32 dp to the camera cutout. The shutter
sits right-centre. Controls occupy a 2-column grid of cells around it: `rows_above` rows above,
`4 - rows_above` rows below, filled left-to-right, top-to-bottom from the `order` permutation.

Every scorer returns [0, 1], higher is better. Nothing here is a judgement; the judgement
about whether the winner is worth the disruption is made in the critique.
"""
import math

SCREEN_W, SCREEN_H = 804.0, 360.0
PREVIEW_W = 480.0
BAND = (SCREEN_W - PREVIEW_W) / 2          # 162 dp each side
CUTOUT = 32.0                              # left band only
SEEKBAR_W = 50.0
STRIP_H = 50.0
CELL_GAP = 4.0

# taps per capture session, from the walk's task list (module/gallery/flash dominate)
FREQ = {"module": 3.0, "gallery": 3.0, "flash": 1.5, "ael": 1.0, "focus": 1.0,
        "histo": 1.0, "timer": 0.5, "camswitch": 0.5}
# controls that change together and are looked for together
PAIRS = [("flash", "ael"), ("focus", "timer"), ("module", "gallery")]
# right thumb anchor when the phone is held two-handed in landscape
THUMB = (SCREEN_W - 24.0, SCREEN_H * 0.55)


def _cells(vector):
    """Cell centre (x, y) and size for each slot index 0..7."""
    shutter = float(vector["shutter_dp"])
    cell = float(vector["column_dp"])
    rows_above = int(vector["rows_above"])
    col_w = 2 * cell + CELL_GAP
    right_edge = SCREEN_W - 8.0
    x_left = right_edge - col_w + cell / 2
    x_right = right_edge - cell / 2
    sy = SCREEN_H / 2                      # shutter centre y
    out = []
    for r in range(rows_above):
        y = sy - shutter / 2 - CELL_GAP - (rows_above - r - 0.5) * (cell + CELL_GAP)
        out.append((x_left, y, cell)); out.append((x_right, y, cell))
    for r in range(4 - rows_above):
        y = sy + shutter / 2 + CELL_GAP + (r + 0.5) * (cell + CELL_GAP)
        out.append((x_left, y, cell)); out.append((x_right, y, cell))
    return out


def _fitts(x, y, w):
    d = math.hypot(x - THUMB[0], y - THUMB[1])
    return math.log2(d / w + 1.0)


def reach(vector, context):
    """Frequency-weighted Fitts index of difficulty from the right thumb, normalised."""
    cells = _cells(vector)
    order = list(vector["order"])[:8]
    worst = math.log2(math.hypot(SCREEN_W, SCREEN_H) / 24.0 + 1.0)
    num = den = 0.0
    for slot, item in enumerate(order):
        x, y, w = cells[slot]
        f = FREQ.get(item, 0.5)
        num += f * _fitts(x, y, w)
        den += f * worst
    # "worst" assumes a 24 dp target; a cell narrower than that can exceed it, so clamp to keep
    # the documented [0, 1] range (fresh-eyes 2026-09-28: column_dp < 24 went negative)
    return max(0.0, 1.0 - num / den)


def grouping(vector, context):
    """Fraction of related pairs that sit in adjacent cells (same row, or same column one row apart)."""
    order = list(vector["order"])[:8]
    pos = {item: (i // 2, i % 2) for i, item in enumerate(order)}
    hit = 0
    for a, b in PAIRS:
        if a in pos and b in pos:
            (ra, ca), (rb, cb) = pos[a], pos[b]
            if (ra == rb and abs(ca - cb) == 1) or (ca == cb and abs(ra - rb) == 1):
                hit += 1
    return hit / len(PAIRS)


def preview_clear(vector, context):
    """1 - fraction of the preview covered by chrome (column, seekbar, manual strip)."""
    cell = float(vector["column_dp"])
    shutter = float(vector["shutter_dp"])
    right_footprint = max(shutter, 2 * cell + CELL_GAP) + 8.0
    if vector["seekbar_side"] == "right":
        right_footprint += SEEKBAR_W + CELL_GAP
        left_footprint = 0.0
    else:
        left_footprint = SEEKBAR_W + CELL_GAP
    covered = 0.0
    covered += max(0.0, right_footprint - BAND) * SCREEN_H
    covered += max(0.0, left_footprint - (BAND - CUTOUT)) * SCREEN_H
    if vector["strip_pos"] == "bottom":
        covered += STRIP_H * PREVIEW_W          # the strip lies across the preview's bottom edge
    elif vector["strip_pos"] == "top":
        covered += STRIP_H * PREVIEW_W * 0.5    # shares the band with the info overlay
    # left: the strip stands in the left letterbox band and covers nothing
    return max(0.0, 1.0 - covered / (PREVIEW_W * SCREEN_H))


def targets(vector, context):
    """Touch-target compliance: every cell is column_dp square; 48 dp is the floor."""
    cell = float(vector["column_dp"])
    return min(1.0, cell / 48.0) ** 2


def strip_reach(vector, context):
    """The manual strip is used constantly; bottom is under the thumbs, left is far from the right thumb."""
    return {"bottom": 1.0, "left": 0.55, "top": 0.35}[vector["strip_pos"]]


SCORERS = {
    "glasscam.reach": reach,
    "glasscam.grouping": grouping,
    "glasscam.preview_clear": preview_clear,
    "glasscam.targets": targets,
    "glasscam.strip_reach": strip_reach,
}
