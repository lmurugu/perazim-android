package com.example.app.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Room Entity for table "bible_chapters".
 */
@Entity(tableName = "bible_chapters")
public class BibleChapterEntity {

    @PrimaryKey
    @NonNull
    private String id;
    private String bookId;
    private int chapterNumber;
    private int verseCount;

    public BibleChapterEntity(@NonNull String id, String bookId, int chapterNumber, int verseCount) {
        this.id = id;
        this.bookId = bookId;
        this.chapterNumber = chapterNumber;
        this.verseCount = verseCount;
    }

    @NonNull
    public String getId() { return id; }
    public void setId(@NonNull String id) { this.id = id; }

    public String getBookId() { return bookId; }
    public void setBookId(String bookId) { this.bookId = bookId; }

    public int getChapterNumber() { return chapterNumber; }
    public void setChapterNumber(int chapterNumber) { this.chapterNumber = chapterNumber; }

    public int getVerseCount() { return verseCount; }
    public void setVerseCount(int verseCount) { this.verseCount = verseCount; }
}
