"""build_artifact.py -- assemble the critique artifact (HTML with embedded, downscaled screenshots).

    python tools/critique/build_artifact.py   -> tools/critique/GlassCam_Critique.html
"""
import base64
import io
import os
from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
SHOTS = os.path.join(HERE, "screenshots")
OUT = os.path.join(HERE, "GlassCam_Critique.html")


def img(name, width=760, crop=None, quality=78):
    im = Image.open(os.path.join(SHOTS, name)).convert("RGB")
    if crop:
        im = im.crop(crop)
    if im.width > width:
        im = im.resize((width, round(im.height * width / im.width)), Image.LANCZOS)
    buf = io.BytesIO()
    im.save(buf, "JPEG", quality=quality, optimize=True)
    return "data:image/jpeg;base64," + base64.b64encode(buf.getvalue()).decode()


def fig(name, caption, width=760, crop=None, alt=None):
    return (f'<figure><img src="{img(name, width, crop)}" alt="{alt or caption}" loading="lazy">'
            f'<figcaption><span class="shot">{name.split("_")[0]}</span> {caption}</figcaption></figure>')


def chip(sev):
    return f'<span class="sev sev-{sev.lower()}">{sev}</span>'


def voice(v):
    return f'<span class="voice voice-{v}">{v}</span>'


FINDINGS = [
    ("High", "measured", "Camera1 API is dead on this device",
     "Etc ▸ API ▸ camera1 leaves a live preview with no controls at all. <code>NullPointerException</code> in <code>PictureModule.createPreview</code> (<code>apis/camera1/modules/PictureModule.java:201</code>, the <code>Preview</code> is null) aborts <code>ParametersHandler.initParameters</code>, so the UI is never built. Switching back to camera2 does not rebuild it; only a force-stop recovers.",
     "Create the post-processing <code>Preview</code> before <code>PictureModule.InitModule</code> on the Camera1 path (or null-guard to a plain <code>SurfaceTexture</code>); make the API switch tear down and re-inflate <code>NextGenCameraUiFragment</code>. Until then hide the camera1 row where <code>Camera1.initCamera</code> throws.", "76_camera1_ui.png"),
    ("High", "measured", "AF-bracket range can never be set",
     "<code>NextGenManualsFragment.java:240</code> shows <code>AfBracketSettingsView</code> only when <code>currentButton instanceof ManualButtonMF</code> (the legacy class) while the NextGen strip creates <code>NextGenMfItem</code> — line 157 already checks the right class. Tapping the focus button in Af-Bracket mode opens the plain focus seekbar; Near/Far/frames are unreachable, so every bracket runs with the defaults (10 frames).",
     "<code>instanceof NextGenMfItem</code> at line 240 — one token — then re-walk “AF bracket → focus button”.", "53_afbracket_settings.png"),
    ("High", "measured", "Gallery options wired to the wrong actions; stacker unreachable",
     "<code>ButtonOptionsModel.java:10</code> labels <code>{Delete File, StackJpeg, Raw to Dng}</code> against listeners <code>{delete, rawToDng, dngStack}</code>. On device: <b>RAW TO DNG</b> enters DNGSTACK selection and then toasts “DNG stacking needs files with direct filesystem access…”; <b>STACKJPEG</b> enters RAWTODNG mode. <code>\"DngStack\"</code> is commented out of the array, so the stacker has no honest entry point either.",
     "Align the arrays (<code>{Delete File, Raw to Dng}</code> ↔ <code>{delete, rawToDng}</code>); drop the stack entry until the native <code>TIFFOpen</code> accepts a descriptor; add an empty-state line to RAWTODNG mode.", "68_dngstack_result.png"),
    ("High", "measured", "Seventh settings tab clipped to 15 dp; strip scroll closes the drawer",
     "uiautomator reports the Etc tab at 15.3 × 48 dp — six tabs fit the 60 % panel. Swiping the strip right-to-left is consumed by the pager as “close settings”. Etc is reachable only because selecting Camera auto-scrolls the strip. A first-time user cannot find Storage Location, API or Geo-tag.",
     "Fewer tabs (Interval and Raw become sections of Picture) or a two-row TabLayout below sw600dp; the strip requests disallow-intercept while it can scroll.", "23_settings_open.png"),
    ("High", "measured", "Row headers ellipsize in the two-column settings grid",
     "“Use GlassCam Dn…”, “Enable to use cust…”, “Optical Image Stabilis…”, “Duration till interval…”, “Check for new up…”, “Preview PostProcessi…”, “Switch width and …”, “Auto Zoom on Ma…”. The column is 224 dp; the header is 16 sp bold with the value or switch on the same line.",
     "Single column below 600 dp (<code>spanCount = 1</code>), or header on its own line with the value right-aligned below at 14 sp. The sw600dp bucket keeps two columns.", "25_tab_raw.png"),
    ("High", "measured", "Shutter-column captions marquee and clip",
     "<code>textView_front</code> under the focus glyph scrolls “Continouse Picture” through a 40 dp column (the first five screenshots show “Continou”, “ntinouse”, “Contin”, “icture”); “Pic” and “Af-Brack…” clip under the module glyph; a stray “0” renders at x ≈ 95 dp in the left letterbox band. Animated text next to the shutter is the most distracting thing on the screen.",
     "Caption = the value’s short form (Auto / Macro / C-AF / C-Video / Off; Pic / Vid / Int / AEB / AFB), 9 sp, single line, no marquee; 56 dp column (see the optimizer result).", "13_main_rest.png"),
    ("Med", "measured", "Copy defects that read as bugs",
     "Help page 4 “…to close Manual)” stray paren; “Force Wide Preview” carries the DNG-converter description “Only for 16bit Dng(RAW_SENSOR)…”; Storage Location still says “NOTE: On KitKat… Lollipop and Marshmallow…” in a minSdk 30 app; “Face Dectection”, “Continouse”, “True Potrait”, “reseted”, “wich”; “Picture Size :”; trailing-colon inconsistency (“WB” vs “Scene:”); the Timelapse-frames row has no header at all — just <code>+ − 30.0 fps</code>; value pickers have no title and “Back” is styled like a value.",
     "One pass over <code>strings.xml</code>; a header for <code>settings_expandable_childs_number.xml</code>; a title row and a distinct Back chip in <code>NextGenSettingValuePickerAdapter</code>.", "32_tab_preview_real.png"),
    ("Med", "judgement", "Horizon level is ambiguous and expensive",
     "Two roll ticks at 11 and 5 o’clock <i>and</i> a pitch arrow at top-centre, hairline strokes, no number, no “level now” colour state, drawn over the composition crosshair. <b>Measured:</b> <code>CompassDrawer</code> invalidates continuously — uiautomator could not get an idle state while it was on. Every reference with a level (ProShot, MA Camera, Open Camera) draws one bar that turns green or solid when level.",
     "One horizontal bar + centre tick, 2 dp stroke with a 1 dp dark outline, green at |roll| &lt; 0.5°, roll angle in 10 sp beside it; invalidate only on a ≥ 0.2° change; auto-hide 2 s after level.", "40_main_grid_level.png"),
    ("Med", "measured", "No gallery from the camera screen",
     "No thumbnail, no button; GlassGallery is a second launcher icon. Every reference app has a one-tap gallery from the viewfinder.",
     "Last-capture thumbnail as a cell in the shutter column — the optimizer places it directly under the shutter.", "54_gallery_grid.png"),
    ("Med", "measured", "Contrast is scene-dependent and undefended",
     "92 of 145 text elements fall below 4.5:1 on a mid-grey scene: white 10 dp info-overlay text 4.0:1, white captions 3.95:1, lime <code>#64DD17</code> manual values 2.24:1, <code>#99FFFFFF</code> descriptions 2.46:1 (all ≥ 7:1 over black). The settings scrim rescues its rows (lime 8.6:1, secondary 6.4:1). The overlay’s <code>#02000000</code> background is effectively none.",
     "Give the info overlay and the captions the same <code>#B3000000</code> pill the scrim uses, or the 1 dp shadow outline <code>StyledTextView</code> already draws for the strip.", None),
    ("Med", "judgement", "Recording state is nearly invisible",
     "A red square inside the shutter plus a ~9 sp green timer overlapping the shutter’s lower edge; the info overlay keeps showing the <i>picture</i> format (“Jpg+16bitDng”) in video mode and drops the storage-left count.",
     "Red preview border or a REC pill in the overlay; timer in the overlay at 12 sp; show the video profile there in video mode.", "47_video_recording.png"),
    ("Med", "measured", "Video Profile Editor and DNG converter are a different app",
     "<code>#aaa</code> ground, white text at 2.32:1, the stock pink <code>colorAccent</code> switch, navigation bar visible, CamelCase labels (“AudioBitRate:”), no title.",
     "Restyle on the NextGen tokens (scrim, lime, 16 dp radius) with a title row; immersive like the camera.", "70_video_profile_editor.png"),
    ("Med", "measured", "Session state leaks",
     "The module survives a force-stop (a relaunch fired a 10-frame AF bracket on the first shutter tap); burst stayed at 10 after leaving AF-bracket; the help overlay returns on every launch until its checkbox is ticked; an ISO drag silently switches to manual (“450” replaces “(A)6400”) with no way back to auto on the seekbar.",
     "Reset module and burst on cold start; first-run-only help; an “A” target on the seekbar’s auto value.", "22_manual_iso_dragged.png"),
    ("Low", "measured", "Seekbar labels clip; exposure list starts at 1/100000 s",
     "Rotated labels are cut at the pill edge; the shutter list opens at 1/100000, 1/90000, 1/75000…",
     "Clamp to <code>SENSOR_INFO_EXPOSURE_TIME_RANGE</code>; label every fifth stop.", "17_manual_shutter.png"),
    ("Low", "measured", "Swallowed exception in video",
     "<code>NumberFormatException: For input string: \"Auto\"</code> at <code>VideoModuleApi2.java:461</code> on every video session configure; recording still works.",
     "Parse the ISO/shutter value defensively.", None),
]

REGISTER = [
    ("Picture JPEG", "works", "DCIM/GlassCam/2026_09_16_01_08_46.jpg, 3.8 MB"),
    ("Picture 16-bit DNG", "works", "24.1 MB DNG"),
    ("Picture Jpg+16bitDng", "works", "pair with shared timestamp"),
    ("Video 1080p", "works", "MOV_….mp4 35 MB; 9 sp timer; swallowed NumberFormatException"),
    ("Video into SAF folder", "works", "lands in the chosen folder’s root, no GlassCam/ subfolder"),
    ("Interval", "works", "1 s cadence, stopped by the shutter"),
    ("AE bracket", "works", "3 frames _0 _1 _2"),
    ("AF bracket", "partial", "runs with defaults (10 frames); range unsettable"),
    ("Focus-stack merge", "unverified", "needs the AF range"),
    ("Raw ZSL", "dead", "never registered in ModuleHandlerApi2"),
    ("Manual strip", "partial", "8 of 14 controls on this SoC: zoom, focus, ISO, shutter, EV, burst, saturation, sharpness"),
    ("Seekbar drag", "works", "switches ISO to manual silently"),
    ("Shutter-column popups", "works", "flash chips uncaptioned; tone-curve button absent here"),
    ("Histogram / waveform", "works", "draws in the left letterbox band"),
    ("Composition grid", "works", "9 patterns + None"),
    ("Image Level", "works", "ambiguous, continuous redraw"),
    ("Settings, 7 tabs", "partial", "Etc reachable only by auto-scroll; headers ellipsize"),
    ("Value pickers / switches", "works", "untitled; list scroll resets after a pick"),
    ("Storage Location (SAF)", "works", "documentsui → Pictures → grant"),
    ("Camera1 API", "dead", "NPE, no UI; switch-back needs force-stop"),
    ("Gallery grid / viewer / EXIF", "works", "no entry from camera; Back exits the activity; Play on stills"),
    ("Gallery options", "dead", "labels mis-wired to listeners"),
    ("DNG stacker", "dead", "no entry + SAF refusal"),
    ("Raw→DNG converter", "partial", "reachable only under “STACKJPEG”; empty grid, no message"),
    ("Video Profile Editor", "works", "alien styling"),
    ("Check for updates", "dead", "isGithubRelease = false"),
    ("Share intent", "dead", "activity commented out of the manifest"),
]

TAPS = [
    ("photo", "1", "1", "1", "1", "1", "1", "1", "1", "1"),
    ("switch to video + record", "3", "3", "1", "2", "2", "2", "2", "2", "2"),
    ("set ISO", "2+drag", "2+drag", "1+drag", "1+drag", "1+drag", "1+drag", "3+drag", "—", "1+drag"),
    ("set shutter", "2+drag", "2+drag", "1+drag", "1+drag", "—", "2+drag", "3+drag", "—", "1+drag"),
    ("toggle level", "4", "4", "1", "≈2", "1", "2", "≈3", "—", "—"),
    ("RAW on/off", "4", "4", "2", "1", "—", "3", "2", "—", "≈2"),
    ("open gallery", "launcher", "1", "1", "1", "1", "1", "1", "1", "1"),
]

FEATURES = [
    ("Gallery thumbnail in the column", "all eight", "essential — closes the biggest workflow gap", "S"),
    ("Format chip cycling Jpeg / DNG / Jpg+DNG", "ProShot, Manual Camera Pro", "high — the overlay already shows it", "S"),
    ("Second (red) shutter for video, or long-press", "ProShot", "high — removes the module popup for the common case", "M"),
    ("Single-bar level with green state and angle", "ProShot, MA Camera, Open Camera", "high", "S–M"),
    ("Tap the strip value to open the seekbar", "ProShot, MCP, Libre", "medium", "S"),
    ("Exposure compensation selected by default", "Open Camera, OnePlus", "medium", "S"),
    ("“A” target on the seekbar’s auto value", "MCP, Libre", "medium", "S"),
    ("Level auto-hide when level for 2 s", "Open Camera", "medium", "S"),
    ("Focus-peaking colour in the strip popup", "ProShot, Open Camera", "medium", "S"),
    ("Share intent re-enabled on the SAF path", "all", "low for one owner, but free", "S"),
    ("ZSL revival", "—", "low until verified on HAL3", "L"),
    ("Pro-mode presets (ISO + shutter + WB)", "MCP, ProShot", "low–medium", "M"),
]

BATCHES = [
    ("Batch 1 — one-line and copy", "XML / strings, about an hour", [
        "<code>NextGenManualsFragment.java:240</code> ManualButtonMF → NextGenMfItem",
        "<code>ButtonOptionsModel.java:10</code> align labels with listeners; remove the stack entry",
        "<code>strings.xml</code>: Continuous, Detection, Portrait, reset, which; help paren; Force Wide Preview description; drop the KitKat note; colon policy",
        "Header for the Timelapse-frames row",
        "74 hard-coded layout strings into resources (help, profile editor, curve view, DNG converter)",
        "<code>cameraui_infooverlay_textsize</code> 10dip → 10sp",
    ]),
    ("Batch 2 — layout", "XML + small Java, about a day", [
        "Settings grid single column below sw600dp; header on its own line",
        "Five tabs (Interval and Raw fold into Picture); strip keeps its own horizontal scroll",
        "Shutter column: 56 dp cells, 72 dp shutter, gallery cell under the shutter, no marquee, seekbar in the left band",
        "Info overlay: scrim pill; format chip as tap-to-cycle; REC timer and video profile in video mode",
        "Value picker title row and distinct Back chip; keep list scroll across pickers",
        "Level: single bar + tick + angle, green state, change-gated invalidation, auto-hide",
        "Gallery: Back returns to the grid; hide Play on stills; 48 dp targets; thumbnails fill the width",
        "Profile editor, DNG converter, curve view on the NextGen tokens, immersive",
    ]),
    ("Batch 3 — function", "Java, one to two days", [
        "Camera1: build the Preview before PictureModule.InitModule; rebuild the UI on API switch; hide camera1 where it throws",
        "Reset module and burst on cold start; first-run-only help",
        "<code>VideoModuleApi2.java:461</code> parse “Auto” defensively",
        "SAF: create a GlassCam/ child under the chosen tree so captures don’t land in Pictures/ root",
        "Re-enable the share activity on the UriHolder path; remove the KillerInk link and the dead update switch",
        "Delete PresetGridHarnessActivity and the five unreferenced cameraui_* layouts; retire the stock colorPrimary/colorAccent",
    ]),
]


def rows(data, cls=""):
    return "".join("<tr>" + "".join(f"<td>{c}</td>" for c in r) + "</tr>" for r in data)


def state(s):
    return f'<span class="state state-{s}">{s}</span>'


html = []
html.append("""<title>GlassCam Critique</title>
<link rel="stylesheet" href="https://fonts.googleapis.com/css2?family=Barlow+Condensed:wght@500;600;700&family=IBM+Plex+Sans:ital,wght@0,400;0,500;0,600;1,400&family=IBM+Plex+Mono:wght@400;500&display=swap">
<style>
:root{--bg:#F5F4F0;--bg2:#ECEBE5;--ink:#1B1C1A;--ink2:#4F544F;--rule:#D6D5CE;--accent:#2E7D14;--accent-ink:#1F5A0E;
--high:#B3261E;--med:#9A6200;--low:#5F6663;--ok:#2E7D14;--dead:#B3261E;--partial:#9A6200;--unv:#5F6663;
--chip-bg:#FFFFFF;--code-bg:#E9EAE4;--figure-bg:#0E0F0D}
@media (prefers-color-scheme: dark){:root:not([data-theme="light"]){--bg:#131412;--bg2:#1B1D1A;--ink:#E6E4DF;--ink2:#A9ADA7;--rule:#2C2F2B;--accent:#7BD63B;--accent-ink:#9BE562;
--high:#F28B82;--med:#F0B84A;--low:#9AA09B;--ok:#7BD63B;--dead:#F28B82;--partial:#F0B84A;--unv:#9AA09B;--chip-bg:#1B1D1A;--code-bg:#232622;--figure-bg:#000}}
:root[data-theme="dark"]{--bg:#131412;--bg2:#1B1D1A;--ink:#E6E4DF;--ink2:#A9ADA7;--rule:#2C2F2B;--accent:#7BD63B;--accent-ink:#9BE562;
--high:#F28B82;--med:#F0B84A;--low:#9AA09B;--ok:#7BD63B;--dead:#F28B82;--partial:#F0B84A;--unv:#9AA09B;--chip-bg:#1B1D1A;--code-bg:#232622;--figure-bg:#000}
html{color-scheme:light dark}
body{margin:0;background:var(--bg);color:var(--ink);font:16px/1.6 "IBM Plex Sans",system-ui,sans-serif;padding-inline:16px;padding-block:0 80px}
h1,h2,h3{font-family:"Barlow Condensed","Arial Narrow",sans-serif;text-wrap:balance;line-height:1.1;margin:0}
h1{font-size:clamp(40px,7vw,64px);font-weight:700;letter-spacing:-.01em}
h2{font-size:30px;font-weight:600;margin-top:64px;padding-top:16px;border-top:2px solid var(--ink)}
h3{font-size:22px;font-weight:600;margin-top:8px}
code{font:13.5px/1.5 "IBM Plex Mono",ui-monospace,monospace;background:var(--code-bg);padding:1px 5px;border-radius:3px}
a{color:var(--accent-ink)}
.wrap{max-width:1180px;margin:0 auto;display:grid;grid-template-columns:200px minmax(0,68ch);gap:48px}
@media (max-width:1000px){.wrap{grid-template-columns:minmax(0,1fr)}.rail{display:none}}
.rail{position:sticky;top:24px;align-self:start;font-size:14px;padding-top:120px}
.rail a{display:block;color:var(--ink2);text-decoration:none;padding:4px 0;border-left:2px solid var(--rule);padding-left:12px}
.rail a:hover,.rail a:focus-visible{color:var(--ink);border-color:var(--accent)}
header{padding-block:56px 8px}
.eyebrow{font:500 12px/1 "IBM Plex Mono",monospace;letter-spacing:.12em;text-transform:uppercase;color:var(--ink2);margin-bottom:16px}
.lede{font-size:18px;color:var(--ink2);max-width:60ch;margin-top:16px}
.facts{display:grid;grid-template-columns:repeat(auto-fit,minmax(150px,1fr));gap:12px 24px;margin-top:28px;padding-top:20px;border-top:1px solid var(--rule)}
.facts div{font-size:13px;color:var(--ink2)}
.facts b{display:block;font:600 26px/1.1 "Barlow Condensed",sans-serif;color:var(--ink);font-variant-numeric:tabular-nums}
.voice{font:500 11px/1 "IBM Plex Mono",monospace;letter-spacing:.08em;text-transform:uppercase;padding:3px 6px;border:1px solid var(--rule);border-radius:2px;color:var(--ink2);vertical-align:middle}
.voice-measured{border-color:var(--accent);color:var(--accent-ink)}
.sev{font:600 12px/1 "Barlow Condensed",sans-serif;letter-spacing:.06em;text-transform:uppercase;padding:4px 7px;border-radius:2px;color:#fff;vertical-align:middle}
.sev-high{background:var(--high)}.sev-med{background:var(--med)}.sev-low{background:var(--low)}
ol.findings{list-style:none;padding:0;margin:24px 0 0;counter-reset:f}
ol.findings>li{counter-increment:f;padding:24px 0;border-top:1px solid var(--rule);display:grid;grid-template-columns:36px minmax(0,1fr);gap:0 12px}
ol.findings>li::before{content:counter(f);font:600 22px/1.2 "Barlow Condensed",sans-serif;color:var(--ink2);font-variant-numeric:tabular-nums}
.f-head{display:flex;flex-wrap:wrap;gap:8px;align-items:center;margin-bottom:8px}
.f-body{color:var(--ink);max-width:66ch}
.f-fix{margin-top:10px;padding-left:12px;border-left:3px solid var(--accent);color:var(--ink2)}
.f-fix b{color:var(--ink);font-weight:600}
figure{margin:16px 0 0;grid-column:1/-1}
figure img{display:block;width:100%;max-width:100%;height:auto;background:var(--figure-bg);border-radius:3px}
figcaption{font-size:13px;color:var(--ink2);margin-top:6px}
.shot{font:500 12px "IBM Plex Mono",monospace;color:var(--accent-ink);margin-right:6px}
.tablewrap{overflow-x:auto;margin-top:16px}
table{border-collapse:collapse;width:100%;font-size:14px;font-variant-numeric:tabular-nums}
th{font:500 11px/1.3 "IBM Plex Mono",monospace;letter-spacing:.08em;text-transform:uppercase;color:var(--ink2);text-align:left;padding:8px 10px 8px 0;border-bottom:1px solid var(--ink);vertical-align:bottom}
td{padding:8px 10px 8px 0;border-bottom:1px solid var(--rule);vertical-align:top}
td:first-child{font-weight:500}
.state{font:600 11px/1 "Barlow Condensed",sans-serif;letter-spacing:.06em;text-transform:uppercase;padding:3px 6px;border-radius:2px;border:1px solid currentColor;white-space:nowrap}
.state-works{color:var(--ok)}.state-dead{color:var(--dead)}.state-partial{color:var(--partial)}.state-unverified{color:var(--unv)}
.gc td:nth-child(2){font-weight:600}
.dim{margin-top:20px}.dim h3{color:var(--ink)}.dim p{margin:6px 0 0;max-width:66ch}
.two{display:grid;grid-template-columns:repeat(auto-fit,minmax(240px,1fr));gap:16px;margin-top:16px}
.opt{display:grid;grid-template-columns:1fr auto auto;gap:6px 18px;font-size:14px;margin-top:12px;font-variant-numeric:tabular-nums}
.opt div:nth-child(3n+2),.opt div:nth-child(3n+3){text-align:right}
.opt .h{font:500 11px "IBM Plex Mono",monospace;letter-spacing:.08em;text-transform:uppercase;color:var(--ink2);border-bottom:1px solid var(--ink);padding-bottom:6px}
.opt .win{color:var(--accent-ink);font-weight:600}
.batch{margin-top:20px}.batch h3 span{font:400 14px "IBM Plex Sans",sans-serif;color:var(--ink2);margin-left:8px}
.batch ol{margin:8px 0 0;padding-left:22px}.batch li{margin:4px 0}
ul.plain{padding-left:20px}ul.plain li{margin:4px 0}
.note{background:var(--bg2);padding:14px 16px;border-radius:3px;font-size:14px;color:var(--ink2);margin-top:16px}
@media (prefers-reduced-motion:no-preference){html{scroll-behavior:smooth}}
:focus-visible{outline:2px solid var(--accent);outline-offset:2px}
</style>
<div class="wrap">
<nav class="rail" aria-label="Sections">
<a href="#priorities">Top priorities</a><a href="#dimensions">Dimension by dimension</a><a href="#level">Horizon level</a><a href="#register">Feature register</a><a href="#workflow">Capture workflow</a><a href="#optimizer">Optimizer result</a><a href="#features">Additional features</a><a href="#working">What’s working</a><a href="#cannot">Cannot verify</a><a href="#plan">Fix plan</a><a href="#status">Batch 1 status</a><a href="#status2">Batch 2 status</a><a href="#status3">Batch 3 status</a><a href="#status3v">Batch 3 verified</a><a href="#status4">Batch 4 drawer</a><a href="#status5">Batch 5 re-score</a><a href="#settings">Settings vs references</a>
</nav>
<main>
<header>
<div class="eyebrow">Design critique · layout + feature function · 2026-09-16</div>
<h1>GlassCam Critique</h1>
<p class="lede">GlassCam 4.3.82 (<code>com.hazia.glasscam</code>) built from this tree, installed on the OnePlus 9 Pro (Android 14, 804 × 360 dp landscape) and driven over adb in English. Every finding is labelled <span class="voice voice-measured">measured</span> — arithmetic or observed device behaviour — or <span class="voice">judgement</span>, and, where a screen shows it, points at a screenshot number in <code>tools/critique/screenshots/</code>.</p>
<div class="facts">
<div><b>128</b>screenshots + a11y dumps</div><div><b>15</b>findings · 10 closed by Batches 1–3, all three device-verified</div><div><b>27</b>features walked</div><div><b>92 / 145</b>text elements under 4.5:1 on a grey scene</div><div><b>22</b>tap targets under 48 dp on device</div><div><b>8</b>reference apps on the same phone</div>
</div>
</header>
""")

html.append('<h2 id="priorities">Top priorities</h2><ol class="findings">')
for sev, v, title, body, fix, shot in FINDINGS:
    html.append(f'<li><div></div><div><div class="f-head">{chip(sev)}{voice(v)}<h3>{title}</h3></div>'
                f'<div class="f-body">{body}</div><div class="f-fix"><b>Fix.</b> {fix}</div></div>')
    if shot:
        cap = {"76_camera1_ui.png": "Camera1 selected: preview and info overlay, no controls.",
               "53_afbracket_settings.png": "Af-Bracket mode, focus button tapped: the plain seekbar, no Near/Far view.",
               "68_dngstack_result.png": "After “RAW TO DNG” → DNGSTACK → run: the storage toast (itself truncated).",
               "23_settings_open.png": "Settings drawer at rest: six tabs visible, Etc clipped at the right edge; the Timelapse row has no header.",
               "25_tab_raw.png": "Raw tab: two headers and one description ellipsized.",
               "13_main_rest.png": "Main screen at rest: 4:3 preview in 162 dp bands, marquee caption under the focus glyph, strip inside the preview.",
               "32_tab_preview_real.png": "Preview tab: ellipsized headers, Force Wide Preview with the DNG-converter description.",
               "40_main_grid_level.png": "Image Level on with the Center + grid: arcs, pitch arrow, two roll ticks, crosshair.",
               "54_gallery_grid.png": "GlassGallery root: one folder tile with its label over the icon, 30 dp back chevron.",
               "47_video_recording.png": "Recording: red square in the shutter, green timer on its edge, overlay still says Jpg+16bitDng.",
               "70_video_profile_editor.png": "Video Profile Editor: #aaa ground, pink stock switch, navigation bar.",
               "22_manual_iso_dragged.png": "After one seekbar drag the strip reads “450” — ISO is now manual with no auto affordance.",
               "17_manual_shutter.png": "Shutter seekbar: rotated labels clipped at the pill edge, list starting at 1/100000."}[shot]
        html.append(fig(shot, cap))
    html.append("</li>")
html.append("</ol>")

html.append("""<h2 id="dimensions">Dimension by dimension</h2>
<div class="dim"><h3>First impression <span class="voice">judgement</span></h3><p>The eye lands on the white 80 dp shutter — the measured focal point is (0.79, 0.53) by edge energy, which is correct for a camera. Purpose is clear: a manual camera. Register is “engineering tool”: icon-font glyphs, no colour except the lime state, a 4:3 preview letterboxed inside a 20:9 screen. Chrome covers 5 % of the frame, which is admirably restrained. The marquee caption is the one thing that fights the calm.</p></div>
<div class="dim"><h3>Visual hierarchy <span class="voice">judgement</span></h3><p>Shutter, then the manual strip’s eight icon + value pairs along the preview’s bottom edge (inside the preview, so they compete with the scene), then the seven text-switches. The info overlay reads as a fourth tier but carries the state you check most: format and storage. Whitespace is structural (the bands) rather than designed — the left band holds only the histogram and the stray “0”.</p></div>
<div class="dim"><h3>Typography <span class="voice voice-measured">measured</span></h3><p>Scale in use: 9 sp manual values · 10 <b>dp</b> info overlay (ignores font scaling) · 12 / 14 / 16 sp settings description / value / header · 13 sp tabs · 22 sp glyphs · 40 dp text size in <code>styled_textview.xml</code>. Two text dimens are authored in dp. The settings ladder 12/14/16 is too tight for a two-column grid; the icon font is the app’s signature and holds everywhere except the flash popup, whose four chips are near-identical 12 px glyphs with no captions.</p></div>
<div class="dim"><h3>Colour and contrast <span class="voice voice-measured">measured</span></h3><p>20 colours defined. The NextGen tokens (<code>#B3000000</code> scrim, <code>#64DD17</code> accent, <code>#99FFFFFF</code> secondary, <code>#33FFFFFF</code> outline) are coherent across drawer, strip and captions. Stock <code>colorPrimary #3f51b5</code> / <code>colorAccent #FF4081</code> still appear — the pink switch in the profile editor. 84 hard-coded colours in layouts, five of them <code>#02000000</code> backgrounds that do nothing. Rendered palette of the main screen: <code>#080808 #181818 #180808 #181828 #282828</code>, mean chroma 3 — the UI adds no colour of its own except the lime, which is right.</p></div>
<div class="dim"><h3>Layout and spacing <span class="voice voice-measured">measured</span></h3><p>150 margin/padding literals across 40 layouts, 25 distinct values (0–150 dp); 128 off the 8 dp grid, 122 off the 4 dp grid (5, 10, 29, 93, 141, 147 dp). The shutter column is a ragged two-column stack: three rows above the shutter with two empty left cells, two rows below. Tap targets: text-switch glyphs 30 × 29 dp in a 40 dp column, manual buttons 40–71 × 25 dp, Etc tab 15 dp, gallery buttons 30–40 dp, switches 47 × 27 dp — 18 declared under 48 dp in XML, 22 observed on device. The preview switches from 4:3 to 16:9 in video mode; the chrome does not move with it.</p></div>
<div class="dim"><h3>Copy <span class="voice voice-measured">measured</span></h3><p>278 default strings, 130 Italian (5 empty) — 45 % by key. 74 strings hard-coded in layouts (help overlay, the whole profile editor, curve view, DNG converter). Tab titles hard-coded in Java. Typos: Continouse, Dectection, Potrait, reseted, wich. Obsolete: the KitKat storage note. Wrong: the Force Wide Preview description. “Check for new updates” is a switch for a feature that is hard-off and still links to KillerInk’s GitHub.</p></div>
<div class="dim"><h3>Consistency <span class="voice">judgement</span></h3><p>Three visual systems coexist: NextGen (camera + drawer, coherent), the viewer (black, white glyph column, stock grey uppercase buttons), and AppCompat-default forms (profile editor, DNG converter, curve view). Corner radius: 16 dp on the drawer and seekbar pill, 0 elsewhere. The signature stops at the drawer’s edge.</p></div>
<div class="dim"><h3>Interactivity <span class="voice voice-measured">measured</span></h3><p>Ripple on settings rows — good. No pressed state on text-switches or manual buttons beyond the lime swap. No focus ring anywhere. Gestures: L→R opens settings, R→L closes, up opens manuals — the tab strip’s own scroll collides with the close gesture. Back from the image viewer exits the whole gallery activity. The drawer remembers its tab but the list loses its scroll after a picker, so the same coordinate hits a different row.</p></div>
<div class="dim"><h3>Fit for purpose <span class="voice">judgement</span></h3><p>For one owner who wants RAW + manual on an unrooted phone, the core loop — frame, set a manual value, shutter, DNG on disk — works and is fast. What does not fit: landscape-locked camera against a portrait gallery; no gallery from the viewfinder; a drawer that hides four of the most-used rows behind an unreachable tab; a level you cannot trust at a glance.</p></div>
""")

html.append('<h2 id="level">Horizon level</h2>')
html.append('<p><span class="voice voice-measured">measured</span> <code>HorizontLineFragment</code> / <code>CompassDrawer</code>, gauge 250 dp (320 dp on sw600dp), enabled via Preview ▸ Image Level ▸ On — two pickers, four taps. Draws two tick arcs, a pitch arrow, two roll ticks and, with a grid selected, the crosshair. Redraws continuously. No numeric readout, no colour state, no auto-hide.</p>')
html.append(fig("40_main_grid_level.png", "The gauge at native size: the arrow says level, the diagonal ticks say ~30° of roll.", crop=(850, 180, 1650, 900)))
html.append('<p><span class="voice">judgement</span> It is an artificial-horizon instrument, not a level. At arm’s length you want one answer — am I level — and one direction to correct. ProShot draws one line with end ticks that turns green when level; MA Camera one bar with a centre dot on a dark shadow; Open Camera one line plus the angle. Recommendation: one bar + centre tick + roll angle, pitch as a small vertical tick only when shooting downward, green within 0.5°, and no invalidation when nothing changed.</p>')
html.append('<div class="two">' + fig("93_proshot_main.png", "ProShot: single green line, ticks at the ends.", width=360, crop=(0, 900, 1080, 1500)) + fig("92_ma_level.png", "MA Camera: one bar, centre dot, shadowed.", width=360, crop=(0, 700, 1080, 1300)) + "</div>")

html.append('<h2 id="register">Feature walk register</h2><p>Camera2 unless stated. Every “works” row has a file on disk behind it.</p><div class="tablewrap"><table><thead><tr><th>Feature</th><th>Result</th><th>Evidence</th></tr></thead><tbody>')
for f, s, e in REGISTER:
    html.append(f"<tr><td>{f}</td><td>{state(s)}</td><td>{e}</td></tr>")
html.append("</tbody></table></div>")

html.append('<h2 id="workflow">Capture workflow, tap for tap</h2><p>Taps after the app is on screen; “+drag” is a slider gesture; “—” means the app has no such control. GlassCam, ProShot, Manual Camera Pro, MA Camera and Open Camera were walked; stock OnePlus, Fossify and Libre are read from their main screens and marked ≈.</p><div class="tablewrap"><table class="gc"><thead><tr><th>Task</th><th>GlassCam</th><th>FreeDcam</th><th>ProShot</th><th>Manual Cam Pro</th><th>MA Camera</th><th>Open Camera</th><th>OnePlus ≈</th><th>Fossify ≈</th><th>Libre ≈</th></tr></thead><tbody>')
html.append(rows(TAPS))
html.append('</tbody></table></div><div class="note"><b>The leaner flow.</b> Three changes remove most of the gap and none needs a new screen: a gallery thumbnail in the shutter column (launcher → 1 tap); a format chip in the info overlay that cycles Jpeg / DNG / Jpg+DNG on tap (4 → 1); a long-press on the shutter to record, or a second red shutter as ProShot does (3 → 1). The level toggle can move into the grid popup (4 → 2).</div>')

html.append('<h2 id="optimizer">Optimizer result</h2><p><span class="voice voice-measured">measured</span> Zero-turn search over the shutter column: permutation of 8 items (7 existing + gallery) on a 2-column grid, rows above the shutter 1–3, shutter 72–96 dp, cell 40–56 dp, strip bottom / left / top, seekbar right / left. Components, all computable: thumb reach 0.30, grouping 0.15, preview clear 0.25, target size 0.15, strip reach 0.15. 60 generations × 24, HS+DE.</p>')
html.append('<div class="opt"><div class="h">component</div><div class="h">current</div><div class="h">winner</div>' + "".join(f"<div>{a}</div><div>{b}</div><div class=\"win\">{c}</div>" for a, b, c in [("fitness", "0.772", "0.884"), ("reach", "0.675", "0.728"), ("grouping", "0.667", "1.000"), ("preview clear", "0.861", "0.861"), ("targets", "0.694", "1.000"), ("strip reach", "1.000", "1.000")]) + "</div>")
html.append('<p style="margin-top:16px">Winner: one row above the shutter (camera switch, histogram), then directly below it <b>gallery + module</b>, <b>AE-lock + flash</b>, <b>timer + focus</b>; 56 dp cells; 72 dp shutter; strip at the bottom; <b>seekbar in the left band</b>. The two runners-up (0.883, 0.882) keep two rows above with the same 56 dp cells — the cell size and gallery-under-shutter are the stable part of the result, the exact top-row order is not.</p><p><span class="voice">judgement</span> The win is real but modest and comes from three things worth doing regardless: 56 dp cells, the two most-used items under the thumb, and the seekbar out of the crowded right band. The bottom strip is the fixed cost in preview clearance; moving it left scores the preview at 1.0 but drops strip reach to 0.55 — I would not move it. The baseline is an approximation because the current column is ragged; treat the delta as directional.</p>')

html.append('<h2 id="features">Additional features, against the references</h2><div class="tablewrap"><table><thead><tr><th>Candidate</th><th>Seen in</th><th>Fit (judgement)</th><th>Effort</th></tr></thead><tbody>' + rows(FEATURES) + "</tbody></table></div>")

html.append("""<h2 id="working">What’s working</h2><ul class="plain">
<li>The capture core: JPEG, 16-bit DNG, JPEG+DNG, video, interval and AE bracket all produced correct files on the first try, into MediaStore and into a SAF folder.</li>
<li>The NextGen settings drawer: translucent scrim over the live preview, lime value column, ripple feedback, 48 dp rows, remembered tab — it reads as one system with the manual strip.</li>
<li>Icon font as signature: 22 sp glyphs at 30 dp are crisp at 480 dpi and the lime “active” swap is unambiguous.</li>
<li>Chrome restraint: 5 % ink coverage on the main screen; the histogram sits in the letterbox band, not over the picture.</li>
<li>The measured focal point is the shutter.</li>
<li>Nine composition grids including golden-ratio variants, in a clean two-column picker.</li>
<li>Build hygiene: AGP 8.13 / Gradle 8.14 / NDK 27 built clean in five minutes.</li></ul>
<h2 id="cannot">Cannot verify</h2><ul class="plain">
<li><code>values-sw600dp</code> scaling — no tablet or foldable.</li>
<li>Camera1 on other SoCs — the LE2123 path NPEs before parameters load.</li>
<li>Focus-stack merge output — the AF range is unsettable, so the merge ran only with defaults.</li>
<li>Tone-curve editor — the button does not appear on this device.</li>
<li>Physical-camera pinning — no “Physical Camera” row appears here.</li>
<li>Level under real tilt — the phone lay flat; the ambiguity finding is from the rendering.</li>
<li>Stock OnePlus, Fossify and Libre tap counts — read from their main screens, not walked.</li>
<li>Italian rendering — the walk was in English by decision; the coverage number is from the resources.</li></ul>
<h2 id="plan">Fix plan</h2><p>Prioritised, nothing applied. After each batch: <code>python tools/critique/walk.py build</code>, <code>install</code>, then the walk steps named in each finding, and <code>python tools/critique/measure.py</code> for the numbers.</p>""")
for title, sub, items in BATCHES:
    html.append(f'<div class="batch"><h3>{title}<span>{sub}</span></h3><ol>' + "".join(f"<li>{i}</li>" for i in items) + "</ol></div>")
html.append("""<h2 id="status">Status — Batch 1 applied</h2><p>Build 4.3.83 (versionCode 283), rebuilt and re-walked on the phone (screenshots 106–116).</p><div class="tablewrap"><table><thead><tr><th>Item</th><th>Change</th><th>Verified</th></tr></thead><tbody>
<tr><td>1</td><td><code>NextGenManualsFragment.java:240</code> ManualButtonMF → NextGenMfItem</td><td>Af-Bracket + focus tap now shows NEAR / FAR — priority 2 closed</td></tr>
<tr><td>2</td><td><code>ButtonOptionsModel.java</code> labels and listeners aligned, stack entry removed, labels in resources</td><td>Menu is “Delete file / Raw to DNG”; RAW TO DNG enters RAWTODNG mode — priority 3 closed</td></tr>
<tr><td>3</td><td>Copy pass: typos, colon policy from the row type, trailing spaces, KitKat note replaced, Force Wide Preview description key fixed, help page 4</td><td>“Continuous Picture”, “Face Detection”, correct Force Wide Preview text, new storage description</td></tr>
<tr><td>4</td><td>Timelapse row rebuilt on the row tokens: header, description, lime value, 48 dp −/+</td><td>Video tab shows “Timelapse frame rate”</td></tr>
<tr><td>5</td><td>74 hard-coded layout strings → resources (+ 42 Italian); runtime placeholders removed</td><td>2 hard-coded strings left, both in the disposable harness; Italian coverage 45 % → 52 %</td></tr>
<tr><td>6</td><td><code>cameraui_infooverlay_textsize</code> 10dip → 10sp</td><td>Overlay text follows the system font scale</td></tr>
<tr><td>—</td><td>versionCode 282 → 283 so the feature detector re-runs and stale cached values are replaced</td><td>Popup values read “Continuous …” after install</td></tr>
</tbody></table></div><p>Not touched (Batch 2/3): ellipsized headers, the 7-tab strip, marquee captions, level, gallery entry, Camera1, session state.</p>""")
html.append("""<h2 id="status2">Status — Batch 2 applied</h2><p>Build 4.3.84 (versionCode 284), rebuilt and re-walked on the phone (screenshots 116–141). Decisions taken with the owner first: five tabs with Raw and Interval as sections; the optimizer's column; a last-capture thumbnail as the gallery cell; a single-bar level with auto-hide. During the walk the owner asked for the shutter back at the centre, so the column uses the search's runner-up (two rows above, two below, 0.883 vs 0.884).</p><div class="tablewrap"><table><thead><tr><th>Item</th><th>Change</th><th>Verified</th></tr></thead><tbody>
<tr><td>7</td><td>Settings list one column below a 600 dp panel</td><td>No ellipsized header on any tab — priority 5 closed</td></tr>
<tr><td>8</td><td>Five tabs; Raw and Interval are sections of Picture (new Section row); tab strip on a scrim</td><td>All five tabs visible, no strip scroll — priority 4 closed</td></tr>
<tr><td>9</td><td>Column: 56 dp cells, 72 dp centred shutter, whole-cell targets, static short captions, gallery thumbnail cell, seekbar in the left band</td><td>7 cells at 56×48 dp; thumbnail after the first capture opens the gallery — priorities 6 and 9 closed</td></tr>
<tr><td>10</td><td>Info overlay: REC pill, “Video” label in video mode, format chip tap-cycles the picture format, timer 12 sp</td><td>REC pill and timer while recording — priority 11 closed</td></tr>
<tr><td>11</td><td>Value picker: title row, outlined Back chip, two columns, list scroll preserved</td><td>“Picture Format / ‹ Back / values”</td></tr>
<tr><td>12</td><td>Level: single bar + tick + angle, dark outline, lime when level, change-gated redraw, 2 s auto-hide; two pre-existing bugs fixed (saved value never loaded; visibility set off the UI thread)</td><td>Bar reads “+55.0°” on a bright scene and persists across relaunch — priority 8 closed</td></tr>
<tr><td>13</td><td>Gallery: Back returns to the grid, no Play on stills, 48 dp buttons, thumbnails fill the width</td><td>Back → grid</td></tr>
<tr><td>14</td><td>Form screens on the app tokens (FormTheme); stock indigo/pink replaced app-wide</td><td>Profile editor dark with a title row — priority 12 closed</td></tr>
</tbody></table></div><p>Left over: the form activities still show the navigation bar; the gallery opens at its folder root; the module popup lists modules in reverse order. Contrast rows below 4.5:1 on a grey scene: 64 of 153 (was 92 of 145; the overlay rows in the first table were a measurement artefact).</p>""")
html.append(fig("139_b2_main_centered2.png", "After Batch 2: 56 dp cells around a centred 72 dp shutter — camera switch | histogram, timer | C-AF above; thumbnail | Pic, AE-lock | flash below."))
html.append(fig("128_b2_picker_top.png", "Value picker with its title and Back chip.", crop=(96, 0, 1500, 1080)))
html.append(fig("134_b2_level5.png", "The single-bar level with its angle, over a bright scene.", crop=(700, 100, 1700, 1000)))

html.append("""<h2 id="settings">Settings page vs the references</h2><p>Measured with <code>settings_compare.py</code>: each app’s settings opened from its viewfinder, the accessibility tree dumped, rows counted and sized. Scores 1–5; <em>entry</em>, <em>room</em> and <em>context</em> rest on the numbers, <em>ease</em> and <em>legibility</em> are judgement.</p><div class="tablewrap"><table class="gc"><thead><tr><th>App</th><th>Entry</th><th>Room to click</th><th>Ease of use</th><th>Context kept</th><th>Legibility</th><th>Total / 25</th></tr></thead><tbody>
<tr><td>GlassCam 4.3.84</td><td>swipe anywhere, no visible control (3)</td><td>median 48 dp, 0 under 48 (4)</td><td>tab → row → titled picker; no search (3)</td><td>translucent drawer over the live preview (5)</td><td>one column, one type ladder, sections (4)</td><td><b>19</b></td></tr>
<tr><td>FreeDcam</td><td>same swipe (3)</td><td>median 41 dp, 16 of 18 under 48 (2)</td><td>all groups on one long two-column page (3)</td><td>opaque (1)</td><td>green/orange monospace, cramped (2)</td><td>11</td></tr>
<tr><td>Open Camera</td><td>gear top-right, 731 dp from thumb (2)</td><td>median 60 dp, 2 under 48 (4)</td><td>deep preference list, no search (3)</td><td>opaque (1)</td><td>plain prefs (4)</td><td>14</td></tr>
<tr><td>ProShot</td><td>gear top-right, 748 dp (2)</td><td>median 60 dp, 2 under 48 (4)</td><td>customisation panel; shooting settings are on-screen readouts (3)</td><td>panel beside the preview (3)</td><td>icon grid + swatches (3)</td><td>15</td></tr>
<tr><td>Manual Camera Pro</td><td>gear bottom-right, 110 dp (5)</td><td>median 45 dp, 13 of 23 under 48 (2)</td><td>dense icon grid (2)</td><td>opaque (1)</td><td>small icons (2)</td><td>12</td></tr>
<tr><td>MA Camera</td><td>gear bottom-left, 330 dp (4)</td><td>median 48 dp, 0 under 48 (5)</td><td>three rows + search, little to set (4)</td><td>opaque (1)</td><td>Material list (4)</td><td>18</td></tr>
<tr><td>Fossify Camera</td><td>gear top-right, 749 dp (2)</td><td>median 56 dp, 1 under 48 (5)</td><td>categorised prefs (4)</td><td>opaque (1)</td><td>Material list (4)</td><td>16</td></tr>
<tr><td>Libre Camera</td><td>gear top-right, 741 dp (2)</td><td>median 80 dp, 1 under 48 (5)</td><td>categorised prefs (4)</td><td>opaque (1)</td><td>Material list (4)</td><td>16</td></tr>
<tr><td>OnePlus stock</td><td>quick pill, then Impostazioni: 2 taps (2)</td><td>median 40 dp, 19 of 27 under 48 (2)</td><td>two levels (3)</td><td>quick sheet over the preview (4)</td><td>brand-styled, mixed sizes (3)</td><td>14</td></tr>
</tbody></table></div><div class="note"><b>Judgement.</b> GlassCam’s drawer is the only one that keeps the live preview behind the settings while meeting the 48 dp floor on every row. Its two remaining weaknesses are where the references beat it: a <b>discoverable entry</b> (Manual Camera Pro’s gear sits 110 dp from the thumb; GlassCam has no visible control) and <b>search</b> (only MA Camera has it). Next step: a small gear at the bottom of the empty left band that opens the drawer, and a filter field in the drawer header.</div>""")
html.append(fig("set_glasscam.png", "GlassCam 4.3.84: five tabs, single column, over the live preview."))
html.append(fig("set_freedcam.png", "Upstream FreeDcam: every group on one opaque two-column page, 41 dp rows."))

html.append("""<h2 id="status3">Status — Batch 3 applied</h2><p>Build 4.3.85 (versionCode 285). Phone testing was paused by the owner part-way through verification (screenshots 142–158).</p><div class="tablewrap"><table><thead><tr><th>Item</th><th>Change</th><th>State</th></tr></thead><tbody>
<tr><td>15</td><td>Root cause of the Camera1 NPE: <code>ActivityAbstract.context</code> is a static every activity overwrote, so after a gallery visit a new Camera1 got the viewer's empty PreviewController. Foreground activity re-claims it on resume; preview access null-safe</td><td><b>verified</b> — Camera1 builds its UI, switching back rebuilds Camera2 — priority 1 closed</td></tr>
<tr><td>16</td><td>Cold start opens in Picture with burst 1; help overlay first-run only (no longer re-enabled on version bumps)</td><td><b>verified</b> — relaunch after Af-Bracket shows Pic / burst 1</td></tr>
<tr><td>17</td><td>Non-numeric stored manual values are re-seeded, not logged as exceptions</td><td>built</td></tr>
<tr><td>18</td><td>External storage writes into a GlassCam/ child of the picked tree</td><td>built, not verified (paused)</td></tr>
<tr><td>19</td><td>IMAGE_CAPTURE share activity re-enabled on the SAF path; KillerInk URL and the dead update row removed</td><td>built, not verified (paused)</td></tr>
<tr><td>20</td><td>Harness activity and five orphan layouts deleted (38 layouts, was 44)</td><td>built, clean</td></tr>
<tr><td>B2 leftovers</td><td>Profile editor hides the nav bar; module popup in registration order; gear cell opens the drawer; gallery cell opens the newest folder</td><td><b>verified</b>: popup order, gear; newest-folder landing built only</td></tr>
</tbody></table></div><p>Still open: search in the drawer; device verification of 18, 19 and the newest-folder landing; Camera1 flash cell shows “off” as text. Repo numbers: 52 of 139 text elements below 4.5:1 on a grey scene (was 92 of 145), 203 dp/sp literals across 38 layouts (was 244 across 40).</p>""")
html.append(fig("149_b3_camera1.png", "Camera1 after the container fix: full column, strip and gear — it had no controls at all before."))

html.append("""<h2 id="status3v">Status — Batch 3 verified on the phone</h2><p>Build 4.3.87 (versionCode 287). The three items left unverified at the pause, driven on the OnePlus 9 Pro (screenshots 159–183); two needed a fix first.</p><div class="tablewrap"><table><thead><tr><th>Item</th><th>What the phone showed</th><th>Fix</th><th>State</th></tr></thead><tbody>
<tr><td>16 · help after a version bump</td><td>285 → 286 re-ran the feature detector, no overlay; CLOSE persists through onPause</td><td>—</td><td><b>verified</b></td></tr>
<tr><td>gallery cell → newest folder</td><td>lands in GlassCam/ (root → folder in 360 ms), Back chevron; lists the SAF folder under External</td><td>—</td><td><b>verified</b></td></tr>
<tr><td>18 · SAF GlassCam/ child</td><td>read first: stills still went to the tree's <i>root</i> (only movies/DNG used the child). After the fix: External → picker → GlassCamTest → capture → <code>GlassCamTest/GlassCam/…jpg</code>, root empty</td><td><code>getNewImgFileHolder</code> → <code>getGlassCamDocumentFolder()</code></td><td><b>verified after fix</b></td></tr>
<tr><td>19 · IMAGE_CAPTURE share</td><td>(a) crashed 300 ms in — the only camera activity without <code>configChanges</code>, recreated by the forced landscape, first instance's camera callback on a detached fragment; (b) returned the <i>previous</i> photo with no capture — "newest at launch" read before the async folder load. After both: waits with full UI, shutter → capture → byte-identical copy in the caller's EXTRA_OUTPUT (md5 match) → finish</td><td>manifest configChanges; isAdded() guard on the posted callback (both fragments); new <code>onCaptureSaved</code> event raised only by the capture path</td><td><b>verified after fixes</b></td></tr>
</tbody></table></div><p><b>Reach of 19 is a platform limit:</b> since Android 11 an implicit IMAGE_CAPTURE resolves only to the pre-installed camera — <code>query-activities</code> on this phone lists <code>com.oplus.camera</code> alone though GlassCam's filter is registered, and DuckDuckGo's file input went to the stock camera by explicit component. The activity serves callers that name the package, nothing more.</p><p><b>Found on the way, not fixed (owner's call):</b> BACK on the settings page exits the app (nothing intercepts it; one OnBackPressedCallback → <code>setCurrentItem(1)</code>); the picker title row is 38 dp, the drawer's only sub-48 clickable; a stale tree URI stays stored after Internal is re-chosen; a mid-layout uiautomator dump made "External" look dead (the tap hit Back) — <code>walk.py shot</code> should re-dump when the tree moved. Camera1 flash cell text and drawer search remain open.</p><p class="note">Phone left as found: locale/timeout restored, test tree and captures deleted (including the 12:42 leftover), Storage back to Internal, 4.3.87 installed.</p>""")
html.append(fig("183_share_activity_waiting.png", "The share activity after the fixes: launched by IMAGE_CAPTURE with an EXTRA_OUTPUT, waiting for the shutter instead of crashing or returning early."))

html.append("""<h2 id="status4">Status — Batch 4: the drawer rebuilt</h2><p>Build 4.3.88 (versionCode 288). The owner rejected the drawer the comparison had scored top: 60 % × 85 % on the far side from the shutter thumb, tabs holding ~3 · 9 · 20 · 10 · 4 rows. Decided together: full-width, full-height panel over the scrimmed preview, one two-column list with sticky section headers, a jump-chip row, search on top, no tabs.</p><div class="tablewrap"><table><thead><tr><th>Change</th><th>What it is</th><th>State</th></tr></thead><tbody>
<tr><td>Panel</td><td>full width and height, scrim fill, insets on all edges; header row = search (left) + jump chips (right)</td><td><b>verified</b></td></tr>
<tr><td>One list</td><td>every group merged under Section rows; two columns; sections span full width; the tab pager adapter deleted</td><td><b>verified</b></td></tr>
<tr><td>Jump chips</td><td>one pill per section, tap scrolls it to the top; the section in view is lime; a section the search emptied is dimmed</td><td><b>verified</b></td></tr>
<tr><td>Sticky section</td><td>pinned label over the list while the section row is scrolled off</td><td><b>verified</b></td></tr>
<tr><td>Search</td><td>header + description, case-insensitive; a matching section label keeps its rows; empty state; keyboard dropped on row/chip tap and swipe</td><td><b>verified</b></td></tr>
<tr><td>BACK closes the drawer</td><td>gesture <i>and</i> key path — <code>onKeyUp</code> used to send every BACK straight to <code>moveTaskToBack</code></td><td><b>verified</b></td></tr>
<tr><td>Picker title row</td><td>48 dp, not clickable, a11y heading</td><td>built</td></tr>
</tbody></table></div><p class="note">Watch: the grid aligns row tops, so a tall cell (timelapse stepper) leaves a gap beside it; the comparison table scored the 4.3.84 drawer and has not been re-scored.</p>""")
html.append(fig("184_b4_drawer_all.png", "The rebuilt drawer: full width over the scrimmed preview, search and jump chips in one 48 dp row, two columns of rows under section labels."))
html.append(fig("187_b4_sticky_scrolled.png", "Scrolled inside Picture: the chip is lime, the section label is pinned, Raw and Interval follow as sub-sections."))

html.append("""<h2 id="status5">Status — Batch 5: gaps, optimizer, re-score</h2><p>Build 4.3.89 (versionCode 289). A settings-panel optimizer run (section order, columns, packing, stepper, row order, chip position; computable scorers; converged in 20 generations) confirmed two columns and chose a grid with a compact stepper and chips at the bottom.</p><div class="tablewrap"><table><thead><tr><th>Change</th><th>What it is</th><th>State</th></tr></thead><tbody>
<tr><td>Uniform rows</td><td>every row ≥ 76 dp; the timelapse stepper compacted to one line so it fits; GridLayoutManager equalises a grid row, so equal rows = no gaps</td><td><b>verified</b></td></tr>
<tr><td>Chips at the bottom</td><td>own 48 dp row under the list, right-aligned; 36 dp pills in 48 dp targets</td><td><b>verified</b></td></tr>
<tr><td>Section rows</td><td>not clickable</td><td><b>verified</b></td></tr>
<tr><td>Camera1 flash cell</td><td>driver words mapped to the flash glyphs</td><td>built</td></tr>
<tr><td>Stale SAF tree</td><td>a vanished or revoked tree reads as none</td><td>built</td></tr>
</tbody></table></div>
<h3>Settings comparison, re-scored</h3><p>The script now measures fill (screen share covered by tappable rows), reach (mean dp from the right thumb) and rows per screen, and waits for the app to be in front before measuring.</p><div class="tablewrap"><table><thead><tr><th>App</th><th>Entry from thumb (dp)</th><th>Rows / screen</th><th>Row median / under 48</th><th>Search</th><th>Fill</th><th>Reach dp</th><th></th></tr></thead><tbody>
<tr><td>**GlassCam 4.3.89**</td><td>734</td><td>11</td><td>48 / 0</td><td>yes</td><td>0.68</td><td>321</td><td></td></tr>
<tr><td>FreeDcam</td><td>swipe L->R</td><td>16</td><td>41 / 16</td><td>yes</td><td>0.78</td><td>378</td><td></td></tr>
<tr><td>Open Camera</td><td>731</td><td>—</td><td>60 / 2</td><td>no</td><td>—</td><td>—</td><td><i>rows not exposed as tappable to accessibility; fill/reach not measurable</i></td></tr>
<tr><td>ProShot</td><td colspan=6>—</td><td><i>tree never idled</i></td></tr>
<tr><td>Manual Camera Pro</td><td>110</td><td>—</td><td>45 / 13</td><td>no</td><td>—</td><td>—</td><td><i>rows not exposed as tappable to accessibility; fill/reach not measurable</i></td></tr>
<tr><td>MA Camera</td><td>330</td><td>—</td><td>48 / 0</td><td>yes</td><td>—</td><td>—</td><td><i>rows not exposed as tappable to accessibility; fill/reach not measurable</i></td></tr>
<tr><td>Fossify Camera</td><td>749</td><td>10</td><td>56 / 1</td><td>no</td><td>0.67</td><td>284</td><td></td></tr>
<tr><td>Libre Camera</td><td>741</td><td>—</td><td>80 / 1</td><td>no</td><td>—</td><td>—</td><td><i>rows not exposed as tappable to accessibility; fill/reach not measurable</i></td></tr>
<tr><td>OnePlus stock</td><td colspan=6>—</td><td><i>entry control not on screen; nothing measured</i></td></tr>
</tbody></table></div><p>GlassCam: fill 0.42 → 0.68, reach 535 → 321 dp, rows per screen 8 → 11 against its own 4.3.84 drawer. Fill and reach exist only for pages that expose rows as tappable (GlassCam, FreeDcam, Fossify); the script now says so for the other four instead of scoring the window behind. Its weak column is now the entry: the gear is 734 dp from the thumb.</p>""")
html.append(fig("190_b5_drawer_final2.png", "4.3.89: uniform 76 dp rows, the stepper on one line, chips in their own row at the bottom."))

ERRATA = [
    ("Settings judgement", "search: “MA Camera has it, nobody else does”", "FreeDcam has search too (<code>settings_compare_4384.json</code>, and the Batch 5 table says yes)."),
    ("Settings table, room to click", "GlassCam 48 dp / 0 under 48 scored 4, MA Camera the same numbers scored 5", "Same numbers, different scores in a column said to rest on the numbers; on the numbers alone GlassCam totals 20, not 19. “Context kept” is judgement, not measured."),
    ("Batch 5 comparison", "GlassCam “11 rows”, “median row 48 dp”, rows 8 → 11", "The script counts top-level tappables: 11 = search + 5 setting rows + 5 chips; 8 = 4 tabs + 3 rows + an 8 dp sliver. The 48 dp median is chips, search and −/+; setting rows are 76 dp. Settings fully on screen: 3 → 5."),
    ("Batch 5 optimizer", "two columns; chips at the bottom (+0.055); section order noise (±0.001)", "The scorer modelled 4.3.88 row heights and one 48 dp header. Re-modelled on the shipped panel and enumerated (5,760 layouts): two columns hold; chips top vs bottom tie within 0.003 and flip with the unmeasured thumb rest point (0.57 × height); section order spans 0.811–0.818."),
    ("Batch 2 column", "“7 cells at 56 × 48 dp” beside an 8-item caption", "7 is the glyph cells; with the gallery thumbnail the column holds 8 on this phone. With the OpenGL preview mode more toggles join the upper right and the shutter leaves the centre (SPEC §11)."),
    ("Batch 2 viewer", "“48 dp bar and viewer buttons”", "The two viewer buttons were 48 dp tall but 40 dp wide; 48 × 48 since 4.3.90."),
    ("Verified cells", "rows marked verified without a screenshot number; picker title row “built”", "SPEC counts a row verified only with a screenshot number; those rows and the picker title row are owed on the phone."),
]
html.append('<h2 id="errata">Errata — fresh-eyes audit, 2026-09-28 (build 4.3.90)</h2><p>The critique above is left as written; these corrections were each checked by hand against the named file. Full register: <code>docs/fresh-eyes-2026-09-28.md</code>.</p><div class="tablewrap"><table><thead><tr><th>Where</th><th>Said</th><th>Correction</th></tr></thead><tbody>' + "".join(f"<tr><td>{a}</td><td>{b}</td><td>{c}</td></tr>" for a, b, c in ERRATA) + "</tbody></table></div>")
html.append('<p class="note" style="margin-top:40px">Tooling that produced this page lives in <code>tools/critique/</code>: <code>walk.py</code> (build · install · drive · screenshot · restore), <code>measure.py</code> (contrast, targets, spacing, copy from the resources), <code>analyse.sh</code> (rendered value/focal/palette), <code>layout_spec.json</code> + <code>scorers.py</code> (optimizer), <code>checklist.md</code>. The full text is in <code>CRITIQUE_2026-09-16.md</code> beside <code>MIGRATION_NOTES.md</code>.</p>')
html.append("</main></div>")

with open(OUT, "w", encoding="utf-8", newline="\n") as f:
    f.write("\n".join(html))
print(OUT, round(os.path.getsize(OUT) / 1e6, 2), "MB")
