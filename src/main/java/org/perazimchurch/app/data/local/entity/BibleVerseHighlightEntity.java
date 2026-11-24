package org.perazimchurch.app.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;

/**
 * Room entity for table "bible_verse_highlights".
 *
 * A highlight belongs to a verse position (book, chapter, verse) rather than to one
 * translation, so a highlight made in one version also shows in the others.
 * Highlights are user data: they are kept across starter and full-text imports.
 */
@Entity(
    tableName = "bible_verse_highlights",
    primaryKeys = {"bookNumber", "chapterNumber", "verseNumber"}
)
public class BibleVerseHighlightEntity {

    private int bookNumber;
    private int chapterNumber;
    private int verseNumber;
    private String color;
    private long updatedMillis;

    public BibleVerseHighlightEntity(int bookNumber, int chapterNumber, int verseNumber,
                                     String color, long updatedMillis) {
        this.bookNumber = bookNumber;
        this.chapterNumber = chapterNumber;
        this.verseNumber = verseNumber;
        this.color = color;
        this.updatedMillis = updatedMillis;
    }

    public int getBookNumber() { return bookNumber; }
    public void setBookNumber(int bookNumber) { this.bookNumber = bookNumber; }

    public int getChapterNumber() { return chapterNumber; }
    public void setChapterNumber(int chapterNumber) { this.chapterNumber = chapterNumber; }

    public int getVerseNumber() { return verseNumber; }
    public void setVerseNumber(int verseNumber) { this.verseNumber = verseNumber; }

    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }

    public long getUpdatedMillis() { return updatedMillis; }
    public void setUpdatedMillis(long updatedMillis) { this.updatedMillis = updatedMillis; }

    @NonNull
    @Override
    public String toString() {
        return "BibleVerseHighlightEntity{" + bookNumber + "." + chapterNumber + "." + verseNumber + "=" + color + "}";
    }
}
