"""Prove the purpose map's checks can fail: plant one defect per check in docs/purpose-map.json,
require the named FAIL from tools/purpose_map.py --check, then restore the file. An unplanted control
must pass first.

    python tools/purpose/map_mutants.py
"""
import json
import os
import shutil
import subprocess
import sys

ROOT = os.path.normpath(os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", ".."))
SRC = os.path.join(ROOT, "docs", "purpose-map.json")
BACKUP = SRC + ".orig"


def run():
    p = subprocess.run([sys.executable, os.path.join(ROOT, "tools", "purpose_map.py"), "--check"],
                       capture_output=True, text=True, encoding="utf-8")
    return p.returncode, p.stdout


def plant(fn):
    m = json.load(open(BACKUP, encoding="utf-8"))
    fn(m)
    with open(SRC, "w", encoding="utf-8", newline="\n") as fh:
        json.dump(m, fh, ensure_ascii=False, indent=1)
        fh.write("\n")


CASES = [
    ("dropped row", lambda m: next(s for s in m["screens"] if len(s["code"]) > 1)["code"].pop(), "in the code, not on the map"),
    ("renamed in code", lambda m: m["screens"][0]["code"].append("setting:NO_SUCH_KEY"), "gone from the code"),
    ("placed twice", lambda m: m["screens"][1]["code"].append(m["screens"][0]["code"][0]), "placed 2 times"),
    ("unknown purpose", lambda m: m["screens"][0].__setitem__("purpose", "P9"), "unknown purpose"),
]


def main():
    shutil.copy2(SRC, BACKUP)
    bad = 0
    try:
        code, _ = run()
        print(("ok  " if code == 0 else "BAD ") + "control passes")
        bad += code != 0
        for name, fn, want in CASES:
            plant(fn)
            code, out = run()
            hit = code != 0 and want in out
            bad += not hit
            print(("ok  " if hit else "LIVE") + " " + name)
    finally:
        shutil.move(BACKUP, SRC)
    print("%d case(s) failed" % bad)
    return 1 if bad else 0


if __name__ == "__main__":
    sys.exit(main())
