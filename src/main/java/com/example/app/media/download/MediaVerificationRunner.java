package com.example.app.media.download;

import android.content.Context;
import android.content.ContextWrapper;
import android.os.Looper;
import android.util.Log;

import com.example.app.data.local.PerazimDatabase;
import com.example.app.data.local.dao.DownloadedContentDao;
import com.example.app.data.local.dao.SermonDao;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Standalone entry point to execute Phase 1.8 Media Verification Gate via app_process / adb.
 */
public class MediaVerificationRunner {

    private static final String TAG = "PerazimMediaDownload";

    public static void main(String[] args) {
        try {
            if (Looper.getMainLooper() == null) {
                Looper.prepareMainLooper();
            }

            // Suppress SQLiteCompatibilityWalFlags trying to contact Settings ContentProvider under app_process
            try {
                Class<?> walFlagsClass = Class.forName("android.database.sqlite.SQLiteCompatibilityWalFlags");
                Field initField = walFlagsClass.getDeclaredField("sInitialized");
                initField.setAccessible(true);
                initField.setBoolean(null, true);
            } catch (Throwable ignored) {
            }

            Class<?> activityThreadClass = Class.forName("android.app.ActivityThread");
            Method systemMainMethod = activityThreadClass.getMethod("systemMain");
            Object activityThread = systemMainMethod.invoke(null);

            Method getSystemContextMethod = activityThreadClass.getMethod("getSystemContext");
            Context systemContext = (Context) getSystemContextMethod.invoke(activityThread);

            Context packageContext = systemContext.createPackageContext(
                    "com.example.app",
                    Context.CONTEXT_INCLUDE_CODE | Context.CONTEXT_IGNORE_SECURITY
            );

            Context appContext = new ContextWrapper(packageContext) {
                @Override
                public Context getApplicationContext() {
                    return this;
                }
            };

            System.out.println("Starting Phase 1.8 Media Verification Runner with appContext: " + appContext.getPackageName());

            AtomicBoolean pass = new AtomicBoolean(false);
            AtomicReference<Throwable> errorRef = new AtomicReference<>();

            Thread worker = new Thread(() -> {
                try {
                    PerazimDatabase db = PerazimDatabase.getInstance(appContext);
                    DownloadedContentDao downloadDao = db.downloadedContentDao();
                    SermonDao sermonDao = db.sermonDao();
                    DownloadManager downloadManager = new DownloadManager(appContext, downloadDao, sermonDao);

                    boolean success = MediaVerificationHelper.verifyInternal(appContext, downloadManager, downloadDao, sermonDao);
                    pass.set(success);
                } catch (Throwable t) {
                    errorRef.set(t);
                }
            });

            worker.start();
            worker.join();

            if (errorRef.get() != null) {
                Throwable t = errorRef.get();
                System.err.println("[PERAZIM-MEDIA-DOWNLOAD-GATE-1.8: FAILED] Exception: " + t.getMessage());
                t.printStackTrace(System.err);
                Log.e(TAG, "[PERAZIM-MEDIA-DOWNLOAD-GATE-1.8: FAILED] " + t.getMessage(), t);
                System.exit(1);
            } else if (pass.get()) {
                System.out.println("[PERAZIM-MEDIA-DOWNLOAD-GATE-1.8: SUCCESS]");
                Log.i(TAG, "[PERAZIM-MEDIA-DOWNLOAD-GATE-1.8: SUCCESS]");
                System.exit(0);
            } else {
                System.err.println("[PERAZIM-MEDIA-DOWNLOAD-GATE-1.8: FAILED]");
                Log.e(TAG, "[PERAZIM-MEDIA-DOWNLOAD-GATE-1.8: FAILED] verifyInternal returned false");
                System.exit(1);
            }
        } catch (Throwable t) {
            System.err.println("[PERAZIM-MEDIA-DOWNLOAD-GATE-1.8: FAILED] Exception: " + t.getMessage());
            t.printStackTrace(System.err);
            Log.e(TAG, "[PERAZIM-MEDIA-DOWNLOAD-GATE-1.8: FAILED] " + t.getMessage(), t);
            System.exit(1);
        }
    }
}
