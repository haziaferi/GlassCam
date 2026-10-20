# Purpose map

GlassCam's screens, capture modes, camera-screen controls, gallery buttons and settings, by what they are for. Drafted 2026-10-20 to find redundancy: the six purposes (set E6) were scored against eight reference camera apps (coverage, exclusivity, balance, GlassCam fit, potential improvement) and chosen by the owner after a mock-up; the granularity (G4) lists every control, and settings rows by section except the rows in a redundancy-candidate pair. `refs` names the reference apps that do the same job; `overlaps` names the map entries it is a redundancy candidate with.

Generated from `purpose-map.json` by `tools/purpose_map.py`; edit the JSON, not this page.

## Purposes

| | Purpose | Its job | Entries |
|---|---|---|---|
| P1 | **Frame and shoot** | see the scene, pick the lens and the mode, and take the picture | 17 |
| P2 | **Control exposure and focus** | get brightness and sharpness right - by hand, or by steering the automatics - and see whether they are right | 20 |
| P3 | **Shape the image** | colour, processing, and the file that comes out | 25 |
| P4 | **Sequences and video** | many frames for one result, frames over time, and video with its sound | 18 |
| P5 | **Review and keep** | look at what was taken, keep it where I want it, and what the file says about it | 6 |
| P6 | **Make it work on this phone** | the camera API, phone-specific workarounds, and how the app itself behaves | 5 |

## P1 Frame and shoot

_see the scene, pick the lens and the mode, and take the picture_

| Entry | In the code | Where | API | Reference apps | Overlaps |
|---|---|---|---|---|---|
| **Camera screen**<br>The main camera screen: live preview, shutter, controls and settings. | `activity:ActivityGlassCamMain` | screen | app | Fossify, FreeDcam, Libre Camera, Manual Camera, OnePlus Camera, Open Camera, ProShot |  |
| **Camera from the lock screen**<br>Opens the camera from the lock screen without unlocking the phone. | `activity:ActivityGlassCamMainSecure` | screen | app | Fossify, FreeDcam, MA Camera, OnePlus Camera, Open Camera |  |
| **Take a photo for another app**<br>Lets another app ask GlassCam to take a photo and hands the result back. | `activity:ActivityGlassCamShare` | screen | app | Fossify, FreeDcam, Libre Camera, MA Camera, Open Camera |  |
| **Photo mode**<br>Normal photo mode. | `module:PictureModuleApi2` | capture mode | camera2 | Fossify, FreeDcam, Libre Camera, MA Camera, OnePlus Camera, Open Camera, ProShot |  |
| **Video mode**<br>Video recording mode. | `module:VideoModuleApi2` | capture mode | camera2 | Fossify, FreeDcam, Libre Camera, MA Camera, OnePlus Camera, Open Camera, ProShot |  |
| **Photo mode (MediaTek)**<br>Photo mode for MediaTek phones, including their raw capture path. | `module:PictureModuleMTK` | capture mode | camera1 | - |  |
| **Photo mode**<br>Normal photo mode on the older camera API. | `module:PictureModule` | capture mode | camera1 | - |  |
| **Video mode (LG)**<br>Video mode for LG phones using LG's own recorder. | `module:VideoModuleG3` | capture mode | camera1 | - |  |
| **Video mode**<br>Video recording mode on the older camera API. | `module:VideoModule` | capture mode | camera1 | - |  |
| **Camera switch**<br>Switches between the back and front camera (and other cameras). | `column:CAMERA_SWITCH` | camera screen, shutter column | app | Fossify, FreeDcam, Libre Camera, MA Camera, OnePlus Camera, Open Camera, ProShot | Dual Camera, Physical Camera |
| **Self-timer**<br>Sets a countdown before the photo is taken. | `column:SELF_TIMER` | camera screen, shutter column | app | Fossify, FreeDcam, Libre Camera, MA Camera, Manual Camera, OnePlus Camera, Open Camera, ProShot | Shutter speed |
| **Capture mode switch**<br>Switches capture mode (photo, video, interval, bracketing...). | `column:MODULE` | camera screen, shutter column | both | Fossify, FreeDcam, Libre Camera, OnePlus Camera, Open Camera, ProShot |  |
| **Zoom**<br>Zooms in and out. | `strip:M_ZOOM` | camera screen, manual strip | both | Fossify, FreeDcam, Libre Camera, MA Camera, OnePlus Camera, Open Camera, ProShot |  |
| **Physical Camera**<br>Pins capture to one physical lens instead of letting the phone switch automatically. | `setting:PHYSICAL_CAMERA_ID` | settings > Picture | camera2 | OnePlus Camera, Open Camera, ProShot | Camera switch, Dual Camera |
| **Digital Image Stabilization**<br>Electronic image stabilisation. | `setting:DIGITAL_IMAGE_STABILIZATION` | settings > Camera | camera1 | FreeDcam | Optical Image Stabilisation, Video Stabilization |
| **Optical Image Stabilisation**<br>Turns optical image stabilisation on or off. | `setting:OIS_MODE` | settings > Camera | both | FreeDcam, ProShot | Digital Image Stabilization, Video Stabilization |
| **Dual Camera**<br>Chooses which sensor(s) of a dual camera to use. | `setting:DUAL_PRIMARY_CAMERA_MODE` | settings > Camera | both | FreeDcam | Camera switch, Physical Camera |

## P2 Control exposure and focus

_get brightness and sharpness right - by hand, or by steering the automatics - and see whether they are right_

| Entry | In the code | Where | API | Reference apps | Overlaps |
|---|---|---|---|---|---|
| **Histogram**<br>Shows a live histogram of brightness on the preview. | `column:HISTOGRAM` | camera screen, shutter column | app | FreeDcam, OnePlus Camera, Open Camera, ProShot | Histogram (camera hardware) |
| **Focus peaking**<br>Highlights the sharp parts of the preview (focus peaking). | `column:FOCUSPEAK` | camera screen, shutter column | app | FreeDcam, OnePlus Camera, Open Camera, ProShot |  |
| **Clipping (zebras)**<br>Marks over- and under-exposed areas on the preview (zebras). | `column:CLIPPING` | camera screen, shutter column | app | FreeDcam, Open Camera |  |
| **Histogram (camera hardware)**<br>Shows a histogram computed by the camera hardware itself. | `column:HISTOGRAM_STATS_QCOM` | camera screen, shutter column | camera2 | - | Histogram |
| **Focus mode**<br>Chooses the focus mode (auto, continuous, macro, manual, ...). | `column:FOCUS_MODE` | camera screen, shutter column | both | FreeDcam, Libre Camera, MA Camera, Open Camera | Manual focus |
| **Exposure lock (AEL)**<br>Locks the automatic exposure so it stops changing. | `column:EXPOSURE_LOCK` | camera screen, shutter column | both | FreeDcam, Libre Camera, MA Camera, Open Camera |  |
| **Flash**<br>Sets the flash: off, on, auto or torch. | `column:FLASH_MODE` | camera screen, shutter column | both | Fossify, FreeDcam, Libre Camera, MA Camera, OnePlus Camera, Open Camera, ProShot | Auto Exposure Mode |
| **Manual focus**<br>Sets focus distance by hand. | `strip:M_FOCUS` | camera screen, manual strip | both | FreeDcam, Manual Camera, OnePlus Camera, Open Camera, ProShot | Focus mode |
| **ISO**<br>Sets ISO (sensor sensitivity) by hand. | `strip:M_MANUAL_ISO` | camera screen, manual strip | both | FreeDcam, MA Camera, Manual Camera, OnePlus Camera, Open Camera, ProShot | Auto Exposure Mode |
| **Shutter speed**<br>Sets shutter speed by hand. | `strip:M_EXPOSURE_TIME` | camera screen, manual strip | both | FreeDcam, MA Camera, Manual Camera, OnePlus Camera, Open Camera, ProShot | Self-timer |
| **Exposure compensation (EV)**<br>Makes the automatic exposure brighter or darker (EV). | `strip:M_EXPOSURE_COMPENSATION` | camera screen, manual strip | both | FreeDcam, Libre Camera, MA Camera, Manual Camera, OnePlus Camera, Open Camera, ProShot |  |
| **Aperture**<br>Sets the lens aperture on phones with a variable aperture. | `strip:M_APERTURE` | camera screen, manual strip | camera2 | FreeDcam, Open Camera |  |
| **Zebra high level**<br>Sets the brightness level above which zebras mark highlights as clipped. | `strip:M_ZEBRA_HIGH` | camera screen, manual strip | app | FreeDcam |  |
| **Zebra low level**<br>Sets the brightness level below which zebras mark shadows as clipped. | `strip:M_ZEBRA_LOW` | camera screen, manual strip | app | - |  |
| **Scene**<br>Picks a scene preset (night, sports, portrait...). | `setting:SCENE_MODE` | settings > Camera | both | FreeDcam, Open Camera | HDR Mode |
| **Camera Control**<br>Turns the camera's automatic controls (exposure, focus, white balance) on or off. | `setting:CONTROL_MODE` | settings > Camera | camera2 | FreeDcam | Auto Exposure Mode |
| **Red-Eye Reduction**<br>Fires a pre-flash to reduce red eye. | `setting:RED_EYE` | settings > Camera | camera1 | FreeDcam | Auto Exposure Mode |
| **Auto Exposure Mode**<br>Sets the automatic exposure mode (on, off, with flash...). | `setting:EXPOSURE_MODE` | settings > Camera | both | FreeDcam | Flash, AE Meter Area, Camera Control, Red-Eye Reduction, ISO |
| **AE Meter Area**<br>Chooses the area used to measure exposure. | `setting:AE_METERING` | settings > Camera | camera2 | FreeDcam, ProShot | Auto Exposure Mode |
| Preview settings (the other rows) | 13 rows: `PREVIEW_POST_PROCESSING_MODE`, `FOCUSPEAK_COLOR`, `ORIENTATION_HACK`, `USE_GLASSCAM_AE`, `MAX_ISO`, `MIN_ISO`, `MIN_EXPOSURE`, `MAX_EXPOSURE`, `GUIDE_LIST`, `HORIZONT_LVL`, `ZOOM_ON_MANUALFOCUS`, `ZOOM_ON_MANUALFOCUS_ZOOMFACTOR`, `ZOOM_ON_MANUALFOCUS_ZOOMDURATION` | settings > Preview | mixed | - |  |

## P3 Shape the image

_colour, processing, and the file that comes out_

| Entry | In the code | Where | API | Reference apps | Overlaps |
|---|---|---|---|---|---|
| **Tone curve**<br>Opens a tone curve to shape contrast and brightness of the image. | `column:TONE_CURVE_PARAMETER` | camera screen, shutter column | camera2 | FreeDcam, MA Camera, Open Camera, ProShot | ToneMapProfile, Tonemap, Contrast |
| **White balance (Kelvin)**<br>Sets white balance colour temperature by hand (Kelvin). | `strip:M_WHITEBALANCE` | camera screen, manual strip | both | FreeDcam, MA Camera, Manual Camera, OnePlus Camera, Open Camera, ProShot | Correlated Color Temperature, WB |
| **Contrast**<br>Sets image contrast. | `strip:M_CONTRAST` | camera screen, manual strip | camera1 | FreeDcam | Tone curve |
| **Brightness**<br>Sets image brightness. | `strip:M_Brightness` | camera screen, manual strip | camera1 | FreeDcam |  |
| **Saturation**<br>Sets colour saturation. | `strip:M_SATURATION` | camera screen, manual strip | both | FreeDcam, OnePlus Camera |  |
| **Sharpness**<br>Sets image sharpening. | `strip:M_SHARPNESS` | camera screen, manual strip | both | FreeDcam | Edge Mode |
| **Size**<br>Sets the photo resolution. | `setting:PICTURE_SIZE` | settings > Picture | both | Fossify, FreeDcam, Libre Camera, MA Camera, OnePlus Camera, Open Camera, ProShot | Raw size, YUV size |
| **YUV size**<br>Sets the size of the YUV image stream used for capture. | `setting:YUV_SIZE` | settings > Picture | camera2 | FreeDcam | Size, Raw size |
| **CustomMatrix**<br>Picks a custom colour matrix (DNG profile) for raw files. | `setting:MATRIX_SET` | settings > Raw | both | FreeDcam | Use custom DNG profile |
| **ToneMapProfile**<br>Picks the tone curve profile written into DNG files. | `setting:TONEMAP_SET` | settings > Raw | camera2 | FreeDcam | Tone curve, Tonemap |
| **Use GlassCam DngConverter**<br>Uses GlassCam's own DNG converter for 16-bit raw. | `setting:FORCE_RAW_TO_DNG` | settings > Raw | app | FreeDcam | Raw to DNG converter |
| **Use custom DNG profile**<br>Uses the chosen custom DNG colour profile under Camera2. | `setting:USE_CUSTOM_MATRIX_ON_CAMERA_2` | settings > Raw | app | FreeDcam | CustomMatrix |
| **Raw size**<br>Sets the raw image resolution. | `setting:RAW_SIZE` | settings > Raw | camera2 | FreeDcam | Size, YUV size |
| **WB**<br>Picks a white balance preset (auto, daylight, cloudy...). | `setting:WHITE_BALANCE_MODE` | settings > Camera | both | FreeDcam, Open Camera | White balance (Kelvin) |
| **Color Effect**<br>Applies a colour effect (mono, sepia, negative...). | `setting:COLOR_MODE` | settings > Camera | both | FreeDcam, OnePlus Camera, Open Camera | Virtual Lens Filter |
| **Correlated Color Temperature**<br>Sets how the camera corrects colour (auto, manual transform matrix). | `setting:COLOR_CORRECTION_MODE` | settings > Camera | camera2 | FreeDcam | White balance (Kelvin) |
| **Tonemap**<br>Sets how the camera maps tones (contrast curve mode). | `setting:TONE_MAP_MODE` | settings > Camera | camera2 | FreeDcam | Tone curve, ToneMapProfile |
| **Noise reduction**<br>Sets noise reduction for photos. | `setting:DENOISE` | settings > Camera | both | FreeDcam, MA Camera, Open Camera, ProShot | Correlated double sampling, MNFR, Temporal Denoise, Xiaomi MNFR |
| **Temporal Denoise**<br>Multi-frame temporal noise reduction for photos. | `setting:TNR` | settings > Camera | camera1 | FreeDcam | Correlated double sampling, Noise reduction, MNFR |
| **Virtual Lens Filter**<br>Applies a virtual lens filter effect. | `setting:LENS_FILTER` | settings > Camera | camera1 | FreeDcam | Color Effect |
| **Correlated double sampling**<br>Correlated double sampling: reads the sensor twice to reduce noise. | `setting:CDS_MODE` | settings > Camera | camera1 | FreeDcam | Noise reduction, Temporal Denoise |
| **Edge Mode**<br>Sets edge enhancement (sharpening). | `setting:EDGE_MODE` | settings > Camera | camera2 | FreeDcam, MA Camera, Open Camera, ProShot | Sharpness |
| Camera settings (the other rows) | 19 rows: `ANTI_BANDING_MODE`, `IMAGE_POST_PROCESSING`, `LENS_SHADE`, `SCENE_DETECT`, `TNR_V`, `PDAF`, `TRUE_POTRAIT`, `RDI`, `CHROMA_FLASH`, `OPTI_ZOOM`, `RE_FOCUS`, `SEE_MORE`, `MEMORY_COLOR_ENHANCEMENT`, `ZSL`, `NON_ZSL_MANUAL_MODE`, `HOT_PIXEL_MODE`, `DISTORTION_CORRECTION_MODE`, `FACE_DETECTOR_MODE`, `AE_TARGET_FPS` | settings > Camera | mixed | - |  |
| Picture settings (the other rows) | 4 rows: `PICTURE_FORMAT`, `SECONDARY_SENSOR_SIZE`, `JPEG_QUALITY`, `MTK_AOV_MODE` | settings > Picture | mixed | - |  |
| Raw settings (the other rows) | `setting:OPCODE`, `setting:BAYERFORMAT` | settings > Raw | mixed | - |  |

## P4 Sequences and video

_many frames for one result, frames over time, and video with its sound_

| Entry | In the code | Where | API | Reference apps | Overlaps |
|---|---|---|---|---|---|
| **DNG stacker**<br>Merges several DNG frames into one stacked DNG. | `activity:DngStackActivity` | screen | app | MA Camera, OnePlus Camera, ProShot |  |
| **Video profile editor**<br>Screen to create and edit custom video profiles (resolution, fps, bitrate, codec). | `activity:VideoProfileEditorActivity` | screen | app | FreeDcam, Libre Camera, MA Camera, OnePlus Camera |  |
| **Interval mode**<br>Interval/timelapse mode: takes a photo every set number of seconds. | `module:IntervalApi2` | capture mode | camera2 | FreeDcam, MA Camera, OnePlus Camera, Open Camera, ProShot |  |
| **Exposure bracketing mode**<br>Exposure bracketing mode: several shots at different exposures in one press. | `module:AeBracketApi2` | capture mode | camera2 | FreeDcam, Open Camera, ProShot |  |
| **Exposure bracketing mode (Huawei)**<br>Exposure bracketing mode using Huawei's own bracketing support. | `module:HuaweiAeBracketApi2` | capture mode | camera2 | - |  |
| **Focus bracketing mode**<br>Focus bracketing mode: several shots at different focus distances, for focus stacking. | `module:AfBracketApi2` | capture mode | camera2 | FreeDcam, Open Camera |  |
| **Interval mode**<br>Interval/timelapse mode on the older camera API. | `module:IntervalModuleCamera1` | capture mode | camera1 | - |  |
| **Exposure bracketing mode**<br>Exposure bracketing mode on the older camera API. | `module:BracketModule` | capture mode | camera1 | FreeDcam |  |
| **Burst**<br>Sets how many photos one shutter press takes (burst). | `strip:M_BURST` | camera screen, manual strip | both | FreeDcam, MA Camera, Open Camera |  |
| **setting:TIMELAPSE_FRAMES**<br>Sets the playback frame rate of the assembled timelapse video. | `setting:TIMELAPSE_FRAMES` | settings > Video | app | FreeDcam | Shutter fire interval |
| **Video HDR**<br>Turns on HDR video recording. | `setting:VIDEO_HDR` | settings > Video | camera1 | FreeDcam, OnePlus Camera | HDR Mode |
| **Video Stabilization**<br>Turns video stabilisation on or off. | `setting:VIDEO_STABILIZATION` | settings > Video | both | FreeDcam, MA Camera, OnePlus Camera, Open Camera, ProShot | Digital Image Stabilization, Optical Image Stabilisation |
| **MNFR**<br>Merges several frames to reduce noise (multi-frame noise reduction). | `setting:MFNR` | settings > Picture | camera2 | FreeDcam, OnePlus Camera, Open Camera | Noise reduction, Temporal Denoise, Xiaomi MNFR |
| **Xiaomi MNFR**<br>Xiaomi's multi-frame noise reduction. | `setting:XIAOMI_MFNR` | settings > Picture | camera2 | FreeDcam | Noise reduction, MNFR |
| **Shutter fire interval**<br>Sets the time between shots in interval mode. | `setting:INTERVAL_SHUTTER_SLEEP` | settings > Interval | app | FreeDcam, Open Camera | setting:TIMELAPSE_FRAMES |
| **HDR Mode**<br>Turns the camera's built-in HDR on or off. | `setting:HDR_MODE` | settings > Camera | camera1 | FreeDcam, OnePlus Camera, Open Camera, ProShot | Scene, Video HDR |
| Interval settings (the other rows) | `setting:INTERVAL_DURATION` | settings > Interval | mixed | - |  |
| Video settings (the other rows) | `setting:VIDEO_PROFILES`, `setting:@setting_videoprofileeditor_header`, `setting:VIDEO_AUDIO_SOURCE` | settings > Video | mixed | - |  |

## P5 Review and keep

_look at what was taken, keep it where I want it, and what the file says about it_

| Entry | In the code | Where | API | Reference apps | Overlaps |
|---|---|---|---|---|---|
| **GlassGallery**<br>Gallery to browse, view and delete the photos and videos taken. | `activity:ActivityGlassCamViewer` | screen | app | Fossify, FreeDcam, Libre Camera, MA Camera, OnePlus Camera, Open Camera, ProShot |  |
| **Raw to DNG converter**<br>Converts raw files into DNG files the person can edit. | `activity:DngConvertingActivity` | screen | app | FreeDcam | Use GlassCam DngConverter |
| **Photo info (EXIF)**<br>Shows the photo's details (EXIF information). | `viewer:button_info` | gallery viewer | app | FreeDcam |  |
| **Back to the grid**<br>Closes the full-screen view and returns to the gallery. | `viewer:button_closeView` | gallery viewer | app | - |  |
| **Play video**<br>Plays the selected video. | `viewer:button_play` | gallery viewer | app | FreeDcam |  |
| **Delete**<br>Deletes the shown file. | `viewer:button_delete` | gallery viewer | app | FreeDcam, Open Camera |  |

## P6 Make it work on this phone

_the camera API, phone-specific workarounds, and how the app itself behaves_

| Entry | In the code | Where | API | Reference apps | Overlaps |
|---|---|---|---|---|---|
| **Save Camera Parameters**<br>Saves the camera's parameters to a file for debugging. | `setting:@setting_savecamparams_header` | settings > Etc | app | FreeDcam | Dump vendor keys |
| **Dump vendor keys**<br>Dumps the phone's vendor camera keys to a file for debugging. | `setting:@setting_dump_vendor_keys_header` | settings > Etc | app | FreeDcam | Save Camera Parameters |
| **Swap preview width and height**<br>Swaps preview width and height when the preview looks stretched. | `setting:SWITCH_ASPECT_RATIO` | settings > Preview | app | FreeDcam | Force Wide Preview |
| **Force Wide Preview**<br>Uses the widest available preview size. | `setting:FORCE_WIDE_PREVIEW` | settings > Preview | camera2 | FreeDcam, Open Camera | Swap preview width and height |
| Etc settings (the other rows) | 12 rows: `@setting_api_header`, `SD_SAVE_LOCATION`, `LOCATION_MODE`, `NIGHT_OVERLAY`, `TOUCH_TO_CAPTURE`, `PLAY_SHUTTER_SOUND`, `FOCUS_STACK_MERGE`, `OPEN_CAMERA_1_LEGACY`, `@setting_featuredetector_header`, `PREVIEW_TEMPLATE`, `CAPTURE_TEMPLATE`, `HIDE_CAMERA_UI` | settings > Etc | mixed | - |  |

## Parts

Fragments that are only part of a screen: `fragment:DngConvertingFragment`, `fragment:EmptyFragment`, `fragment:GridViewFragment`, `fragment:HelpFragment`, `fragment:HorizontLineFragment`, `fragment:ImageFragment`, `fragment:ManualFragment`, `fragment:NextGenCameraSettingFragment`, `fragment:NextGenCameraUiFragment`, `fragment:NextGenHorizontalValuesFragment`, `fragment:NextGenMainFragment`, `fragment:NextGenManualsFragment`, `fragment:NextGenSettingDummyFragment`, `fragment:ScreenSlideFragment`, `fragment:VideoProfileEditorFragment`.
