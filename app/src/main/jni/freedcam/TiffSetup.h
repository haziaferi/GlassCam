//
// What GlassCam's DNG code needs from libtiff beyond the stock library, without patching it (VENDORED.json):
//
//  - the DNG tags it writes that libtiff 4.7.2 compiles out with #if 0 (the DNG 1.2/1.3 block and TIFF/EP
//    StandardID in tif_dirinfo.c), registered through libtiff's tag-extender API with the definitions FreeDcam's
//    patched libtiff 4.0.9 used, so the DNGs come out as before (A/B on the 6T, 2026-10-22);
//  - libtiff's warnings and errors in logcat (FreeDcam patched tif_unix.c for that).
//
#ifndef GLASSCAM_TIFFSETUP_H
#define GLASSCAM_TIFFSETUP_H

#include <android/log.h>
#include <stdarg.h>
#include "tiffio.h"

static const TIFFFieldInfo glasscamDngFields[] = {
    // tag, readcount, writecount, type, bit, oktochange, passcount, name
    {TIFFTAG_PROFILEHUESATMAPDIMS, -1, -1, TIFF_LONG, FIELD_CUSTOM, 1, 1, (char *)"ProfileHueSatMapDims"},
    {TIFFTAG_PROFILEHUESATMAPDATA1, -1, -1, TIFF_FLOAT, FIELD_CUSTOM, 1, 1, (char *)"ProfileHueSatMapData1"},
    {TIFFTAG_PROFILEHUESATMAPDATA2, -1, -1, TIFF_FLOAT, FIELD_CUSTOM, 1, 1, (char *)"ProfileHueSatMapData2"},
    {TIFFTAG_PROFILETONECURVE, -1, -1, TIFF_FLOAT, FIELD_CUSTOM, 1, 1, (char *)"ProfileToneCurve"},
    {TIFFTAG_FORWARDMATRIX1, -1, -1, TIFF_SRATIONAL, FIELD_CUSTOM, 1, 1, (char *)"ForwardMatrix1"},
    {TIFFTAG_FORWARDMATRIX2, -1, -1, TIFF_SRATIONAL, FIELD_CUSTOM, 1, 1, (char *)"ForwardMatrix2"},
    {TIFFTAG_OPCODELIST2, TIFF_VARIABLE2, TIFF_VARIABLE2, TIFF_UNDEFINED, FIELD_CUSTOM, 1, 1, (char *)"OpcodeList2"},
    {TIFFTAG_OPCODELIST3, TIFF_VARIABLE2, TIFF_VARIABLE2, TIFF_UNDEFINED, FIELD_CUSTOM, 1, 1, (char *)"OpcodeList3"},
    {TIFFTAG_NOISEPROFILE, -1, -1, TIFF_DOUBLE, FIELD_CUSTOM, 1, 1, (char *)"NoiseProfile"},
    {TIFFTAG_BASELINEEXPOSUREOFFSET, 1, 1, TIFF_SRATIONAL, FIELD_CUSTOM, 1, 0, (char *)"BaselineExposureOffset"},
    {TIFFTAG_EP_STANDARDID, 4, 4, TIFF_BYTE, FIELD_CUSTOM, 1, 0, (char *)"TIFFEPStandardID"},
};

static TIFFExtendProc glasscamParentExtender = NULL;

static void glasscamTagExtender(TIFF *tif)
{
    TIFFMergeFieldInfo(tif, glasscamDngFields, sizeof(glasscamDngFields) / sizeof(glasscamDngFields[0]));
    if (glasscamParentExtender)
        (*glasscamParentExtender)(tif);
}

static void glasscamTiffWarning(const char *module, const char *fmt, va_list ap)
{
    if (module != NULL)
        __android_log_print(ANDROID_LOG_WARN, "freedcam.libtiff", "%s:", module);
    __android_log_vprint(ANDROID_LOG_WARN, "freedcam.libtiff", fmt, ap);
}

static void glasscamTiffError(const char *module, const char *fmt, va_list ap)
{
    if (module != NULL)
        __android_log_print(ANDROID_LOG_ERROR, "freedcam.libtiff", "%s:", module);
    __android_log_vprint(ANDROID_LOG_ERROR, "freedcam.libtiff", fmt, ap);
}

// Call before TIFFOpen/TIFFFdOpen; the extender then runs for every file libtiff opens. Process-wide, set once.
static inline void installTiffSetup()
{
    static bool installed = false;
    if (installed)
        return;
    installed = true;
    glasscamParentExtender = TIFFSetTagExtender(glasscamTagExtender);
    TIFFSetWarningHandler(glasscamTiffWarning);
    TIFFSetErrorHandler(glasscamTiffError);
}

#endif //GLASSCAM_TIFFSETUP_H
