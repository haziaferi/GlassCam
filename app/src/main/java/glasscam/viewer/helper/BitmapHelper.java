/*
 *
 *     Copyright (C) 2015 Ingo Fuchs
 *     This program is free software; you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation; either version 2 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License along
 *     with this program; if not, write to the Free Software Foundation, Inc.,
 *     51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 * /
 */

package glasscam.viewer.helper;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory.Options;
import android.media.ThumbnailUtils;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import glasscam.file.holder.BaseHolder;
import glasscam.utils.Log;
import glasscam.utils.StringUtils.FileEnding;

/**
 * Created by troop on 09.03.2016.
 */
public class BitmapHelper
{
    private static final String TAG = BitmapHelper.class.getSimpleName();
    // Target for the decoded "full view" bitmap's longest edge. inSampleSize is a power of two, so
    // the decoded edge lands between this and twice it (an 8000 px edge decodes to 2000 px), not at
    // it - generous for full-screen display, bounded for high-megapixel sources.
    private static final int MAX_FULLVIEW_DIMENSION = 1600;
    private final CacheHelper CACHE;
    // Raw files LibRaw could not decode. A headerless .bayer dump reads its whole 20 MB on every
    // grid bind otherwise, on several loader threads at once (6T walk, 2026-10-22); each file is
    // tried once per process, so a dump LibRaw recognises by its size still previews.
    private final Set<String> undecodableRaws = ConcurrentHashMap.newKeySet();
    private final int mImageThumbSizeW;
    private final Context context;

    private static int calculateInSampleSize(Options boundsOptions, int maxDimension)
    {
        int longestEdge = Math.max(boundsOptions.outWidth, boundsOptions.outHeight);
        int inSampleSize = 1;
        while (longestEdge / (inSampleSize * 2) >= maxDimension) {
            inSampleSize *= 2;
        }
        return inSampleSize;
    }


    public BitmapHelper(Context context, int mImageThumbSizeW)
    {
        this.context = context;
        CACHE = new CacheHelper(context);
        this.mImageThumbSizeW = mImageThumbSizeW;
    }

    public Bitmap getBitmap(final BaseHolder file, boolean thumb)
    {
        Bitmap response = null;
        try {
            if (CACHE == null)
                return null;

            response = getCacheBitmap(file,thumb);
            if (response == null)
            {
                response = createCacheImage(file,thumb);
            }

        } catch (NullPointerException e) {
            Log.d(TAG, e.getMessage());
        }
        return  response;
    }

    public Bitmap getCacheBitmap(final BaseHolder file, boolean thumb)
    {

        Bitmap response = null;

        try {
            if (CACHE == null)
                return null;
            if (!file.exists()) {
                response = CACHE.getBitmapFromDiskCache(file.getName() + "_thumb");
                if (response != null)
                    DeleteCache(file.getName());
                return null;
            }
            if (thumb)
            {
                if (response == null)
                {
                    response = CACHE.getBitmapFromDiskCache(file.getName() + "_thumb");
                }
            }
            else
            {
                if (response == null) {
                    response = CACHE.getBitmapFromDiskCache(file.getName());
                }
            }


        } catch (NullPointerException e) {
            Log.WriteEx(e);
        }
        return  response;
    }

    public  void DeleteCache(String name)
    {
        if (CACHE == null)
            return;
        CACHE.deleteFileFromDiskCache(name);
        CACHE.deleteFileFromDiskCache(name+"_thumb");
    }

    private Bitmap createCacheImage(BaseHolder file, boolean thumb)
    {
        Bitmap response = null;
        if (!file.exists())
            return response;
        if (response == null && file.getName() !=null)
        {
            if (file.getName().toLowerCase().endsWith(FileEnding.JPG) ||
                    file.getName().toLowerCase().endsWith(FileEnding.JPS)
                    )
            {
                // A flat inSampleSize=2 decodes at half-resolution regardless of the source's
                // actual size - fine for an old ~8MP sensor, but a 48MP+ photo still decodes to
                // a ~24MP/~96MB ARGB_8888 bitmap just to end up as a 130dp grid thumbnail (or a
                // "full view" bitmap far larger than any phone screen needs). Read the real
                // dimensions first and scale the sample size to the source, targeting a cap
                // that's still generous for the full-screen viewer.
                Options boundsOptions = new Options();
                boundsOptions.inJustDecodeBounds = true;
                file.getBitmap(context, boundsOptions);
                Options options = new Options();
                options.inSampleSize = calculateInSampleSize(boundsOptions, MAX_FULLVIEW_DIMENSION);
                response = file.getBitmap(context,options);
            }
            else if (file.getName().toLowerCase().endsWith(FileEnding.MP4)) {
                try {
                    response = file.getVideoThumb(context);
                } catch (IOException e) {
                    Log.d(TAG, e.getMessage());
                }
                //
            }
            else if (file.getName().toLowerCase().endsWith(FileEnding.DNG)
                    || file.getName().toLowerCase().endsWith(FileEnding.RAW) || file.getName().toLowerCase().endsWith(FileEnding.BAYER))
            {
                if (undecodableRaws.contains(file.getName()))
                    return null;
                try {
                    response = file.getBitmapFromDng(context);
                    if (response == null)
                        undecodableRaws.add(file.getName());
                }
                catch (IllegalArgumentException ex)
                {
                    Log.WriteEx(ex);

                }catch (UnsatisfiedLinkError ex)
                {
                    Log.WriteEx(ex);

                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (response != null && CACHE != null)
            {
                CACHE.addBitmapToCache(file.getName(), response);
                Bitmap thumbbitmap = ThumbnailUtils.extractThumbnail(response, mImageThumbSizeW, mImageThumbSizeW);
                CACHE.addBitmapToCache(file.getName() + "_thumb", thumbbitmap);
                if (thumb) {
                    response.recycle();
                    response = thumbbitmap;
                }
                else
                    thumbbitmap.recycle();

            }
        }
        else
        {
            Log.e(TAG, "failed to get file name");
        }
        return response;
    }

}

