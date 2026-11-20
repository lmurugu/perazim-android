package org.perazimchurch.app.data.local.text;

import androidx.annotation.NonNull;
import androidx.room.Entity;

/**
 * One verse of Bible text in the separate, read-only text database (bible_text.db).
 *
 * <p>Only public text lives here. Bookmarks, notes, highlights and topics stay in the private encrypted database.
 */
@Entity(
    tableName = "bible_text_verses",
    primaryKeys = {"translationId", "bookNumber", "chapterNumber", "verseNumber"}
)
public class BibleTextVerseEntity {

    @NonNull
    private String translationId;
    private int bookNumber;
    private int chapterNumber;
    private int verseNumber;
    private String text;

    public BibleTextVerseEntity(@NonNull String translationId, int bookNumber, int chapterNumber,
                                int verseNumber, String text) {
        this.translationId = translationId;
        this.bookNumber = bookNumber;
        this.chapterNumber = chapterNumber;
        this.verseNumber = verseNumber;
        this.text = text;
    }

    @NonNull
    public String getTranslationId() { return translationId; }
    public void setTranslationId(@NonNull String translationId) { this.translationId = translationId; }

    public int getBookNumber() { return bookNumber; }
    public void setBookNumber(int bookNumber) { this.bookNumber = bookNumber; }

    public int getChapterNumber() { return chapterNumber; }
    public void setChapterNumber(int chapterNumber) { this.chapterNumber = chapterNumber; }

    public int getVerseNumber() { return verseNumber; }
    public void setVerseNumber(int verseNumber) { this.verseNumber = verseNumber; }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }
}
