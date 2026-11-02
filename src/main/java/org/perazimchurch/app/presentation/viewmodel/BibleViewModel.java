package org.perazimchurch.app.presentation.viewmodel;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import org.perazimchurch.app.domain.model.BibleBook;
import org.perazimchurch.app.domain.model.BibleVerse;
import org.perazimchurch.app.domain.repository.BibleRepository;
import org.perazimchurch.app.presentation.state.UiState;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/**
 * ViewModel managing Holy Scripture books, chapter verses, keyword searches,
 * verse bookmarking, and personal study notes.
 */
public class BibleViewModel extends ViewModel {

    private static final String TAG = "BibleViewModel";

    private final BibleRepository bibleRepo;
    private final Executor executor;

    private final MutableLiveData<UiState<List<BibleBook>>> booksState = new MutableLiveData<>();
    private final MutableLiveData<UiState<List<BibleVerse>>> versesState = new MutableLiveData<>();

    public BibleViewModel(@NonNull BibleRepository bibleRepo) {
        this(bibleRepo, Executors.newSingleThreadExecutor());
    }

    public BibleViewModel(@NonNull BibleRepository bibleRepo, @NonNull Executor executor) {
        this.bibleRepo = bibleRepo;
        this.executor = executor;
    }

    public LiveData<UiState<List<BibleBook>>> getBooksState() {
        return booksState;
    }

    public LiveData<UiState<List<BibleVerse>>> getVersesState() {
        return versesState;
    }

    /**
     * Returns all 66 canonical Bible books.
     */
    public void loadBooks(@Nullable Consumer<List<BibleBook>> callback) {
        booksState.postValue(UiState.loading());
        executor.execute(() -> {
            try {
                List<BibleBook> books = bibleRepo.getBooks();
                if (books == null || books.isEmpty()) {
                    booksState.postValue(UiState.empty());
                } else {
                    booksState.postValue(UiState.success(books));
                }
                if (callback != null) {
                    final List<BibleBook> res = books != null ? books : Collections.emptyList();
                    postCallback(() -> callback.accept(res));
                }
            } catch (Exception e) {
                Log.e(TAG, "Error loading books", e);
                booksState.postValue(UiState.error(e.getMessage() != null ? e.getMessage() : "Failed to load books"));
                if (callback != null) {
                    postCallback(() -> callback.accept(Collections.emptyList()));
                }
            }
        });
    }

    /**
     * Loads chapter verses for a specific book and chapter number with default KJV translation.
     */
    public void loadVerses(@NonNull String bookId, int chapter, @Nullable Consumer<List<BibleVerse>> callback) {
        loadVerses("KJV", bookId, chapter, callback);
    }

    /**
     * Loads chapter verses for a specific translation, book, and chapter number.
     */
    public void loadVerses(@NonNull String translationId, @NonNull String bookId, int chapter, @Nullable Consumer<List<BibleVerse>> callback) {
        versesState.postValue(UiState.loading());
        executor.execute(() -> {
            try {
                String trans = (translationId != null && !translationId.trim().isEmpty()) ? translationId.trim() : "KJV";
                List<BibleVerse> verses = bibleRepo.getVersesForChapter(trans, bookId, chapter);
                if (verses == null || verses.isEmpty()) {
                    versesState.postValue(UiState.empty());
                } else {
                    versesState.postValue(UiState.success(verses));
                }
                if (callback != null) {
                    final List<BibleVerse> res = verses != null ? verses : Collections.emptyList();
                    postCallback(() -> callback.accept(res));
                }
            } catch (Exception e) {
                Log.e(TAG, "Error loading verses for trans: " + translationId + " book: " + bookId + " ch: " + chapter, e);
                versesState.postValue(UiState.error(e.getMessage() != null ? e.getMessage() : "Failed to load verses"));
                if (callback != null) {
                    postCallback(() -> callback.accept(Collections.emptyList()));
                }
            }
        });
    }

    /**
     * Searches Holy Scripture across canonical verses for matching keywords using default KJV.
     */
    public void searchScripture(@NonNull String query, @Nullable Consumer<List<BibleVerse>> callback) {
        searchScripture("KJV", query, callback);
    }

    /**
     * Searches Holy Scripture for matching keywords in a specific translation.
     */
    public void searchScripture(@NonNull String translationId, @NonNull String query, @Nullable Consumer<List<BibleVerse>> callback) {
        executor.execute(() -> {
            try {
                String trans = (translationId != null && !translationId.trim().isEmpty()) ? translationId.trim() : "KJV";
                List<BibleVerse> results = bibleRepo.searchScripture(trans, query);
                if (callback != null) {
                    final List<BibleVerse> res = results != null ? results : Collections.emptyList();
                    postCallback(() -> callback.accept(res));
                }
            } catch (Exception e) {
                Log.e(TAG, "Error searching scripture for trans: " + translationId + " query: " + query, e);
                if (callback != null) {
                    postCallback(() -> callback.accept(Collections.emptyList()));
                }
            }
        });
    }

    /**
     * Toggles bookmark favorite state for a verse.
     */
    public void toggleBookmark(@NonNull String verseId, boolean isBookmarked, @Nullable Runnable onDone) {
        executor.execute(() -> {
            try {
                bibleRepo.bookmarkVerse(verseId, isBookmarked);
            } catch (Exception e) {
                Log.e(TAG, "Error toggling bookmark for verse: " + verseId, e);
            } finally {
                if (onDone != null) {
                    postCallback(onDone);
                }
            }
        });
    }

    /**
     * Saves study note for a verse.
     */
    public void saveStudyNote(@NonNull String verseId, @NonNull String note, @Nullable Runnable onDone) {
        executor.execute(() -> {
            try {
                bibleRepo.saveVerseNote(verseId, note);
            } catch (Exception e) {
                Log.e(TAG, "Error saving study note for verse: " + verseId, e);
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
