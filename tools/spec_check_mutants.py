"""spec_check_mutants.py -- prove each spec_check check can fail.

    python tools/spec_check_mutants.py        -> one line per mutant, exit 1 if any survives

Copies the tree spec_check reads into a scratch folder, plants one defect per mutant, runs the
copy's spec_check.py and requires a FAIL line for the named tag (not just a non-zero exit: a crash
or an unrelated failure would pass as a kill otherwise). An unplanted control copy must pass.
Several mutants reproduce holes the checks had before the fresh-eyes audit (2026-09-28): S-11
read the root view, S-14 compared find() = -1, S-19 matched "orientation" in screenOrientation,
S-21 tested one layout, removal checks read a duplicated file as absent.
Stdlib only.
"""
import os
import re
import shutil
import subprocess
import sys
import tempfile

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
PARTS = ["SPEC.md", "build.gradle", "settings.gradle", "gradle/libs.versions.toml", "app/build.gradle",
         "tools/spec_check.py", "app/src/main/AndroidManifest.xml", "app/src/main/java", "app/src/main/res"]
J = "app/src/main/java/glasscam"
L = "app/src/main/res/layout"

# (tag that must FAIL, description, [(path, old, new) | ("copy", src, dst)])
MUTANTS = [
    ("S-01", "allowBackup turned on, with the old string left in a comment",
     [("app/src/main/AndroidManifest.xml", 'android:allowBackup="false"', 'android:allowBackup="true"'),
      ("app/src/main/AndroidManifest.xml", "    <application", '    <!-- android:allowBackup="false" -->\n    <application')]),
    ("S-05", "a revoked grant rethrown instead of reading as no folder",
     [(f"{J}/file/FileListController.java",
       "        } catch (SecurityException ex) {\n            Log.WriteEx(ex);\n            return null;\n        }",
       "        } catch (SecurityException ex) {\n            Log.WriteEx(ex);\n            throw ex;\n        }")]),
    ("S-06", "a second raiser of onCaptureSaved",
     [(f"{J}/file/FileListController.java", "            files.add(file);\n            SortFileHolder(files);",
       "            files.add(file);\n            fireCaptureSaved(file);\n            SortFileHolder(files);")]),
    ("S-08", "the cell no longer asks for the newest folder",
     [(f"{J}/cam/ui/themenextgen/view/NextGenGalleryButton.java",
       "            intent.putExtra(ActivityGlassCamViewer.EXTRA_OPEN_NEWEST_FOLDER, true);\n", "")]),
    ("S-08", "a duplicate NextGenGalleryButton.java (jfile used to read it as absent)",
     [("copy", f"{J}/cam/ui/themenextgen/view/NextGenGalleryButton.java", f"{J}/viewer/NextGenGalleryButton.java")]),
    ("S-11", "setting_holder shrinks to wrap_content while the root stays match_parent",
     [(f"{L}/nextgen_setting_fragment.xml", None, None)]),   # handled in plant_s11
    ("S-14", "the chips row loses its id (find() = -1 used to pass)",
     [(f"{L}/nextgen_setting_fragment.xml", '@+id/settings_chips_scroll"', '@+id/settings_chips_scrollx"')]),
    ("S-17", "a GitHub URL in a string resource",
     [("app/src/main/res/values/strings.xml", "</resources>",
       '    <string name="x_update">https://github.com/KillerInk/FreeDcam/releases</string>\n</resources>')]),
    ("S-19", "the share activity drops orientation from configChanges (screenOrientation still matches)",
     [("app/src/main/AndroidManifest.xml", None, None)]),   # handled in plant_s19
    ("S-21", "an orphan layout",
     [("copy", f"{L}/nextgen_setting_section.xml", f"{L}/zz_orphan_layout.xml")]),
    ("S-22", "a viewer button back to 40 dp wide",
     [(f"{L}/glasscamviewer_screenslide_fragment.xml",
       '                android:layout_width="@dimen/glasscamviewer_topbar_height"\n                android:layout_height="@dimen/glasscamviewer_topbar_height"\n                android:id="@+id/button_info"',
       '                android:layout_width="40dp"\n                android:layout_height="@dimen/glasscamviewer_topbar_height"\n                android:id="@+id/button_info"')]),
    ("S-23", "a text size in dp",
     [(f"{L}/glasscamviewer_screenslide_fragment.xml",
       'setVisibility="@{screenslideFragmentModel.infoButtonModel.visibility}"\n                android:textSize="10sp"',
       'setVisibility="@{screenslideFragmentModel.infoButtonModel.visibility}"\n                android:textSize="10dp"')]),
    ("S-24", "GlassGallery loses its landscape lock",
     [("app/src/main/AndroidManifest.xml",
       '            android:label="GlassGallery"\n            android:screenOrientation="landscape"',
       '            android:label="GlassGallery"')]),
    ("S-25", "the value scale goes back to the left band",
     [(f"{L}/nextgen_cameraui_manual_fragment_rotatingseekbar.xml",
       '        android:layout_alignParentEnd="true"\n        android:layout_alignParentRight="true"',
       '        android:layout_alignParentStart="true"\n        android:layout_alignParentLeft="true"')]),
    ("S-26", "Play on a DNG again",
     [(f"{J}/viewer/screenslide/modelview/ScreenSlideFragmentModelView.java",
       "playButton.setVisibility(false);   // a DNG is a still too", "playButton.setVisibility(true);   // a DNG is a still too")]),
    ("S-26", "the Play button unbound again (the Batch 2 bug)",
     [(f"{L}/glasscamviewer_screenslide_fragment.xml",
       '                setVisibility="@{screenslideFragmentModel.playButton.visibility}"\n', "")]),
    ("S-27", "a detector passes the translated array again (the 6T's noise reduction Off)",
     [(f"{J}/cam/apis/featuredetector/camera2/DenoisParameterDetector.java",
       "settingsManager.get(SettingKeys.DENOISE), R.array.denoiseModes,settingsManager);",
       "settingsManager.get(SettingKeys.DENOISE), GlassCamApplication.getStringArrayFromRessource(R.array.denoiseModes),settingsManager);")]),
    ("S-28", "a preview write bypasses previewValue",
     [(f"{J}/cam/apis/camera2/CaptureSessionHandler.java",
       "        Log.d(TAG, \"SetParameter :\" + key.getName() + \" to \" + value);\n        mPreviewRequestBuilder.set(key,previewValue(key,value));",
       "        Log.d(TAG, \"SetParameter :\" + key.getName() + \" to \" + value);\n        mPreviewRequestBuilder.set(key,value);")]),
    ("S-29", "two histogram buttons again",
     [(f"{J}/cam/ui/themenextgen/fragment/NextGenCameraUiFragment.java",
       "if (parameterHandler.get(SettingKeys.HISTOGRAM) != null && parameterHandler.get(SettingKeys.HISTOGRAM_STATS_QCOM) == null)",
       "if (parameterHandler.get(SettingKeys.HISTOGRAM) != null)")]),
    ("S-30", "the AE mode row offers the flash variants again",
     [(f"{J}/cam/apis/featuredetector/camera2/AutoExposureModeDetector.java",
       "        settingsManager.get(SettingKeys.EXPOSURE_MODE).setValues(keep.toArray(new String[0]));\n", "")]),
    ("S-30", "the on/off filter compares whole 'label,mode' entries again (hid the row on the 6T)",
     [(f"{J}/cam/apis/featuredetector/camera2/AutoExposureModeDetector.java",
       "                if (label.equals(on) || label.equals(off))", "                if (v.equals(on) || v.equals(off))")]),
    ("S-30", "red-eye dropped from the Flash toggle",
     [(f"{J}/cam/apis/camera2/parameters/modes/FlashMode.java",
       "CaptureRequest.CONTROL_AE_MODE,CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH_REDEYE,false",
       "CaptureRequest.CONTROL_AE_MODE,CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH,false")]),
    ("S-31", "the camera key does nothing again",
     [(f"{J}/cam/ui/KeyPressedController.java",
       "            if (keyCode == KeyEvent.KEYCODE_CAMERA || keyCode == KeyEvent.KEYCODE_3D_MODE)\n                startWork(keyCode);\n", "")]),
    ("S-32", "a second MFNR row",
     [(f"{J}/cam/ui/themenextgen/layoutconfig/SettingGroupConfig.java",
       "        group.add(new SettingItemConfig(SettingKeys.MTK_AOV_MODE,",
       "        group.add(new SettingItemConfig(SettingKeys.XIAOMI_MFNR, R.string.setting_mfnr_header,R.string.setting_mfnr_description,false, SettingItemConfig.ViewType.Boolean));\n        group.add(new SettingItemConfig(SettingKeys.MTK_AOV_MODE,")]),
    ("S-33", "the wildcard MIME type that crashed every capture on Android 14",
     [(f"{J}/file/MediaStoreController.java",
       "newImg.put(MediaStore.Images.Media.MIME_TYPE, mime);", 'newImg.put(MediaStore.Images.Media.MIME_TYPE, "image/*");')]),
    ("S-33", "raw dumps back into DCIM",
     [(f"{J}/file/MediaStoreController.java",
       'public static final String DUMPS_PATH = "Documents/GlassCam/";', 'public static final String DUMPS_PATH = "DCIM/GlassCam/";')]),
    ("S-30", "the auto and always flash icons swapped back",
     [("app/src/main/res/values/fontglyphs.xml",
       'name="font_flash_auto" translatable="false">\\uE021<', 'name="font_flash_auto" translatable="false">\\uE018<')]),
    ("S-13", "the same tag on two clauses",
     [("SPEC.md", "| T6 |", "| T6 | `[S-13]` |")]),
]


def plant_s11(path):
    x = open(path, encoding="utf-8").read()
    i = x.index('android:id="@+id/setting_holder"')
    start = x.rindex("<", 0, i)
    end = x.index(">", i)
    tag = x[start:end]
    new = tag.replace('android:layout_width="match_parent"', 'android:layout_width="wrap_content"', 1)
    assert new != tag, "setting_holder has no match_parent width to plant on"
    open(path, "w", encoding="utf-8", newline="").write(x[:start] + new + x[end:])


def plant_s19(path):
    x = open(path, encoding="utf-8").read()
    i = x.index('android:name="glasscam.cam.ActivityGlassCamShare"')
    j = x.index("</activity>", i)
    block = x[i:j]
    new = re.sub(r'(android:configChanges="[^"]*?)\borientation\|', r"\1", block, count=1)
    assert new != block, "share activity has no orientation| to remove"
    open(path, "w", encoding="utf-8", newline="").write(x[:i] + new + x[j:])


def copy_tree(dst):
    for p in PARTS:
        s, d = os.path.join(ROOT, p), os.path.join(dst, p)
        if os.path.isdir(s):
            shutil.copytree(s, d)
        elif os.path.exists(s):
            os.makedirs(os.path.dirname(d), exist_ok=True)
            shutil.copy2(s, d)


def run(dst):
    out = subprocess.run([sys.executable, os.path.join(dst, "tools", "spec_check.py")], capture_output=True,
                         text=True, encoding="utf-8", env=dict(os.environ, PYTHONUTF8="1"))
    return out.returncode, out.stdout + out.stderr


def main():
    base = tempfile.mkdtemp(prefix="spec_mutants_")
    survivors = 0
    try:
        clean = os.path.join(base, "clean")
        copy_tree(clean)
        code, out = run(clean)
        ok = code == 0 and re.search(r"\b0 failing", out)
        print(("ok  " if ok else "BAD ") + "control: unplanted copy passes")
        if not ok:
            print(out)
            return 1
        for n, (tag, what, plants) in enumerate(MUTANTS):
            d = os.path.join(base, f"m{n}")
            shutil.copytree(clean, d)
            for path, old, new in plants:
                if path == "copy":
                    shutil.copy2(os.path.join(d, old), os.path.join(d, new))
                elif tag == "S-11" and old is None:
                    plant_s11(os.path.join(d, path))
                elif tag == "S-19" and old is None:
                    plant_s19(os.path.join(d, path))
                else:
                    p = os.path.join(d, path)
                    x = open(p, encoding="utf-8").read()
                    assert x.count(old) == 1, f"{tag}: plant anchor found {x.count(old)} times in {path}"
                    open(p, "w", encoding="utf-8", newline="").write(x.replace(old, new))
            code, out = run(d)
            killed = code != 0 and re.search(r"(?m)^FAIL %s\b" % re.escape(tag), out)
            survivors += not killed
            print(("ok  " if killed else "LIVE") + f" {tag}  {what}")
            if not killed:
                print("     " + "\n     ".join(l for l in out.splitlines() if l.startswith("FAIL")) or "     (no FAIL line)")
    finally:
        shutil.rmtree(base, ignore_errors=True)
    print(f"{len(MUTANTS)} mutants, {survivors} surviving")
    return 1 if survivors else 0


if __name__ == "__main__":
    sys.exit(main())
