package org.perazimchurch.app.domain.model;

/** A verse position: canonical book number (1-66), chapter and verse. */
public class VerseLocation implements Comparable<VerseLocation> {
    public final int bookNumber;
    public final int chapter;
    public final int verse;

    public VerseLocation(int bookNumber, int chapter, int verse) {
        this.bookNumber = bookNumber;
        this.chapter = chapter;
        this.verse = verse;
    }

    @Override
    public int compareTo(VerseLocation other) {
        if (bookNumber != other.bookNumber) return Integer.compare(bookNumber, other.bookNumber);
        if (chapter != other.chapter) return Integer.compare(chapter, other.chapter);
        return Integer.compare(verse, other.verse);
    }
}
