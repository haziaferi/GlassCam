"""Dump a DNG/TIFF's directories tag by tag, or compare two (standard library only).

    python tools/dng_dump.py FILE.dng                 # every IFD (0, SubIFDs, EXIF, GPS): tag, type, count, values
    python tools/dng_dump.py OLD.dng NEW.dng          # the same for both, then the tags that differ

Arrays longer than 16 values are shown as count + SHA-256; the image data (strips or tiles) is hashed, so two
files with the same pixels and tags compare equal whatever their layout. Written for the libtiff upgrade
(4.0.9 -> 4.7.2): a DNG from the old and the new build of the same input must match except the tags listed
in the report.
"""
import hashlib
import struct
import sys

TYPES = {1: ("B", 1), 2: ("s", 1), 3: ("H", 2), 4: ("I", 4), 5: ("II", 8), 6: ("b", 1), 7: ("B", 1),
         8: ("h", 2), 9: ("i", 4), 10: ("ii", 8), 11: ("f", 4), 12: ("d", 8), 13: ("I", 4), 16: ("Q", 8)}
NAMES = {254: "NewSubfileType", 256: "ImageWidth", 257: "ImageLength", 258: "BitsPerSample", 259: "Compression",
         262: "Photometric", 266: "FillOrder", 270: "ImageDescription", 271: "Make", 272: "Model",
         273: "StripOffsets", 274: "Orientation", 277: "SamplesPerPixel", 278: "RowsPerStrip",
         279: "StripByteCounts", 282: "XResolution", 283: "YResolution", 284: "PlanarConfig", 296: "ResolutionUnit",
         305: "Software", 306: "DateTime", 322: "TileWidth", 323: "TileLength", 324: "TileOffsets",
         325: "TileByteCounts", 330: "SubIFDs", 339: "SampleFormat", 33421: "CFARepeatPatternDim", 33422: "CFAPattern",
         34665: "ExifIFD", 34853: "GPSIFD", 37398: "TIFF-EPStandardID", 50706: "DNGVersion",
         50707: "DNGBackwardVersion", 50708: "UniqueCameraModel", 50710: "CFAPlaneColor", 50711: "CFALayout",
         50713: "BlackLevelRepeatDim", 50714: "BlackLevel", 50717: "WhiteLevel", 50718: "DefaultScale",
         50719: "DefaultCropOrigin", 50720: "DefaultCropSize", 50721: "ColorMatrix1", 50722: "ColorMatrix2",
         50723: "CameraCalibration1", 50724: "CameraCalibration2", 50728: "AsShotNeutral",
         50730: "BaselineExposure", 50778: "CalibrationIlluminant1", 50779: "CalibrationIlluminant2",
         50829: "ActiveArea", 50937: "ProfileHueSatMapDims", 50938: "ProfileHueSatMapData1",
         50939: "ProfileHueSatMapData2", 50940: "ProfileToneCurve", 50964: "ForwardMatrix1",
         50965: "ForwardMatrix2", 51009: "OpcodeList2", 51022: "OpcodeList3", 51041: "NoiseProfile",
         51109: "BaselineExposureOffset"}
DATA_TAGS = {273, 279, 324, 325}


def read_ifds(data):
    end = {b"II": "<", b"MM": ">"}[data[:2]]
    magic, first = struct.unpack(end + "HI", data[2:8])
    assert magic == 42, "not a classic TIFF"
    ifds, todo, seen = [], [("IFD0", first)], set()
    while todo:
        name, off = todo.pop(0)
        if not off or off in seen or off >= len(data):
            continue
        seen.add(off)
        n = struct.unpack(end + "H", data[off:off + 2])[0]
        tags = {}
        for i in range(n):
            e = off + 2 + 12 * i
            tag, typ, count = struct.unpack(end + "HHI", data[e:e + 8])
            fmt, size = TYPES.get(typ, ("B", 1))
            total = size * count
            where = e + 8 if total <= 4 else struct.unpack(end + "I", data[e + 8:e + 12])[0]
            raw = data[where:where + total]
            if typ == 2:
                vals = raw.rstrip(b"\0").decode("latin-1")
            elif typ in (5, 10):
                nums = struct.unpack(end + fmt[0] * (2 * count), raw)
                vals = tuple(round(nums[k] / nums[k + 1], 6) if nums[k + 1] else None for k in range(0, len(nums), 2))
            else:
                vals = struct.unpack(end + fmt * count, raw)
            tags[tag] = (typ, count, vals)
        nxt = struct.unpack(end + "I", data[off + 2 + 12 * n:off + 6 + 12 * n])[0]
        ifds.append((name, tags))
        if name == "IFD0" or name.startswith("IFD"):
            todo.append((f"IFD{len([i for i in ifds if i[0].startswith('IFD')])}", nxt))
        for sub, label in ((330, "SubIFD"), (34665, "EXIF"), (34853, "GPS")):
            if sub in tags:
                for k, o in enumerate(tags[sub][2]):
                    todo.append((f"{label}{k}" if sub == 330 else label, o))
    return ifds


def image_hash(data, tags):
    for offs, counts in ((273, 279), (324, 325)):
        if offs in tags and counts in tags:
            h = hashlib.sha256()
            for o, c in zip(tags[offs][2], tags[counts][2]):
                h.update(data[o:o + c])
            return h.hexdigest()[:16], sum(tags[counts][2])
    return None, 0


def describe(data):
    out = {}
    for name, tags in read_ifds(data):
        for tag, (typ, count, vals) in sorted(tags.items()):
            label = NAMES.get(tag, str(tag))
            if tag in DATA_TAGS:
                shown = f"{count} entries"
            elif isinstance(vals, tuple) and len(vals) > 16:
                shown = f"{len(vals)} values sha256:{hashlib.sha256(repr(vals).encode()).hexdigest()[:12]}"
            else:
                shown = repr(vals)
            out[f"{name}.{label}"] = f"type {typ} count {count}: {shown}"
        h, n = image_hash(data, tags)
        if h:
            out[f"{name}.<image data>"] = f"{n} bytes sha256:{h}"
    return out


def main():
    files = sys.argv[1:]
    if len(files) not in (1, 2):
        sys.exit(__doc__)
    dumps = [describe(open(f, "rb").read()) for f in files]
    if len(dumps) == 1:
        for k, v in dumps[0].items():
            print(f"{k:42} {v}")
        return
    a, b = dumps
    same = [k for k in a if k in b and a[k] == b[k]]
    print(f"{len(same)} tags identical")
    for k in sorted(set(a) | set(b)):
        if a.get(k) != b.get(k):
            print(f"DIFF {k}\n   old: {a.get(k, '(absent)')}\n   new: {b.get(k, '(absent)')}")


if __name__ == "__main__":
    main()
