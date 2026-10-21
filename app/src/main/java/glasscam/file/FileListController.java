package glasscam.file;

import android.content.Context;
import android.net.Uri;
import android.text.TextUtils;

import androidx.documentfile.provider.DocumentFile;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import glasscam.GlassCamApplication;
import glasscam.file.holder.BaseHolder;
import glasscam.file.holder.DocumentHolder;
import glasscam.file.holder.FileHolder;
import glasscam.file.holder.UriHolder;
import glasscam.settings.SettingsManager;
import glasscam.utils.Log;
import glasscam.utils.MediaScannerManager;

public class FileListController {

    private final String TAG = FileListController.class.getSimpleName();

    public void addFromEventFile(BaseHolder fileHolder)
    {
        MediaScannerManager.ScanMedia(context,fileHolder);
        AddFile(fileHolder);
        fireCaptureSaved(fileHolder);
    }

    public void addFromEventFiles(BaseHolder[] fileHolder)
    {
        MediaScannerManager.ScanMedia(context,fileHolder);
        AddFiles(fileHolder);
        for (BaseHolder f : fileHolder)
            if (f != null) { fireCaptureSaved(f); break; }
    }

    /** Only the capture path raises this - a folder load fires onFilesChanged but never this. */
    private void fireCaptureSaved(BaseHolder file) {
        for (NotifyFilesChanged n : notifyFilesChangedList)
            n.onCaptureSaved(file);
    }

    public enum FormatTypes
    {
        all,
        raw,
        dng,
        jpg,
        jps,
        mp4,
    }


    public interface NotifyFilesChanged
    {
        void onFilesChanged();
        void onFileDeleted(int id);
        /** A capture from this session was just saved (never raised by a folder load). */
        default void onCaptureSaved(BaseHolder file) {}
    }


    private List<BaseHolder> files =new ArrayList<>();
    private final MediaStoreController mediaStoreController;
    private final Context context;
    private final List<NotifyFilesChanged> notifyFilesChangedList;
    private final Object filesLock = new Object();
    private final SettingsManager settingsManager;

    public FileListController(Context context)
    {
        this.context = context;
        mediaStoreController = new MediaStoreController(context);
        // registered/unregistered on the UI thread, walked from capture threads (fireCaptureSaved):
        // copy-on-write so a listener leaving mid-walk cannot throw ConcurrentModificationException
        notifyFilesChangedList = new java.util.concurrent.CopyOnWriteArrayList<>();
        settingsManager = GlassCamApplication.settingsManager();
    }

    public void setNotifyFilesChanged(NotifyFilesChanged notifyFilesChanged)
    {
        if (!notifyFilesChangedList.contains(notifyFilesChanged))
            notifyFilesChangedList.add(notifyFilesChanged);
    }

    public void removeNotifyFilesChanged(NotifyFilesChanged notifyFilesChanged)
    {
        notifyFilesChangedList.remove(notifyFilesChanged);
    }

    public List<BaseHolder> getFiles()
    {
        return files;
    }

    /** The newest non-folder entry, read under the list's own lock (the list is sorted newest
     *  first). Callers on other threads must use this, not synchronize on getFiles(): that is a
     *  different monitor and the list reference itself is replaced by a folder load. */
    public BaseHolder newestFile()
    {
        synchronized (filesLock) {
            for (BaseHolder f : files)
                if (f != null && !f.IsFolder())
                    return f;
            return null;
        }
    }

    public void loadDefaultFiles()
    {
        try {
            files = mediaStoreController.getFolders();

            DocumentFile documentFile = getGlassCamDocumentFolder();
            if (documentFile != null)
            {
                DocumentHolder documentHolder = new DocumentHolder(documentFile.getName(),documentFile.lastModified(),documentFile.isDirectory(),true,documentFile);
                files.add(documentHolder);
            }

            SortFileHolder(files);
            Log.d(TAG, "loadDefaultFiles found Files:" + files.size());
            fireNotifyFilesChanged();
        }
        catch (SecurityException ex)
        {
            Log.e(TAG, ex.getMessage());
            Log.WriteEx(ex);
        }
    }

    public void LoadGlassCamDCIMDirsFiles() {
        synchronized (filesLock) {
            files.clear();
            files = mediaStoreController.getFilesFromFolder("GlassCam");
            SortFileHolder(files);
            fireNotifyFilesChanged();
        }
    }

    public void LoadFolder(BaseHolder fileHolder,FormatTypes types)
    {
        synchronized (filesLock) {
            if (fileHolder instanceof DocumentHolder)
            {
                files.clear();
                new DocumentFileController().readFilesFromFolder((DocumentHolder) fileHolder,files,types,fileHolder.isExternalSD());
            }
            else {
                files.clear();
                List<BaseHolder> tmplist = mediaStoreController.getFilesFromFolder(fileHolder.getName());
                if (types != FormatTypes.all) {
                    for (BaseHolder fh : tmplist) {
                        if (fh.getName() != null) {
                            if (fh.getName().endsWith("jpg") && types == FormatTypes.jpg)
                                files.add(fh);
                            if (fh.getName().endsWith("jps") && types == FormatTypes.jps)
                                files.add(fh);
                            if (fh.getName().endsWith("dng") && types == FormatTypes.dng)
                                files.add(fh);
                            if (fh.getName().endsWith("bayer") && types == FormatTypes.raw)
                                files.add(fh);
                            if (fh.getName().endsWith("mp4") && types == FormatTypes.mp4)
                                files.add(fh);
                        }
                    }
                } else
                    files = tmplist;
                SortFileHolder(files);
            }
            fireNotifyFilesChanged();
        }
    }

    private void SortFileHolder(List<BaseHolder> f)
    {
        try {
            Collections.sort(f, (f1, f2) -> Long.valueOf(f2.lastModified()).compareTo(Long.valueOf(f1.lastModified())));
        }
        catch (NullPointerException ex)
        {
            Log.WriteEx(ex);
        }
    }

    public static DocumentFile getExternalSdDocumentFile(Context context)
    {
        DocumentFile sdDir = null;
        String extSdFolder =  GlassCamApplication.settingsManager().GetBaseFolder();
        if (extSdFolder == null || TextUtils.isEmpty(extSdFolder))
            return null;
        Uri uri = Uri.parse(extSdFolder);
        sdDir = DocumentFile.fromTreeUri(context, uri);
        // a stored tree can be gone (folder deleted) or its grant revoked: then it is no folder
        // at all, not a DocumentFile that fails on first use (walk 2026-09-16)
        try {
            if (sdDir == null || !sdDir.exists())
                return null;
        } catch (SecurityException ex) {
            Log.WriteEx(ex);
            return null;
        }
        return sdDir;
    }

    private DocumentFile getDCIMDocumentFolder(boolean create) {
        DocumentFile documentFile = null;
        DocumentFile sdDir;
        if ((sdDir = getExternalSdDocumentFile(context)) != null) {
            documentFile = sdDir.findFile("DCIM");
            if (documentFile == null && create)
                documentFile = sdDir.createDirectory("DCIM");
        }
        return documentFile;
    }

    private DocumentFile getChoosenDocumentFolder()
    {
        return getExternalSdDocumentFile(context);
    }

    /**
     * The folder captures are written to and the gallery lists under "External": a GlassCam/
     * child of the tree the user picked (created on first use). Batch 3 (2026-09-16): before,
     * captures landed directly in the picked folder's root - five bracket frames straight into
     * Pictures/. A picked folder already named GlassCam is used as is.
     */
    public DocumentFile getGlassCamDocumentFolder()
    {
        DocumentFile root = getChoosenDocumentFolder();
        if (root == null)
            return null;
        if ("GlassCam".equalsIgnoreCase(root.getName()))
            return root;
        try {
            DocumentFile child = root.findFile("GlassCam");
            if (child == null)
                child = root.createDirectory("GlassCam");
            return child;   // null when it cannot be made: never the picked root (owner, 4.3.91; SPEC ST2)
        } catch (Exception ex) {
            Log.WriteEx(ex);
            return null;
        }
    }

    /** The GlassCam/ child could not be made in a tree that exists: the capture is refused, not
     *  written loose into the picked folder, and the user is told why (owner, 4.3.91). */
    private void refuseCapture(String what) {
        Log.d(TAG, "refused " + what + ": GlassCam/ folder unavailable in the picked tree");
        new android.os.Handler(android.os.Looper.getMainLooper()).post(() ->
                android.widget.Toast.makeText(context, com.hazia.glasscam.R.string.storage_glasscam_folder_failed,
                        android.widget.Toast.LENGTH_LONG).show());
    }

    public boolean DeleteFile(BaseHolder file) {
        boolean deleted;
        int index = -1;
        synchronized (filesLock) {   // index and removal under one lock: a load in between shifted it
            for (int i = 0; i < files.size(); i++)
            {
                if (files.get(i).getName().equals(file.getName()))
                    index = i;
            }
            deleted = deleteFile(file);
        }
        Log.d(TAG, "delete file: " + file.getName() + " " + deleted);
        if (deleted && index != -1) {
            fireNotifyFilesDeleted(index);
        }

        return deleted;
    }

    public void DeleteFiles(List<BaseHolder> files) {
        if (files == null || files.isEmpty())
            return;
        synchronized (filesLock) {
            // per holder: the first file's type used to decide for the whole batch
            List<BaseHolder> viaStore = new ArrayList<>();
            for (BaseHolder f : files) {
                if (f instanceof FileHolder || f instanceof DocumentHolder)
                    deleteFile(f);
                else
                    viaStore.add(f);
            }
            if (!viaStore.isEmpty())
                mediaStoreController.deleteFiles(viaStore);
            fireNotifyFilesChanged();
        }
    }

    public BaseHolder findFile(String name)
    {
        if (files != null)
        {
            for (BaseHolder f : files)
                if (f.getName().equals(name))
                    return f;
        }
        return null;
    }

    private boolean deleteFile(BaseHolder file)
    {
        boolean del = false;

        del = file.delete(context);
        if (del && files != null)
            files.remove(file);

        return del;

    }

    public void AddFile(BaseHolder file)
    {
        synchronized (filesLock) {
            files.add(file);
            SortFileHolder(files);
            fireNotifyFilesChanged();
        }

    }

    public void AddFiles(BaseHolder[] fil)
    {
        synchronized (filesLock) {
            for (BaseHolder fh : fil)
            {
                if (fh != null)
                    files.add(fh);
            }
            SortFileHolder(files);
            fireNotifyFilesChanged();
        }
    }

    private void fireNotifyFilesChanged() {
        for (NotifyFilesChanged n : notifyFilesChangedList)
            n.onFilesChanged();
    }

    private void fireNotifyFilesDeleted(int id) {
        for (NotifyFilesChanged n : notifyFilesChangedList)
            n.onFileDeleted(id);
    }

    /** Where a new still goes, or null when the capture is refused (see refuseCapture). A picked tree
     *  that no longer exists reads as no folder (ST3) and the still goes to MediaStore, as before. */
    public BaseHolder getNewImgFileHolder(File file)
    {
        if (settingsManager.GetWriteExternal() && getChoosenDocumentFolder() != null) {
            DocumentFile df = getGlassCamDocumentFolder();   // the GlassCam/ child, like movies and DNG output
            String mime = MediaStoreController.mimeFor(file.getName());
            DocumentFile wr = df == null ? null : df.createFile(mime != null ? mime : "application/octet-stream", file.getName());
            if (wr == null) {
                refuseCapture(file.getName());
                return null;
            }
            Log.d(TAG,"Filepath: " + wr.getUri());
            return new UriHolder(wr.getUri(), file.getName(), 0, wr.lastModified(), wr.isDirectory(), settingsManager.GetWriteExternal());
        }
        else {
            Uri uri = mediaStoreController.addImg(file);
            if (uri == null) {   // MediaStore said no: refuse with the message instead of a crash below
                refuseCapture(file.getName());
                return null;
            }
            return new UriHolder(uri,file.getName(),Long.valueOf(uri.getLastPathSegment()), System.currentTimeMillis(),false,settingsManager.GetWriteExternal());
        }
    }

    /** Where a new video goes, or null when the recording is refused (see refuseCapture). */
    public BaseHolder getNewMovieFileHolder(File file)
    {
        if (settingsManager.GetWriteExternal() && getChoosenDocumentFolder() != null) {
            DocumentFile df = getGlassCamDocumentFolder();
            DocumentFile wr = df == null ? null : df.createFile("video/mp4", file.getName());
            if (wr == null) {
                refuseCapture(file.getName());
                return null;
            }
            Log.d(TAG,"Filepath: " + wr.getUri());
            Uri uri = wr.getUri();
            return new UriHolder(uri, file.getName(), 0, wr.lastModified(), wr.isDirectory(), settingsManager.GetWriteExternal());
        }
        else {
            Uri uri = mediaStoreController.addMovie(file);
            if (uri == null) {
                refuseCapture(file.getName());
                return null;
            }
            return new UriHolder(uri,file.getName(),Long.valueOf(uri.getLastPathSegment()), System.currentTimeMillis(),false,settingsManager.GetWriteExternal());
        }
    }

    public String getNewFilePath(boolean external, String fileending)
    {
        return mediaStoreController.getNewFilePath(external,fileending);
    }

    public String getNewFilePath(String external, String fileending)
    {
        return mediaStoreController.getNewFilePath(external,fileending);
    }

    public String getNewFilePathBurst(boolean external, String fileending, int count)
    {
        return mediaStoreController.getNewFilePathBurst(external,fileending,count);
    }


}
