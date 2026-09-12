package com.example.app.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Room Entity for table "bible_books".
 */
@Entity(tableName = "bible_books")
public class BibleBookEntity {

    @PrimaryKey
    @NonNull
    private String id;
    private String translationId;
    private int bookNumber;
    private String name;
    private String testament;
    private int chapterCount;

    public BibleBookEntity(@NonNull String id, String translationId, int bookNumber,
                           String name, String testament, int chapterCount) {
        this.id = id;
        this.translationId = translationId;
        this.bookNumber = bookNumber;
        this.name = name;
        this.testament = testament;
        this.chapterCount = chapterCount;
    }

    @NonNull
    public String getId() { return id; }
    public void setId(@NonNull String id) { this.id = id; }

    public String getTranslationId() { return translationId; }
    public void setTranslationId(String translationId) { this.translationId = translationId; }

    public int getBookNumber() { return bookNumber; }
    public void setBookNumber(int bookNumber) { this.bookNumber = bookNumber; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getTestament() { return testament; }
    public void setTestament(String testament) { this.testament = testament; }

    public int getChapterCount() { return chapterCount; }
    public void setChapterCount(int chapterCount) { this.chapterCount = chapterCount; }
}
