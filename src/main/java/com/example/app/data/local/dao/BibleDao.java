package com.example.app.data.local.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.app.data.local.entity.BibleBookEntity;
import com.example.app.data.local.entity.BibleChapterEntity;
import com.example.app.data.local.entity.BibleTranslationEntity;
import com.example.app.data.local.entity.BibleVerseEntity;

import java.util.List;

@Dao
public interface BibleDao {

    // --- Translations ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertTranslation(BibleTranslationEntity translation);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertTranslations(List<BibleTranslationEntity> translations);

    @Query("SELECT * FROM bible_translations")
    List<BibleTranslationEntity> getTranslations();

    @Query("SELECT * FROM bible_translations WHERE id = :id LIMIT 1")
    BibleTranslationEntity getTranslationById(String id);

    @Query("SELECT * FROM bible_translations WHERE isDefault = 1 LIMIT 1")
    BibleTranslationEntity getDefaultTranslation();

    // --- Books ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertBook(BibleBookEntity book);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertBooks(List<BibleBookEntity> books);

    @Query("SELECT * FROM bible_books WHERE translationId = :translationId ORDER BY bookNumber ASC")
    List<BibleBookEntity> getBooksByTranslation(String translationId);

    @Query("SELECT * FROM bible_books WHERE translationId = :translationId AND bookNumber = :bookNumber LIMIT 1")
    BibleBookEntity getBook(String translationId, int bookNumber);

    // --- Chapters ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertChapter(BibleChapterEntity chapter);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertChapters(List<BibleChapterEntity> chapters);

    @Query("SELECT * FROM bible_chapters WHERE bookId = :bookId ORDER BY chapterNumber ASC")
    List<BibleChapterEntity> getChaptersByBook(String bookId);

    @Query("SELECT * FROM bible_chapters WHERE bookId = :bookId AND chapterNumber = :chapterNumber LIMIT 1")
    BibleChapterEntity getChapter(String bookId, int chapterNumber);

    // --- Verses ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertVerse(BibleVerseEntity verse);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertVerses(List<BibleVerseEntity> verses);

    @Update
    void updateVerse(BibleVerseEntity verse);

    @Delete
    void deleteVerse(BibleVerseEntity verse);

    @Query("SELECT * FROM bible_verses WHERE id = :id LIMIT 1")
    BibleVerseEntity getVerseById(String id);

    @Query("SELECT * FROM bible_verses WHERE bookNumber = :bookNumber AND chapterNumber = :chapterNumber ORDER BY verseNumber ASC")
    List<BibleVerseEntity> getVerses(int bookNumber, int chapterNumber);

    @Query("SELECT * FROM bible_verses WHERE bookNumber = :bookNumber AND chapterNumber = :chapterNumber AND verseNumber = :verseNumber LIMIT 1")
    BibleVerseEntity getVerse(int bookNumber, int chapterNumber, int verseNumber);

    @Query("SELECT * FROM bible_verses WHERE text LIKE '%' || :query || '%' ORDER BY bookNumber ASC, chapterNumber ASC, verseNumber ASC LIMIT 50")
    List<BibleVerseEntity> searchVerses(String query);

    @Query("UPDATE bible_verses SET isFavorite = :isFavorite WHERE id = :id")
    void updateBookmark(String id, boolean isFavorite);

    @Query("UPDATE bible_verses SET note = :note WHERE id = :id")
    void updateVerseNote(String id, String note);

    @Query("DELETE FROM bible_verses WHERE id = :id")
    void deleteVerseById(String id);

    @Query("DELETE FROM bible_verses")
    void deleteAllVerses();

    @Query("DELETE FROM bible_chapters")
    void deleteAllChapters();

    @Query("DELETE FROM bible_books")
    void deleteAllBooks();

    @Query("DELETE FROM bible_translations")
    void deleteAllTranslations();
}
