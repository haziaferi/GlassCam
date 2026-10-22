//
// Created by troop on 22.11.2020.
//

#include "LibRawWrapper.h"
#include <jni.h>
#include <android/log.h>
#include <stdlib.h>
#include <stdio.h>
#include "JniUtils.h"
#include <cstring>
#include <unistd.h>

#define  LOG_TAG    "freedcam.LibRawWrapper"
#define  LOGD(...)  __android_log_print(ANDROID_LOG_DEBUG,LOG_TAG,__VA_ARGS__)

void copyMatrix(float* dest, float colormatrix[4][3])
{
    int m = 0;
    for (int i = 0; i < 3; i++)
    {
        for (int t = 0; t < 3; t++)
        {
            dest[m++] = colormatrix[i][t];
        }
    }
}
void copyMatrix(float* dest, float colormatrix[3][4])
{
    int m = 0;
    for (int i = 0; i < 3; i++)
    {
        for (int t = 0; t < 3; t++)
        {
            dest[m++] = colormatrix[i][t];
        }
    }
}

void copyMatrix(float* dest, float colormatrix[4])
{
    int m = 0;
    for (int i = 0; i < 3; i++)
    {
        dest[m++] = colormatrix[i];
    }
}

void LibRawWrapper::openFD(int fd) {
    FILE *f = fdopen(fd, "r" );
    if (f == NULL)
    {
        LOGD("ERROR fdopen failed");
        close(fd); // the Java side detached it, so it is ours to close
        return;
    }
    LOGD("FileDescriptor open");
    fseek(f, 0, SEEK_END);
    long fsize = ftell(f);
    LOGD("size:%ld",fsize);
    fseek(f, 0, SEEK_SET);  /* same as rewind(f); */
    if (fsize <= 0 || (buffer = (char *)malloc(fsize + 1)) == NULL)
    {
        LOGD("ERROR no buffer for %ld bytes", fsize);
        fclose(f);
        return;
    }
    isFP = true;
    size_t read = fread(buffer, 1, fsize, f);
    fclose(f); // Close the file
    if (read != (size_t)fsize)
    {
        LOGD("ERROR short read %zu of %ld", read, fsize);
        return; // getBitmap() then fails in unpack(); recycle() frees the buffer
    }
    LOGD("closed file, libraw open buffer");
    raw.open_buffer(buffer,fsize);
}

void LibRawWrapper::openFile(const char *strFilename) {
    int ret;
    if((ret = raw.open_file(strFilename)) != LIBRAW_SUCCESS)
    {
        LOGD("ERROR open File %s",libraw_strerror(ret));
    }
}

jobject LibRawWrapper::getBitmap(JNIEnv* env) {
    raw.imgdata.params.no_auto_bright = 1;
    raw.imgdata.params.use_camera_wb = 1;
    raw.imgdata.params.output_bps = 8;
    raw.imgdata.params.user_qual = 0;
    raw.imgdata.params.half_size = 1;
    int ret;
    if((ret = raw.unpack()) != LIBRAW_SUCCESS)
    {
        LOGD("ERROR unpack File %s",libraw_strerror(ret));
        return NULL;
    }
    LOGD("unpacked img %i", ret);
    if((ret = raw.dcraw_process()) != LIBRAW_SUCCESS)
    {
        LOGD("ERROR unpack File %s",libraw_strerror(ret));
        return NULL;
    }
    LOGD("processing dcraw %i", ret);
    libraw_processed_image_t *image = raw.dcraw_make_mem_image(&ret);
    if (image == NULL)
    {
        LOGD("ERROR make_mem_image %s",libraw_strerror(ret));
        return NULL;
    }

    LOGD("processed image, creating bitmap");
    jobject ob = NULL;
    if(image->width != 0 && image->height != 0)
        ob = copyToJavaBitmap(env,image->data,image->data_size,image->width,image->height);

    // dcraw_make_mem_image() mallocs the image; only dcraw_clear_mem() may free it
    // (until 4.3.98 it went through C++ delete: a malloc/delete mismatch). The file buffer stays alive
    // until recycle(): LibRaw still points into it.
    LibRaw::dcraw_clear_mem(image);

    return ob;
}

void LibRawWrapper::recycle() {
    raw.recycle();
    if (isFP)
    {
        free(buffer);
        buffer = nullptr;
        isFP = false;
    }
}

ushort * LibRawWrapper::getRawData() {
    raw.imgdata.params.no_auto_bright = 0; //-W
    raw.imgdata.params.use_camera_wb = 1;
    raw.imgdata.params.output_bps = 16; // -6
    raw.imgdata.params.output_color = 0;
    //raw.imgdata.params.user_qual = 0;
    //raw.imgdata.params.half_size = 1;
    raw.imgdata.params.no_auto_scale = 0;
    raw.imgdata.params.gamm[0] = 1.0; //-g 1 1
    raw.imgdata.params.gamm[1] = 1.0; //-g 1 1
    raw.imgdata.params.output_tiff = 0;
    raw.imgdata.params.no_interpolation = 1;
    int ret;
    if ((ret = raw.unpack()) != LIBRAW_SUCCESS)
        return NULL;
    width = raw.imgdata.sizes.raw_width;
    height =  raw.imgdata.sizes.raw_height;
    int t = 0;
    return raw.imgdata.rawdata.raw_image;
}

void LibRawWrapper::getExifInfo(ExifInfo * exifInfo) {
    exifInfo->_iso = raw.imgdata.other.iso_speed;
    exifInfo->_exposure = raw.imgdata.other.shutter;
    exifInfo->_fnumber = raw.imgdata.other.aperture;
    exifInfo->_focallength = raw.imgdata.other.focal_len;
    exifInfo->_orientation = static_cast<char*>(static_cast<void*>(&raw.imgdata.sizes.flip));
}

bool match(char* ar1, char* ar2)
{
    bool ret = true;
    for (int i = 0; i < 4; ++i) {
        if (ar1[i] != ar2[i])
            return false;
    }
    return ret;
}

void LibRawWrapper::getDngProfile(DngProfile *dngprofile) {
    float* bl = new float[4];
    int black = (raw.imgdata.color.dng_levels.dng_cblack[6]);
    for (size_t i = 0; i < 4; i++)
    {
        bl[i] = black;
    }
    dngprofile->blacklevel = bl;
    dngprofile->whitelevel = raw.imgdata.color.dng_levels.dng_whitelevel[0];
    dngprofile->rawwidht = raw.imgdata.sizes.raw_width;
    dngprofile->rawheight = raw.imgdata.sizes.raw_height;
    dngprofile->rowSize = 0;

    char cfaar[4];
    cfaar[0] = raw.imgdata.idata.cdesc[raw.COLOR(0, 0)];
    cfaar[1] = raw.imgdata.idata.cdesc[raw.COLOR(0, 1)];
    cfaar[2] = raw.imgdata.idata.cdesc[raw.COLOR(1, 0)];
    cfaar[3] = raw.imgdata.idata.cdesc[raw.COLOR(1, 1)];

    char * cfa = cfaar;
    char* bggr = "BGGR";
    char* rggb = "RGGB";
    char* grbg = "GRBG";
    if (match(cfa,bggr))
    {
        dngprofile->bayerformat = "bggr";
    }
    else if (match(cfa,rggb))
    {
        dngprofile->bayerformat = "rggb";
    }
    else if (match(cfa,grbg))
    {
        dngprofile->bayerformat = "grbg";
    }
    else
    {
        dngprofile->bayerformat = "gbrg";
    }

    dngprofile->rawType = 6;

}

void LibRawWrapper::getCustomMatrix(CustomMatrix *matrix) {
    matrix->colorMatrix1 = new float[9];
    matrix->colorMatrix2 = new float[9];
    matrix->neutralColorMatrix = new float[3];
    matrix->fowardMatrix1 = new float[9];
    matrix->fowardMatrix2 = new float[9];
    matrix->reductionMatrix1 = new float[9];
    matrix->reductionMatrix2 = new float[9];

    copyMatrix(matrix->colorMatrix1, raw.imgdata.color.dng_color[0].colormatrix);
    copyMatrix(matrix->colorMatrix2, raw.imgdata.color.dng_color[1].colormatrix);
    copyMatrix(matrix->neutralColorMatrix, raw.imgdata.color.cam_mul);
    matrix->neutralColorMatrix[0] = 1 / matrix->neutralColorMatrix[0];
    matrix->neutralColorMatrix[1] = 1 / matrix->neutralColorMatrix[1];
    matrix->neutralColorMatrix[2] = 1 / matrix->neutralColorMatrix[2];
    copyMatrix(matrix->fowardMatrix1, raw.imgdata.color.dng_color[0].forwardmatrix);
    copyMatrix(matrix->fowardMatrix2, raw.imgdata.color.dng_color[1].forwardmatrix);
    copyMatrix(matrix->reductionMatrix1, raw.imgdata.color.dng_color[0].calibration);
    copyMatrix(matrix->reductionMatrix2, raw.imgdata.color.dng_color[1].calibration);
}

