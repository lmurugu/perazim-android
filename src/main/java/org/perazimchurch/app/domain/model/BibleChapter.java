package org.perazimchurch.app.domain.model;

import java.io.Serializable;

/**
 * Domain model representing a Chapter of a Book of the Bible.
 * Compliant with Guidebook §34.
 */
public class BibleChapter implements Serializable {

    private String id; // e.g. "2SA.5"
    private String bookId; // e.g. "2SA"
    private int chapterNumber;
    private int verseCount;

    public BibleChapter() {
    }

    public BibleChapter(String id, String bookId, int chapterNumber, int verseCount) {
        this.id = id;
        this.bookId = bookId;
        this.chapterNumber = chapterNumber;
        this.verseCount = verseCount;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getBookId() { return bookId; }
    public void setBookId(String bookId) { this.bookId = bookId; }

    public int getChapterNumber() { return chapterNumber; }
    public void setChapterNumber(int chapterNumber) { this.chapterNumber = chapterNumber; }

    public int getVerseCount() { return verseCount; }
    public void setVerseCount(int verseCount) { this.verseCount = verseCount; }
}
