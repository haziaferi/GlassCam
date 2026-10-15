GlassCam
========

A de-googled, SAF-only manual camera for Android, forked from
[KillerInk/FreeDcam](https://github.com/KillerInk/FreeDcam) (GPLv2). Package `com.hazia.glasscam`,
minSdk 30, built for one person on one phone (OnePlus 9 Pro, Android 14) and sideloaded — it is not
published in any store.

What it is, decision by decision, is in **[SPEC.md](SPEC.md)**: storage (SAF tree or MediaStore, every
capture in a `GlassCam/` folder), the landscape camera page, the settings drawer, the gallery, intents,
copy rules, what was removed, and the open questions.

Documents
---------

| file | what it holds |
|---|---|
| [SPEC.md](SPEC.md) | the standing specification; its revision line is the `versionName` it describes |
| [MIGRATION_NOTES.md](MIGRATION_NOTES.md) | the changelog of the port from FreeDcam (Stages 1–9) and later rounds |
| [CRITIQUE_2026-09-16.md](CRITIQUE_2026-09-16.md) | the design critique, its batch log (4.3.83–4.3.89) and errata |
| [docs/fresh-eyes-2026-09-28.md](docs/fresh-eyes-2026-09-28.md) | the fallacy and inconsistency audit (4.3.90) |

Build
-----

Windows 11, JDK 21, AGP 8.13.2, Gradle 8.14.3, NDK 27.3.13750724 (details in MIGRATION_NOTES,
"Building on Windows 11").

```
python tools/spec_check.py              # the spec gate: SPEC.md against the tree
python tools/critique/walk.py build     # arm64-v8a debug APK; runs the gate first
python tools/critique/walk.py install   # adb install -r -g -t on the attached phone
```

`gradlew.bat assembleDebug` works too. Debug builds are signed with the standard Android debug key.
When a check in `tools/spec_check.py` changes, `python tools/spec_check_mutants.py` proves it can fail.

Credits
-------

GlassCam is FreeDcam by KillerInk and contributors, changed as recorded in MIGRATION_NOTES. It uses
[LibRaw](https://github.com/LibRaw/LibRaw), [libtiff](http://www.remotesensing.org/libtiff/),
[TouchImageView](https://github.com/MikeOrtiz/TouchImageView), and Dave Coffin's
[dcraw](https://www.cybercom.net/~dcoffin/dcraw/) for the 16-bit bayer extraction.

The upstream README (device tables, custom matrix and tone-map guides) described FreeDcam and its
store listing, not this fork; it was retired in 4.3.91 and is kept outside the tree with the round's
working files.

License
-------

This program is free software; you can redistribute it and/or modify it under the terms of the GNU
General Public License as published by the Free Software Foundation; either version 2 of the License,
or (at your option) any later version. See [license.txt](license.txt).

This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public
License for more details.
