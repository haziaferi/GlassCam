# GlassCam — specification

**Revision 4.3.96 (versionCode 296), 2026-10-21.** The revision is the `versionName` of the build this
file describes; `app/build.gradle` must say the same, and `python tools/spec_check.py` refuses a tree
where it does not. A tag `[S-nn]` certifies **the clause it follows**, not the whole row: that
clause is checked against the source by the script, and each tag appears exactly once. Everything
untagged, including the other clauses of a tagged row, is a decision or intent, held here so the
next round verifies instead of re-discovering. (Until 4.3.90 tags sat at the end of whole rows while
their checks covered one clause; the fresh-eyes audit of 2026-09-28 lists every such overclaim -
`docs/fresh-eyes-2026-09-28.md`.)

GlassCam is the de-googled, SAF-only, minSdk 30 fork of KillerInk/FreeDcam (GPLv2), built for one
person on one phone (OnePlus 9 Pro, Android 14, 1080×2412 @ 480 dpi), sideloaded, never published.
`MIGRATION_NOTES.md` is the changelog of the port (Stages 1–9, 2026-08); `CRITIQUE_2026-09-16.md`
holds the design critique and the five batch tables whose decisions this file consolidates.

---

## 1. Target

| # | requirement | since |
|---|---|---|
| T1 | SAF-only storage: no `READ/WRITE_EXTERNAL_STORAGE`, `allowBackup="false"` `[S-01]` | Stage 1 |
| T2 | Local-first: no cloud, no update checker; the Sony local-Wi-Fi remote camera and geotagging stay; HTTP client restricted to private/link-local/loopback hosts | Stage 1 |
| T3 | De-googled: no GMS/Firebase/Play, no Hilt/Dagger | Stage 4 |
| T4 | `minSdk 30`, `targetSdk 36`, `compileSdk 36` `[S-02]` | Stage 1 |
| T5 | Unrooted device, sideload only, personal use; Windows 11 / JDK 21 / Java + XML Views + Data Binding, no Kotlin/TS | — |
| T6 | Landscape-locked manual camera; the shutter stays at the **vertical centre** of its column (owner, Batch 2). Every activity is locked to landscape, GlassGallery included since 4.3.91 (owner) `[S-24]` | Batch 2 |
| T7 | Every tappable control is at least 48 dp on its short side. No layout declares a tappable view under 48 dp, by fixed size or by `min*` size, and T7 names no exceptions `[S-22]`. Scope of that check: sizes *declared in layouts*. Since 4.3.91 (owner) the format chip, the manual-strip items and the camera column's upper-left cells declare their 48 dp too (the strip items were ~25 dp tall, the two cells 44 dp wide at runtime). Views sized only in Java stay outside the check | Batch 2/5; 4.3.91 |
| T8 | English is the app's first language, Italian its translation; user-visible copy lives in resources, never in layouts or Java. Every `textSize` in a layout is `sp` `[S-23]` | Batch 1 |

---

## 2. Build and versioning

- Debug build only: `python tools/critique/walk.py build` runs `gradlew.bat assembleDebug` for arm64-v8a alone (the v7a/x86_64 LibRaw and libtiff native builds are skipped); the log goes to `tools/critique/logs/build.log` and the exit code is read from the file, never from a pipe.
- `versionCode` is bumped on **every** install that must re-run the feature detector (the detector re-seeds cached parameter strings, so a copy fix to one of *those* strings is invisible until it runs; ordinary labels update on install). A bump keeps the user's choices (owner, 4.3.91): the detector refreshes the value lists and supported flags but writes the defaults - API, theme, grid, manual strip shown, shutter sound off, focus-stack merge off, focus-peak colour, preview post-processing - only on a first run (or after Etc ▸ Run feature detector, which resets everything) or where a stored value is no longer offered (`CameraFeatureDetector.detectFeatures(firstRun)`). Until 4.3.90 every bump reset them. `versionName` = `4.3.<versionCode−200>` `[S-03]`.
- Settings persist in `onPause`, and a theme change and the feature detector also save at once (`ThemeMode.setValue`, `CameraFeatureDetector`); anything else toggled just before `adb install` is lost unless HOME is pressed first.
- Toolchain: AGP 8.13.2, Gradle 8.14.3, NDK 27.3.13750724 (`gradle/libs.versions.toml`; the downgrade to 8.12.1 was later re-checked and bumped - `MIGRATION_NOTES.md`, toolchain re-check 2026-08-25).

---

## 3. Storage

| # | rule |
|---|---|
| ST1 | Internal storage = MediaStore, folder `DCIM/GlassCam`. |
| ST2 | External storage = a SAF tree the user picks; **every** capture (still, DNG, video, DNG output) lands in a `GlassCam/` child of that tree, created on first use; a picked tree already named `GlassCam` is used as is (`FileListController.getGlassCamDocumentFolder`). The still-image path asks for that folder `[S-04]`. When the child cannot be created in a tree that exists, the capture is **refused** and a toast says why (`storage_glasscam_folder_failed`; owner, 4.3.91) - nothing lands loose in the picked folder; stills, DNGs, videos, focus stacks and DNG conversions all check for it. A tree that no longer exists is ST3. A finished video is reported to the file list by the Uri the recorder wrote (4.3.90; it used to be a nonexistent `/MOV_….mp4` path, so the gallery cell and the media scan missed every video). |
| ST3 | A stored tree URI whose document no longer exists, or whose grant was revoked, reads as *no folder* (`getExternalSdDocumentFile` returns null) rather than a dead handle `[S-05]`. Built in 4.3.89, not device-verified. |
| ST4 | The capture path raises `NotifyFilesChanged.onCaptureSaved(BaseHolder)` from `addFromEventFile(s)` only `[S-06]`; listeners that need "a new capture happened" use it, never a diff of the file list (the list loads asynchronously and looks like a capture). It is raised on capture threads, so the listener list is copy-on-write and the share activity claims its one delivery atomically (4.3.90). |
| ST5 | The DNG stacker refuses SAF/MediaStore holders (`dngstack_unsupported_storage`) — a known dead end under T1, recorded, not fixed. |
| ST6 | Every capture is stored with a concrete MIME type: `image/jpeg`, `image/x-adobe-dng`, `video/mp4` (`MediaStoreController.mimeFor`), in MediaStore and in a picked SAF tree alike `[S-33]`. Until 4.3.96 the app sent the wildcards `image/*`/`video/*`; Android 14 rejects them, so every capture crashed on the 9 Pro (walk 2026-10-21, `Unsupported MIME type image/*`). Raw sensor dumps (.bayer, .raw, .jps: no image MIME type) cannot live in DCIM through MediaStore, so with internal storage they go to `Documents/GlassCam`, on every Android version, and the gallery lists them in its GlassCam folder (owner, 4.3.96). A MediaStore insert that returns nothing refuses the capture with the storage message instead of crashing. |

---

## 4. Camera page (NextGen theme, landscape)

| # | decision | decided |
|---|---|---|
| C1 | Shutter column on the right, two cells wide: **56 dp cells, 72 dp shutter** (`nextgen_cameraui_textswitch`, `nextgen_shutterbutton_size`) `[S-07]`; on this phone in the default preview mode, eight cells in four rows of two around the shutter (camera switch | histogram, self-timer | focus mode above; gallery | module, AE-lock | flash below). The above-right holder takes every toggle the camera exposes (histogram, focus peak, clipping, QCOM histogram, tone curve, focus mode), so with the OpenGL preview mode (focus peak, clipping) that side grows past two rows and the shutter is no longer centred (T6) - open, §11 (a mockup placing them beside the shutter, to the right, is parked until the owner returns to it); since 4.3.91 the upper-left holder has no side padding, so its cells are 56 dp like the rest (they were 44 dp wide); the whole cell is the target; captions short and static (`shortCaption`), no marquee. | Batch 2, optimizer #1648 |
| C2 | Column order is the layout optimizer's **runner-up** (two rows above the shutter, two below, score 0.883 vs 0.884): the winner put a third row under the shutter that clipped at 360 dp, and the owner wants the shutter centred (T6). | Batch 2, owner |
| C3 | The **gallery cell** is a live thumbnail of the last capture (`NextGenGalleryButton`); it opens GlassGallery inside the newest folder and refreshes when the file list changes `[S-08]`. The newest file is read under the controller's lock and a stale decode never overwrites a newer one (4.3.90). Videos reach it only since 4.3.90 (ST2). | Batch 2 / 3 |
| C4 | A **gear cell** (56×48 dp) at the bottom of the left letterbox band opens the settings drawer; the swipe-to-settings gesture stays. Known weakness: the gear is 734 dp from the right thumb (Manual Camera Pro: 110); moving it into the right column costs one of that column's eight cells — **owner's call, open**. | Batch 3; open |
| C5 | The **value scale** (the vertical seekbar a strip item opens) sits beside the shutter column, anchored to the end, clear of its two 56 dp cells `[S-25]`, centred on the shutter: ~138 dp from the right thumb, where Manual Camera Pro, MA Camera and Libre open theirs (critique walk frames 85, 91, 95). It lived in the left band (712 dp from the thumb) from Batch 2 to 4.3.90. The AF-bracket Near/Far view sits to its left. | Batch 2; owner 4.3.91 |
| C6 | Info overlay: scrim pill; REC pill + 12 sp timer bound to capture state; "Video" label in video mode; the **format chip** tap-cycles through every picture format the camera reports (on the 9 Pro: Jpeg → DNG → Jpg+DNG; on the 6T: JPEG → Jpg+16bitDng → yuv → Bayer10 → 10bitDNG → 16bitDNG → …, walk 2026-09-28 - open, §11); its target is 48 dp tall while the pill still draws 32 dp (the row is 48 dp, its background inset 16 dp at the bottom; owner, 4.3.91). Overlay text is `sp`, never `dip` (T8). | Batch 2 |
| C7 | Horizon level = `LevelBarView`: one bar, centre tick, signed angle, dark outline; the constants are lime within 0.5°, a 0.2° redraw gate, a 2 s fade after level, and the sensor runs at `SENSOR_DELAY_UI` `[S-09]`. It redraws on a ≥ 0.2° change **or** when it crosses the 0.5° tolerance (the colour flips however small the move). The saved Image Level value is applied on start-up. The angle it shows is `orientation[1]` after a landscape remap - pitch by the platform's naming; the Batch 2 walk found the level working (shot 40) but did not test which axis it follows - owed, §11. | Batch 2 |
| C8 | Help overlay shows on **first run only**; closing it is saved at the next `onPause` (an `adb install` over a foreground app loses it - §2); a version bump does not re-show it (`switchCamera` no longer re-enables it). | Batch 3 |
| C9 | Cold start opens in **Picture** with burst 1 regardless of the last module. | Batch 3 |
| C10 | Module popup lists modules in registration order: Picture · Video · Interval · Ae-Bracket · Af-Bracket (`LinkedHashMap`). `RawZsl` is present in the tree but not registered — dead, recorded. | Batch 3 |
| C11 | The flash cell shows glyphs for both APIs: Camera1's driver words off/auto/on/torch/red-eye map to `font_flash_*`. Auto is the "A" + bolt glyph (U+E021) and always the plain bolt (U+E018); the two names pointed at each other's glyph until 4.3.96 (rendered from the font). Red-eye (auto flash with a pre-flash) is the auto glyph with a dot badge (`font_flash_redeye`; the font has no red-eye glyph) and is a Camera2 Flash state too, offered where the camera lists `CONTROL_AE_MODE_ON_AUTO_FLASH_REDEYE` `[S-30]`. Flash behaviour is set on the Flash toggle only: the Auto Exposure Mode row offers on and off, its flash variants moved here (owner, purpose map 2026-10-20). | Batch 5; owner 4.3.95 |
| C12 | Camera1 works after a gallery visit: `ActivityAbstract.context` is re-claimed in `onResume` and released only by its owner in `onDestroy` (the Camera1 NPE was a static overwritten by the viewer activity). The shared image queues and settings store are torn down only by the last live activity (4.3.90; the camera's `onDestroy` used to reset the settings under an open gallery). | Batch 3 |
| C13 | BACK: gesture **and** key. `ActivityGlassCamMain.onKeyUp` handles the BACK key itself (it bypasses `OnBackPressedDispatcher`), so both paths call `closeSettingsIfOpen()` first `[S-10]`. When nothing was open the two paths end differently: the key moves the task to the back, the gesture falls through to the system Back. | Batch 4 |
| C14 | The preview is live. Camera2 mode defaults (noise reduction, edge, hot pixel, white balance and ten more) are picked by the English label of the mode, so a phone's language never changes them `[S-27]`; until 4.3.93 they were looked up by the translated label, so the Italian 6T got noise reduction Off where an English phone got Fast. Noise reduction Off applies to the photo only: the preview request runs at Fast (`CaptureSessionHandler.previewValue`) `[S-28]`, because the 6T's Off preview keeps delivering frames but shows a frozen, smeared image (proved with the owner by five object-in-front rounds, screenshots 224-228). The 4.3.93 bump resets the stored noise reduction once - the detector re-seeds these per-camera modes on every bump (§11 item 13). | owner, 4.3.93 |
| C15 | One histogram button (owner, purpose map 2026-10-20): the camera hardware's histogram (Qualcomm statistics, works in the normal preview) where the phone has one; the app's own, drawn by the OpenGL preview, only where it does not `[S-29]`. Until 4.3.95 a Qualcomm phone in OpenGL mode showed two buttons with the same icon. | owner, 4.3.95 |
| C16 | Hardware keys: the camera key (and a 3D-mode key) takes the picture on press; a headset key takes it on a hold and steps the manual control on a tap; volume keys step the active manual control `[S-31]`. Until 4.3.95 the camera key did nothing - `startWork` was only reached from a held headset key. | purpose map 2026-10-20 |

---

## 5. Settings drawer (page 0 of the pager)

The drawer the settings comparison had ranked first (4.3.84: five tabs, 60 % × 85 % panel anchored left)
was rejected by the owner on 2026-09-16: part-screen, on the far side from the shutter thumb, tabs of
3 · 9 · ~20 · ~10 · 4 rows with blank space. Decided by three questions and one clarification:

| # | decision | decided |
|---|---|---|
| D1 | **Full-width, full-height** panel with search, sticky label, chips and list inside it `[S-11]`, over the scrimmed live preview. Insets for the system bars and the display cutout are applied in code on all four edges (`NextGenCameraSettingFragment`, not the layout); the side gesture strips are not inset. | owner, Batch 4 |
| D2 | **One list** of every setting — Video · Picture (Raw and Interval as sections) · Camera · Preview · Etc — as `NextGenSettingDummyFragment.GROUP_ALL`, section rows full-span and unclickable, no tab adapter `[S-12]`. Section rows span every column, however many there are, and are not counted as targets. | owner, Batch 4 |
| D3 | **Two columns** on this phone: column count = panel width ÷ 300 dp, one column below 600 dp. Scored exactly (`settings_enumerate.py`, 5,760 layouts, shipped row model): best two-column 0.821, best one-column 0.807 (wins fill, loses reach), best three-column 0.743 (loses legibility). Two holds. | owner + scorer, Batch 4/5; re-scored 2026-09-28 |
| D4 | Every settings row is at least **76 dp** (`nextgen_settings_row_min_height`: header + two description lines), the compact stepper on one line with 48 dp −/+ `[S-13]`. `GridLayoutManager` already equalises a grid row's height, so uniform rows leave no gap beside a short cell. This was an engineering decision, not an optimizer result: the 2026-09-16 run modelled the 4.3.88 row heights (56–66 dp) and never scored 76 dp rows; with every row at 76 dp, grid and staggered packing score the same. | Batch 5 |
| D5 | **Sticky section label** pinned over the list while its own row is scrolled off (`settings_sticky_section`, `nextgen_settings_sticky` `#E6121212`). | owner, Batch 4 |
| D6 | **Jump chips** — one outlined pill per top-level section, in their own row below the list, 48 dp targets `[S-14]`; right-aligned toward the shutter thumb; tap scrolls the section to the top and closes an open picker; the section in view is lime (`manual_button_active`); a chip whose section the search emptied is dimmed and disabled; 36 dp pill inside the 48 dp target. The "+0.055 chip reach over the top row" came from a model that put top chips inside the search header (y = 24 dp); a real top row sits under search (y = 72 dp). Re-scored exactly, top and bottom differ by 0.003 and flip with the assumed thumb rest point; the owner settled it on a rule the scorer does not model: a finger reaching toward the top of the screen covers it, so the chips stay at the bottom (owner, 2026-09-28). | Batch 5; owner 2026-09-28 |
| D7 | **Search** on top (`settings_search`, 48 dp row, clear glyph ✕), strings in EN and IT, filtering inside the list `[S-15]`: header + description, case-insensitive; a section whose own label matches keeps all its rows, nested sections included (4.3.90: a match on "Picture" used to drop its Raw and Interval rows); orphaned labels are pruned; empty state `settings_search_empty`; the keyboard drops on row tap, chip tap, Search action and when the page is swiped away. | owner, Batch 4 |
| D8 | **Value picker**: a title row at least 48 dp, not clickable, an accessibility heading, with no click listener `[S-16]`; the title spans the full width, an outlined "‹ Back" chip, two columns of values, list scroll preserved after a pick. The title row was built in Batch 4 and has not been seen on the phone since - owed, §11. | Batch 2 / 4 |
| D9 | Row order is the declared order (`SettingGroupConfig`). Section order is **not** noise: re-scored exactly it spans 0.811-0.818 at the shipped settings (the "±0.001" came from the model D4 and D6 describe), and Camera first scores best - open, §11. | Batch 5; re-scored 2026-09-28 |
| D10 | Dead rows removed: "Check for new updates", and no GitHub/KillerInk URL in code or resources outside the upstream device database; the checker is hard-off `[S-17]`. The device database (`res/raw/supported_devices.xml`) still carries upstream opcode URLs; they are parsed and written to the config file but never fetched. | Batch 3 |
| D11 | One row per job (owner, purpose map 2026-10-20) `[S-32]`: one MFNR row (Xiaomi's key stands in on phones without Qualcomm's), one "Save camera info" row (Camera2 dumps the vendor keys, Camera1 saves the driver's parameters). Edge sharpening reads as the camera's stage and says that Sharpness on the manual strip sets its strength (its Italian label was "Vignettatura", vignetting). Rows that could never show were removed: Secondary Size (registration commented out), SceneDetection (no detector), Correlated Color Temperature (set by the white-balance mode, never registered). The purpose map (`docs/purpose-map.md`, `tools/purpose_map.py`) places every row, control and screen by purpose and fails when the code and the map disagree. | owner, 4.3.95 |

Measured on 4.3.89 (`tools/critique/settings_compare.py`, whose "rows" are top-level **tappables**, not
settings): 11 tappables on the first screen = the search field, 5 setting rows and 5 chips; the 48 dp
median is taken over all 14 clickables (nested −/+ included) and is the chips', search's and steppers'
48 dp (the setting rows are 76 dp, one clipped at 59 dp by the screen edge); fill 0.68 and mean reach
321 dp are over the 11 top-level tappables. On 4.3.84 the same measure gave 8 = 4 tabs + 3 setting rows
+ an 8 dp sliver of a fourth, fill 0.42, reach 535 dp. Settings fully visible on the first screen: 3 → 5
(re-read from `screenshots/set_glasscam.xml` and `123_b2_drawer.xml`, 2026-09-28).

---

## 6. Gallery and forms

| # | decision |
|---|---|
| G1 | GlassGallery: system BACK returns from the viewer to the grid (`OnBackPressedCallback`), also while the zoom animation runs (4.3.90); no Play button on any still, DNG included `[S-26]` (owner, 4.3.91; until 4.3.91 the button was never bound to its model, so the Batch 2 "no Play on stills" never reached the screen: the 6T walk showed Play on a DNG); Info and Delete are bound the same way, so an empty folder shows neither; bar and viewer buttons 48 × 48 dp (4.3.90: the two viewer buttons were 40 dp wide); a raw `.bayer` file shows no EXIF panel (it has none; it used to show the previous file's); thumbnails fill the width; the popup menu is "Delete file / Raw to DNG". Deleting a file removes it from the grid and a batch delete that empties a folder clears it (4.3.90); each file is deleted through its own storage path. |
| G2 | The gallery opens inside the newest folder, not at the folder root. |
| G3 | Video Profile Editor, DNG converter and curve view use the app's tokens (`FormTheme`: dark ground, lime accent, fullscreen, navigation bar hidden); `colorPrimary`/`colorAccent` are the app's dark ground and lime, not the stock indigo/pink `[S-18]`. The curve view's buttons are 48 dp tall (4.3.90; they were 30 dp, T7). |

---

## 7. Intents

| # | decision |
|---|---|
| I1 | `ActivityGlassCamShare` answers `ACTION_IMAGE_CAPTURE` with the same `configChanges` as the main activity (so the forced landscape does not recreate it) and listens for `onCaptureSaved` `[S-19]`: full camera UI, then the first saved capture - claimed once, atomically, since a Jpg+DNG pair saves twice (4.3.90) - is streamed into the caller's `EXTRA_OUTPUT` (or returned as a thumbnail in `data`), the result is set and it finishes. |
| I2 | Platform limit, recorded: since Android 11 an implicit `IMAGE_CAPTURE` resolves only to the pre-installed camera; the share activity serves callers that name the package. Not a general "use GlassCam" hook. |
| I3 | `ActivityGlassCamViewer` is exported with a custom action; its intent extras are untrusted input (audit note, MIGRATION_NOTES). |

---

## 8. Copy and localisation

| # | rule |
|---|---|
| L1 | Colon policy follows the row type: value rows ("Picture Format:") end with a colon, switch and action rows do not. |
| L2 | No hard-coded strings in layouts or Java. `measure.py` counts the layouts only (0 in this revision; the two left in 4.3.85 went with the harness); Java is not measured. Help pages, form labels and picker titles are resources. |
| L3 | Typos fixed in Batch 1 stay fixed: Continuous, Detection, Portrait, reset, which `[S-20]`. |
| L4 | Italian coverage is reported by `measure.py` as non-empty Italian entries over default keys: 56.3 % in this revision (206 Italian entries, 5 of them empty, over 357 keys) — a gap, not a rule; new keys get an Italian row when added (4.3.90's two did). |

---

## 9. Removed from the tree

`PresetGridHarnessActivity` + `PresetGridLayout`, `SettingTabPagerAdapter`, no orphan layout left
(the five `cameraui_*` orphans went in Batch 3, the unused `StyledTextView` + `styled_textview.xml` in
4.3.90: 37 layouts remain), the classic `themesample` theme (Stage 4), Hilt (Stage 4) `[S-21]`. The
KillerInk update link and the "Check for new updates" row are D10. Upstream's store listing
(`fastlane/`) and README went in 4.3.91 (owner): the README now describes GlassCam; both originals
are kept with the round's working files, outside the tree.

---

## 10. Decision log

| date | decision | by | evidence |
|---|---|---|---|
| 2026-08 | SAF-only, minSdk 30, de-googled, no Hilt (Stages 1–9) | owner | `MIGRATION_NOTES.md` |
| 2026-09-16 | Critique scope: layout + feature function, English via per-app locale, eight reference apps, critique first with no code changes | owner (asked upfront) | `CRITIQUE_2026-09-16.md` |
| 2026-09-16 | Batch 1: copy pass, colon policy, 74 strings to resources, stepper header, `10dip → 10sp` | owner ("apply Batch 1") | critique status table, screenshots 106–116 |
| 2026-09-16 | Batch 2: five tabs (Raw + Interval as sections), optimizer column with the **shutter centred** (runner-up), gallery thumbnail cell, single-bar level with auto-hide, titled picker, `FormTheme` | owner (AskUserQuestion; centre insisted on during the walk) | status table, 116–141 |
| 2026-09-16 | Batch 3: Camera1 static-context root cause, cold-start Picture/burst 1, help first-run only, SAF `GlassCam/` child, share activity re-enabled, harness + orphan layouts deleted, gear cell, newest-folder landing | owner ("continue in dependency order") | status table, 142–158; verified 159–183 |
| 2026-09-16 | Batch 4: tabbed drawer **rejected**; full-width full-height panel, one two-column list, sticky sections + jump chips, search on top, BACK closes the drawer, picker title 48 dp heading | owner (three questions + one clarification) | status table, 184–187 |
| 2026-09-16 | Batch 5: settings optimizer run → two columns confirmed, grid + uniform 76 dp rows, compact stepper, chips at the bottom; section order left declared; comparison re-scored with fill/reach | owner ("fix all gaps, optimize this workflow") + optimizer (`opt_run_settings/`, 0.821 → 0.836) | status table, 188–190; `settings_compare.json` |
| 2026-09-18 | This file created; `tools/spec_check.py` gates it against the tree | owner ("update the spec file … optimize this workflow") | — |
| 2026-09-28 | Fresh-eyes audit (4.3.90): gaps that needed no decision fixed - tags moved onto the clauses their checks prove, 9 checks hardened, S-22 (48 dp floor over every layout) and S-23 (text in sp) added, each check proven able to fail (`spec_check_mutants.py`, 13 plants); the settings scorer re-modelled on the shipped panel and enumerated exactly: two columns hold, "chips at the bottom" and "section order is noise" do not (D6, D9); code fixes in the gallery, search, video reporting, share delivery, activity teardown, T7 sizes. Decisions listed in §11 | owner ("fresh eyes … fix all gaps that do not require a decision") | `docs/fresh-eyes-2026-09-28.md` |
| 2026-09-28 | 4.3.91, the owner's answers: settings survive a version bump; a capture is refused (with a toast) when the GlassCam/ folder cannot be made; the format chip, the manual strip and the narrow column cells are 48 dp targets; GlassGallery is landscape-locked; no Play on stills; the value scale opens beside the shutter column (chosen after a survey of the eight reference apps rotated into GlassCam's grip); the chips stay at the bottom (a finger reaching up covers the screen); upstream README and store listing retired. Parked: the OpenGL column mockup, the sections | owner (AskUserQuestion, mockups from real frames) | `docs/fresh-eyes-2026-09-28.md` |
| 2026-10-21 | 4.3.96 walked on the 6T (Android 11), the 9 Pro (Android 14) and a Nord CE4 Lite 5G (Android 16): capture, camera key, raw dumps to Documents, live preview (object rounds on the 9 Pro and the Nord), one histogram button, one MFNR row (seen on the Nord) | walks with the owner | `MIGRATION_NOTES.md` |
| 2026-10-21 | 4.3.96: captures crashed on Android 14 (9 Pro walk) - concrete MIME types, raw dumps to Documents/GlassCam (owner's choice), ST6 | 9 Pro walk + owner | `MIGRATION_NOTES.md` |
| 2026-10-20 | 4.3.95 walked on the 6T: every change seen on the phone; the Auto Exposure Mode row had vanished in the first build (filter compared `label,mode` pairs to bare labels) - fixed and gated before release; the Flash list's auto/always glyphs may be swapped (older, to check) | walk | `docs/purpose/findings-2026-10-20.md` |
| 2026-10-20 | 4.3.95: the purpose map's redundancy decisions - one histogram button, flash set on the Flash toggle only (red-eye moved there), edge sharpening and sharpness explained, one MFNR row, one save-camera-info row - plus the fixes it found: the camera key never took a picture, "MNFR", three rows that could never show, Edge Mode's Italian label | owner (AskUserQuestion after a scored mock-up) | `docs/purpose/findings-2026-10-20.md` |
| 2026-09-28 | 4.3.94: audio-source values labelled in Italian (display only, `ValueLabels`, scoped by setting); the noise-reduction row renamed from "wavelet" | owner ("Yes, proceed") | `MIGRATION_NOTES.md` |
| 2026-09-28 | 4.3.93: the preview is live on the 6T - Camera2 defaults picked by English label, noise reduction Off kept off the preview, a one-time reset to Fast (C14) | owner (object-in-front test, then three answers) | `MIGRATION_NOTES.md`, screenshots 224-228 |
| 2026-09-28 | 4.3.92: the five English strings the 6T walk found are in Italian (§11 item 11) | owner ("Translate the five English strings into Italian") | `MIGRATION_NOTES.md` |
| 2026-09-28 | 4.3.91 walked on the OnePlus 6T (the 9 Pro deferred; the app must behave the same on both): every change verified on the device, two found broken and fixed before release - the format pill drew 48 dp tall (an InsetDrawable's insets became the row's padding) and Play still showed on stills (the button was never bound to its model, since Batch 2; now S-26) | owner ("walk the 4.3.91 changes on the phone") | `docs/fresh-eyes-2026-09-28.md`, walk screenshots 191-209 |

---

## 11. Open — owner's call

1. **Gear placement** (C4): 734 dp from the thumb; right-column placement costs a cell.
2. **Not device-verified**: on the 9 Pro, everything since 4.3.89 (the 4.3.91 changes were walked on the 6T, 2026-09-28); on either phone, C11 (Camera1 flash glyphs), ST3 (stale SAF tree), C7's axis and the 4.3.90 list in the register's *owed on the phone* section. D8's picker title row was seen on the 6T.
3. **Comparison gaps**: ProShot's tree never idles, OnePlus's entry label was not found; Open Camera, Manual Camera Pro, MA Camera and Libre Camera do not expose rows as tappable, so their fill/reach stay unmeasured.
4. **Known dead ends kept**: DNG stacker under SAF (ST5), `RawZsl` unregistered (C10), the IMAGE_CAPTURE reach (I2), LibRaw 0.19.1 / libtiff 4.0.9 CVEs (MIGRATION_NOTES Stage 8).
5. Italian coverage (L4).
6. **Camera column overflow** (C1/T6): with the OpenGL preview mode the above-right holder grows past two rows (focus peak, clipping join), so the shutter leaves the centre. A mockup putting them beside the shutter, to the right (the shutter shifted left in the band, the blocks 7 dp further out, still centred) is parked for the owner.
7. **Settings sections** (D9): "Camera" holds the sensor's processing modes (scene, white balance, HDR, denoise…), which the name does not say; Camera first also scores best (+0.007). Rename, reorder, or keep - parked for the owner.
8. ~~**Camera1 "red-eye" flash** (C11): no glyph in the font.~~ - done in 4.3.95: the auto glyph with a dot badge, shared with Camera2's new red-eye Flash state (owner).
9. **MTK raw path**: `PictureModuleMTK` has the driver write raw files under `/sdcard/DCIM/GlassCam` outside SAF (T1), on MediaTek devices only.
10. **Format chip cycle** (C6): it steps through every format the camera reports, three on the 9 Pro and six or more on the 6T (yuv, Bayer10, 10bitDNG…); "the same on both phones" would mean limiting it to JPEG / DNG / JPEG+DNG.
11. ~~**Italian gaps seen on the 6T**~~ - done in 4.3.92 (owner): the help overlay's Close (hard-coded in `HelpFragment`, against T8) is a resource; the shutter-sound, focus-stack-merge and audio-source rows have Italian headers and descriptions; the storage values show as Interna/Esterna through `glasscam.settings.ValueLabels` while the stored values stay Internal/External (the picker passes the value back, so only the display is translated). Seen on the 6T 2026-09-28 (screenshots 212, 231-236): Chiudi, Suono dell'otturatore, Unisci il bracketing AF in una pila di fuoco, Sorgente audio, Posizione memoria = Interna, picker Interna/Esterna. The audio-source values (compared by text in `VideoRecorder`, so marked untranslatable) show Italian labels through the same `ValueLabels` since 4.3.94 (Predefinita, Microfono, Videocamera, …), and the noise-reduction row is titled "Riduzione rumore" / "Noise reduction" (it was "wavelet", a Camera1 filter's name) - owner, 4.3.94; seen on the 6T (screenshots 237-239: row Predefinita, picker Predefinita/Microfono/Videocamera/Riconoscimento vocale/Comunicazione vocale, row "Riduzione rumore: Veloce").
12. ~~**The live preview is not live on the 6T**~~ - fixed in 4.3.93 (owner's three answers): C14. Walk on the 6T owed with the owner, object-in-front rounds with noise reduction Fast and Off.
13. **A bump re-seeds the per-camera Camera2 modes**: `Camera2Util.detectIntMode` sets the default every time the detector runs, so a version bump resets noise reduction, edge, hot pixel, white balance, AE/AF mode and the rest - §2's "a bump keeps the user's choices" covers the global settings only. Found 2026-09-28; not changed (4.3.93's one-time noise-reduction reset relies on it). Open: keep them across bumps like the globals.

---

## 12. Verification — what a round runs

| step | command | what it proves |
|---|---|---|
| spec gate | `python tools/spec_check.py` | revision = `app/build.gradle`; every `[S-nn]` here has a check, every check has a tag, and each tag appears once; each check passes against the source |
| gate can fail | `python tools/spec_check_mutants.py` | each changed check FAILs on its planted defect in a scratch copy (24 plants), and an unplanted copy passes; run it whenever a check changes |
| purpose map | `python tools/purpose_map.py --check` (also run by `walk.py build`) | every screen, capture mode, camera-screen control, gallery button and settings row the code can show is placed once on `docs/purpose-map.json`, nothing on the map is gone from the code, the page is current; `tools/purpose/map_mutants.py` proves each check can fail. The method that drafted it: `docs/purpose/findings-2026-10-20.md` |
| repo numbers | `python tools/critique/measure.py` | contrast rows, dp/sp literals, strings hard-coded in layouts, Italian coverage |
| build + install | `python tools/critique/walk.py build` then `install` | exit code from the log file; `-r -g -t`, en-US per-app locale, screen timeout saved |
| device walk | `walk.py shot / tapt / nodes / free / log` | the steps named per finding; `tapt` retries up to 3 dumps for two identical ones (and says so when it gives up); `free` waits out another session on the phone |
| drawer layout | `python tools/critique/settings_enumerate.py` | every one of the 5,760 layouts scored under the shipped and the old row model (`settings_scorers.py`); a layout change is scored, not argued. The space is small enough to enumerate, so no heuristic search is needed |
| comparison | `python tools/critique/settings_compare.py [app …]` | fill / reach / top-level tappables per screen against the references |
| restore | `walk.py restore` | locale cleared, timeout restored, `/sdcard/critique` removed, HOME pressed if our app (or the picker) is in front |

A batch is done when: its rows in `CRITIQUE_2026-09-16.md` say *verified* with a screenshot number, this
file's revision and tables are updated, `spec_check.py` passes, the artifact is republished and the
phone is restored.
