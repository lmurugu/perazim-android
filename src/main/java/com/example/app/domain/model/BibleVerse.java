package com.example.app.domain.model;

import java.io.Serializable;

/**
 * Domain model representing a single Holy Scripture Verse.
 * Compliant with Guidebook §34.
 */
public class BibleVerse implements Serializable {

    private String id; // e.g. "2SA.5.20"
    private String translationId;
    private String bookId;
    private int chapterNumber;
    private int verseNumber;
    private String text;
    private boolean isFavorite;
    private String note;

    public BibleVerse() {
    }

    public BibleVerse(String id, String translationId, String bookId,
                      int chapterNumber, int verseNumber, String text,
                      boolean isFavorite, String note) {
        this.id = id;
        this.translationId = translationId;
        this.bookId = bookId;
        this.chapterNumber = chapterNumber;
        this.verseNumber = verseNumber;
        this.text = text;
        this.isFavorite = isFavorite;
        this.note = note;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTranslationId() { return translationId; }
    public void setTranslationId(String translationId) { this.translationId = translationId; }

    public String getBookId() { return bookId; }
    public void setBookId(String bookId) { this.bookId = bookId; }

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
