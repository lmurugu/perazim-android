package com.example.app.presentation.viewmodel;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.app.data.local.entity.DownloadedContentEntity;
import com.example.app.domain.model.Sermon;
import com.example.app.domain.repository.SermonRepository;
import com.example.app.media.download.DownloadManager;
import com.example.app.presentation.state.UiState;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/**
 * ViewModel managing sermon broadcasts, searching, series filtering,
 * and offline media downloads via {@link DownloadManager}.
 */
public class SermonsViewModel extends ViewModel {

    private static final String TAG = "SermonsViewModel";

    private final SermonRepository sermonRepo;
    private final DownloadManager downloadManager;
    private final Executor executor;

    private final MutableLiveData<UiState<List<Sermon>>> sermonsState = new MutableLiveData<>();

    public SermonsViewModel(@NonNull SermonRepository sermonRepo, @NonNull DownloadManager downloadManager) {
        this(sermonRepo, downloadManager, Executors.newSingleThreadExecutor());
    }

    public SermonsViewModel(@NonNull SermonRepository sermonRepo,
                            @NonNull DownloadManager downloadManager,
                            @NonNull Executor executor) {
        this.sermonRepo = sermonRepo;
        this.downloadManager = downloadManager;
        this.executor = executor;
    }

    public LiveData<UiState<List<Sermon>>> getSermonsState() {
        return sermonsState;
    }

    /**
     * Returns recent sermons.
     */
    public void loadSermons(@Nullable Consumer<List<Sermon>> callback) {
        sermonsState.postValue(UiState.loading());
        executor.execute(() -> {
            try {
                List<Sermon> sermons = sermonRepo.getRecentSermons();
                if (sermons == null || sermons.isEmpty()) {
                    sermonsState.postValue(UiState.empty());
                } else {
                    sermonsState.postValue(UiState.success(sermons));
                }
                if (callback != null) {
                    final List<Sermon> res = sermons != null ? sermons : Collections.emptyList();
                    postCallback(() -> callback.accept(res));
                }
            } catch (Exception e) {
                Log.e(TAG, "Error loading sermons", e);
                sermonsState.postValue(UiState.error(e.getMessage() != null ? e.getMessage() : "Failed to load sermons"));
                if (callback != null) {
                    postCallback(() -> callback.accept(Collections.emptyList()));
                }
            }
        });
    }

    /**
     * Searches sermons by title, preacher, or scripture reference keywords.
     */
    public void searchSermons(@NonNull String query, @Nullable Consumer<List<Sermon>> callback) {
        executor.execute(() -> {
            try {
                List<Sermon> results = sermonRepo.searchSermons(query);
                if (callback != null) {
                    final List<Sermon> res = results != null ? results : Collections.emptyList();
                    postCallback(() -> callback.accept(res));
                }
            } catch (Exception e) {
                Log.e(TAG, "Error searching sermons for query: " + query, e);
                if (callback != null) {
                    postCallback(() -> callback.accept(Collections.emptyList()));
                }
            }
        });
    }

    /**
     * Filters recent sermons matching the specified sermon series title.
     */
    public void filterBySeries(@Nullable String series, @Nullable Consumer<List<Sermon>> callback) {
        executor.execute(() -> {
            try {
                List<Sermon> all = sermonRepo.getRecentSermons();
                if (all == null) {
                    all = Collections.emptyList();
                }
                List<Sermon> filtered;
                if (series == null || series.trim().isEmpty()) {
                    filtered = all;
                } else {
                    filtered = new ArrayList<>();
                    for (Sermon s : all) {
                        if (s.getSeries() != null && s.getSeries().equalsIgnoreCase(series.trim())) {
                            filtered.add(s);
                        }
                    }
                }
                if (callback != null) {
                    final List<Sermon> res = filtered;
                    postCallback(() -> callback.accept(res));
                }
            } catch (Exception e) {
                Log.e(TAG, "Error filtering sermons by series: " + series, e);
                if (callback != null) {
                    postCallback(() -> callback.accept(Collections.emptyList()));
                }
            }
        });
    }

    /**
     * Enqueues an offline media download for the sermon asset.
     */
    public void requestDownload(@NonNull String sermonId, @Nullable Consumer<Boolean> callback) {
        executor.execute(() -> {
            boolean success = false;
            try {
                DownloadedContentEntity entity = downloadManager.enqueueDownload("SERMON", sermonId);
                success = (entity != null);
            } catch (Exception e) {
                Log.e(TAG, "Error requesting download for sermon: " + sermonId, e);
            } finally {
                if (callback != null) {
                    final boolean finalSuccess = success;
                    postCallback(() -> callback.accept(finalSuccess));
                }
            }
        });
    }

    /**
     * Deletes local physical payload and download records for the specified sermon.
     */
    public void deleteDownload(@NonNull String sermonId, @Nullable Consumer<Boolean> callback) {
        executor.execute(() -> {
            boolean success = false;
            try {
                success = downloadManager.deleteDownload(sermonId);
            } catch (Exception e) {
                Log.e(TAG, "Error deleting download for sermon: " + sermonId, e);
            } finally {
                if (callback != null) {
                    final boolean finalSuccess = success;
                    postCallback(() -> callback.accept(finalSuccess));
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
