package org.perazimchurch.app.domain.games.mosaic;

/** A parsed Bible reference such as "1 Samuel 16:13". Pure Java, no Android types. */
public final class VerseReference {

    public final String book;
    public final int chapter;
    public final int verse;

    private VerseReference(String book, int chapter, int verse) {
        this.book = book;
        this.chapter = chapter;
        this.verse = verse;
    }

    /** Parses "Book C:V". Returns null when the text does not have that shape. */
    public static VerseReference parse(String reference) {
        if (reference == null) return null;
        String text = reference.trim();
        int space = text.lastIndexOf(' ');
        if (space <= 0) return null;
        String book = text.substring(0, space).trim();
        String location = text.substring(space + 1);
        int colon = location.indexOf(':');
        if (colon <= 0) return null;
        try {
            int chapter = Integer.parseInt(location.substring(0, colon));
            int verse = Integer.parseInt(location.substring(colon + 1));
            if (chapter < 1 || verse < 1) return null;
            return new VerseReference(book, chapter, verse);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
