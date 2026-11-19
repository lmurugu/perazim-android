package org.perazimchurch.app.data.local.entity;

import androidx.room.Entity;

/** Room entity for table "bible_note_links": one verse linked from a note. */
@Entity(
    tableName = "bible_note_links",
    primaryKeys = {"noteId", "bookNumber", "chapterNumber", "verseNumber"}
)
public class BibleNoteLinkEntity {

    private long noteId;
    private int bookNumber;
    private int chapterNumber;
    private int verseNumber;

    public BibleNoteLinkEntity(long noteId, int bookNumber, int chapterNumber, int verseNumber) {
        this.noteId = noteId;
        this.bookNumber = bookNumber;
        this.chapterNumber = chapterNumber;
        this.verseNumber = verseNumber;
    }

    public long getNoteId() { return noteId; }
    public void setNoteId(long noteId) { this.noteId = noteId; }

    public int getBookNumber() { return bookNumber; }
    public void setBookNumber(int bookNumber) { this.bookNumber = bookNumber; }

    public int getChapterNumber() { return chapterNumber; }
    public void setChapterNumber(int chapterNumber) { this.chapterNumber = chapterNumber; }

    public int getVerseNumber() { return verseNumber; }
    public void setVerseNumber(int verseNumber) { this.verseNumber = verseNumber; }
}
