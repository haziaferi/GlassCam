# Image-quality bench: plan (2026-10-22)

**Status:** the scoring tools and their self-test are built (`tools/bench/`, 14 checks, each also failing on
a planted error; in CI). Still to build: `shoot.py`, which drives the phone. It's built on the first
connected phone, by watching the running app rather than guessing its screens. After that comes the
baseline run.

Purpose: give every image-processing change (the list in SPEC §11 once it is adopted: HQ modes, DNG
metadata, bracket merge, burst merge, new formats) a number instead of an impression.

## Owner's choices (2026-10-22)

| Question | Answer |
|---|---|
| Axes | noise, colour accuracy, sharpness/detail, dynamic range: all four |
| Scene | a chart on a monitor (no colour printer, no physical chart); re-check later with a real chart |
| Phones | all three, one at a time (6T, Nord CE4 Lite, 9 Pro) |
| Outputs | JPEG and DNG (the DNG developed by one fixed developer on the PC) |
| Sources | Stanford CS348K lecture 2 (slides 54-101), UNC COMP 590/776 lecture 4 (slides 13-109) |
| Printer | black-and-white: the slanted edges are printed (`edges.html`, A4 landscape at 100 %) |
| Judging | the photos are pulled to the computer and scored from their pixel values, never by comparing how two screens show them. The monitor is only the light source; its own colour error is the same in every run, so it cancels when two runs are compared |

The axes are reported side by side and never summed: a change is accepted when it improves at least one
axis and makes no other one worse by more than that axis's measured repeatability. This needs no
weights, so there are none to choose.

## What the lectures change in the design

- **Measure in raw, linear space where possible.** Raw values are linear in scene light
  (`I = clip[t·L + noise]`, UNC 79-80), while JPEG/sRGB values describe "the photo, not the scene" and
  differ per phone and per picture style (UNC 61-63, 104). So noise and dynamic range are measured from
  the DNG's linear data; the JPEG is scored as the user sees it, and is never used to infer sensor
  facts.
- **Dynamic range does not need a high-contrast scene.** A monitor reaches about 1000:1 (10 stops),
  less than the sensors. Engineering dynamic range = (white level − black level) / read-noise σ, both
  measured in raw: white and black levels from the DNG tags, and read noise from a dark frame (lens
  covered). The UNC exposure-stack merge (valid pixels 0.05 < p < 0.95, weight by 1/t) is the method
  for list item 3, not for the bench.
- **Noise as a curve, not one number.** Noise variance grows with signal (shot noise) on top of a read
  noise floor (UNC 13-14, Stanford "measurement noise"). From flat fields at several brightness levels
  we fit `variance = a·signal + b`. That is exactly what the DNG `NoiseProfile` tag should contain, so
  the bench also checks list item 2: do GlassCam's DNGs carry the phone's real noise model?
- **Sharpness by slanted edge** (MTF50), plus the edge's **overshoot**: the 3x3 sharpen kernel on
  Stanford 91 is what makes halos, so oversharpening shows up as overshoot, not as "sharper".
- **Demosaic artefacts** (Stanford 60-65: false colour on fine stripes and diagonals) are a diagnostic
  on a fine-line target: reported, not scored, because they're not one of the four axes.
- **Colour in two places:** the DNG developed with the camera's own matrices (UNC 43-45: white balance
  diagonal, then a colour-space transform interpolated between two illuminants by CCT) measures the
  metadata; the JPEG measures the phone's photo-finishing.

## Monitor-specific traps and their fixes

| Trap | Fix |
|---|---|
| The monitor's pixel grid makes moiré, which reads as noise on flat patches | Noise flat fields are shot **defocused** (manual focus at the nearest distance), which blurs the grid away; that is the standard way to get flat fields |
| Backlight PWM / refresh makes banding at short exposures | Exposure time fixed at a multiple of the refresh period (1/30 s or 1/15 s on the 59.94 Hz monitor: whole refreshes); bands are checked for on the flat field |
| Room light reflects on the screen | A dark room; the monitor is the only light source |
| The monitor's colours are not true | Set to sRGB mode and a fixed brightness. Colour error is relative to what the monitor shows, so it compares changes but is not an absolute score. Re-baseline later with a real chart |
| The phone moves between shots | On the stand, shutter fired by `adb` (the camera key, S-31), with no touch |
| Auto modes change between shots | Manual ISO, shutter, white balance and focus, saved per phone in the bench run file; only the variable under test changes |

## Shots per configuration (one "run")

1. **Dark frame** (lens covered), DNG: black level, read noise, hot pixels (diagnostic for the
   hot-pixel mode).
2. **Flat fields**, defocused, at 6 grey levels shown full screen, DNG + JPEG: the noise curve, and
   vignetting / lens shading as a diagnostic (list item 2: the lens-shading map).
3. **Chart**, in focus, DNG + JPEG: the 24 ColorChecker colour patches, drawn from published sRGB
   reference values (the bench generates the page; no image is needed from the repo); 4 slanted edges
   (about 5°) at centre and corners; one fine-line patch for demosaic artefacts.
4. Each shot taken **3 times**: the spread between them is the axis's repeatability, which sets the
   "worse by more than" threshold above.

## Scores

| Axis | JPEG | DNG (fixed developer, camera matrices, no auto-brightening) |
|---|---|---|
| Noise | SNR on the flat greys (dB) at 18 % grey | read noise (DN), shot-noise gain, SNR at 18 % grey; fitted `a`, `b` vs the file's NoiseProfile |
| Colour | mean and max ΔE2000 over 24 patches | the same, after developing |
| Sharpness | MTF50 (cycles/pixel) per edge, overshoot % | the same, after a fixed demosaic |
| Dynamic range | stops from the darkest grey step still above SNR 1 to clipping | log2((white − black) / read σ) |

## Tools (tools/bench/, Python; built after this plan is approved)

- `chart.py`: writes the chart page and the flat-field pages as full-screen HTML for the PC monitor.
- `shoot.py`: drives the phone with `walk.py`'s adb helpers: sets the manual values, fires the shutter
  with the camera key, waits for the files, pulls them into `bench_runs/<phone>/<run>/`.
- `score.py`: computes the table above for one run; writes `scores.json`.
- `compare.py`: compares two runs per axis against their repeatability; says better / same / worse.
- A self-test on synthetic images with known noise, edge blur and colour shift: each score must recover
  the planted value. That is the bench's version of the mutant proof, and it runs in CI.

Run outputs (photos) stay out of git (`bench_runs/` in .gitignore); scores.json files are small and can
be kept as the baseline.

## Order

1. Build the tools and their self-test (no phone needed).
2. Baseline run on whichever phone is connected: GlassCam as it is, with JPEG + DNG.
3. Repeatability check: the same run twice; set the thresholds.
4. Only then list item 1 (HQ noise reduction/edge/hot-pixel for the photo), measured against the
   baseline; then item 2 (DNG metadata), whose NoiseProfile and colour checks the bench already makes.

## Built so far

| File | Does |
|---|---|
| `imaging.py` | the measurements: CIEDE2000, sRGB/XYZ/Lab, the DNG-spec camera-to-XYZ matrix from the file's own tags (ColorMatrix/ForwardMatrix interpolated by CCT), screen location + homography + orientation search, photon-transfer fit, slanted-edge MTF50 and overshoot, square finder |
| `chart.py` | writes `bench_charts/`: `colour.html`, `flat_<level>.html` for 8 levels, `edges.html` (print), `index.html` (the shot list) |
| `score.py RUN_DIR` | scores the pulled photos into `RUN_DIR/scores.json` |
| `compare.py BASE CAND` | better / same / worse per axis against the repeat spread; accept or reject |
| `selftest.py` | 14 checks on synthetic images, including a whole JPEG run through `score.py` and the display pairing |
| `displays.json`, `runinfo.py` | the display pairing, set once, and the check of each run's `run.json` against it |
| `requirements.txt` | numpy 2.5.4, Pillow 12.2.0, rawpy 0.27.1 (LibRaw 0.22.1, only to unpack sensor values), pinned |

Checked beyond the self-test: the real `colour.html`, rendered by a browser at 16:9 and 4:3 and scored, gives
ΔE 0.000. That round trip found a real bug the synthetic chart had missed (a screen filling the frame lost
its background), which is now its own self-test case. The rendered `edges.html` yields 16 usable edges. On
the two DNGs from the 4.3.100 A/B test, each file's neutral maps to D50 white through its own matrices.

## Displays: paired once (owner, 2026-10-22)

The chart and the flat greys are shown on an OLED phone screen in its sRGB mode for within-phone runs,
because OLED colour barely shifts with viewing angle and its blacks are true blacks. The L2151w is most
likely a TN panel, whose colour changes with angle across the screen. The pairing never changes:

| Camera | Paired screen (within-phone verdicts) |
|---|---|
| OnePlus 6T | Nord CE4 Lite screen |
| OnePlus Nord CE4 Lite | 6T screen |
| OnePlus 9 Pro | 6T screen |

The **monitor is the shared target**: two different phones are compared with each other only on runs
shot against it, because paired screens differ between cameras and would bias such a comparison.

Every run folder has a `run.json` (camera, target "pair" or "shared", display, the display's settings).
`score.py` checks it against `tools/bench/displays.json` and keeps the result in `scores.json`;
`compare.py` refuses (exit 2) two runs whose display or settings differ, or two phones on paired screens.

Settings recorded for the 6T's screen (read from the phone, 2026-10-22): Screen Calibration →
Advanced → sRGB, Cold–Warm slider `oem_screen_better_value` = 60. It stays at 60: what counts is that it
never moves. Whether 60 or the centre is the factory D65 point can't be told without a colorimeter.
Night Light must be off: on the 6T it is set to turn on automatically from sunset to sunrise, which would
warm the screen during an evening session. Brightness at maximum with auto-brightness off, which also
keeps OLED flicker (PWM dimming) at its weakest.

The Nord's screen (read 2026-10-22): its colour mode is vendor mode 256, which Android's own Colors page
can only list as "Reserved and unset: 256". The display system shows what it does: both phones render in
**ColorMode::SRGB**, with an identity colour transform and Night display and saturation boost off. So
the Nord is already in the equivalent of the 6T's sRGB mode, and nothing needs changing. The two screens
still differ in panel and factory calibration (and in the 6T's slider, which works in the panel and
doesn't show in the transform). The pairing makes that harmless: each camera always sees the same screen.
Read on the Nord's own eye-comfort page (2026-10-22, opened from its Quick Settings tile, nothing
changed): eye comfort was off, but its **schedule switches it on at 22:00**, in black-and-white mode. A
session either ends before 22:00 or has the schedule turned off first. The page has **no low-flicker
option**. The eye-comfort app lists its low-flicker entries (`single_pulse`, `dc_rm`) as `nonIndexKey`,
the settings it withholds from search because the device doesn't offer them. The Nord's Display page
showed brightness 100 %, adaptive brightness off, and Night Light off and never automatic.

## The monitor (owner, 2026-10-22)

L2151w, 1920×1080 at 59.94 Hz, 8 bit, SDR. Two settings follow from it:

- The graphics card sends **YCbCr 4:4:4**. Any colour shift it causes is the same in every run, so it
  cancels when two runs are compared (owner's point). What would not cancel is limited range merging the
  darkest flat levels (4, 8, 16) into one black: those shots would then carry no information, and the
  JPEG dynamic range would be capped identically in every run, blind to shadow changes. So it is
  measured, not assumed: if the baseline's raw means for levels 4 < 8 < 16 are distinct, YCbCr stays;
  if they merge, switch to RGB 4:4:4 full range (AMD Software: Display → Pixel Format). Either way the
  setting must stay the same for every run.
- One refresh lasts 16.68 ms, so flat fields and the chart are shot at **1/30 s or 1/15 s** (whole
  refreshes) to avoid banding from the backlight; 1/60 s is not quite one refresh.

Open: whether the monitor's own menu has an sRGB or 6500 K preset. If it has neither, the bench still
works, because every run is compared against another run shown on the same monitor.

The edge sheet's first version printed blank: its squares were coloured boxes, and browsers drop
background colours when printing. They're SVG shapes now, proven by printing both versions to PDF with
background graphics off. `bench_charts/edges.pdf` is the printable sheet: print at 100 %, A4 landscape.
