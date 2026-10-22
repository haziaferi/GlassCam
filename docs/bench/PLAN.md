# Image-quality bench: plan (2026-10-22)

**Status:** the scoring tools and their self-test are built (`tools/bench/`, 19 checks, each also failing on
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
| `selftest.py` | 19 checks on synthetic images, including a whole JPEG run through `score.py`, the display pairing, the gain maps and `shoot.py`'s helpers |
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

## shoot.py and the first calibration (2026-10-22)

`shoot.py configure` sets GlassCam's camera 0 through its config file, never by tapping. With the app
stopped it sets: JPEG + 16-bit DNG, auto-exposure off, a manual ISO and shutter, manual focus at a
distance, a white-balance preset (the 6T offers no manual colour temperature), and the processing modes.
The phone's own settings are saved to `bench_runs/settings_backup/` on the first run; `shoot.py restore`
puts them back. `show` puts a target on the display phone over USB (`adb reverse`, the PC serves
`bench_charts/` on localhost only) and taps it full screen. `shot` fires GlassCam with the camera key and
pulls the JPEG + DNG pair. `record` writes `run.json` from the display phone's live settings. Photos stay
on the phone; the bench only copies them.

The 6T on the Nord, read back from GlassCam's screen: Jpg+16bitDng, focus Off (manual 16.1 cm). A test
shot of `flat_245` at ISO 100 and 1/30 s came out unclipped: green mean 0.44 of the raw range, red 0.25,
blue 0.28. There's no flicker banding: once the falloff is removed, row means vary by 0.37 % and column
means by 0.53 %. So the baseline runs at ISO 100, 1/30 s.

**Correction (same night):** that test shot was really exposed at 1/92 s, not 1/30 s; see the first run below.

## First run, 2026-10-22 23:48: a dry run, not a baseline

The 6T against the Nord, dark room: 2 dark frames, 8 flat levels × 2 and 3 colour shots, all pulled and
scored. The files' own ExposureTime tags show **the exposure was not fixed**. ISO held at 100, but the
time drifted from 1/92 to 1/30 to 1/45 s, because GlassCam took the photos with auto-exposure on
(SPEC §11 item 16). The run checked the bench itself, and it found five errors in the scorer, all fixed
the same night:

| Found on the real files | Fix |
|---|---|
| Read noise 1.57 DN from the whole frame: a light leak at one edge | Dark frames use the same centre crop as the flats: **0.72 DN** |
| The 6T's DNG BlackLevel tag says 63; the sensor sits at 63.9 | The black level is measured in the dark frames, and both values are reported (item 2 finding) |
| 146,084 "hot pixels" (leak + black offset) | Counted in the centre against the measured black: **0** |
| The unweighted noise fit put its floor at 0.20 DN² against 0.52 measured | Weighted 1/variance: **0.48** |
| The Nord's full white gave less light than its grey 180 (OLED power limiting), and it crushes levels 4 and 8 | JPEG dynamic range uses the light the DNG measured at each level, not nominal sRGB |
| Two flat pairs had different exposures inside the pair | A pair is left out unless both shots match in ExposureTime and ISO |

The colour score now reports ΔE as shot, ΔE after white balance on the grey patches (Imatest/DXOMARK
practice), and the cast. The cast is large and comes from fixed choices: the Daylight preset (about
5500 K) against the screen's D65, and the Nord's slightly magenta white. As-shot ΔE is JPEG 9.4 and DNG
10.2; after white balance it is JPEG 5.5 and DNG 7.4.

Noise profile check (list item 2): the 6T's per-shot NoiseProfile is right to within about 10 %
(tag S 1.14e-4 / O 6.0e-7, measured 1.24e-4 / 5.3e-7).

Still to do before the real baseline: (1) fix GlassCam's manual shutter (§11 item 16); (2) apply the
DNG's lens-shading GainMap (OpcodeList2) in the bench's developer, because the DNG colour is otherwise
scored on uncorrected shading, which differs per shot.

## 6T baseline, 2026-10-23 00:40 (4.3.101, the Nord's screen, dark room)

The first valid run. All 34 shots, settle shots included, carry 1/30 s and ISO 100. Each target got an
8 s wait and one settle shot, the DNGs are shading-corrected, and the Nord's Smart Sidebar (an overlay
that screenshots leave out, but the camera sees) was turned off. Scores in `baselines/6T_2026-10-23_scores.json`.

| | JPEG | DNG |
|---|---|---|
| Noise, SNR at 18 % grey | 37.3 dB | 31.6 dB |
| Colour ΔE as shot (spread of 3) | 9.01 (0.03) | 9.60 (0.04) |
| Colour ΔE after white balance (spread) | 5.47 (0.05) | 6.68 (0.07) |
| Cast on the greys, a* / b* | +8.1 / −14.4 | +3.9 / −9.0 |
| Dynamic range | 10.1 stops | 10.4 stops |
| Sharpness, MTF50 (spread of 3) / overshoot | 0.244 (0.014) / 7.8 % | 0.098 (0.001) / 1.8 % |

Raw read noise is 0.717 DN (0.723 in the dry run). The noise fit's floor agrees with the dark frames. The
6T's NoiseProfile is within about 8 %. Its DNG BlackLevel tag still says 63, against 63.9 measured.
Shading maps agree across the chart repeats (corner gain 3.15-3.19). The JPEG crushes its darkest
levels: grey 4 is below SNR 1, and grey 8 reads cleaner than grey 16 because the tone curve and noise
reduction flatten near-black.

### Edge sheet, first attempt (2026-10-23, 01:00): not usable yet

The sheet was hand-held, and the owner's hands shake. The framing moved between checks, and a 1 s exposure
(needed under a dim lamp at ISO 100) blurred: JPEG MTF50 0.057 at 1 s against 0.10-0.13 at 1/10 s, and
the two 1/10 s shots differed by 25 %. Next time, the sheet rests on something solid (taped to a wall or
a box, or flat on the desk with the 6T looking down) and nobody touches it during the shots.

The attempt fixed four bench faults:

- GlassCam's single-shot "Auto" focus is never triggered by its capture path, so it shot out of focus.
  `shoot.py --focus auto` now means continuous AF, which the capture waits on.
- The square finder used half the paper level as its threshold, and that merged a square with the
  lamp-dimmed paper at the sheet's edge. It now thresholds at the geometric middle of ink and paper.
- The DNG mosaic was balanced with the camera's Daylight neutral under a warm lamp, which left a red/blue
  checkerboard. For the black-and-white sheet, each CFA site is now divided by its own paper level.
- The edge locator used the gradient centroid over the whole row, which mottled toner pulls off the edge.
  It now uses the halfway crossing nearest the window's centre, refined on a window. A self-test case
  with mottled ink fails on the old locator (0.123 against 0.180) and passes on the new one.

Lamp light flickers at 100 Hz (50 Hz mains), so exposures are whole multiples of 10 ms: 1/100, 1/25,
1/10 s or longer. GlassCam offers no 1/50.

### Edge sheet, second attempt (2026-10-23, 01:12): done

The sheet stood upright on a board, with nothing hand-held; two views 5 s apart showed no movement. The
paper sat at 8 % of the raw range at ISO 100 and 1/10 s, so the shots used 0.8 s (paper 0.79, a whole
number of 100 Hz lamp cycles) with continuous AF: a settle shot and three repeats. Every repeat gave 15
usable edges. JPEG MTF50 0.252 / 0.238 / 0.243 with 7.7-8.1 % overshoot; DNG 0.099 / 0.098 / 0.098 with
1.8-1.9 %. The JPEG's 2.5x and its halo are the 6T's built-in sharpening (edge mode Fast), which list
item 1 will be measured against.

The square finder now thresholds by Otsu on log brightness. The midpoint of the darkest 5 % and the paper
fell into the ink once a black desk was in view (0 squares). A self-test with a black desk beside a
blotchy-ink sheet fails on that midpoint and passes with Otsu. 18 checks.
