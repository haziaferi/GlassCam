# GlassCam — fresh-eyes audit, 2026-09-28 (builds 4.3.90 and 4.3.91)

A search for logical fallacies and inconsistencies across the whole project: docs against docs,
docs against code, the tooling's own logic, and the code's logic. Every gap that needed no decision
is fixed in 4.3.90; the owner then answered the questions, and 4.3.91 builds those answers; what is
still open is SPEC.md §11. Per-finding data (all 235 model findings plus 10 found while fixing, each with
its disposition, reason and, for a decision, what became of it): `fresh-eyes-2026-09-28.json`.

## Summary

| | count |
|---|---|
| findings raised by the models | 235 (chunk lanes 194, whole-document lane 34, cross-file lane 7) |
| verifier verdicts | 174 confirmed, 42 narrowed, 19 refuted |
| **fixed** in 4.3.90 | **145** + 8 found while fixing (two of them in 4.3.91) |
| needed your decision | 14 findings: **11 decided and built in 4.3.91**, 3 still open (SPEC §11 items 6, 8, 9) |
| owed on the phone | 1 finding (C7's axis) + the verification of the fixes, below |
| inherited upstream code, registered only | 9 |
| no impact (dead code, inert attributes, residual gate weakness) | 14 + 2 found while fixing |
| editorial (a cause or guarantee stated in a comment without evidence; left as written) | 10 |
| withdrawn | 42 (19 by the verifier, 23 by hand with the evidence named) |

Gates after 4.3.91: `tools/spec_check.py` 25 checks, 0 failing; `tools/spec_check_mutants.py` 15 of 15
planted defects caught, control green; `walk.py build` BUILD SUCCESSFUL, exit 0 read from `logs/build.log`.

## How it was run

1. **The reviewer prompt was measured on this repo first.** Seed corpus `~/.claude/skills/fresh-eyes-audit/references/seed_glasscam.py`:
   7 real excerpts (SPEC, CRITIQUE, spec_check.py, ActivityAbstract, LevelBarView), 12 planted defects
   (unit slip inside the row that states the unit rule, px→dp arithmetic, a gate that compares SPEC with
   itself, a comment left over a removed guard, …) and 7 near-miss controls. Sonnet, one sample per case,
   scored by `promptlab`:

   | prompt | plants caught | controls flagged |
   |---|---|---|
   | skill reviewer v1 | 8/12 | 0/7 |
   | + Android units/lifecycle wording | 9/12 | 0/7 |
   | **+ "do not stop after the first finding in a paragraph"** (used) | **10/12, re-sampled 9/12** | 0/7 |
   | + universal-claim clause | 8/12 | 0/7 |
   | skill reviewer v2 (another session's) | 8/12, twice | 0/7 |

   The two plants no prompt caught (a value breaking a unit rule stated beside it; a literal list claimed as
   "every") became deterministic gates instead: S-22 and S-23.
2. **Chunk lanes** (sonnet reviewer, opus verifier): 158 chunks of tier A — every code file touched since the
   2026-08-25 extraction or named by SPEC (91 files), every `tools/` script, one chunk per `[S-nn]` tag
   (its SPEC clause + its check), and SPEC id cross-references.
3. **Whole-document lane** (opus, one agent per document family, each refuted by a second opus agent):
   SPEC, CRITIQUE, MIGRATION_NOTES, README + fastlane. Whole documents, because a statement and the
   section that reverses it can be 200 lines apart.
4. **Cross-file lane** (4 sonnet agents with Read/Grep, opus refuters): lifecycle and static state, SAF
   storage paths, settings persistence, the drawer's state.
5. **By hand:** every confirmed or narrowed finding read against the source, "missing X" findings
   grep-probed, dispositions in the JSON.

## Fixed in 4.3.90

**Bugs a user could meet**
- "Save Camera Parameters" appeared in the drawer on Camera2 (walk screenshots 29, 73, 102, 111) and threw
  `ClassCastException` on tap; the API_1 test sat inside an `else if` and fell through
  (`NextGenSettingItemAdapterTools`).
- Videos never reached the gallery cell or the media scan: both video modules reported
  `new File("/MOV_….mp4")`, a root path that does not exist, instead of the Uri the recorder wrote
  (`VideoRecorder.getRecordingHolder`, `AbstractVideoModule`, `VideoModuleApi2`).
- The gallery kept a deleted file's cell (the deletion touched the full list, the grid binds the visible
  one) and a batch delete that emptied a folder left the old list on screen (`FilesHolderModel`).
- Searching "picture" dropped Picture's own Raw and Interval rows: the nested section label reset the
  match flag (`NextGenSettingDummyFragment.filteredKeyList`) — SPEC D7 said the opposite.
- The camera's `onDestroy` released the shared settings store and cancelled the shared image queues even
  while GlassGallery was open, and a closing gallery cancelled the camera's pending saves
  (`ActivityAbstract`: now the last live activity only).
- The share activity could deliver twice for a Jpg+DNG pair (`ActivityGlassCamShare`, `AtomicBoolean`).
- The SD-card row announced "External" before the folder picker answered and never corrected a failed
  or cancelled grant (`SDModeParameter`, `ActivityAbstract.onActivityResult`).
- A mixed selection deleted through the first file's storage path (a leading Uri cast the others and threw)
  (`GridViewFragmentModelView`, `FileListController.DeleteFiles`).
- Back during the gallery zoom finished the activity instead of returning to the grid
  (`ActivityGlassCamViewer`).
- A `.bayer` file showed the previous file's EXIF panel (`ScreenSlideFragmentModelView`).
- The White Balance row showed no description — it borrowed the distortion row's, which is empty
  (new `setting_wb_description`, EN + IT).
- "DNG stacking … isn't supported for SAF" was shown for a selection with no DNG at all (new
  `dngstack_no_dng`, EN + IT).
- Tap targets under T7's 48 dp: the viewer's two buttons (40 dp wide) and the curve view's nine buttons
  (30 dp tall).

**Concurrency and state** — `FileListController`'s listener list is walked from capture threads while
activities add and remove themselves (now copy-on-write); `DeleteFile` indexed outside the lock;
`NextGenGalleryButton` synchronized on the list instead of the controller's lock and could let an older
decode overwrite a newer thumbnail; `ShutterAnimationHandler`'s draw-thread fields are volatile; two
fragment `onCameraClose` callbacks gained the detach guard their `onCameraOpenFinished` twins had; the
manuals fragment's `Hidden` state is posted and null-guarded; a never-true AF-bracket branch replaced by
the reset it always produced.

**The gate** (`tools/spec_check.py`)
- A tag now certifies the clause it follows and appears once; SPEC's header says so and `main()` enforces
  it. Before, tags sat at the end of whole rows while checks covered one clause (50 tag-lane findings).
- Hardened: S-01 (parsed, not substring), S-05 (both guards must return null), S-06 ("only"), S-08 (it
  passed on the word "viewer" in an import line), S-11 (fell back to the file's first 800 characters and
  read the root view), S-14 (`find()` = −1 passed), S-17 (Java only; now resources, URLs only so the GPL
  author lines stay), S-19 (compares with the main activity's `configChanges`), S-21 (one of five layouts;
  now no orphan layout at all and the stated count). `jfile()` read a duplicated file as absent.
- New: **S-22** no layout declares a tappable view under 48 dp, by fixed or `min*` size, except T7's named
  exceptions; **S-23** every `textSize` is sp.
- Proven: `tools/spec_check_mutants.py` plants 13 defects in a scratch copy and requires the named check to
  FAIL, with an unplanted control that must pass. **The pre-audit gate, run on the same plants with a green
  control, missed 8 of 10.**

**The settings-panel scorer** (`tools/critique/settings_scorers.py`) modelled the 4.3.88 row heights
(56–66 dp) and one 48 dp header, so the 2026-09-16 run scored a panel that was never built, and a nested
section divider misplaced cells. Re-modelled on the shipped panel (76 dp rows, a search row and a chip row)
and **enumerated exactly** (`settings_enumerate.py`, all 5,760 layouts, both models):

| SPEC claim | exact result, shipped model | verdict |
|---|---|---|
| D3 two columns | 2 cols 0.821 · 1 col 0.807 · 3 cols 0.743 | holds |
| D6 chips at the bottom, "+0.055" | top-under-search 0.8208 vs bottom 0.8181; flips when the assumed thumb rest point passes 0.57 × height (never measured) | not an optimizer result → §11 |
| D9 section order "noise (±0.001)" | 0.811–0.818 at the shipped settings; Camera first best | does not hold → §11 |
| the shipped layout | 0.8150, rank 349 of 5,760 | — |

**Documents** — SPEC.md: revision, toolchain (AGP 8.13.2), §2's "persist only in onPause" and "a copy fix
is invisible" (both wider than the code), the detector's seven resets, ST2's fallback, C1's column, C7's redraw
rule, C13's end states, D1's insets, §5's measurement (the "11 rows" are 11 tappables: search, 5 setting rows,
5 chips; the 48 dp median is chips and search; 3 → 5 settings fully on screen), L2, L4 (54.1 %), §9, §11, §12.
MIGRATION_NOTES: the Windows build steps (NDK 21 → 27, no keystore) corrected in place; dated corrections
appended (the CVE list names five distinct ids; the libtiff tile checks' stated reason was wrong - the read path reaches them - but in read mode they only warned, so the input surface is unchanged; the MTK raw path).
CRITIQUE and the published critique page: an errata table, the log itself left as written. Manifest comments
that said every screen is orientation-locked. Code comments that described something else (column width,
picker rows, BitmapHelper's cap, the adapter's refresh mechanism, the camera column).

**Tooling** — `walk.py restore` never pressed HOME although SPEC said "launcher in front" (now: over our own
app only); `free` counted an unknown foreground as free; `tapt` fell back to an unsettled tree silently;
`settings_compare.py` now says its "rows" are tappables and that "context kept" is not measured;
`scorers.py` could leave [0, 1].

## Your decisions (answered 2026-09-28, built in 4.3.91)

| question | answer | built |
|---|---|---|
| settings reset by every versionCode bump | keep my choices | the detector writes defaults only on a first run (or after Run feature detector); a bump refreshes value lists and keeps the API, theme, grid, manual strip, shutter sound, focus-stack merge, focus-peak colour, preview post-processing |
| GlassCam/ folder cannot be created in the picked tree | refuse with a message | stills, DNGs, videos, focus stacks and DNG conversions are refused with a toast (EN + IT); nothing lands loose in the picked folder |
| format chip 48×32 dp | raise to 48 dp | 48 dp target, pill still drawn 32 dp (inset background); T7 has no exceptions left |
| GlassGallery not orientation-locked | lock to landscape | manifest + gate S-24 (every activity landscape) |
| Play button on a DNG | "why do I need a play button on stills?" | none on any still |
| chip row top or bottom | a finger reaching up covers the screen; the shutter stays centred with the items evenly split; the bottom strip stays | chips stay at the bottom, recorded as the owner's rule (SPEC D6) |
| where the value scale opens | A, beside the column — after a survey of the eight reference apps rotated into GlassCam's grip | anchored to the end beside the column, ~138 dp from the thumb (was 712 dp); gate S-25 |
| remaining targets under 48 dp | fix | manual-strip items and the camera column's upper-left cells (measured 44 dp wide) declare 48 dp |
| upstream README and store listing | replace / remove | a GlassCam README (credits and GPL kept); `fastlane/` retired, kept outside the tree |

Parked or open: the OpenGL column mockup (§11 item 6), the settings sections — rename "Camera", reorder (§11
item 7), Camera1 red-eye (item 8), the MTK raw path (item 9). The mockups and the reference sheet are in
`../_fresh-eyes-2026-09-28/` (local only, not in the repository: `mockups_camera.png`, `refs_grip.png`, `mockup_A2.png`), built from the app's
real frames at their measured bounds.

## Owed on the phone

Every 4.3.90 change is built, gated and unit-free (the project has no tests); none is device-verified.
In daytime, on the OnePlus 9 Pro:
1. The drawer on Camera2 no longer lists "Save Camera Parameters".
2. Record a video (Internal, then External): the gallery cell shows it and GlassGallery lists it.
3. Delete one file in the grid; batch-delete a whole folder; delete a mixed selection.
4. Search "picture": Raw and Interval rows stay.
5. With GlassGallery open, let the camera be destroyed (Developer options → Don't keep activities), return:
   settings intact, a pending save not cancelled.
6. Share: an app that names the package, Jpg+DNG format — one result.
7. SD-card row: pick a folder, cancel the picker, deny — the row shows the real state each time.
8. Back during the gallery zoom; a `.bayer` file's panel; the viewer's 48 dp buttons; the curve view's rows.
9. The White Balance description (EN and IT); the new DNG-stack toast.
10. Carried over: C11 flash glyphs, ST3 stale tree, D8's picker title row, C7's level axis (A077).
11. 4.3.91: install over 4.3.90 and check shutter sound, grid and API survive the bump; open a strip item —
    the value scale opens beside the column, centred on the shutter, and does not cover the shutter;
    the strip at 48 dp does not hide too much preview; the format chip taps from just below the pill;
    the upper-left column cells look like the others; GlassGallery stays landscape; a DNG shows no Play.
12. The refusal needs a tree where GlassCam/ cannot be made (e.g. a read-only one): expect the toast, no file.

## 4.3.91 walked on the OnePlus 6T (2026-09-28)

The 9 Pro was not attached; the owner chose the 6T (API 30, 832 × 384 dp landscape, Italian) because the
app must behave the same on both. Screenshots 191–209 in `tools/critique/screenshots/`.

| change | result on the 6T |
|---|---|
| settings survive a version bump | **pass** - a throwaway 4.3.92 over 4.3.91 kept the API (camera1), grid, focus-peak colour, manual strip, shutter sound and focus-stack merge; control: the first-run path reset all six (the check can fail) |
| capture refused when GlassCam/ cannot be made | **pass** - a picked tree holding a *file* named GlassCam: Italian toast, `refused …` in the log, no file written anywhere; the next capture runs normally (not stuck) |
| format chip 48 dp target, pill 32 dp | **failed, fixed, passed** - the pill drew 48 dp: an InsetDrawable reports its insets as padding and the row adopted it; explicit paddingTop/Bottom 0. A tap 9 dp below the pill cycles the format |
| manual strip items 48 dp; column cells 56 dp | **pass** - strip items 48 dp tall; the upper-left cells 158 × 135 px = 56 × 48 dp (view hierarchy) |
| value scale beside the column | **pass** - 659–709 dp, left of the column (717 dp), centred on the shutter, clear of it |
| GlassGallery landscape-locked | **pass** - with rotation forced to portrait the gallery stays at ROTATION_90 |
| no Play on stills | **failed, fixed, passed** - Play still showed on a DNG: the button was never bound to its model, so the Batch 2 "no Play on stills" never reached the screen either; Play, Info and Delete are now bound (S-26) |
| also seen | 4.3.90's SD-card row reports the granted state (External); D8's picker (title, ‹ Indietro, two columns); D7's search and empty state in Italian; the gallery opens in the newest folder with the new capture's thumbnail on the camera cell |

Found on the walk and registered: the format chip cycles every format the camera reports (six or more on
the 6T - SPEC §11 item 10); five strings show in English (item 11); the 6T's accessibility tree clips
everything right of x = 2125 px (the column's right half, the shutter, the viewer's buttons report 0–18 dp
although they are 56 dp and tappable - TalkBack could not reach them); in landscape the search keyboard
leaves room for half a result row; the help overlay's CLOSE is saved only at `onPause` (C8 reworded).
`walk.py` needed three fixes: its saved screen timeout was not keyed to a phone, per-app locales do not exist
below API 33, and the documented `build --no-spec` crashed. The system folder picker ignores injected taps on
its folder cards (keyboard navigation works). Left on the phone: one test capture,
`DCIM/GlassCam/2026_09_28_19_47_33.dng`; everything else was restored.

## The preview was never live on the 6T (found 2026-09-28, fixed in 4.3.93)

The owner noticed it while the 4.3.92 Italian strings were being checked: moving a hand in front of the
lens only distorted the picture already on screen. Method (the owner's): before each round the owner put a
new object in front of the camera; one screenshot of the preview, then one picture, compared.

| round | build / noise reduction | preview | picture |
|---|---|---|---|
| 1 cup | 4.3.92 / Off | old dark smear | cup |
| 2 brush | 4.3.92 / Off | the same smear | brush |
| 3 pencil | 4.3.92 / Off, format cycled | one sharp frame + ghost of the cup, no pencil | pencil |
| 4 eraser | 4.3.92 / Fast (config edit) | eraser | eraser |
| 5 USB drive | 4.3.92 / Fast | USB drive + live focus overlay | USB drive |
| 6 scissors | 4.3.93 / Fast (default after the bump) | scissors | scissors |
| 7 tape | 4.3.93 / Off chosen | tape | tape, 13.2 MB (unfiltered; Fast 3.3 MB) |

The camera service showed the preview stream producing ~30 frames/s throughout, so frames arrived; with
`android.noiseReduction.mode` OFF their content was frozen and temporally smeared. Every screenshot that
looked right before this (e.g. walk frame 201) was a single frame from session start. OFF was stored because
`Camera2Util.detectIntMode` chose defaults by translated label; fixed by the owner's three answers (C14,
S-27, S-28). New: §11 item 13 (a bump re-seeds every per-camera Camera2 mode).

## Inherited, no impact, editorial

- **Inherited upstream code (9), registered only:** `CameraApiManager.onResume` reads the camera right after
  an async switch; `setModule` refires on the same module; Camera1 `PictureModule`: `isWorking`
  check-then-act, the Moto preview reset writing the camera id twice, BAYER/RAW routed through the JPEG
  save path; zebra high/low defaults; the timelapse stepper cannot reach (1, 2) and floors at 1/60; an empty
  `PersistableBundle` `onCreate` override.
- **No impact (16):** residual substring checks in S-10/S-12/S-13 (recorded, not worth an XML parser each);
  S-03's redundant failure reason; a harmless keyboard-hide superset; a ripple skipped only if a theme
  attribute is missing; `NextGenSettingButton.setArrts` dead; three inert layout attributes; a wrong
  mechanism in a CRITIQUE aside; upstream opcode URLs in `res/raw/supported_devices.xml` parsed but never
  fetched; the OPCODE settings row never built, so its "download" text never shows.
- **Editorial (10), left as written:** comments that state a cause or a guarantee without evidence
  (`app/build.gradle`, the manifest's compat-tooltip note, `Horizont`, `NextGenMainFragment`,
  `HideNavBarHelper` ×2, the switch colours, the camera layout's "every reference app has a gear", the
  boolean-row layout).

## Withdrawn (42)

19 refuted by the verifier (reasons in the JSON). 23 by hand, each with its evidence, among them:
`ReleaseChecker` (its only caller is guarded), the gallery popup (`PopupMenuModel.setStrings` shows it),
the curve panels (hidden at inflation), `startWork` on low storage (can only stop), the settings adapter's
stale views (a new adapter per rebuild clears the pool), `minSdkVersion` in S-02 (the file's own key),
the S-19 substring hole (not live: the old check caught its mutant), four xref "findings" that were the
id regex matching phone names like "LG G3" in README's device table, the IMAGE_CAPTURE rule (documented
Android 11 behaviour), 74 literals vs 56 keys (different things counted), the colon-less headers (Boolean
rows, as L1 says), and two pre-Lollipop branches unreachable at minSdk 30.

## Not reviewed

- **Tier B: 615 files (757 chunks) of inherited FreeDcam code untouched since the extraction** — the Camera1/
  Camera2 parameter layers, modules, the DNG/raw pipeline, viewer internals. Their review would cost about five
  times tier A; tier A's inherited findings (9) were upstream races and naming, none on a GlassCam claim.
  Run it with `chunk_repo.py … --only tierB.txt --lanes code` when wanted.
- Native C/C++ (`jni/`: LibRaw, libtiff) beyond the libtiff item the doc lane traced.
- `tools/critique/opt_run*/state.json`, screenshots and `GlassCam_Critique.html` (generated).
- `tools/critique/measured.json` was regenerated by `measure.py` (2026-09-26 and again today); it is
  output, not reviewed input.

## Files changed

Code and resources (34 modified): `ActivityAbstract`, `ActivityGlassCamShare`, `SDModeParameter`, `VideoRecorder`,
`AbstractVideoModule`, `VideoModuleApi2`, `LevelBarView`, `NextGenSettingItemAdapterTools`,
`NextGenSettingItemRecyclerAdapter`, `NextGenCameraSettingFragment`, `NextGenCameraUiFragment`,
`NextGenManualsFragment`, `NextGenSettingDummyFragment`, `SettingGroupConfig`, `NextGenCameraUiTextSwitch`,
`NextGenGalleryButton`, `FileListController`, `ActivityGlassCamViewer`, `FilesHolderModel`,
`GridViewFragmentModelView`, `BitmapHelper`, `ScreenSlideFragmentModelView`, `ShutterAnimationHandler`;
`AndroidManifest.xml`; layouts `curve_view_control`, `glasscamviewer_screenslide_fragment`,
`nextgen_cameraui_text_switch`, `nextgen_setting_item`, `nextgen_setting_picker_title`; `dimens.xml`,
`strings.xml` (EN, IT); both launcher-icon drawables. Removed: `StyledTextView.java`, `styled_textview.xml`
(unused). `app/build.gradle` (4.3.90 / 290).
Docs: `SPEC.md`, `MIGRATION_NOTES.md`, `CRITIQUE_2026-09-16.md`, this register and its JSON.
Tools: `spec_check.py`, `critique/{walk,scorers,settings_scorers,settings_compare,build_artifact}.py`; new
`spec_check_mutants.py`, `critique/settings_enumerate.py` (+ `settings_enumerate.json`).
4.3.91 (the owner's answers): `CameraFeatureDetector`, `CameraApiManager`, `FileListController`,
`ImageSaveTask`, `ImageTaskDngConverter`, `FocusStackProcessor`, `DngConvertingFragment`, `VideoRecorder`,
`ScreenSlideFragmentModelView`; `AndroidManifest.xml`; layouts `nextgen_cameraui_manual_fragment_rotatingseekbar`,
`nextgen_manual_button`, `nextgen_cameraui_info_overlay`, `nextgen_cameraui_fragment`; new drawable
`infooverlay_pill`; `dimens.xml`, `strings.xml` (EN, IT); `README.md` rewritten; `fastlane/` retired;
`spec_check.py` S-24/S-25 and two more mutants; `SPEC.md`, `MIGRATION_NOTES.md`.
Pre-audit copy of everything above: `../_fresh-eyes-2026-09-28/snapshot/` (local only, outside the repository; removed in the 2026-10-11 cleanup).
