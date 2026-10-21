package glasscam.file;

import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.net.Uri;
import android.os.RemoteException;
import android.provider.MediaStore;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import glasscam.file.holder.BaseHolder;
import glasscam.file.holder.UriHolder;
import glasscam.utils.Log;
import glasscam.utils.StorageFileManager;

public class MediaStoreController {

    private final String TAG = MediaStoreController.class.getSimpleName();

    private final Context context;

    public MediaStoreController(Context context)
    {
        this.context = context;
    }

    /** Where raw sensor dumps (.bayer, .raw, .jps - files with no image MIME type) go: MediaStore only
     *  lets photos and videos into DCIM (owner, 4.3.96: Documents/GlassCam, on every Android version). */
    public static final String DUMPS_PATH = "Documents/GlassCam/";
    static final String DUMP_MIME = "application/octet-stream";

    /** The concrete MIME type for a capture's file name, or null when it has none (a raw dump).
     *  MediaStore on Android 14 rejects the wildcards "image/*" and "video/*" the app used to send
     *  ("Unsupported MIME type image/*": every capture crashed on the 9 Pro, walk 2026-10-21). */
    public static String mimeFor(String fileName)
    {
        String lower = fileName.toLowerCase(java.util.Locale.ROOT);
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg"))
            return "image/jpeg";
        if (lower.endsWith(".dng"))
            return "image/x-adobe-dng";
        if (lower.endsWith(".mp4"))
            return "video/mp4";
        return null;
    }

    public Uri addImg(File name)
    {
        // Add a specific media item.
        ContentResolver resolver = context.getContentResolver();
        String mime = mimeFor(name.getName());
        long now = System.currentTimeMillis() / 1000;
        ContentValues newImg = new ContentValues();
        newImg.put(MediaStore.MediaColumns.TITLE, name.getName());
        newImg.put(MediaStore.MediaColumns.DISPLAY_NAME, name.getName());
        newImg.put(MediaStore.MediaColumns.DATE_ADDED, now);
        newImg.put(MediaStore.MediaColumns.DATE_MODIFIED, now);
        if (mime == null) {
            // a raw dump is not an image MediaStore will keep in DCIM: Documents/GlassCam, through Files
            newImg.put(MediaStore.MediaColumns.MIME_TYPE, DUMP_MIME);
            newImg.put(MediaStore.MediaColumns.RELATIVE_PATH, DUMPS_PATH);
            return resolver.insert(MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY), newImg);
        }
        newImg.put(MediaStore.Images.Media.MIME_TYPE, mime);
        newImg.put(MediaStore.Images.Media.DATE_TAKEN, now);
        newImg.put(MediaStore.Images.Media.RELATIVE_PATH, "DCIM/GlassCam/");
        return resolver.insert(getUri(), newImg);
    }

    public Uri addMovie(File name)
    {
        // Add a specific media item.
        ContentResolver resolver = context.getContentResolver();

        Uri extpath = getVideoUri();

        // Publish a new img.
        ContentValues newImg = new ContentValues();
        newImg.put(MediaStore.Video.Media.DISPLAY_NAME, name.getName());
        newImg.put(MediaStore.MediaColumns.TITLE, name.getName());
        newImg.put(MediaStore.Video.Media.MIME_TYPE, "video/mp4");   // a wildcard is rejected on Android 14
        newImg.put(MediaStore.Video.Media.DATE_ADDED, (System.currentTimeMillis() / 1000));
        newImg.put(MediaStore.Video.Media.DATE_TAKEN, (System.currentTimeMillis()/1000));
        newImg.put(MediaStore.Video.Media.DATE_MODIFIED, (System.currentTimeMillis()/1000));
        newImg.put(MediaStore.Video.Media.RELATIVE_PATH, "DCIM/GlassCam/");
        Uri ur = resolver.insert(extpath, newImg);
        return ur;

    }

    private Uri getUri() {
        return MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY);
    }

    private Uri getVideoUri() {
        return MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY);
    }


    /** Raw dumps the app saved in Documents/GlassCam (shown in the gallery's GlassCam folder). */
    private void getDumps(List<BaseHolder> fileHolders)
    {
        String[] projection = { MediaStore.MediaColumns._ID, MediaStore.MediaColumns.DISPLAY_NAME, MediaStore.MediaColumns.DATE_MODIFIED };
        Uri files = MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY);
        try (Cursor cursor = context.getContentResolver().query(files, projection,
                MediaStore.MediaColumns.RELATIVE_PATH + " = ?", new String[]{DUMPS_PATH},
                MediaStore.MediaColumns.DATE_MODIFIED + " DESC")) {
            if (cursor == null)
                return;
            int idColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID);
            int nameColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME);
            int dateColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_MODIFIED);
            while (cursor.moveToNext()) {
                long id = cursor.getLong(idColumn);
                String name = cursor.getString(nameColumn);
                if (name != null)
                    fileHolders.add(new UriHolder(ContentUris.withAppendedId(files, id), name, id, cursor.getLong(dateColumn), false, false));
            }
        }
        catch (SecurityException | IllegalArgumentException ex)
        {
            Log.WriteEx(ex);
        }
    }

    public List<BaseHolder> getFolders()
    {
        List<BaseHolder> fileHolders = new ArrayList<>();
        List<String> folders = new ArrayList<>();
        getImageFolders(folders);
        getMovieFolders(folders);
        List<BaseHolder> dumps = new ArrayList<>();
        getDumps(dumps);
        if (!dumps.isEmpty() && !folders.contains("GlassCam"))
            folders.add("GlassCam");
        for (String name : folders)
            if (name != null)
                fileHolders.add(new UriHolder(null, name,0, 0,true,false));
        return fileHolders;
    }

    private void getImageFolders(List<String> folders) {
        String[] projection = new String[] {
                MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
        };
        String sortOrder = MediaStore.Images.Media.BUCKET_DISPLAY_NAME + " ASC";
        try (Cursor cursor = context.getContentResolver().query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                null,
                null,
                sortOrder
        )) {
            // Cache column indices.
            int foldername = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME);
            //int idcol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_ID);

            while (cursor.moveToNext()) {
                // Get values of columns for a given image.
                //int id = cursor.getInt(idcol);
                String name = cursor.getString(foldername);
                if (!folders.contains(name))
                    folders.add(name);

                /*Uri contentUri = ContentUris.withAppendedId(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id);*/

            }


        }
        catch (SecurityException ex)
        {
            Log.WriteEx(ex);
        }
    }

    private void getMovieFolders(List<String> folders) {
        String[] projection = new String[] {
                MediaStore.Video.Media.BUCKET_DISPLAY_NAME,
        };
        String sortOrder = MediaStore.Video.Media.BUCKET_DISPLAY_NAME + " ASC";
        try (Cursor cursor = context.getContentResolver().query(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                projection,
                null,
                null,
                sortOrder
        )) {
            // Cache column indices.
            int foldername = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.BUCKET_DISPLAY_NAME);
            //int idcol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_ID);

            while (cursor.moveToNext()) {
                // Get values of columns for a given image.
                //int id = cursor.getInt(idcol);
                String name = cursor.getString(foldername);
                if (!folders.contains(name))
                    folders.add(name);

                /*Uri contentUri = ContentUris.withAppendedId(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id);*/

            }
        }
        catch (SecurityException ex)
        {
            Log.WriteEx(ex);
        }
    }

    public List<BaseHolder> getFiles()
    {
        List<BaseHolder> fileHolders = new ArrayList<>();
        getImages(fileHolders);
        getMovies(fileHolders);
        getDumps(fileHolders);
        return fileHolders;
    }

    private void getImages(List<BaseHolder> fileHolders) {
        String[] projection = new String[] {
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.DISPLAY_NAME,
                MediaStore.Images.Media.DATE_MODIFIED,
                MediaStore.Images.Media.DATE_ADDED,
                MediaStore.Images.Media.SIZE,
        };

        String sortOrder = MediaStore.Images.Media.DATE_TAKEN + " DESC";

        try (Cursor cursor = context.getContentResolver().query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                null,
                null,
                sortOrder
        )) {
            // Cache column indices.
            int idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID);
            int nameColumn =
                    cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME);
            int datetakenCol =
                    cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_MODIFIED);

            int dateaddedCol =
                    cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED);

            Log.d(TAG,"Date Mod: " + dateaddedCol + " date added: " + dateaddedCol);

            int sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE);
            //int relativPathColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.RELATIVE_PATH);

            while (cursor.moveToNext()) {
                // Get values of columns for a given image.
                long id = cursor.getLong(idColumn);
                String name = cursor.getString(nameColumn);
                long datetaken  = cursor.getLong(datetakenCol);
                int size = cursor.getInt(sizeColumn);

                Uri contentUri = ContentUris.withAppendedId(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id);
                if (name != null)
                    fileHolders.add(new UriHolder(contentUri, name,id, datetaken,false,false));
            }

        }
        catch (SecurityException ex)
        {
            Log.WriteEx(ex);
        }
    }

    private void getMovies(List<BaseHolder> fileHolders) {
        String[] projection = new String[] {
                MediaStore.Video.Media._ID,
                MediaStore.Video.Media.DISPLAY_NAME,
                MediaStore.Video.Media.DATE_MODIFIED,
                MediaStore.Video.Media.SIZE,
        };

        String sortOrder = MediaStore.Video.Media.DATE_TAKEN + " DESC";

        try (Cursor cursor = context.getContentResolver().query(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                projection,
                null,
                null,
                sortOrder
        )) {
            // Cache column indices.
            int idColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID);
            int nameColumn =
                    cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME);
            int datetakenCol =
                    cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_MODIFIED);
            int sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE);
            //int relativPathColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.RELATIVE_PATH);

            while (cursor.moveToNext()) {
                // Get values of columns for a given image.
                long id = cursor.getLong(idColumn);
                String name = cursor.getString(nameColumn);
                long datetaken  = cursor.getLong(datetakenCol);
                int size = cursor.getInt(sizeColumn);

                Uri contentUri = ContentUris.withAppendedId(
                        MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id);
                if (name != null)
                    fileHolders.add(new UriHolder(contentUri, name,id, datetaken,false,false));
            }

        }
        catch (SecurityException ex)
        {
            Log.WriteEx(ex);
        }
    }

    public List<BaseHolder> getFilesFromFolder(String folder)
    {
        List<BaseHolder> fileHolders = new ArrayList<>();
        getImagesFromFolder(folder, fileHolders);
        getMoviesFromFolder(folder,fileHolders);
        if ("GlassCam".equals(folder))
            getDumps(fileHolders);   // Documents/GlassCam: the raw dumps DCIM cannot hold
        return fileHolders;
    }

    private void getImagesFromFolder(String folder, List<BaseHolder> fileHolders) {
        String[] projection = new String[] {
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.DISPLAY_NAME,
                MediaStore.Images.Media.DATE_MODIFIED,
                MediaStore.Images.Media.SIZE,
                MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
        };

        String sortOrder = MediaStore.Images.Media.DATE_MODIFIED + " DESC";
        String selection = MediaStore.Images.Media.BUCKET_DISPLAY_NAME + " =?";
        String[] selectionargs = {folder };

        try (Cursor cursor = context.getContentResolver().query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                selectionargs,
                sortOrder
        )) {
            // Cache column indices.
            int idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID);
            int nameColumn =
                    cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME);
            int datetakenCol =
                    cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_MODIFIED);
            int sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE);
            int bucketdisplaynameColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME);
            //int relativPathColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.RELATIVE_PATH);

            while (cursor.moveToNext()) {
                // Get values of columns for a given image.
                long id = cursor.getLong(idColumn);
                String name = cursor.getString(nameColumn);
                long datetaken  = cursor.getLong(datetakenCol);
                int size = cursor.getInt(sizeColumn);
                String bucketdisname = cursor.getString(bucketdisplaynameColumn);

                Uri contentUri = ContentUris.withAppendedId(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id);
                if (name != null && folder.equals(bucketdisname))
                    fileHolders.add(new UriHolder(contentUri, name,id, datetaken,false,false));
            }
        }
        catch (SecurityException ex)
        {
            Log.WriteEx(ex);
        }
    }

    private void getMoviesFromFolder(String folder, List<BaseHolder> fileHolders) {
        String[] projection = new String[] {
                MediaStore.Video.Media._ID,
                MediaStore.Video.Media.DISPLAY_NAME,
                MediaStore.Video.Media.DATE_MODIFIED,
                MediaStore.Video.Media.SIZE,
                MediaStore.Video.Media.BUCKET_DISPLAY_NAME,
        };

        String sortOrder = MediaStore.Video.Media.DATE_MODIFIED + " DESC";
        String selection = MediaStore.Video.Media.BUCKET_DISPLAY_NAME + " =?";
        String[] selectionargs = {folder };

        try (Cursor cursor = context.getContentResolver().query(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                selectionargs,
                sortOrder
        )) {
            // Cache column indices.
            int idColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID);
            int nameColumn =
                    cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME);
            int datetakenCol =
                    cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_MODIFIED);
            int sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE);
            int bucketdisplaynameColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.BUCKET_DISPLAY_NAME);
            //int relativPathColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.RELATIVE_PATH);

            while (cursor.moveToNext()) {
                // Get values of columns for a given image.
                long id = cursor.getLong(idColumn);
                String name = cursor.getString(nameColumn);
                long datetaken  = cursor.getLong(datetakenCol);
                int size = cursor.getInt(sizeColumn);
                String bucketdisname = cursor.getString(bucketdisplaynameColumn);

                Uri contentUri = ContentUris.withAppendedId(
                        MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id);
                if (name != null && folder.equals(bucketdisname))
                    fileHolders.add(new UriHolder(contentUri, name,id, datetaken,false,false));
            }

        }
        catch (SecurityException ex)
        {
            Log.WriteEx(ex);
        }
    }

    public String getNewFilePath(boolean externalSd, String fileEnding)
    {
        StringBuilder builder = new StringBuilder();

        if (fileEnding.equals(".jpg") || fileEnding.equals(".dng") || fileEnding.equals(".jps"))
            builder.append(File.separator).append("IMG_");
        if (fileEnding.equals(".mp4"))
            builder.append(File.separator).append("MOV_");
        builder.append(StorageFileManager.getStringDatePAttern().format(new Date()))
                .append(fileEnding);
        return builder.toString();
    }

    public String getNewFilePath(String name, String fileEnding)
    {
        StringBuilder builder = new StringBuilder();

        if (fileEnding.equals(".jpg") || fileEnding.equals(".dng") || fileEnding.equals(".jps"))
            builder.append(File.separator).append("IMG_");
        if (fileEnding.equals(".mp4"))
            builder.append(File.separator).append("MOV_");
        builder.append(name)
                .append(fileEnding);
        return builder.toString();
    }

    public String getNewFilePathBurst(boolean externalSd, String fileEnding, int hdrcount)
    {
        StringBuilder builder = new StringBuilder();
        if (fileEnding.equals(".jpg") || fileEnding.equals(".dng") || fileEnding.equals(".jps"))
            builder.append(File.separator).append("IMG_");
        if (fileEnding.equals(".mp4"))
            builder.append(File.separator).append("MOV_");
        Date date = new Date();
        builder.append(StorageFileManager.getStringDatePAttern().format(date));
        builder.append("_BURST" + hdrcount);
        builder.append(fileEnding);
        return builder.toString();
    }

    public void deleteFiles(List<BaseHolder> baseHolders)
    {
        ArrayList<ContentProviderOperation> operationList = new ArrayList<ContentProviderOperation>();
        ContentProviderOperation contentProviderOperation;
        for (BaseHolder baseHolder: baseHolders)
        {
            UriHolder uriHolder = (UriHolder) baseHolder;
            contentProviderOperation = ContentProviderOperation.newDelete(uriHolder.getMediaStoreUri()).withSelection(MediaStore.Images.Media._ID +" = ?", new String[]{String.valueOf(uriHolder.getID())}).build();
            operationList.add(contentProviderOperation);
        }
        try {
            context.getContentResolver().applyBatch(MediaStore.AUTHORITY,operationList);
        } catch (OperationApplicationException e) {
            e.printStackTrace();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }
}
