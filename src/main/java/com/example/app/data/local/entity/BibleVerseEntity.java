package com.example.app.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Room Entity for table "bible_verses".
 * Includes composite index on (bookNumber, chapterNumber, verseNumber) for fast verse lookups.
 */
@Entity(
    tableName = "bible_verses",
    indices = {
        @Index(value = {"bookNumber", "chapterNumber", "verseNumber"})
    }
)
public class BibleVerseEntity {

    @PrimaryKey
    @NonNull
    private String id;
    private String chapterId;
    private int bookNumber;
    private int chapterNumber;
    private int verseNumber;
    private String text;
    private boolean isFavorite;
    private String note;

    @androidx.room.Ignore
    public BibleVerseEntity(@NonNull String id, String chapterId, int bookNumber,
                            int chapterNumber, int verseNumber, String text) {
        this(id, chapterId, bookNumber, chapterNumber, verseNumber, text, false, null);
    }

    public BibleVerseEntity(@NonNull String id, String chapterId, int bookNumber,
                            int chapterNumber, int verseNumber, String text,
                            boolean isFavorite, String note) {
        this.id = id;
        this.chapterId = chapterId;
        this.bookNumber = bookNumber;
        this.chapterNumber = chapterNumber;
        this.verseNumber = verseNumber;
        this.text = text;
        this.isFavorite = isFavorite;
        this.note = note;
    }

    @NonNull
    public String getId() { return id; }
    public void setId(@NonNull String id) { this.id = id; }

    public String getChapterId() { return chapterId; }
    public void setChapterId(String chapterId) { this.chapterId = chapterId; }

    public int getBookNumber() { return bookNumber; }
    public void setBookNumber(int bookNumber) { this.bookNumber = bookNumber; }

    public int getChapterNumber() { return chapterNumber; }
    public void setChapterNumber(int chapterNumber) { this.chapterNumber = chapterNumber; }

    public int getVerseNumber() { return verseNumber; }
    public void setVerseNumber(int verseNumber) { this.verseNumber = verseNumber; }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    public boolean isFavorite() { return isFavorite; }
    public void setFavorite(boolean favorite) { isFavorite = favorite; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
}
