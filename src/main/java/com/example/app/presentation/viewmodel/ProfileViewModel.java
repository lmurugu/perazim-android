package com.example.app.presentation.viewmodel;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.app.data.local.session.SessionManager;
import com.example.app.domain.model.BibleVerse;
import com.example.app.domain.model.DownloadedContent;
import com.example.app.domain.model.Hymn;
import com.example.app.domain.model.User;
import com.example.app.domain.repository.BibleRepository;
import com.example.app.domain.repository.DownloadedContentRepository;
import com.example.app.domain.repository.HymnRepository;
import com.example.app.domain.repository.UserRepository;
import com.example.app.presentation.state.UiState;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/**
 * ViewModel managing user profile details, active session state via {@link SessionManager},
 * campus switching, and offline saved media aggregation (downloads, bookmarks, favorite hymns).
 */
public class ProfileViewModel extends ViewModel {

    private static final String TAG = "ProfileViewModel";

    private final UserRepository userRepo;
    private final DownloadedContentRepository downloadedContentRepo;
    private final BibleRepository bibleRepo;
    private final HymnRepository hymnRepo;
    private final SessionManager sessionManager;
    private final Executor executor;

    private final MutableLiveData<UiState<User>> profileState = new MutableLiveData<>();
    private final MutableLiveData<UiState<SavedContentData>> savedContentState = new MutableLiveData<>();

    public ProfileViewModel(@NonNull UserRepository userRepo,
                            @NonNull DownloadedContentRepository downloadedContentRepo,
                            @NonNull BibleRepository bibleRepo,
                            @NonNull HymnRepository hymnRepo,
                            @NonNull SessionManager sessionManager) {
        this(userRepo, downloadedContentRepo, bibleRepo, hymnRepo, sessionManager, Executors.newSingleThreadExecutor());
    }

    public ProfileViewModel(@NonNull UserRepository userRepo,
                            @NonNull DownloadedContentRepository downloadedContentRepo,
                            @NonNull BibleRepository bibleRepo,
                            @NonNull HymnRepository hymnRepo,
                            @NonNull SessionManager sessionManager,
                            @NonNull Executor executor) {
        this.userRepo = userRepo;
        this.downloadedContentRepo = downloadedContentRepo;
        this.bibleRepo = bibleRepo;
        this.hymnRepo = hymnRepo;
        this.sessionManager = sessionManager;
        this.executor = executor;
    }

    public LiveData<UiState<User>> getProfileState() {
        return profileState;
    }

    public LiveData<UiState<SavedContentData>> getSavedContentState() {
        return savedContentState;
    }

    /**
     * Loads current active user profile.
     */
    public void loadProfile(@Nullable Consumer<User> callback) {
        profileState.postValue(UiState.loading());
        executor.execute(() -> {
            try {
                User user = userRepo.getCurrentUser();
                if (user == null) {
                    profileState.postValue(UiState.empty());
                } else {
                    profileState.postValue(UiState.success(user));
                }
                if (callback != null) {
                    postCallback(() -> callback.accept(user));
                }
            } catch (Exception e) {
                Log.e(TAG, "Error loading profile", e);
                profileState.postValue(UiState.error(e.getMessage() != null ? e.getMessage() : "Failed to load profile"));
                if (callback != null) {
                    postCallback(() -> callback.accept(null));
                }
            }
        });
    }

    /**
     * Switches church campus affiliation for the active user.
     */
    public void switchCampus(@NonNull String campusId, @Nullable Runnable onDone) {
        executor.execute(() -> {
            try {
                userRepo.switchCampus(campusId);
            } catch (Exception e) {
                Log.e(TAG, "Error switching campus: " + campusId, e);
            } finally {
                if (onDone != null) {
                    postCallback(onDone);
                }
            }
        });
    }

    /**
     * Updates user streak freeze status.
     */
    public void setStreakFreeze(boolean frozen, @Nullable Runnable onDone) {
        executor.execute(() -> {
            try {
                User user = userRepo.getCurrentUser();
                if (user != null) {
                    userRepo.updateStreak(user.getStreakCount(), frozen);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error updating streak freeze", e);
            } finally {
                if (onDone != null) {
                    postCallback(onDone);
                }
            }
        });
    }

    /**
     * Aggregates downloaded sermons, favorite hymns, and bookmarked verses into {@link SavedContentData}.
     */
    public void loadSavedContent(@Nullable Consumer<SavedContentData> callback) {
        savedContentState.postValue(UiState.loading());
        executor.execute(() -> {
            try {
                List<DownloadedContent> downloads = downloadedContentRepo.getAllDownloads();
                List<Hymn> favoriteHymns = hymnRepo.getFavoriteHymns();
                List<BibleVerse> bookmarks = bibleRepo.getBookmarkedVerses();

                SavedContentData data = new SavedContentData(downloads, favoriteHymns, bookmarks);
                savedContentState.postValue(UiState.success(data));

                if (callback != null) {
                    postCallback(() -> callback.accept(data));
                }
            } catch (Exception e) {
                Log.e(TAG, "Error loading saved content", e);
                savedContentState.postValue(UiState.error(e.getMessage() != null ? e.getMessage() : "Failed to load saved content"));
                if (callback != null) {
                    SavedContentData empty = new SavedContentData(
                            Collections.emptyList(),
                            Collections.emptyList(),
                            Collections.emptyList()
                    );
                    postCallback(() -> callback.accept(empty));
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
