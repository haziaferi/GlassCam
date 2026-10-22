"""Write the bench's targets: monitor pages (colour chart, flat greys) and a printable edge sheet.

    python tools/bench/chart.py [OUT_DIR]        # default bench_charts/ (gitignored)

Open a page in a browser on the monitor or the display phone and tap or click it once: it goes full
screen (F11 also works on the monitor). edges.html is for the
black-and-white printer: print at 100 % scale (no "fit to page"), A4 landscape.
The geometry comes from imaging.py, so the scorer always looks where the page drew.
"""
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import imaging as im  # noqa: E402

PAGE = """<!doctype html><html><head><meta charset="utf-8"><title>{title}</title><style>
html,body{{margin:0;height:100%;overflow:hidden;cursor:none;background:{bg}}}
div{{position:fixed}}</style></head><body>{body}
<script>document.addEventListener("click",function(){{var e=document.documentElement;
(e.requestFullscreen||e.webkitRequestFullscreen).call(e)}})</script></body></html>
"""


def rgb(c):
    return "rgb(%d,%d,%d)" % tuple(c)


def colour_page():
    divs = []
    for i, (_, c) in enumerate(im.PATCHES):
        x0, y0, x1, y1 = im.patch_rect(i)
        divs.append(f'<div style="left:{x0 * 100:.3f}vw;top:{y0 * 100:.3f}vh;width:{(x1 - x0) * 100:.3f}vw;'
                    f'height:{(y1 - y0) * 100:.3f}vh;background:{rgb(c)}"></div>')
    bg = (im.CHART_BACKGROUND,) * 3
    return PAGE.format(title="Bench colour chart", bg=rgb(bg), body="".join(divs))


def flat_page(level):
    return PAGE.format(title=f"Bench flat {level}", bg=rgb((level,) * 3), body="")


def edges_page():
    """The squares are SVG shapes, not coloured boxes: browsers drop background colours when printing
    unless "background graphics" is ticked, and the first version printed blank (owner, 2026-10-22)."""
    side_mm, gap_x, gap_y = 70, 140, 95
    squares = []
    for row in (0, 1):
        for col in (0, 1):
            cx = 148.5 + (col - 0.5) * gap_x
            cy = 105 + (row - 0.5) * gap_y
            squares.append(f'<rect x="{cx - side_mm / 2}" y="{cy - side_mm / 2}" width="{side_mm}" '
                           f'height="{side_mm}" transform="rotate({im.EDGE_ANGLE_DEG} {cx} {cy})"/>')
    return f"""<!doctype html><html><head><meta charset="utf-8"><title>Bench edge sheet</title><style>
@page {{size: A4 landscape; margin: 0}}
html,body{{margin:0;background:#fff}}
svg{{display:block;width:297mm;height:210mm}}
</style></head><body><svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 297 210" fill="#000">{''.join(squares)}</svg>
</body></html>
"""


def index_page(levels):
    flats = "".join(f'<li><a href="flat_{lv}.html">flat {lv}</a></li>' for lv in levels)
    return f"""<!doctype html><html><head><meta charset="utf-8"><title>Bench targets</title></head><body
style="font:16px sans-serif;max-width:40em;margin:2em auto;line-height:1.5">
<h1>Bench targets</h1>
<p>Dark room; the monitor in its sRGB mode at a fixed brightness; tap or click each page once to make it full screen.</p>
<ol>
<li><b>Dark frames</b>: lens covered, two DNGs.</li>
<li><b>Flat fields</b> (phone close to the screen, focus at its nearest so the screen is blurred,
the screen filling the frame): two shots of each level, JPEG and DNG.<ul>{flats}</ul></li>
<li><b>Colour chart</b> (in focus, the whole screen in the frame): three shots.
<a href="colour.html">colour.html</a></li>
<li><b>Edges</b>: <a href="edges.html">edges.html</a> printed at 100 % on A4, evenly lit, the whole sheet in
the frame, in focus: three shots.</li>
</ol></body></html>
"""


def main(out):
    os.makedirs(out, exist_ok=True)
    files = {"index.html": index_page(im.FLAT_LEVELS), "colour.html": colour_page(), "edges.html": edges_page()}
    files.update({f"flat_{lv}.html": flat_page(lv) for lv in im.FLAT_LEVELS})
    for name, text in files.items():
        with open(os.path.join(out, name), "w", encoding="utf-8", newline="\n") as fh:
            fh.write(text)
    print(f"wrote {len(files)} pages to {out}")


if __name__ == "__main__":
    main(sys.argv[1] if len(sys.argv) > 1 else os.path.join(os.path.dirname(__file__), "..", "..", "bench_charts"))
