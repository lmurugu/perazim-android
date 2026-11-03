package org.perazimchurch.app.domain.repository;

import org.perazimchurch.app.domain.model.BibleBook;
import org.perazimchurch.app.domain.model.BibleChapter;
import org.perazimchurch.app.domain.model.BibleTranslation;
import org.perazimchurch.app.domain.model.BibleVerse;
import java.util.List;

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
}
