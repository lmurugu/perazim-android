package com.example.app.presentation.ui;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.ScrollView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.app.domain.model.Hymn;
import com.example.app.domain.model.Sermon;
import com.example.app.presentation.viewmodel.SermonsViewModel;
import com.example.app.presentation.viewmodel.ViewModelFactory;
import com.example.app.presentation.viewmodel.WorshipViewModel;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Verification helper for Workstreams 2.4 & 2.5: Sermons & Worship UI Binders.
 * Validates SermonsUiBinder and WorshipUiBinder view creation, bindings,
 * filter chips, search filtering, audio player controls, speed toggling, download workflows,
 * and hymn chord/lyric detail views.
 */
public final class UiBinderVerificationHelper {

    private static final String TAG = "PerazimUiBinderGate";
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();

    public interface VerificationCallback {
        void onComplete(boolean success, @Nullable String message);
    }

    private UiBinderVerificationHelper() {}

    public static void runVerification(@NonNull Activity activity) {
        runVerification(activity, null);
    }

    public static void runVerification(@NonNull Activity activity, @Nullable VerificationCallback callback) {
        EXECUTOR.execute(() -> {
            try {
                Log.i(TAG, "=================================================================");
                Log.i(TAG, ">>> STARTING PERAZIM SERMONS & WORSHIP UI BINDERS VERIFICATION <<<");
                Log.i(TAG, "=================================================================");

                ViewModelFactory factory = ViewModelFactory.getInstance(activity.getApplicationContext());
                SermonsViewModel sermonsVM = factory.createSermonsViewModel();
                WorshipViewModel worshipVM = factory.createWorshipViewModel();

                // -----------------------------------------------------------------
                // STEP 1: SermonsUiBinder Construction & View Hierarchy Binding
                // -----------------------------------------------------------------
                Log.i(TAG, "[UI STEP 1] Validating SermonsUiBinder instantiation and view hierarchy...");
                final AtomicReference<SermonsUiBinder> sermonsBinderRef = new AtomicReference<>();
                final AtomicReference<ScrollView> sermonsScrollRef = new AtomicReference<>();
                final CountDownLatch latchSermonsBind = new CountDownLatch(1);

                activity.runOnUiThread(() -> {
                    try {
                        SermonsUiBinder binder = new SermonsUiBinder(activity, sermonsVM);
                        sermonsBinderRef.set(binder);
                        ScrollView sv = binder.bind();
                        sermonsScrollRef.set(sv);
                    } catch (Throwable t) {
                        Log.e(TAG, "Error binding SermonsUiBinder", t);
                    } finally {
                        latchSermonsBind.countDown();
                    }
                });

                if (!latchSermonsBind.await(5, TimeUnit.SECONDS) || sermonsScrollRef.get() == null) {
                    throw new IllegalStateException("SermonsUiBinder.bind() timed out or returned null view");
                }
                SermonsUiBinder sermonsBinder = sermonsBinderRef.get();
                Log.i(TAG, "[UI STEP 1: COMPLETED] SermonsUiBinder view hierarchy built successfully.");

                // -----------------------------------------------------------------
                // STEP 2: Sermons Data Loading & Audio Player Controller
                // -----------------------------------------------------------------
                Log.i(TAG, "[UI STEP 2] Validating SermonsViewModel feed and Audio Player Controller...");
                final CountDownLatch latchSermonsFeed = new CountDownLatch(1);
                final AtomicReference<List<Sermon>> sermonsListRef = new AtomicReference<>();

                sermonsVM.loadSermons(sermons -> {
                    sermonsListRef.set(sermons);
                    latchSermonsFeed.countDown();
                });

                if (!latchSermonsFeed.await(5, TimeUnit.SECONDS) || sermonsListRef.get() == null || sermonsListRef.get().isEmpty()) {
                    throw new IllegalStateException("SermonsViewModel.loadSermons() failed or returned empty list");
                }
                List<Sermon> sermons = sermonsListRef.get();
                Log.i(TAG, "Sermons loaded: count=" + sermons.size() + ", first=" + sermons.get(0).getTitle());

                // Test audio player controller operations on UI thread
                final CountDownLatch latchPlayerOps = new CountDownLatch(1);
                activity.runOnUiThread(() -> {
                    try {
                        Sermon s = sermons.get(0);
                        sermonsBinder.startPlayback(s);

                        if (!sermonsBinder.isPlaying()) {
                            throw new IllegalStateException("Audio controller failed to start playback");
                        }

                        // Test speed toggle cycle: 1.0x -> 1.25x -> 1.5x -> 1.0x
                        float initialSpeed = sermonsBinder.getPlaybackSpeed();
                        sermonsBinder.cycleSpeed();
                        float speed2 = sermonsBinder.getPlaybackSpeed();
                        sermonsBinder.cycleSpeed();
                        float speed3 = sermonsBinder.getPlaybackSpeed();
                        sermonsBinder.cycleSpeed();
                        float speed4 = sermonsBinder.getPlaybackSpeed();

                        if (Math.abs(speed2 - 1.25f) > 0.01f || Math.abs(speed3 - 1.5f) > 0.01f || Math.abs(speed4 - 1.0f) > 0.01f) {
                            throw new IllegalStateException("Speed cycle mismatch: " + speed2 + ", " + speed3 + ", " + speed4);
                        }

                        // Test pause
                        sermonsBinder.pausePlayback();
                        if (sermonsBinder.isPlaying()) {
                            throw new IllegalStateException("Audio controller failed to pause");
                        }

                        // Test togglePlayPause
                        sermonsBinder.togglePlayPause();
                        if (!sermonsBinder.isPlaying()) {
                            throw new IllegalStateException("Audio controller failed to resume on togglePlayPause");
                        }
                        sermonsBinder.pausePlayback();
                    } catch (Throwable t) {
                        Log.e(TAG, "Audio player controller verification failed", t);
                        throw new RuntimeException(t);
                    } finally {
                        latchPlayerOps.countDown();
                    }
                });

                if (!latchPlayerOps.await(5, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("Audio player controller verification timed out");
                }
                Log.i(TAG, "[UI STEP 2: COMPLETED] Audio Player Controller verified (Play, Pause, Speed toggle).");

                // -----------------------------------------------------------------
                // STEP 3: Series Filter Chips & Search
                // -----------------------------------------------------------------
                Log.i(TAG, "[UI STEP 3] Validating Sermons series filter chips and search...");
                for (String chip : SermonsUiBinder.SERIES_CHIPS) {
                    CountDownLatch latchChip = new CountDownLatch(1);
                    sermonsVM.filterBySeries(chip, list -> {
                        latchChip.countDown();
                    });
                    if (!latchChip.await(5, TimeUnit.SECONDS)) {
                        throw new IllegalStateException("filterBySeries timed out for chip: " + chip);
                    }
                }

                CountDownLatch latchSearchSermon = new CountDownLatch(1);
                sermonsVM.searchSermons("Mutweri", results -> {
                    if (results != null && !results.isEmpty()) {
                        latchSearchSermon.countDown();
                    }
                });
                if (!latchSearchSermon.await(5, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("searchSermons('Mutweri') timed out or empty");
                }
                Log.i(TAG, "[UI STEP 3: COMPLETED] Series filter chips & search verified.");

                // -----------------------------------------------------------------
                // STEP 4: Download Workflow
                // -----------------------------------------------------------------
                Log.i(TAG, "[UI STEP 4] Validating Sermon download action workflow...");
                String sermonId = sermons.get(0).getId();
                CountDownLatch latchDl = new CountDownLatch(1);
                AtomicBoolean dlSuccess = new AtomicBoolean(false);

                sermonsVM.requestDownload(sermonId, success -> {
                    dlSuccess.set(Boolean.TRUE.equals(success));
                    latchDl.countDown();
                });

                if (!latchDl.await(5, TimeUnit.SECONDS) || !dlSuccess.get()) {
                    throw new IllegalStateException("Download request failed for sermon: " + sermonId);
                }

                CountDownLatch latchDel = new CountDownLatch(1);
                sermonsVM.deleteDownload(sermonId, delSuccess -> latchDel.countDown());
                if (!latchDel.await(5, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("Delete download timed out for sermon: " + sermonId);
                }
                Log.i(TAG, "[UI STEP 4: COMPLETED] Download & Delete action verified.");

                // -----------------------------------------------------------------
                // STEP 5: WorshipUiBinder Construction & View Hierarchy Binding
                // -----------------------------------------------------------------
                Log.i(TAG, "[UI STEP 5] Validating WorshipUiBinder instantiation and view hierarchy...");
                final AtomicReference<WorshipUiBinder> worshipBinderRef = new AtomicReference<>();
                final AtomicReference<ScrollView> worshipScrollRef = new AtomicReference<>();
                final CountDownLatch latchWorshipBind = new CountDownLatch(1);

                activity.runOnUiThread(() -> {
                    try {
                        WorshipUiBinder binder = new WorshipUiBinder(activity, worshipVM);
                        worshipBinderRef.set(binder);
                        ScrollView sv = binder.bind();
                        worshipScrollRef.set(sv);
                    } catch (Throwable t) {
                        Log.e(TAG, "Error binding WorshipUiBinder", t);
                    } finally {
                        latchWorshipBind.countDown();
                    }
                });

                if (!latchWorshipBind.await(5, TimeUnit.SECONDS) || worshipScrollRef.get() == null) {
                    throw new IllegalStateException("WorshipUiBinder.bind() timed out or returned null view");
                }
                WorshipUiBinder worshipBinder = worshipBinderRef.get();
                Log.i(TAG, "[UI STEP 5: COMPLETED] WorshipUiBinder view hierarchy built successfully.");

                // -----------------------------------------------------------------
                // STEP 6: Worship Categories, Search & Favorite Toggle
                // -----------------------------------------------------------------
                Log.i(TAG, "[UI STEP 6] Validating WorshipViewModel categories, search, and favorites...");
                final CountDownLatch latchHymnsFeed = new CountDownLatch(1);
                final AtomicReference<List<Hymn>> hymnsListRef = new AtomicReference<>();

                worshipVM.loadHymns(hymns -> {
                    hymnsListRef.set(hymns);
                    latchHymnsFeed.countDown();
                });

                if (!latchHymnsFeed.await(5, TimeUnit.SECONDS) || hymnsListRef.get() == null || hymnsListRef.get().isEmpty()) {
                    throw new IllegalStateException("WorshipViewModel.loadHymns() failed or returned empty list");
                }
                List<Hymn> hymns = hymnsListRef.get();
                Log.i(TAG, "Hymns loaded: count=" + hymns.size() + ", first=" + hymns.get(0).getTitle());

                // Verify category chips
                for (String cat : WorshipUiBinder.CATEGORY_CHIPS) {
                    CountDownLatch latchCat = new CountDownLatch(1);
                    worshipVM.filterByCategory(cat, list -> latchCat.countDown());
                    if (!latchCat.await(5, TimeUnit.SECONDS)) {
                        throw new IllegalStateException("filterByCategory timed out for category: " + cat);
                    }
                }

                // Verify keyword search
                CountDownLatch latchSearchHymn = new CountDownLatch(1);
                worshipVM.searchHymns("Grace", results -> {
                    if (results != null && !results.isEmpty()) {
                        latchSearchHymn.countDown();
                    }
                });
                if (!latchSearchHymn.await(5, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("searchHymns('Grace') timed out or empty");
                }

                // Verify favorite toggle
                Hymn testHymn = hymns.get(0);
                boolean prevFav = testHymn.isFavorite();
                CountDownLatch latchFav = new CountDownLatch(1);
                worshipVM.toggleFavorite(testHymn.getId(), !prevFav, latchFav::countDown);
                if (!latchFav.await(5, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("toggleFavorite timed out");
                }
                // Revert
                CountDownLatch latchFavRev = new CountDownLatch(1);
                worshipVM.toggleFavorite(testHymn.getId(), prevFav, latchFavRev::countDown);
                latchFavRev.await(5, TimeUnit.SECONDS);

                Log.i(TAG, "[UI STEP 6: COMPLETED] Category filter chips, search, and favorite toggle verified.");

                // -----------------------------------------------------------------
                // STEP 7: Hymn Detail Sheet Structure (Number, Key, Time, Chords, Lyrics)
                // -----------------------------------------------------------------
                Log.i(TAG, "[UI STEP 7] Validating Hymn Detail Sheet metadata & chords...");
                if (testHymn.getNumber() <= 0) {
                    throw new IllegalStateException("Hymn number invalid: " + testHymn.getNumber());
                }
                if (testHymn.getKeySignature() == null || testHymn.getKeySignature().isEmpty()) {
                    throw new IllegalStateException("Hymn key signature missing");
                }
                if (testHymn.getTimeSignature() == null || testHymn.getTimeSignature().isEmpty()) {
                    throw new IllegalStateException("Hymn time signature missing");
                }
                if (testHymn.getChords() == null || testHymn.getChords().isEmpty()) {
                    throw new IllegalStateException("Hymn chord progression missing");
                }
                if (testHymn.getLyrics() == null || testHymn.getLyrics().isEmpty()) {
                    throw new IllegalStateException("Hymn lyrics missing");
                }
                Log.i(TAG, "Hymn Detail verified: #" + testHymn.getNumber() + " " + testHymn.getTitle()
                        + ", Key=" + testHymn.getKeySignature() + ", Time=" + testHymn.getTimeSignature()
                        + ", Chords=" + testHymn.getChords());
                Log.i(TAG, "[UI STEP 7: COMPLETED] Hymn detail metadata, chords and lyrics verified.");

                Log.i(TAG, "=================================================================");
                Log.i(TAG, "[PERAZIM-UI-BINDER-GATE-2.4-2.5: ALL CHECKS PASSED]");
                Log.i(TAG, "=================================================================");

                if (callback != null) {
                    callback.onComplete(true, "All UI binder checks passed successfully.");
                }
            } catch (Exception e) {
                Log.e(TAG, "UI Binder verification failed", e);
                if (callback != null) {
                    callback.onComplete(false, e.getMessage());
                }
            }
        });
    }
}
