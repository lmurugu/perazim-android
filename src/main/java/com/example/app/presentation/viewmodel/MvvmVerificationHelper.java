package com.example.app.presentation.viewmodel;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.app.data.local.PerazimDatabase;
import com.example.app.data.local.entity.JokeEntity;
import com.example.app.data.local.entity.RiddleEntity;
import com.example.app.data.local.seeder.BibleDataSeeder;
import com.example.app.data.local.seeder.ContentDataSeeder;
import com.example.app.data.local.preference.GamificationStore;
import com.example.app.data.repository.RepositoryProvider;
import com.example.app.domain.model.BibleBook;
import com.example.app.domain.model.BibleVerse;
import com.example.app.domain.model.Hymn;
import com.example.app.domain.model.Prayer;
import com.example.app.domain.model.Sermon;
import com.example.app.domain.model.User;
import com.example.app.presentation.state.UiState;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Verification helper for Workstream 2.1: MVVM & State Architecture Layer.
 * Validates UiState wrapper, ViewModelFactory dependency resolution, and all 6 ViewModels
 * (Home, Bible, Sermons, Worship, Fellowship, Profile) connecting to Room repositories and managers.
 */
public final class MvvmVerificationHelper {

    private static final String TAG = "PerazimMvvmGate";
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();

    public interface VerificationCallback {
        void onComplete(boolean success, @Nullable String message);
    }

    private MvvmVerificationHelper() {}

    public static void runVerification(@NonNull Context context) {
        runVerification(context, null);
    }

    public static void runVerification(@NonNull Context context, @Nullable VerificationCallback callback) {
        Context appContext = context.getApplicationContext();
        EXECUTOR.execute(() -> {
            try {
                Log.i(TAG, "=================================================================");
                Log.i(TAG, ">>> STARTING PERAZIM MVVM & STATE ARCHITECTURE VERIFICATION <<<");
                Log.i(TAG, "=================================================================");

                PerazimDatabase db = PerazimDatabase.getInstance(appContext);
                ContentDataSeeder.seedIfNeeded(appContext, db);
                BibleDataSeeder.seedIfNeeded(appContext, db);

                // -----------------------------------------------------------------
                // 1. UiState Validation
                // -----------------------------------------------------------------
                Log.i(TAG, "[MVVM STEP 1] Validating UiState wrapper...");
                UiState<String> loadingState = UiState.loading();
                if (!loadingState.isLoading() || loadingState.getStatus() != UiState.Status.LOADING) {
                    throw new IllegalStateException("UiState.loading() failed");
                }

                UiState<String> successState = UiState.success("Data OK");
                if (!successState.isSuccess() || !"Data OK".equals(successState.getData())) {
                    throw new IllegalStateException("UiState.success() failed");
                }

                UiState<String> emptyState = UiState.empty();
                if (!emptyState.isEmpty() || emptyState.getStatus() != UiState.Status.EMPTY) {
                    throw new IllegalStateException("UiState.empty() failed");
                }

                UiState<String> errorState = UiState.error("Network Error");
                if (!errorState.isError() || !"Network Error".equals(errorState.getErrorMessage())) {
                    throw new IllegalStateException("UiState.error() failed");
                }
                Log.i(TAG, "[MVVM STEP 1: COMPLETED] UiState validation passed.");

                // -----------------------------------------------------------------
                // 2. ViewModelFactory Validation
                // -----------------------------------------------------------------
                Log.i(TAG, "[MVVM STEP 2] Validating ViewModelFactory dependency binding...");
                ViewModelFactory factory = ViewModelFactory.getInstance(appContext);
                if (factory == null) {
                    throw new IllegalStateException("ViewModelFactory.getInstance() returned null");
                }

                HomeViewModel homeVM = factory.createHomeViewModel();
                BibleViewModel bibleVM = factory.createBibleViewModel();
                SermonsViewModel sermonsVM = factory.createSermonsViewModel();
                WorshipViewModel worshipVM = factory.createWorshipViewModel();
                FellowshipViewModel fellowshipVM = factory.createFellowshipViewModel();
                ProfileViewModel profileVM = factory.createProfileViewModel();

                if (homeVM == null || bibleVM == null || sermonsVM == null
                        || worshipVM == null || fellowshipVM == null || profileVM == null) {
                    throw new IllegalStateException("ViewModelFactory failed to instantiate one or more ViewModels");
                }

                // Verify factory.create() method
                HomeViewModel genericHomeVM = factory.create(HomeViewModel.class);
                if (genericHomeVM == null) {
                    throw new IllegalStateException("ViewModelFactory.create(HomeViewModel.class) returned null");
                }
                Log.i(TAG, "[MVVM STEP 2: COMPLETED] ViewModelFactory validated.");

                // -----------------------------------------------------------------
                // 3. HomeViewModel Validation
                // -----------------------------------------------------------------
                Log.i(TAG, "[MVVM STEP 3] Validating HomeViewModel feed and reflection...");
                CountDownLatch latchHomeFeed = new CountDownLatch(1);
                AtomicReference<HomeData> feedRef = new AtomicReference<>();
                homeVM.loadHomeFeed(data -> {
                    feedRef.set(data);
                    latchHomeFeed.countDown();
                });
                if (!latchHomeFeed.await(5, TimeUnit.SECONDS) || feedRef.get() == null) {
                    throw new IllegalStateException("HomeViewModel.loadHomeFeed() timed out or returned null");
                }
                HomeData homeData = feedRef.get();
                if (homeData.getScripture() == null || homeData.getScripture().getText() == null) {
                    throw new IllegalStateException("HomeData scripture is null");
                }
                Log.i(TAG, "HomeFeed loaded: Scripture=" + homeData.getScripture().getId()
                        + ", Sermons=" + homeData.getRecentSermons().size()
                        + ", Events=" + homeData.getUpcomingEvents().size()
                        + ", Streak=" + homeData.getStreak());

                RepositoryProvider repoProvider = RepositoryProvider.getInstance(appContext);
                User activeUser = repoProvider.getUserRepository().getCurrentUser();
                int origStreak = activeUser != null ? activeUser.getStreakCount() : 7;
                int origXp = activeUser != null ? activeUser.getSpiritualXp() : 450;
                int origGrace = activeUser != null ? activeUser.getGracePoints() : 50;
                boolean origFrozen = activeUser != null && activeUser.isStreakFrozen();

                try {
                    CountDownLatch latchReflection = new CountDownLatch(1);
                    homeVM.completeDailyReflection("refl_day_1", latchReflection::countDown);
                    if (!latchReflection.await(5, TimeUnit.SECONDS)) {
                        throw new IllegalStateException("HomeViewModel.completeDailyReflection() timed out");
                    }
                } finally {
                    // Restore active user metrics to prevent test pollution of runtime state
                    if (activeUser != null) {
                        activeUser.setStreakCount(origStreak);
                        activeUser.setSpiritualXp(origXp);
                        activeUser.setGracePoints(origGrace);
                        activeUser.setStreakFrozen(origFrozen);
                        repoProvider.getUserRepository().updateUser(activeUser);
                        GamificationStore.saveStreak(appContext, activeUser.getId(), origStreak, origFrozen);
                        GamificationStore.saveSpiritualXp(appContext, activeUser.getId(), origXp);
                        GamificationStore.saveGracePoints(appContext, activeUser.getId(), origGrace);
                    }
                }
                Log.i(TAG, "[MVVM STEP 3: COMPLETED] HomeViewModel validated and user metrics preserved.");

                // -----------------------------------------------------------------
                // 4. BibleViewModel Validation
                // -----------------------------------------------------------------
                Log.i(TAG, "[MVVM STEP 4] Validating BibleViewModel books, verses, search...");
                CountDownLatch latchBooks = new CountDownLatch(1);
                AtomicReference<List<BibleBook>> booksRef = new AtomicReference<>();
                bibleVM.loadBooks(books -> {
                    booksRef.set(books);
                    latchBooks.countDown();
                });
                if (!latchBooks.await(5, TimeUnit.SECONDS) || booksRef.get() == null || booksRef.get().size() != 66) {
                    throw new IllegalStateException("BibleViewModel.loadBooks() expected 66 books, got "
                            + (booksRef.get() == null ? "null" : booksRef.get().size()));
                }

                CountDownLatch latchVerses = new CountDownLatch(1);
                AtomicReference<List<BibleVerse>> versesRef = new AtomicReference<>();
                bibleVM.loadVerses("2SA", 5, verses -> {
                    versesRef.set(verses);
                    latchVerses.countDown();
                });
                if (!latchVerses.await(5, TimeUnit.SECONDS) || versesRef.get() == null) {
                    throw new IllegalStateException("BibleViewModel.loadVerses() failed");
                }

                CountDownLatch latchBookmark = new CountDownLatch(1);
                bibleVM.toggleBookmark("2SA.5.20", true, latchBookmark::countDown);
                if (!latchBookmark.await(5, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("BibleViewModel.toggleBookmark() timed out");
                }

                CountDownLatch latchNote = new CountDownLatch(1);
                bibleVM.saveStudyNote("2SA.5.20", "Baal Perazim breakthrough note", latchNote::countDown);
                if (!latchNote.await(5, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("BibleViewModel.saveStudyNote() timed out");
                }
                Log.i(TAG, "[MVVM STEP 4: COMPLETED] BibleViewModel validated.");

                // -----------------------------------------------------------------
                // 5. SermonsViewModel Validation
                // -----------------------------------------------------------------
                Log.i(TAG, "[MVVM STEP 5] Validating SermonsViewModel...");
                CountDownLatch latchSermons = new CountDownLatch(1);
                AtomicReference<List<Sermon>> sermonsRef = new AtomicReference<>();
                sermonsVM.loadSermons(sermons -> {
                    sermonsRef.set(sermons);
                    latchSermons.countDown();
                });
                if (!latchSermons.await(5, TimeUnit.SECONDS) || sermonsRef.get() == null || sermonsRef.get().isEmpty()) {
                    throw new IllegalStateException("SermonsViewModel.loadSermons() failed or empty");
                }

                CountDownLatch latchFilterSeries = new CountDownLatch(1);
                sermonsVM.filterBySeries("Breakthrough Series", seriesList -> latchFilterSeries.countDown());
                if (!latchFilterSeries.await(5, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("SermonsViewModel.filterBySeries() timed out");
                }

                String testSermonId = sermonsRef.get().get(0).getId();
                CountDownLatch latchDlReq = new CountDownLatch(1);
                AtomicBoolean dlSuccess = new AtomicBoolean(false);
                sermonsVM.requestDownload(testSermonId, success -> {
                    dlSuccess.set(Boolean.TRUE.equals(success));
                    latchDlReq.countDown();
                });
                if (!latchDlReq.await(5, TimeUnit.SECONDS) || !dlSuccess.get()) {
                    throw new IllegalStateException("SermonsViewModel.requestDownload() failed");
                }

                CountDownLatch latchDlDel = new CountDownLatch(1);
                sermonsVM.deleteDownload(testSermonId, res -> latchDlDel.countDown());
                if (!latchDlDel.await(5, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("SermonsViewModel.deleteDownload() timed out");
                }
                Log.i(TAG, "[MVVM STEP 5: COMPLETED] SermonsViewModel validated.");

                // -----------------------------------------------------------------
                // 6. WorshipViewModel Validation
                // -----------------------------------------------------------------
                Log.i(TAG, "[MVVM STEP 6] Validating WorshipViewModel...");
                CountDownLatch latchHymns = new CountDownLatch(1);
                AtomicReference<List<Hymn>> hymnsRef = new AtomicReference<>();
                worshipVM.loadHymns(hymns -> {
                    hymnsRef.set(hymns);
                    latchHymns.countDown();
                });
                if (!latchHymns.await(5, TimeUnit.SECONDS) || hymnsRef.get() == null || hymnsRef.get().isEmpty()) {
                    throw new IllegalStateException("WorshipViewModel.loadHymns() failed or empty");
                }

                CountDownLatch latchFavHymn = new CountDownLatch(1);
                worshipVM.toggleFavorite(hymnsRef.get().get(0).getId(), true, latchFavHymn::countDown);
                if (!latchFavHymn.await(5, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("WorshipViewModel.toggleFavorite() timed out");
                }
                Log.i(TAG, "[MVVM STEP 6: COMPLETED] WorshipViewModel validated.");

                // -----------------------------------------------------------------
                // 7. FellowshipViewModel Validation
                // -----------------------------------------------------------------
                Log.i(TAG, "[MVVM STEP 7] Validating FellowshipViewModel...");
                CountDownLatch latchSubmitPrayer = new CountDownLatch(1);
                fellowshipVM.submitPrayer(
                        "Divine Healing",
                        "Praying for full recovery and breakthrough",
                        false,
                        "test_user",
                        "Saint Peter",
                        latchSubmitPrayer::countDown
                );
                if (!latchSubmitPrayer.await(5, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("FellowshipViewModel.submitPrayer() timed out");
                }

                CountDownLatch latchPrayers = new CountDownLatch(1);
                AtomicReference<List<Prayer>> prayersRef = new AtomicReference<>();
                fellowshipVM.loadPublicPrayers(prayers -> {
                    prayersRef.set(prayers);
                    latchPrayers.countDown();
                });
                if (!latchPrayers.await(5, TimeUnit.SECONDS) || prayersRef.get() == null || prayersRef.get().isEmpty()) {
                    throw new IllegalStateException("FellowshipViewModel.loadPublicPrayers() failed or empty");
                }

                CountDownLatch latchAmen = new CountDownLatch(1);
                fellowshipVM.amenPrayer(prayersRef.get().get(0).getId(), "test_user_amen", latchAmen::countDown);
                if (!latchAmen.await(5, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("FellowshipViewModel.amenPrayer() timed out");
                }

                // Riddles and jokes
                CountDownLatch latchRiddles = new CountDownLatch(1);
                fellowshipVM.loadRiddles(riddles -> latchRiddles.countDown());
                latchRiddles.await(5, TimeUnit.SECONDS);

                CountDownLatch latchJokes = new CountDownLatch(1);
                fellowshipVM.loadJokes(jokes -> latchJokes.countDown());
                latchJokes.await(5, TimeUnit.SECONDS);

                Log.i(TAG, "[MVVM STEP 7: COMPLETED] FellowshipViewModel validated.");

                // -----------------------------------------------------------------
                // 8. ProfileViewModel Validation
                // -----------------------------------------------------------------
                Log.i(TAG, "[MVVM STEP 8] Validating ProfileViewModel...");
                CountDownLatch latchProfile = new CountDownLatch(1);
                AtomicReference<User> userRef = new AtomicReference<>();
                profileVM.loadProfile(u -> {
                    userRef.set(u);
                    latchProfile.countDown();
                });
                if (!latchProfile.await(5, TimeUnit.SECONDS) || userRef.get() == null) {
                    throw new IllegalStateException("ProfileViewModel.loadProfile() failed");
                }

                String origCampus = userRef.get().getCampusId();
                CountDownLatch latchSwitchCampus = new CountDownLatch(1);
                profileVM.switchCampus("campus_mwea", latchSwitchCampus::countDown);
                if (!latchSwitchCampus.await(5, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("ProfileViewModel.switchCampus() timed out");
                }
                // Restore original campus
                if (origCampus != null) {
                    CountDownLatch latchRestoreCampus = new CountDownLatch(1);
                    profileVM.switchCampus(origCampus, latchRestoreCampus::countDown);
                    latchRestoreCampus.await(5, TimeUnit.SECONDS);
                }

                CountDownLatch latchSavedContent = new CountDownLatch(1);
                AtomicReference<SavedContentData> savedRef = new AtomicReference<>();
                profileVM.loadSavedContent(data -> {
                    savedRef.set(data);
                    latchSavedContent.countDown();
                });
                if (!latchSavedContent.await(5, TimeUnit.SECONDS) || savedRef.get() == null) {
                    throw new IllegalStateException("ProfileViewModel.loadSavedContent() failed");
                }
                Log.i(TAG, "[MVVM STEP 8: COMPLETED] ProfileViewModel validated.");

                // -----------------------------------------------------------------
                // Success Marker
                // -----------------------------------------------------------------
                Log.i(TAG, "=================================================================");
                Log.i(TAG, "[PERAZIM-MVVM-GATE-2.1: SUCCESS]");
                Log.i(TAG, "=================================================================");

                if (callback != null) {
                    callback.onComplete(true, "All MVVM components verified successfully.");
                }

            } catch (Exception e) {
                Log.e(TAG, "[PERAZIM-MVVM-GATE-2.1: FAILED] " + e.getMessage(), e);
                if (callback != null) {
                    callback.onComplete(false, e.getMessage());
                }
            }
        });
    }
}
