package org.perazimchurch.app.data.local.text;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

/** Queries for the separate Bible text database. */
@Dao
public interface BibleTextDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<BibleTextVerseEntity> verses);

    @Query("SELECT * FROM bible_text_verses WHERE translationId = :translationId AND bookNumber = :bookNumber AND chapterNumber = :chapterNumber ORDER BY verseNumber ASC")
    List<BibleTextVerseEntity> getChapter(String translationId, int bookNumber, int chapterNumber);

    @Query("SELECT * FROM bible_text_verses WHERE translationId = :translationId AND bookNumber = :bookNumber AND chapterNumber = :chapterNumber AND verseNumber = :verseNumber LIMIT 1")
    BibleTextVerseEntity getVerse(String translationId, int bookNumber, int chapterNumber, int verseNumber);

    @Query("SELECT * FROM bible_text_verses WHERE translationId = :translationId AND text LIKE '%' || :query || '%' ORDER BY bookNumber ASC, chapterNumber ASC, verseNumber ASC LIMIT 200")
    List<BibleTextVerseEntity> search(String translationId, String query);

    @Query("SELECT * FROM bible_text_verses WHERE translationId = :translationId AND text LIKE '%' || :first || '%' AND text LIKE '%' || :second || '%' ORDER BY bookNumber ASC, chapterNumber ASC, verseNumber ASC LIMIT 50")
    List<BibleTextVerseEntity> searchTerms(String translationId, String first, String second);

    @Query("SELECT COUNT(*) FROM bible_text_verses WHERE translationId = :translationId")
    int count(String translationId);

    @Query("DELETE FROM bible_text_verses WHERE translationId = :translationId")
    void deleteTranslation(String translationId);
}
