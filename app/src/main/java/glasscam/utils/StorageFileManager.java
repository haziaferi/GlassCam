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

package glasscam.utils;

import java.text.SimpleDateFormat;

/**
 * Created by troop on 01.08.2016.
 *
 * Historically also did raw java.io.File-based DCIM/SD-card scanning
 * (FileApiStorageDetector, readFilesFromFolder, getNewFilePath, ...). That
 * whole path guessed at legacy mount points like /storage/sdcard1 - it can
 * never run correctly under scoped storage, is unreachable now that storage
 * is SAF/MediaStore-only, and has been removed. Only the shared date-pattern
 * helpers remain: RawToDng (EXIF date) and MediaStoreController (display
 * name) still use them.
 */
public class StorageFileManager
{
    public static SimpleDateFormat getStringDatePAttern()
    {
        return new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss");
    }

    public static SimpleDateFormat getStringExifPattern()
    {
        return new SimpleDateFormat("yyyy:MM:dd HH:mm:ss");
    }
}
