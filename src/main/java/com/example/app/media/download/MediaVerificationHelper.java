package com.example.app.media.download;

import android.content.Context;
import android.util.Log;

import com.example.app.data.local.PerazimDatabase;
import com.example.app.data.local.dao.DownloadedContentDao;
import com.example.app.data.local.dao.SermonDao;
import com.example.app.data.local.entity.DownloadedContentEntity;
import com.example.app.data.local.entity.SermonEntity;
import com.example.app.data.mapper.DownloadedContentMapper;
import com.example.app.domain.model.DownloadedContent;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Verification helper for Phase 1.8 Media / Download Foundation gate.
 * Validates:
 * 1. Enqueueing download record for test sermon ("sermon_breakthrough_01").
 * 2. Saving simulated local media payload (.mp3 placeholder) in internal storage.
 * 3. Registering DownloadedContent record in DownloadedContentDao.
 * 4. Validating architectural separation (§21): DownloadedContent has physical file path and size,
 *    while Sermon maintains domain metadata and points localAudioPath to local file.
 * 5. Deleting download and cleaning up local storage file.
 * 6. Emitting marker: [PERAZIM-MEDIA-DOWNLOAD-GATE-1.8: SUCCESS].
 */
public class MediaVerificationHelper {

    private static final String TAG = "PerazimMediaDownload";
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();

    public interface VerificationCallback {
        void onVerificationComplete(boolean success, String message);
    }

    public static void runVerification(Context context) {
        runVerification(context, (VerificationCallback) null);
    }

    public static void runVerification(Context context, PerazimDatabase db) {
        try {
            DownloadedContentDao downloadDao = db.downloadedContentDao();
            SermonDao sermonDao = db.sermonDao();
            DownloadManager downloadManager = new DownloadManager(context, downloadDao, sermonDao);
            verifyInternal(context, downloadManager, downloadDao, sermonDao);
            Log.i(TAG, "[PERAZIM-MEDIA-DOWNLOAD-GATE-1.8: SUCCESS]");
        } catch (Exception e) {
            Log.e(TAG, "[PERAZIM-MEDIA-DOWNLOAD-GATE-1.8: FAILED] " + e.getMessage(), e);
            throw new RuntimeException("Media verification failed: " + e.getMessage(), e);
        }
    }

    public static void runVerification(Context context, VerificationCallback callback) {
        EXECUTOR.execute(() -> {
            try {
                Log.i(TAG, "Starting Phase 1.8 Media / Download Foundation Verification Gate...");
                PerazimDatabase db = PerazimDatabase.getInstance(context);
                DownloadedContentDao downloadedContentDao = db.downloadedContentDao();
                SermonDao sermonDao = db.sermonDao();
                DownloadManager downloadManager = new DownloadManager(context, downloadedContentDao, sermonDao);

                verifyInternal(context, downloadManager, downloadedContentDao, sermonDao);

                Log.i(TAG, "[PERAZIM-MEDIA-DOWNLOAD-GATE-1.8: SUCCESS]");
                if (callback != null) {
                    callback.onVerificationComplete(true, "Phase 1.8 Media Download Foundation verified successfully.");
                }
            } catch (Exception e) {
                Log.e(TAG, "[PERAZIM-MEDIA-DOWNLOAD-GATE-1.8: FAILED] " + e.getMessage(), e);
                if (callback != null) {
                    callback.onVerificationComplete(false, e.getMessage());
                }
            }
        });
    }

    /**
     * Executes the Phase 1.8 verification gate synchronously.
     */
    public static boolean verifyInternal(Context context,
                                          DownloadManager downloadManager,
                                          DownloadedContentDao downloadDao,
                                          SermonDao sermonDao) throws Exception {
        String testSermonId = "sermon_breakthrough_01";

        // Setup: Ensure test sermon exists in SermonDao
        SermonEntity existingSermon = sermonDao.getSermonById(testSermonId);
        if (existingSermon == null) {
            SermonEntity newSermon = new SermonEntity(
                    testSermonId,
                    "Supernatural Breakthrough in Hard Times",
                    "Bishop Dr. David Mutweri",
                    "Breakthrough 2026",
                    "2 Samuel 5:20",
                    "https://stream.perazim.org/audio/breakthrough_01.mp3",
                    "https://stream.perazim.org/video/breakthrough_01.mp4",
                    System.currentTimeMillis(),
                    3600,
                    "A profound sermon on breaking through limitations through divine power.",
                    "Key points on divine timing and relentless faith.",
                    false,
                    ""
            );
            sermonDao.insert(newSermon);
            Log.i(TAG, "Prepared test sermon: " + testSermonId);
        } else {
            // Reset existing test sermon state
            existingSermon.setDownloaded(false);
            existingSermon.setLocalAudioPath("");
            sermonDao.update(existingSermon);
        }

        // ---------------------------------------------------------------------
        // 1. Enqueue download record for test sermon ("sermon_breakthrough_01")
        // ---------------------------------------------------------------------
        DownloadedContentEntity queuedEntity = downloadManager.enqueueDownload("SERMON", testSermonId);
        if (queuedEntity == null) {
            throw new IllegalStateException("Step 1 failed: enqueueDownload returned null.");
        }

        DownloadedContentEntity fetchedQueued = downloadManager.findEntity(testSermonId);
        if (fetchedQueued == null) {
            throw new IllegalStateException("Step 1 failed: Download record was not registered in DownloadedContentDao.");
        }
        if (!DownloadStatus.QUEUED.name().equalsIgnoreCase(fetchedQueued.getStatus())) {
            throw new IllegalStateException("Step 1 failed: Expected status QUEUED, got " + fetchedQueued.getStatus());
        }
        Log.i(TAG, "Step 1 passed: Download enqueued for " + testSermonId + " with status QUEUED.");

        // ---------------------------------------------------------------------
        // 2. Saving simulated local media payload (.mp3 placeholder) in internal storage
        // ---------------------------------------------------------------------
        byte[] simulatedPayload = "ID3\u0003\u0000\u0000\u0000\u0000\u0000#PerazimSimulatedMediaBreakthroughPayload2026#".getBytes(StandardCharsets.UTF_8);
        String fileName = testSermonId + ".mp3";
        File payloadFile = downloadManager.saveSimulatedPayload(fileName, simulatedPayload);

        if (payloadFile == null || !payloadFile.exists()) {
            throw new IllegalStateException("Step 2 failed: Simulated media payload file was not created.");
        }
        if (payloadFile.length() != simulatedPayload.length) {
            throw new IllegalStateException("Step 2 failed: Payload file size mismatch. Expected "
                    + simulatedPayload.length + ", got " + payloadFile.length());
        }
        Log.i(TAG, "Step 2 passed: Simulated media payload saved at "
                + payloadFile.getAbsolutePath() + " (" + payloadFile.length() + " bytes).");

        // ---------------------------------------------------------------------
        // 3. Registering DownloadedContent record in DownloadedContentDao
        // ---------------------------------------------------------------------
        DownloadedContentEntity completedEntity = downloadManager.completeDownload("SERMON", testSermonId, payloadFile);
        if (completedEntity == null) {
            throw new IllegalStateException("Step 3 failed: completeDownload returned null.");
        }

        DownloadedContentEntity fetchedCompleted = downloadManager.findEntity(testSermonId);
        if (fetchedCompleted == null) {
            throw new IllegalStateException("Step 3 failed: Completed download record not found in DownloadedContentDao.");
        }
        if (!DownloadStatus.COMPLETED.name().equalsIgnoreCase(fetchedCompleted.getStatus())) {
            throw new IllegalStateException("Step 3 failed: Expected status COMPLETED, got " + fetchedCompleted.getStatus());
        }
        if (fetchedCompleted.getFileSizeBytes() != simulatedPayload.length) {
            throw new IllegalStateException("Step 3 failed: fileSizeBytes in DownloadedContentDao mismatch. Expected "
                    + simulatedPayload.length + ", got " + fetchedCompleted.getFileSizeBytes());
        }
        if (!payloadFile.getAbsolutePath().equals(fetchedCompleted.getLocalUri())) {
            throw new IllegalStateException("Step 3 failed: localUri in DownloadedContentDao mismatch. Expected "
                    + payloadFile.getAbsolutePath() + ", got " + fetchedCompleted.getLocalUri());
        }
        Log.i(TAG, "Step 3 passed: DownloadedContent record registered in DownloadedContentDao with status COMPLETED.");

        // ---------------------------------------------------------------------
        // 4. Validating separation (§21): DownloadedContent has file path and size,
        //    while Sermon maintains metadata and points to local path
        // ---------------------------------------------------------------------
        // 4a. Verify DownloadedContent domain model
        DownloadedContent domainDownload = downloadManager.getDownloadedContent(testSermonId);
        if (domainDownload == null) {
            domainDownload = DownloadedContentMapper.toDomain(fetchedCompleted);
        }
        if (domainDownload == null) {
            throw new IllegalStateException("Step 4 failed: DownloadedContent domain model could not be mapped.");
        }
        if (!domainDownload.isCompleted()) {
            throw new IllegalStateException("Step 4 failed: DownloadedContent domain isCompleted flag must be true.");
        }
        if (domainDownload.getFileSizeBytes() != simulatedPayload.length) {
            throw new IllegalStateException("Step 4 failed: DownloadedContent domain fileSizeBytes mismatch.");
        }
        if (!payloadFile.getAbsolutePath().equals(domainDownload.getLocalFilePath())) {
            throw new IllegalStateException("Step 4 failed: DownloadedContent domain localFilePath mismatch.");
        }

        // 4b. Verify Sermon maintains domain metadata and points localAudioPath to local payload
        SermonEntity updatedSermon = sermonDao.getSermonById(testSermonId);
        if (updatedSermon == null) {
            throw new IllegalStateException("Step 4 failed: SermonEntity not found in database.");
        }
        if (!updatedSermon.isDownloaded()) {
            throw new IllegalStateException("Step 4 failed: SermonEntity isDownloaded flag must be true.");
        }
        if (!payloadFile.getAbsolutePath().equals(updatedSermon.getLocalAudioPath())) {
            throw new IllegalStateException("Step 4 failed: SermonEntity localAudioPath does not match payload file path.");
        }
        if (!"Supernatural Breakthrough in Hard Times".equals(updatedSermon.getTitle())) {
            throw new IllegalStateException("Step 4 failed: SermonEntity title metadata corrupted.");
        }
        if (!"Bishop Dr. David Mutweri".equals(updatedSermon.getPreacher())) {
            throw new IllegalStateException("Step 4 failed: SermonEntity preacher metadata corrupted.");
        }
        if (!"https://stream.perazim.org/audio/breakthrough_01.mp3".equals(updatedSermon.getAudioUrl())) {
            throw new IllegalStateException("Step 4 failed: SermonEntity stream audioUrl was corrupted.");
        }
        Log.i(TAG, "Step 4 passed: Architectural separation validated. DownloadedContent maintains file details; Sermon preserves metadata and points to local audio path.");

        // ---------------------------------------------------------------------
        // 5. Deleting download and cleaning up local file
        // ---------------------------------------------------------------------
        boolean deleted = downloadManager.deleteDownload(testSermonId);
        if (!deleted) {
            throw new IllegalStateException("Step 5 failed: deleteDownload returned false.");
        }
        if (payloadFile.exists()) {
            throw new IllegalStateException("Step 5 failed: Local media payload file was not cleaned up.");
        }

        DownloadedContentEntity postDeleteDownload = downloadManager.findEntity(testSermonId);
        if (postDeleteDownload != null) {
            throw new IllegalStateException("Step 5 failed: DownloadedContent record remains in DownloadedContentDao after deletion.");
        }

        SermonEntity resetSermon = sermonDao.getSermonById(testSermonId);
        if (resetSermon != null) {
            if (resetSermon.isDownloaded()) {
                throw new IllegalStateException("Step 5 failed: SermonEntity isDownloaded flag was not reset to false.");
            }
            if (resetSermon.getLocalAudioPath() != null && !resetSermon.getLocalAudioPath().isEmpty()) {
                throw new IllegalStateException("Step 5 failed: SermonEntity localAudioPath was not cleared.");
            }
            // Clean up test sermon
            sermonDao.deleteById(testSermonId);
        }
        Log.i(TAG, "Step 5 passed: Download deleted and local storage file cleaned up.");

        return true;
    }
}
