# GlassCam feature checklist — code-trace before the device walk (2026-09-16)

Status is filled in during the walk. `expected` is what the code says should happen; a row marked
**dead/stub** here is a prediction from the source, confirmed or refuted on the phone.

## Modules (`apis/camera2/ModuleHandlerApi2.initModules`, `apis/camera1/ModuleHandler`)

| module | entry | handler → capture | artefact expected | prediction | walked |
|---|---|---|---|---|---|
| Picture | module text-switch → `PictureModuleApi2` | `CaptureSessionHandler` → ImageReader → `ImageSaveTask` → `UriHolder` (SAF) | JPEG / DNG in SAF folder | works | |
| Video | `VideoModuleApi2` | MediaRecorder; `VideoModuleApi2.java:344 //TODO handel uri based holder` | MP4 in SAF folder | **risk**: URI path unfinished | |
| Interval | `IntervalApi2` | repeats PictureModule with sleep from `INTERVAL_SHUTTER_SLEEP`/`INTERVAL_DURATION` | N JPEGs | works | |
| AE bracket (HDR) | `AeBracketApi2` (only if `M_EXPOSURE_TIME` supported) | burst with stepped exposure | 3–5 frames | works on Camera2 | |
| AF bracket | `AfBracketApi2` (only if `M_FOCUS` has values) | stepped focus; `FocusStackProcessor` if `FOCUS_STACK_MERGE` on | N frames (+ merged) | works; merge = new code, unverified | |
| Raw ZSL | `RawZslModuleApi2` | **never put in `moduleList`** | — | **dead** | |
| Camera1 Picture/Video/Interval/Bracket | `ModuleHandler` | legacy HAL path via `Camera1` | files | sampled only | |

## Manual controls (`ManualGroupConfig`, 14 buttons, each only if the parameter `isSupported()`)

Zoom · Focus · ISO · Exposure time · EV · Aperture · WB · Burst · Contrast · Brightness · Saturation · Sharpness · Zebra high · Zebra low

Expected on the OnePlus 9 Pro (Qcom, Camera2): zoom, focus, ISO, exposure time, EV, WB(ct) present; aperture absent (fixed lens); contrast/brightness/saturation/sharpness depend on vendor keys (`camera2_hidden_keys/qcom`); zebra high/low are preview post-processing.

## Shutter-column text switches (`NextGenCameraUiFragment.setCameraToUi`)

Focus mode · Self-timer · Module · Flash · AE lock · Tone curve · Camera switch · Histogram / Focus-peak / Clipping / Qcom-histogram toggles

## Settings drawer (`SettingGroupConfig`, tabs hard-coded in `SettingTabPagerAdapter`)

| tab | rows (keys) | prediction |
|---|---|---|
| Video | VIDEO_PROFILES, TIMELAPSE_FRAMES, VIDEO_HDR, VIDEO_STABILIZATION, VIDEO_AUDIO_SOURCE, Video Profile Editor | works |
| Picture | PICTURE_SIZE, PICTURE_FORMAT, YUV_SIZE, SECONDARY_SENSOR_SIZE, PHYSICAL_CAMERA_ID, JPEG_QUALITY, MFNR, XIAOMI_MFNR, MTK_AOV_MODE | physical camera = new code |
| Raw | OPCODE, BAYERFORMAT, MATRIX_SET, TONEMAP_SET, FORCE_RAW_TO_DNG, USE_CUSTOM_MATRIX_ON_CAMERA_2, RAW_SIZE | works |
| Camera | SCENE_MODE … AE_METERING (~37 vendor/Camera2 rows) | many rows unsupported on this SoC → hidden or inert |
| Interval | INTERVAL_SHUTTER_SLEEP, INTERVAL_DURATION | works |
| Preview | PREVIEW_POST_PROCESSING_MODE, FOCUSPEAK_COLOR, ORIENTATION_HACK, SWITCH_ASPECT_RATIO, USE_GLASSCAM_AE, MIN/MAX_ISO, MIN/MAX_EXPOSURE, GUIDE_LIST, HORIZONT_LVL, ZOOM_ON_MANUALFOCUS(+factor,+duration), FORCE_WIDE_PREVIEW | horizon level lives here |
| Etc | SD_SAVE_LOCATION, LOCATION_MODE, NIGHT_OVERLAY, TOUCH_TO_CAPTURE, PLAY_SHUTTER_SOUND, FOCUS_STACK_MERGE, OPEN_CAMERA_1_LEGACY, CHECKFORUPDATES, PREVIEW_TEMPLATE, CAPTURE_TEMPLATE, HIDE_CAMERA_UI, Run FeatureDetector, dump vendor keys, API switch | CHECKFORUPDATES → `ReleaseChecker.isGithubRelease=false` **dead**; THEME key retained, row removed |

## Known dead / stub (from source)

- DNG stacker: `GridViewFragmentModelView.java:310` toasts `dngstack_unsupported_storage` for SAF/MediaStore files → **unusable** under the fork's SAF-only storage.
- `RawZslModuleApi2` unregistered.
- Update check off; `NextGenCameraUiFragment.java:369` still opens `github.com/KillerInk/FreeDcam/releases/latest`.
- `ActivityGlassCamShare` commented out of the manifest → GlassCam cannot serve `IMAGE_CAPTURE` intents.
- `AppTheme` is DayNight but every colour is hard-coded white-on-dark; theme picker removed, `arrays.xml/themes` = `NextGen` only.
- Help overlay strings hard-coded (`help_fragment.xml`: "Large Text", "Next", "don't show again").
- Settings tab titles hard-coded in Java (not translatable).
- `PresetGridHarnessActivity` + 5 legacy `cameraui_*` layouts unreferenced.
- `colorPrimary #3f51b5` / `colorAccent #FF4081` (stock template) still defined; nothing in the NextGen UI uses them, but Switch/EditText/Button defaults in the viewer, DNG converter and video profile editor do.
- Vendor signature permissions in the manifest can never be granted on an unrooted OnePlus.
- Native: LibRaw 0.19.1 / libtiff 4.0.9 CVEs unfixed (`MIGRATION_NOTES.md:142-167`).
