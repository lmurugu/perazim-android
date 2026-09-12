package com.example.app.domain.repository;

import com.example.app.domain.model.BibleBook;
import com.example.app.domain.model.BibleChapter;
import com.example.app.domain.model.BibleTranslation;
import com.example.app.domain.model.BibleVerse;
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
    void bookmarkVerse(String verseId, boolean isFavorite);
    void saveVerseNote(String verseId, String note);
}
