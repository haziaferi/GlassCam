/*
 * Copyright (C) 2014 - 2026 GlassCam contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 */

package glasscam.cam;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;

import java.io.InputStream;
import java.io.OutputStream;

import glasscam.GlassCamApplication;
import glasscam.file.FileListController;
import glasscam.file.holder.BaseHolder;
import glasscam.utils.Log;

/**
 * Serves android.media.action.IMAGE_CAPTURE for other apps. Re-enabled in Batch 3 (2026-09-16):
 * the old version copied java.io.File paths, which SAF/MediaStore-backed captures do not have.
 * It now waits for the first capture saved in this session (NotifyFilesChanged.onCaptureSaved,
 * raised only by the capture path - a folder load must not count: the first cut compared the
 * newest name against launch time and delivered a *previous* capture as soon as the async file
 * list arrived), streams it into the caller's EXTRA_OUTPUT (or returns a small bitmap in "data"
 * when the caller gave none, as the platform contract says), sets the result and finishes.
 *
 * Reach: since Android 11 an implicit IMAGE_CAPTURE resolves only to the pre-installed camera,
 * so this activity is reached only by callers that name the package explicitly.
 */
public class ActivityGlassCamShare extends ActivityGlassCamMain implements FileListController.NotifyFilesChanged
{
    private final String TAG = ActivityGlassCamShare.class.getSimpleName();
    private Uri outputUri;
    private boolean captureRequested;
    /** onCaptureSaved arrives on capture threads, and a Jpg+DNG pair saves twice: claim the single
     *  delivery atomically (fresh-eyes 2026-09-28: a plain flag let both saves through). */
    private final java.util.concurrent.atomic.AtomicBoolean delivered = new java.util.concurrent.atomic.AtomicBoolean();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent callerIntent = getIntent();
        String action = callerIntent == null ? null : callerIntent.getAction();
        Log.d(TAG, "action " + action);
        captureRequested = MediaStore.ACTION_IMAGE_CAPTURE.equals(action);
        if (captureRequested) {
            outputUri = callerIntent.getParcelableExtra(MediaStore.EXTRA_OUTPUT);
            GlassCamApplication.fileListController().setNotifyFilesChanged(this);
        }
    }

    @Override
    protected void onDestroy() {
        GlassCamApplication.fileListController().removeNotifyFilesChanged(this);
        super.onDestroy();
    }

    @Override
    public void onFilesChanged() {
    }

    @Override
    public void onCaptureSaved(BaseHolder file) {
        if (!captureRequested || file == null || !delivered.compareAndSet(false, true))
            return;
        try {
            Intent result = new Intent();
            if (outputUri != null) {
                try (InputStream in = file.getInputStream();
                     OutputStream out = getContentResolver().openOutputStream(outputUri)) {
                    if (out == null)
                        throw new IllegalStateException("caller output not writable: " + outputUri);
                    byte[] buf = new byte[64 * 1024];
                    int len;
                    while ((len = in.read(buf)) > 0)
                        out.write(buf, 0, len);
                }
            } else {
                BitmapFactory.Options o = new BitmapFactory.Options();
                o.inSampleSize = 8;
                Bitmap thumb = file.getBitmap(this, o);
                if (thumb != null)
                    result.putExtra("data", thumb);
            }
            setResult(Activity.RESULT_OK, result);
        } catch (Exception ex) {
            Log.WriteEx(ex);
            setResult(Activity.RESULT_CANCELED);
        }
        runOnUiThread(this::finish);
    }

    @Override
    public void onFileDeleted(int id) {
    }
}
