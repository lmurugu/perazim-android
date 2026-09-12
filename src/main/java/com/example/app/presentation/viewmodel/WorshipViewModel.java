package com.example.app.presentation.viewmodel;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.app.domain.model.Hymn;
import com.example.app.domain.repository.HymnRepository;
import com.example.app.presentation.state.UiState;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/**
 * ViewModel managing worship hymns, categories, lyrics search, and hymn favorites.
 */
public class WorshipViewModel extends ViewModel {

    private static final String TAG = "WorshipViewModel";

    private final HymnRepository hymnRepo;
    private final Executor executor;

    private final MutableLiveData<UiState<List<Hymn>>> hymnsState = new MutableLiveData<>();

    public WorshipViewModel(@NonNull HymnRepository hymnRepo) {
        this(hymnRepo, Executors.newSingleThreadExecutor());
    }

    public WorshipViewModel(@NonNull HymnRepository hymnRepo, @NonNull Executor executor) {
        this.hymnRepo = hymnRepo;
        this.executor = executor;
    }

    public LiveData<UiState<List<Hymn>>> getHymnsState() {
        return hymnsState;
    }

    /**
     * Returns all hymns in the hymnal.
     */
    public void loadHymns(@Nullable Consumer<List<Hymn>> callback) {
        hymnsState.postValue(UiState.loading());
        executor.execute(() -> {
            try {
                List<Hymn> hymns = hymnRepo.getAllHymns();
                if (hymns == null || hymns.isEmpty()) {
                    hymnsState.postValue(UiState.empty());
                } else {
                    hymnsState.postValue(UiState.success(hymns));
                }
                if (callback != null) {
                    final List<Hymn> res = hymns != null ? hymns : Collections.emptyList();
                    postCallback(() -> callback.accept(res));
                }
            } catch (Exception e) {
                Log.e(TAG, "Error loading hymns", e);
                hymnsState.postValue(UiState.error(e.getMessage() != null ? e.getMessage() : "Failed to load hymns"));
                if (callback != null) {
                    postCallback(() -> callback.accept(Collections.emptyList()));
                }
            }
        });
    }

    /**
     * Filters hymns by category.
     */
    public void filterByCategory(@Nullable String category, @Nullable Consumer<List<Hymn>> callback) {
        executor.execute(() -> {
            try {
                List<Hymn> all = hymnRepo.getAllHymns();
                if (all == null) {
                    all = Collections.emptyList();
                }
                List<Hymn> filtered;
                if (category == null || category.trim().isEmpty()) {
                    filtered = all;
                } else {
                    filtered = new ArrayList<>();
                    for (Hymn h : all) {
                        if (h.getCategory() != null && h.getCategory().equalsIgnoreCase(category.trim())) {
                            filtered.add(h);
                        }
                    }
                }
                if (callback != null) {
                    final List<Hymn> res = filtered;
                    postCallback(() -> callback.accept(res));
                }
            } catch (Exception e) {
                Log.e(TAG, "Error filtering hymns by category: " + category, e);
                if (callback != null) {
                    postCallback(() -> callback.accept(Collections.emptyList()));
                }
            }
        });
    }

    /**
     * Searches hymns by title, hymn number, or lyric keywords.
     */
    public void searchHymns(@NonNull String query, @Nullable Consumer<List<Hymn>> callback) {
        executor.execute(() -> {
            try {
                List<Hymn> results = hymnRepo.searchHymns(query);
                if (callback != null) {
                    final List<Hymn> res = results != null ? results : Collections.emptyList();
                    postCallback(() -> callback.accept(res));
                }
            } catch (Exception e) {
                Log.e(TAG, "Error searching hymns for query: " + query, e);
                if (callback != null) {
                    postCallback(() -> callback.accept(Collections.emptyList()));
                }
            }
        });
    }

    /**
     * Toggles favorite state for a hymn.
     */
    public void toggleFavorite(@NonNull String hymnId, boolean isFavorite, @Nullable Runnable onDone) {
        executor.execute(() -> {
            try {
                hymnRepo.toggleFavorite(hymnId, isFavorite);
            } catch (Exception e) {
                Log.e(TAG, "Error toggling hymn favorite: " + hymnId, e);
            } finally {
                if (onDone != null) {
                    postCallback(onDone);
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
