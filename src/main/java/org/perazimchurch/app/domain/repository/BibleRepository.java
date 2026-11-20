package org.perazimchurch.app.domain.repository;

import org.perazimchurch.app.domain.model.BibleBook;
import org.perazimchurch.app.domain.model.BibleChapter;
import org.perazimchurch.app.domain.model.BibleTranslation;
import org.perazimchurch.app.domain.model.BibleNote;
import org.perazimchurch.app.domain.model.BibleVerse;
import org.perazimchurch.app.domain.model.InterlinearWord;
import org.perazimchurch.app.domain.model.StrongsEntry;
import org.perazimchurch.app.domain.model.VerseLocation;
import java.util.List;
import java.util.Map;

/**
 * Repository interface defining operations for Scripture reading, translations, and verse bookmarks.
 */
public interface BibleRepository {
    List<BibleTranslation> getAvailableTranslations();
    List<BibleBook> getBooks();
    List<BibleVerse> getVersesForChapter(String translationId, String bookId, int chapterNumber);
    BibleVerse getVerse(String translationId, String bookId, int chapterNumber, int verseNumber);
    List<BibleVerse> searchScripture(String translationId, String query);
    List<BibleVerse> getBookmarkedVerses();
    void bookmarkVerse(String verseId, boolean isFavorite);
    void saveVerseNote(String verseId, String note);

    /** Translations whose text is bundled and readable offline. */
    List<BibleTranslation> getTranslationsWithText();

    /** Highlight colour names by verse number for one chapter. */
    Map<Integer, String> getHighlightsForChapter(String bookId, int chapterNumber);

    /** Adds a word or character-range highlight to a verse body; offsets are [start, end). */
    void addWordHighlight(String translationId, String bookId, int chapterNumber, int verseNumber,
                          int start, int end, String colour);

    /** Removes every word range in the verse that shares a character with [start, end). Other ranges stay. */
    void clearWordHighlights(String translationId, String bookId, int chapterNumber, int verseNumber,
                             int start, int end);

    /** Sets a verse highlight colour, or clears it when {@code colour} is null. */
    void setHighlight(String bookId, int chapterNumber, int verseNumber, String colour);

    /** Bookmarked verses and highlighted verses together, in reading order, with the default translation text. */
    List<BibleVerse> getSavedVerses();

    /** The same verse in every translation that has text, in picker order. */
    List<BibleVerse> getVerseInAllTranslations(String bookId, int chapterNumber, int verseNumber);

    /** Interlinear words of a verse (Greek for New Testament, Hebrew for Old Testament), or an empty list. */
    List<InterlinearWord> getInterlinear(String bookId, int chapterNumber, int verseNumber);

    /** Offline Strong's dictionary entry for a code such as G3056 or H430, or null. */
    StrongsEntry getStrongsEntry(String code);

    /** New Testament verses where a Greek word carries this Strong's number (offline concordance). */
    List<VerseLocation> getGreekConcordance(int strongsNumber);

    /** Topic labels that have at least one saved verse, alphabetical. */
    List<String> getTopics();

    /** Topic labels attached to one verse. */
    List<String> getTopicsForVerse(String bookId, int chapterNumber, int verseNumber);

    /** Adds or removes a verse from a topic. */
    void setVerseTopic(String bookId, int chapterNumber, int verseNumber, String topic, boolean member);

    /** Every verse saved under a topic, in reading order, with default-translation text. */
    List<BibleVerse> getTopicVerses(String topic);

    /** All study notes with their linked verses, most recently changed first. */
    List<BibleNote> getNotes();

    /** Saves a note (id 0 creates one) and replaces its verse links. Returns the note id. */
    long saveNote(long id, String body, List<VerseLocation> links, List<String> topics);

    /** Deletes a note and its links. */
    void deleteNote(long id);
}
