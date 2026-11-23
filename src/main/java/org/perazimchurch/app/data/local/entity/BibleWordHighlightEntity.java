package org.perazimchurch.app.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Room entity for table "bible_word_highlights".
 *
 * A word or character-range highlight inside one verse of one translation. Offsets are
 * character positions in the verse body as shown in the reader ({@code startOffset} inclusive,
 * {@code endOffset} exclusive). Ranges are stored apart from whole-verse highlights, can overlap
 * them and each other, and a verse can carry several. Highlights are user data and survive imports.
 */
@Entity(tableName = "bible_word_highlights",
        indices = {@Index(value = {"translationId", "bookNumber", "chapterNumber"})})
public class BibleWordHighlightEntity {

    @PrimaryKey(autoGenerate = true)
    private long id;
    private String translationId;
    private int bookNumber;
    private int chapterNumber;
    private int verseNumber;
    private int startOffset;
    private int endOffset;
    private String color;
    private long updatedMillis;

    public BibleWordHighlightEntity(String translationId, int bookNumber, int chapterNumber, int verseNumber,
                                    int startOffset, int endOffset, String color, long updatedMillis) {
        this.translationId = translationId;
        this.bookNumber = bookNumber;
        this.chapterNumber = chapterNumber;
        this.verseNumber = verseNumber;
        this.startOffset = startOffset;
        this.endOffset = endOffset;
        this.color = color;
        this.updatedMillis = updatedMillis;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getTranslationId() { return translationId; }
    public void setTranslationId(String translationId) { this.translationId = translationId; }

    public int getBookNumber() { return bookNumber; }
    public void setBookNumber(int bookNumber) { this.bookNumber = bookNumber; }

    public int getChapterNumber() { return chapterNumber; }
    public void setChapterNumber(int chapterNumber) { this.chapterNumber = chapterNumber; }

    public int getVerseNumber() { return verseNumber; }
    public void setVerseNumber(int verseNumber) { this.verseNumber = verseNumber; }

    public int getStartOffset() { return startOffset; }
    public void setStartOffset(int startOffset) { this.startOffset = startOffset; }

    public int getEndOffset() { return endOffset; }
    public void setEndOffset(int endOffset) { this.endOffset = endOffset; }

    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }

    public long getUpdatedMillis() { return updatedMillis; }
    public void setUpdatedMillis(long updatedMillis) { this.updatedMillis = updatedMillis; }

    @NonNull
    @Override
    public String toString() {
        return "BibleWordHighlightEntity{" + translationId + " " + bookNumber + "." + chapterNumber + "."
                + verseNumber + " [" + startOffset + "," + endOffset + ")=" + color + "}";
    }
}
