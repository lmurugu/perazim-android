package org.perazimchurch.app.domain.repository;

import org.perazimchurch.app.domain.model.BibleBook;
import org.perazimchurch.app.domain.model.BibleChapter;
import org.perazimchurch.app.domain.model.BibleTranslation;
import org.perazimchurch.app.domain.model.BibleVerse;
import org.perazimchurch.app.domain.model.InterlinearWord;
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

    /** Sets a verse highlight colour, or clears it when {@code colour} is null. */
    void setHighlight(String bookId, int chapterNumber, int verseNumber, String colour);

    /** Bookmarked verses and highlighted verses together, in reading order, with the default translation text. */
    List<BibleVerse> getSavedVerses();

    /** The same verse in every translation that has text, in picker order. */
    List<BibleVerse> getVerseInAllTranslations(String bookId, int chapterNumber, int verseNumber);

    /** Greek interlinear words of a New Testament verse, or an empty list when none is bundled. */
    List<InterlinearWord> getGreekInterlinear(String bookId, int chapterNumber, int verseNumber);
}
