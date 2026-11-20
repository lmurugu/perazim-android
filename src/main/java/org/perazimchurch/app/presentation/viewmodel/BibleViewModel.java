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
import org.perazimchurch.app.domain.model.BibleTranslation;
import org.perazimchurch.app.domain.model.BibleNote;
import org.perazimchurch.app.domain.model.BibleVerse;
import org.perazimchurch.app.domain.model.VerseLocation;
import org.perazimchurch.app.domain.model.InterlinearWord;
import org.perazimchurch.app.domain.model.StrongsEntry;
import org.perazimchurch.app.domain.model.VerseLocation;
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
                Log.e(TAG, "Error loading Bible verses");
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
                Log.e(TAG, "Error searching scripture");
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
                Log.e(TAG, "Error toggling Bible bookmark");
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
                Log.e(TAG, "Error saving Bible study note");
            } finally {
                if (onDone != null) {
                    postCallback(onDone);
                }
            }
        });
    }

    /**
     * Loads the translations whose text is bundled and readable offline.
     */
    public void loadReadableTranslations(@Nullable Consumer<List<BibleTranslation>> callback) {
        executor.execute(() -> {
            List<BibleTranslation> result;
            try {
                result = bibleRepo.getTranslationsWithText();
            } catch (Exception e) {
                Log.e(TAG, "Error loading readable translations");
                result = Collections.emptyList();
            }
            if (callback != null) {
                final List<BibleTranslation> res = result != null ? result : Collections.emptyList();
                postCallback(() -> callback.accept(res));
            }
        });
    }

    /**
     * Sets or clears the highlight colour of a verse.
     */
    public void setHighlight(@NonNull String bookId, int chapter, int verse, @Nullable String colour, @Nullable Runnable onDone) {
        executor.execute(() -> {
            try {
                bibleRepo.setHighlight(bookId, chapter, verse, colour);
            } catch (Exception e) {
                Log.e(TAG, "Error saving Bible highlight");
            } finally {
                if (onDone != null) {
                    postCallback(onDone);
                }
            }
        });
    }

    /**
     * Loads the same verse in every readable translation, for side-by-side comparison.
     */
    public void loadVerseInAllTranslations(@NonNull String bookId, int chapter, int verse, @Nullable Consumer<List<BibleVerse>> callback) {
        executor.execute(() -> {
            List<BibleVerse> result;
            try {
                result = bibleRepo.getVerseInAllTranslations(bookId, chapter, verse);
            } catch (Exception e) {
                Log.e(TAG, "Error comparing Bible translations");
                result = Collections.emptyList();
            }
            if (callback != null) {
                final List<BibleVerse> res = result != null ? result : Collections.emptyList();
                postCallback(() -> callback.accept(res));
            }
        });
    }

    /** Loads the interlinear words for a verse: Greek for the New Testament, Hebrew for the Old Testament. */
    public void loadInterlinear(@NonNull String bookId, int chapter, int verse, @Nullable Consumer<List<InterlinearWord>> callback) {
        executor.execute(() -> {
            List<InterlinearWord> words;
            try {
                words = bibleRepo.getInterlinear(bookId, chapter, verse);
            } catch (Exception e) {
                Log.e(TAG, "Error loading interlinear");
                words = Collections.emptyList();
            }
            if (callback != null) {
                final List<InterlinearWord> res = words != null ? words : Collections.emptyList();
                postCallback(() -> callback.accept(res));
            }
        });
    }

    /** Loads a Strong's entry and its New Testament occurrences for the word tapped in the interlinear. */
    public void loadStrongsDetail(String code, int strongsNumber, @Nullable Consumer<StrongsDetail> callback) {
        executor.execute(() -> {
            StrongsEntry entry = null;
            List<VerseLocation> occurrences = Collections.emptyList();
            try {
                entry = bibleRepo.getStrongsEntry(code);
                // The concordance is built from the Greek New Testament only; Hebrew numbers have none.
                if (code == null || !code.toUpperCase(java.util.Locale.US).startsWith("H")) {
                    occurrences = bibleRepo.getGreekConcordance(strongsNumber);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error loading Strong's detail");
            }
            final StrongsDetail detail = new StrongsDetail(entry, occurrences != null ? occurrences : Collections.emptyList());
            if (callback != null) {
                postCallback(() -> callback.accept(detail));
            }
        });
    }

    /** A Strong's entry with its occurrences. The entry may be null when the dictionary lacks it. */
    public static final class StrongsDetail {
        public final StrongsEntry entry;
        public final List<VerseLocation> occurrences;

        StrongsDetail(StrongsEntry entry, List<VerseLocation> occurrences) {
            this.entry = entry;
            this.occurrences = occurrences;
        }
    }

    /** Loads topic labels, alphabetical. */
    public void loadTopics(@Nullable Consumer<List<String>> callback) {
        executor.execute(() -> {
            List<String> topics;
            try {
                topics = bibleRepo.getTopics();
            } catch (Exception e) {
                Log.e(TAG, "Error loading topics");
                topics = Collections.emptyList();
            }
            if (callback != null) {
                final List<String> res = topics != null ? topics : Collections.emptyList();
                postCallback(() -> callback.accept(res));
            }
        });
    }

    /** Loads the topic labels attached to one verse. */
    public void loadTopicsForVerse(@NonNull String bookId, int chapter, int verse, @Nullable Consumer<List<String>> callback) {
        executor.execute(() -> {
            List<String> topics;
            try {
                topics = bibleRepo.getTopicsForVerse(bookId, chapter, verse);
            } catch (Exception e) {
                Log.e(TAG, "Error loading verse topics");
                topics = Collections.emptyList();
            }
            if (callback != null) {
                final List<String> res = topics != null ? topics : Collections.emptyList();
                postCallback(() -> callback.accept(res));
            }
        });
    }

    /** Adds or removes a verse from a topic, then calls back. */
    public void setVerseTopic(@NonNull String bookId, int chapter, int verse, @NonNull String topic, boolean member, @Nullable Runnable onDone) {
        executor.execute(() -> {
            try {
                bibleRepo.setVerseTopic(bookId, chapter, verse, topic, member);
            } catch (Exception e) {
                Log.e(TAG, "Error saving verse topic");
            } finally {
                if (onDone != null) {
                    postCallback(onDone);
                }
            }
        });
    }

    /** Loads every verse saved under a topic. */
    public void loadTopicVerses(@NonNull String topic, @Nullable Consumer<List<BibleVerse>> callback) {
        executor.execute(() -> {
            List<BibleVerse> verses;
            try {
                verses = bibleRepo.getTopicVerses(topic);
            } catch (Exception e) {
                Log.e(TAG, "Error loading topic verses");
                verses = Collections.emptyList();
            }
            if (callback != null) {
                final List<BibleVerse> res = verses != null ? verses : Collections.emptyList();
                postCallback(() -> callback.accept(res));
            }
        });
    }

    /** Loads all study notes. */
    public void loadNotes(@Nullable Consumer<List<BibleNote>> callback) {
        executor.execute(() -> {
            List<BibleNote> notes;
            try {
                notes = bibleRepo.getNotes();
            } catch (Exception e) {
                Log.e(TAG, "Error loading notes");
                notes = Collections.emptyList();
            }
            if (callback != null) {
                final List<BibleNote> res = notes != null ? notes : Collections.emptyList();
                postCallback(() -> callback.accept(res));
            }
        });
    }

    /** Saves a note with its verse links, then calls back with the note id. */
    public void saveNote(long id, @NonNull String body, @NonNull List<VerseLocation> links, @NonNull List<String> topics, @Nullable Consumer<Long> callback) {
        executor.execute(() -> {
            long saved = 0L;
            try {
                saved = bibleRepo.saveNote(id, body, links, topics);
            } catch (Exception e) {
                Log.e(TAG, "Error saving note");
            }
            final long result = saved;
            if (callback != null) {
                postCallback(() -> callback.accept(result));
            }
        });
    }

    /** Deletes a note, then calls back. */
    public void deleteNote(long id, @Nullable Runnable onDone) {
        executor.execute(() -> {
            try {
                bibleRepo.deleteNote(id);
            } catch (Exception e) {
                Log.e(TAG, "Error deleting note");
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
