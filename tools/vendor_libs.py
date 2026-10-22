"""Vendored native libraries (LibRaw, libtiff): import them from their release tarballs, and verify the tree.

    python tools/vendor_libs.py import --libraw LibRaw-X.tar.gz --libtiff tiff-Y.tar.gz
    python tools/vendor_libs.py verify

`import` rewrites app/src/main/jni/LibRaw and app/src/main/jni/tiff from the tarballs. It takes only what the
build compiles: the sources each project's own build lists, the files they #include (followed to closure),
and the licence and README files. It applies GlassCam's patches (PATCHES below, nothing else), generates
libtiff's two configuration headers for Android, and writes jni/vendored_sources.cmake (the source lists
CMakeLists.txt builds) and jni/VENDORED.json (versions, tarball SHA-256, every file's SHA-256, the patches).

`verify` checks, offline, that the vendored trees are exactly what the manifest records: no file changed,
missing or added. spec_check runs it as [S-38]. Text is hashed with CRLF folded to LF, so a Windows checkout
and CI hash alike.

The tarballs are read in memory: regular files under the release's top folder only, never links, nothing
executed. Standard library only.
"""
import argparse
import hashlib
import io
import json
import os
import posixpath
import re
import sys
import tarfile

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
JNI = os.path.join(ROOT, "app", "src", "main", "jni")
MANIFEST = os.path.join(JNI, "VENDORED.json")
SOURCES_CMAKE = os.path.join(JNI, "vendored_sources.cmake")
TREES = {"libraw": "LibRaw", "libtiff": "tiff"}

# GlassCam's only change to an upstream file. libtiff refuses, when writing, a tile width or length that is
# not a multiple of 16; the lossless-JPEG DNG path (DngWriter::process16ToLossless) writes two tiles of
# width/2 x height, so a sensor 3000 rows tall (or 4656 wide) could not save a compressed DNG. FreeDcam's copy
# of libtiff 4.0.9 had both checks commented out; this keeps libtiff's warning and drops the refusal.
#
# The others are behaviour-identical and exist for the repo's CodeQL gate (a PR may not add a high-severity
# alert): CodeQL flags them in upstream code although the values cannot overflow (PR #2, 2026-10-22).
PATCHES = {
    "libtiff": [
        ("libtiff/tif_dir.c",
         "allow non-multiple-of-16 tiles when writing (lossless-JPEG DNG tiles are width/2 x height)",
         "            if (v32 % 16)\n            {\n                if (tif->tif_mode != O_RDONLY)\n                    goto badvalue32;\n",
         "            if (v32 % 16)\n            {\n",
         2),
        ("libtiff/tif_dir.c",
         "CodeQL cpp/integer-multiplication-cast-to-long: multiply in tmsize_t (the same size was overflow-checked when tv->value was allocated)",
         "                    _TIFFmemcpy(tv->value, va_arg(ap, void *),\n                                tv->count * tv_size);\n",
         "                    _TIFFmemcpy(tv->value, va_arg(ap, void *),\n                                (tmsize_t)tv->count * tv_size);\n",
         1),
    ],
    "libraw": [
        ("src/decompressors/losslessjpeg.cpp",
         "CodeQL cpp/comparison-with-wider-type: a uint32_t loop counter (nbits is at most 16, so the uint8_t never wrapped)",
         "    for (uint8_t len = 0; len < nbits; len++)\n",
         "    for (uint32_t len = 0; len < nbits; len++)\n",
         1),
    ],
}

LICENCE_FILES = {"libraw": ["COPYRIGHT", "LICENSE.LGPL", "LICENSE.CDDL", "README.md"],
                 "libtiff": ["LICENSE.md", "README.md", "VERSION"]}

TEXT_EXT = {".c", ".cpp", ".h", ".hpp", ".cxx", ".md", ".txt", ".in", ".cmake", ".json", ".LGPL", ".CDDL", ""}


def digest(data, name):
    if os.path.splitext(name)[1] in TEXT_EXT:
        data = data.replace(b"\r\n", b"\n")
    return hashlib.sha256(data).hexdigest()


def read_tarball(path):
    """{relative path: bytes} for the regular files under the tarball's single top folder."""
    files, top = {}, None
    with tarfile.open(path, "r:gz") as tar:
        for m in tar.getmembers():
            if not m.isfile():
                continue
            parts = m.name.split("/")
            if top is None:
                top = parts[0]
            if parts[0] != top or ".." in parts or m.name.startswith("/"):
                raise SystemExit(f"{path}: unexpected member {m.name!r}")
            files["/".join(parts[1:])] = tar.extractfile(m).read()
    return top, files


def include_closure(files, start, search_dirs):
    """Every file in `files` reachable from `start` through #include "..." (relative to the including file,
    then to each search dir)."""
    seen, todo = set(), list(start)
    while todo:
        f = todo.pop()
        if f in seen:
            continue
        seen.add(f)
        text = files[f].decode("latin-1")
        for inc in re.findall(r'^\s*#\s*include\s*"([^"]+)"', text, flags=re.M):
            for base in [posixpath.dirname(f)] + search_dirs:
                cand = posixpath.normpath(posixpath.join(base, inc))
                if cand in files:
                    todo.append(cand)
                    break
    return seen


def libraw_selection(files):
    mk = files["Makefile.dist"].decode("latin-1")
    objs = re.search(r"^LIB_OBJECTS\s*=(.*?)(?:\n\s*\n)", mk, flags=re.S | re.M).group(1)
    objs = [o for o in re.findall(r"object/(\S+?)\.o\b", objs)]
    rules = dict(re.findall(r"^object/(\S+?)\.o:\s*(\S+)", mk, flags=re.M))
    sources = sorted(rules[o] for o in objs)
    missing = [s for s in sources if s not in files]
    if missing:
        raise SystemExit(f"LibRaw: Makefile.dist names sources the tarball lacks: {missing}")
    return sources, include_closure(files, sources, ["", "libraw", "internal", "src"])


def libtiff_selection(files):
    cm = files["libtiff/CMakeLists.txt"].decode("latin-1")
    # the first target_sources(tiff PRIVATE ...) block is the library's portable sources; tif_unix.c is added below
    block = re.search(r"target_sources\(tiff PRIVATE\s*\n(.*?)\)", cm, flags=re.S).group(1)
    listed = re.findall(r"\b(tif_\w+\.c)\b", block)
    sources = sorted({"libtiff/" + s for s in listed if s != "tif_win32.c"} | {"libtiff/tif_unix.c"})
    missing = [s for s in sources if s not in files]
    if missing:
        raise SystemExit(f"libtiff: CMakeLists.txt names sources the tarball lacks: {missing}")
    # tif_config.h and tiffconf.h are generated below, so the closure skips them
    return sources, include_closure(files, sources, ["libtiff"])


def render(template, defined, values):
    out = []
    for line in template.splitlines():
        m = re.match(r"#cmakedefine01\s+(\w+)", line)
        if m:
            out.append(f"#define {m.group(1)} {1 if m.group(1) in defined else 0}")
            continue
        m = re.match(r"#cmakedefine\s+(\w+)(.*)", line)
        if m:
            name, rest = m.group(1), m.group(2)
            if name in defined:
                out.append(f"#define {name}{re.sub(r'@(\w+)@', lambda v: values[v.group(1)], rest)}".rstrip())
            else:
                out.append(f"/* #undef {name} */")
            continue
        out.append(re.sub(r"@(\w+)@", lambda v: values.get(v.group(1), v.group(0)), line))
    # the templates' comments mention "@VAR@"; a placeholder left on a preprocessor line is a missing value
    left = [l for l in out if l.lstrip().startswith("#") and re.search(r"@\w+@", l)]
    if left:
        raise SystemExit(f"libtiff config: no value for {left}")
    return "\n".join(out) + "\n"


def libtiff_config(files, version):
    """tif_config.h and tiffconf.h for Android (bionic, little endian, ILP32 or LP64), the same codecs as before:
    no JPEG, ZIP, LZMA, ZSTD, WEBP, LERC or JBIG, so nothing outside libc is linked."""
    # the project's own bug-report address, as its CMake build sets it
    m = re.search(r'PACKAGE_BUGREPORT\s+"([^"]*)"', files.get("CMakeLists.txt", b"").decode("latin-1"))
    bugreport = m.group(1) if m else ""
    # SIZEOF_SIZE_T and TIFF_SSIZE_T follow the ABI being compiled: one header serves all four
    values = {"SIZEOF_SIZE_T": "__SIZEOF_SIZE_T__", "STRIP_SIZE_DEFAULT": "8192", "TIFF_MAX_DIR_COUNT": "1048576",
              "PACKAGE_NAME": "LibTIFF Software", "PACKAGE_BUGREPORT": bugreport,
              "PACKAGE_TARNAME": "tiff", "PACKAGE_URL": "", "LIBJPEG_12_PATH": "",
              "TIFF_INT8_T": "int8_t", "TIFF_INT16_T": "int16_t", "TIFF_INT32_T": "int32_t", "TIFF_INT64_T": "int64_t",
              "TIFF_UINT8_T": "uint8_t", "TIFF_UINT16_T": "uint16_t", "TIFF_UINT32_T": "uint32_t",
              "TIFF_UINT64_T": "uint64_t", "TIFF_SSIZE_T": "ptrdiff_t", "HOST_BIG_ENDIAN": "0"}
    common = {"CCITT_SUPPORT", "CHECK_JPEG_YCBCR_SUBSAMPLING", "DEFAULT_EXTRASAMPLE_AS_ALPHA", "LOGLUV_SUPPORT",
              "LZW_SUPPORT", "MDI_SUPPORT", "NEXT_SUPPORT", "PACKBITS_SUPPORT", "STRIPCHOP_DEFAULT",
              "SUBIFD_SUPPORT", "THUNDER_SUPPORT", "HAVE_IEEEFP"}
    platform = {"HAVE_ASSERT_H", "HAVE_DECL_OPTARG", "HAVE_FCNTL_H", "HAVE_FSEEKO", "HAVE_GETOPT", "HAVE_MMAP",
                "HAVE_STRINGS_H", "HAVE_SYS_TYPES_H", "HAVE_UNISTD_H", "STRIP_SIZE_DEFAULT"}
    head = (f"/* Generated by tools/vendor_libs.py from libtiff {version}'s {{}}: Android, all ABIs. Do not edit. */\n")
    return {
        "libtiff/tif_config.h": (head.format("tif_config.h.cmake.in") + render(
            files["libtiff/tif_config.h.cmake.in"].decode("latin-1"), common | platform, values)).encode(),
        "libtiff/tiffconf.h": (head.format("tiffconf.h.cmake.in") + render(
            files["libtiff/tiffconf.h.cmake.in"].decode("latin-1"), common, values)).encode(),
    }


def apply_patches(lib, chosen):
    applied = []
    for rel, why, old, new, count in PATCHES.get(lib, []):
        text = chosen[rel].decode("latin-1")
        found = text.count(old)
        if found != count:
            raise SystemExit(f"{lib} patch on {rel} expects {count} match(es), found {found}: re-check it against this release")
        chosen[rel] = text.replace(old, new).encode("latin-1")
        applied.append({"file": rel, "why": why, "matches": count})
    return applied


def do_import(args):
    entries, cmake = {}, ["# Generated by tools/vendor_libs.py: the vendored sources CMakeLists.txt builds. Do not edit.\n"]
    for lib, tarpath in (("libraw", args.libraw), ("libtiff", args.libtiff)):
        top, files = read_tarball(tarpath)
        if lib == "libraw":
            sources, needed = libraw_selection(files)
            version = top.split("-", 1)[1]
            var = "LIBRAW_SOURCES"
        else:
            sources, needed = libtiff_selection(files)
            version = files["VERSION"].decode().strip()
            var = "LIBTIFF_SOURCES"
        chosen = {rel: files[rel] for rel in sorted(needed | set(LICENCE_FILES[lib]))}
        patches = apply_patches(lib, chosen)
        if lib == "libtiff":
            chosen.update(libtiff_config(files, version))
        dest = os.path.join(JNI, TREES[lib])
        old = [os.path.join(d, f) for d, _, fs in os.walk(dest) for f in fs]
        for p in old:
            os.remove(p)
        for rel, data in chosen.items():
            p = os.path.join(dest, *rel.split("/"))
            os.makedirs(os.path.dirname(p), exist_ok=True)
            with open(p, "wb") as fh:
                fh.write(data)
        for d, subdirs, fs in sorted(os.walk(dest), key=lambda w: -len(w[0])):
            if not subdirs and not fs and d != dest:
                os.rmdir(d)
        with open(tarpath, "rb") as fh:
            tar_sha = hashlib.sha256(fh.read()).hexdigest()
        entries[lib] = {"version": version, "tarball": os.path.basename(tarpath), "tarball_sha256": tar_sha,
                        "dir": TREES[lib], "patches": patches,
                        "generated": sorted(r for r in chosen if lib == "libtiff" and r in ("libtiff/tif_config.h", "libtiff/tiffconf.h")),
                        "files": {rel: digest(data, rel) for rel, data in sorted(chosen.items())}}
        cmake.append(f"set({var}\n" + "".join(f"    {TREES[lib]}/{s}\n" for s in sources) + ")\n")
        print(f"{lib} {version}: {len(sources)} sources, {len(chosen)} files, {len(patches)} patch(es), was {len(old)} files")
    with open(MANIFEST, "w", encoding="utf-8", newline="\n") as fh:
        json.dump(entries, fh, indent=1, sort_keys=True)
        fh.write("\n")
    with open(SOURCES_CMAKE, "w", encoding="utf-8", newline="\n") as fh:
        fh.write("\n".join(cmake))


def verify():
    """[] when the trees match the manifest, else a list of problems."""
    if not os.path.exists(MANIFEST):
        return ["jni/VENDORED.json is missing"]
    entries = json.load(open(MANIFEST, encoding="utf-8"))
    problems = []
    for lib, e in sorted(entries.items()):
        dest = os.path.join(JNI, e["dir"])
        on_disk = {os.path.relpath(os.path.join(d, f), dest).replace(os.sep, "/")
                   for d, _, fs in os.walk(dest) for f in fs}
        for rel in sorted(on_disk - set(e["files"])):
            problems.append(f"{lib}: {rel} is not in the manifest")
        for rel, sha in sorted(e["files"].items()):
            p = os.path.join(dest, *rel.split("/"))
            if not os.path.exists(p):
                problems.append(f"{lib}: {rel} is missing")
            elif digest(open(p, "rb").read(), rel) != sha:
                problems.append(f"{lib}: {rel} differs from {lib} {e['version']} as imported")
    return problems


def main():
    ap = argparse.ArgumentParser(description=__doc__.split("\n\n")[0])
    sub = ap.add_subparsers(dest="cmd", required=True)
    imp = sub.add_parser("import")
    imp.add_argument("--libraw", required=True)
    imp.add_argument("--libtiff", required=True)
    sub.add_parser("verify")
    args = ap.parse_args()
    if args.cmd == "import":
        do_import(args)
    problems = verify()
    for p in problems:
        print("FAIL " + p)
    print(f"vendored trees: {len(problems)} problem(s)")
    sys.exit(1 if problems else 0)


if __name__ == "__main__":
    main()
