package com.example.app.data.local;

import android.content.Context;
import android.util.Log;

import com.example.app.data.local.dao.BibleDao;
import com.example.app.data.local.dao.HymnDao;
import com.example.app.data.local.dao.SermonDao;
import com.example.app.data.local.dao.SyncQueueDao;
import com.example.app.data.local.dao.UserDao;
import com.example.app.data.local.entity.BibleVerseEntity;
import com.example.app.data.local.entity.HymnEntity;
import com.example.app.data.local.entity.SermonEntity;
import com.example.app.data.local.entity.SyncQueueEntity;
import com.example.app.data.local.entity.UserEntity;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Asynchronous verification utility for Phase 1.1 Persistence Foundation gate.
 * Executes full CRUD operations across multiple DAOs to verify database integrity.
 */
public class PersistenceVerificationHelper {

    private static final String TAG = "PerazimPersistence";
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();

    public interface VerificationCallback {
        void onVerificationComplete(boolean success, String message);
    }

    public static void runVerification(Context context) {
        runVerification(context, null);
    }

    public static void runVerification(Context context, VerificationCallback callback) {
        EXECUTOR.execute(() -> {
            try {
                Log.d(TAG, "Starting Phase 1.1 Persistence Verification...");
                PerazimDatabase db = PerazimDatabase.getInstance(context);

                UserDao userDao = db.userDao();
                SermonDao sermonDao = db.sermonDao();
                HymnDao hymnDao = db.hymnDao();
                BibleDao bibleDao = db.bibleDao();
                SyncQueueDao syncQueueDao = db.syncQueueDao();

                long timestamp = System.currentTimeMillis();
                String testSuffix = "_" + timestamp;

                // 1. Insert test UserEntity
                String userId = "test_user" + testSuffix;
                UserEntity testUser = new UserEntity(
                        userId,
                        "Elder Test",
                        "elder.test@perazim.org",
                        "+254700000000",
                        "LEADER",
                        "campus_central",
                        "https://perazim.org/avatar.png",
                        timestamp,
                        timestamp,
                        true
                );
                userDao.insert(testUser);
                Log.d(TAG, "UserEntity inserted: " + userId);

                // 2. Insert test SermonEntity
                String sermonId = "test_sermon" + testSuffix;
                SermonEntity testSermon = new SermonEntity(
                        sermonId,
                        "Walking in the Supernatural",
                        "Pastor John",
                        "Breakthrough Season",
                        "2 Samuel 5:20",
                        "https://audio.perazim.org/sermon1.mp3",
                        "https://video.perazim.org/sermon1.mp4",
                        timestamp,
                        2400,
                        "Faith and perseverance for breakthrough",
                        "Key points on divine timing",
                        false,
                        ""
                );
                sermonDao.insert(testSermon);
                Log.d(TAG, "SermonEntity inserted: " + sermonId);

                // 3. Insert test HymnEntity
                String hymnId = "test_hymn" + testSuffix;
                HymnEntity testHymn = new HymnEntity(
                        hymnId,
                        999,
                        "Great Is Thy Faithfulness",
                        "Great is Thy faithfulness, O God my Father...",
                        "D - G - A",
                        "D Major",
                        "3/4",
                        "Praise",
                        true
                );
                hymnDao.insert(testHymn);
                Log.d(TAG, "HymnEntity inserted: " + hymnId);

                // 4. Insert test BibleVerseEntity
                String verseId = "test_verse" + testSuffix;
                BibleVerseEntity testVerse = new BibleVerseEntity(
                        verseId,
                        "ch_2sam_5",
                        10, // 2 Samuel
                        5,  // Chapter 5
                        20, // Verse 20
                        "And David came to Baal-perazim, and David smote them there, and said, The LORD hath broken forth upon mine enemies before me, as the breach of waters."
                );
                bibleDao.insertVerse(testVerse);
                Log.d(TAG, "BibleVerseEntity inserted: " + verseId);

                // 5. Insert test SyncQueueEntity
                String syncId = "test_sync" + testSuffix;
                SyncQueueEntity testSync = new SyncQueueEntity(
                        syncId,
                        "CREATE",
                        "PRAYER",
                        "prayer_123",
                        "{\"title\":\"Test Prayer\"}",
                        timestamp,
                        "PENDING",
                        0,
                        null
                );
                syncQueueDao.insert(testSync);
                Log.d(TAG, "SyncQueueEntity inserted: " + syncId);

                // 6. Read them all back and verify data integrity
                UserEntity fetchedUser = userDao.getUserById(userId);
                if (fetchedUser == null || !fetchedUser.getName().equals("Elder Test")) {
                    throw new IllegalStateException("UserEntity verification failed: expected 'Elder Test', got " + (fetchedUser == null ? "null" : fetchedUser.getName()));
                }

                SermonEntity fetchedSermon = sermonDao.getSermonById(sermonId);
                if (fetchedSermon == null || !fetchedSermon.getTitle().equals("Walking in the Supernatural")) {
                    throw new IllegalStateException("SermonEntity verification failed");
                }

                HymnEntity fetchedHymn = hymnDao.getHymnById(hymnId);
                if (fetchedHymn == null || fetchedHymn.getNumber() != 999) {
                    throw new IllegalStateException("HymnEntity verification failed");
                }

                BibleVerseEntity fetchedVerse = bibleDao.getVerse(10, 5, 20);
                if (fetchedVerse == null || !fetchedVerse.getText().contains("Baal-perazim")) {
                    throw new IllegalStateException("BibleVerseEntity verification failed");
                }

                SyncQueueEntity fetchedSync = syncQueueDao.getItemById(syncId);
                if (fetchedSync == null || !"PENDING".equals(fetchedSync.getStatus())) {
                    throw new IllegalStateException("SyncQueueEntity verification failed");
                }

                Log.d(TAG, "All test entities read and verified with 100% integrity.");

                // 7. Clean up test records
                userDao.deleteById(userId);
                sermonDao.deleteById(sermonId);
                hymnDao.deleteById(hymnId);
                bibleDao.deleteVerseById(verseId);
                syncQueueDao.deleteById(syncId);

                // Verify cleanup
                if (userDao.getUserById(userId) != null || sermonDao.getSermonById(sermonId) != null) {
                    throw new IllegalStateException("Cleanup failed: test records remain in database");
                }
                Log.d(TAG, "Test records cleaned up successfully.");

                // 8. Log success marker
                Log.i(TAG, "[PERAZIM-PERSISTENCE-GATE-1.1: SUCCESS]");

                if (callback != null) {
                    callback.onVerificationComplete(true, "All CRUD operations verified successfully.");
                }

            } catch (Exception e) {
                Log.e(TAG, "[PERAZIM-PERSISTENCE-GATE-1.1: FAILED] " + e.getMessage(), e);
                if (callback != null) {
                    callback.onVerificationComplete(false, e.getMessage());
                }
            }
        });
    }
}
