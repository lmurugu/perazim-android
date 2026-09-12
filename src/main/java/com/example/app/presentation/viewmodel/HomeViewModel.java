package com.example.app.presentation.viewmodel;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.app.domain.model.Announcement;
import com.example.app.domain.model.BibleVerse;
import com.example.app.domain.model.Event;
import com.example.app.domain.model.Reflection;
import com.example.app.domain.model.Sermon;
import com.example.app.domain.model.User;
import com.example.app.domain.repository.AnnouncementRepository;
import com.example.app.domain.repository.BibleRepository;
import com.example.app.domain.repository.EventRepository;
import com.example.app.domain.repository.ReflectionRepository;
import com.example.app.domain.repository.SermonRepository;
import com.example.app.domain.repository.UserRepository;
import com.example.app.presentation.state.UiState;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/**
 * ViewModel managing data feeds, devotional routine progression, and user metrics
 * for the Perazim Home screen.
 */
public class HomeViewModel extends ViewModel {

    private static final String TAG = "HomeViewModel";

    private final SermonRepository sermonRepo;
    private final BibleRepository bibleRepo;
    private final ReflectionRepository reflectionRepo;
    private final AnnouncementRepository announcementRepo;
    private final EventRepository eventRepo;
    private final UserRepository userRepo;
    private final Executor executor;

    private final MutableLiveData<UiState<HomeData>> feedState = new MutableLiveData<>();

    public HomeViewModel(@NonNull SermonRepository sermonRepo,
                         @NonNull BibleRepository bibleRepo,
                         @NonNull ReflectionRepository reflectionRepo,
                         @NonNull AnnouncementRepository announcementRepo,
                         @NonNull EventRepository eventRepo,
                         @NonNull UserRepository userRepo) {
        this(sermonRepo, bibleRepo, reflectionRepo, announcementRepo, eventRepo, userRepo, Executors.newSingleThreadExecutor());
    }

    public HomeViewModel(@NonNull SermonRepository sermonRepo,
                         @NonNull BibleRepository bibleRepo,
                         @NonNull ReflectionRepository reflectionRepo,
                         @NonNull AnnouncementRepository announcementRepo,
                         @NonNull EventRepository eventRepo,
                         @NonNull UserRepository userRepo,
                         @NonNull Executor executor) {
        this.sermonRepo = sermonRepo;
        this.bibleRepo = bibleRepo;
        this.reflectionRepo = reflectionRepo;
        this.announcementRepo = announcementRepo;
        this.eventRepo = eventRepo;
        this.userRepo = userRepo;
        this.executor = executor;
    }

    public LiveData<UiState<HomeData>> getFeedState() {
        return feedState;
    }

    public LiveData<UiState<HomeData>> getHomeFeedState() {
        return feedState;
    }

    /**
     * Loads the Home Feed data: 2 Samuel 5:20 scripture, today's reflection,
     * recent sermons, upcoming events, active announcements, and current user streak/XP.
     */
    public void loadHomeFeed(@Nullable Consumer<HomeData> callback) {
        feedState.postValue(UiState.loading());
        executor.execute(() -> {
            try {
                // 1. Scripture of the day (2 Samuel 5:20)
                BibleVerse scripture = bibleRepo.getVerse("KJV", "2SA", 5, 20);
                if (scripture == null) {
                    scripture = bibleRepo.getVerse(null, "2SA", 5, 20);
                }
                if (scripture == null) {
                    scripture = new BibleVerse(
                            "2SA.5.20",
                            "KJV",
                            "2SA",
                            5,
                            20,
                            "And David came to Baal-perazim, and David smote them there, and said, The LORD hath broken forth upon mine enemies before me, as the breach of waters.",
                            false,
                            null
                    );
                }

                // 2. Today's reflection devotional
                Reflection reflection = reflectionRepo.getTodayReflection();

                // 3. Recent sermons
                List<Sermon> recentSermons = sermonRepo.getRecentSermons();

                // 4. Upcoming events
                List<Event> upcomingEvents = eventRepo.getUpcomingEvents();

                // 5. Active announcements
                List<Announcement> activeAnnouncements = announcementRepo.getActiveAnnouncements();

                // 6. Current user profile and spiritual progress
                User currentUser = userRepo.getCurrentUser();

                HomeData homeData = new HomeData(
                        scripture,
                        reflection,
                        recentSermons,
                        upcomingEvents,
                        activeAnnouncements,
                        currentUser
                );

                feedState.postValue(UiState.success(homeData));

                if (callback != null) {
                    postCallback(() -> callback.accept(homeData));
                }
            } catch (Exception e) {
                Log.e(TAG, "Error loading home feed", e);
                feedState.postValue(UiState.error(e.getMessage() != null ? e.getMessage() : "Failed to load home feed"));
            }
        });
    }

    /**
     * Marks the daily reflection completed, awards 20 spiritual XP, and updates the user streak.
     */
    public void completeDailyReflection(@Nullable String reflectionId, @Nullable Runnable onComplete) {
        executor.execute(() -> {
            try {
                if (reflectionId != null) {
                    reflectionRepo.markReflectionCompleted(reflectionId);
                }
                userRepo.addSpiritualXp(20);
                User currentUser = userRepo.getCurrentUser();
                int currentStreak = (currentUser != null) ? currentUser.getStreakCount() : 0;
                boolean isFrozen = (currentUser != null) && currentUser.isStreakFrozen();
                userRepo.updateStreak(currentStreak + 1, isFrozen);
            } catch (Exception e) {
                Log.e(TAG, "Error completing daily reflection", e);
            } finally {
                if (onComplete != null) {
                    postCallback(onComplete);
                }
            }
        });
    }

    private void postCallback(Runnable runnable) {
        if (runnable == null) return;
        try {
            if (Looper.getMainLooper() != null && Looper.myLooper() != Looper.getMainLooper()) {
                new Handler(Looper.getMainLooper()).post(runnable);
                return;
            }
        } catch (Throwable ignored) {
        }
        runnable.run();
    }
}
