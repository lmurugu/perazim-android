package org.perazimchurch.app.games;

import org.junit.Test;
import org.perazimchurch.app.domain.games.BookReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class BookReferenceTest {
    private static void check(String citation, String bookId, int chapter) {
        BookReference.Target t = BookReference.parse(citation);
        assertEquals(citation, bookId, t.bookId);
        assertEquals(citation, chapter, t.chapter);
    }

    @Test public void plainBook() { check("Genesis 6:14", "GEN", 6); }
    @Test public void numberedBook() { check("1 Samuel 3:4", "1SA", 3); }
    @Test public void multiWordBook() { check("Song of Solomon 2:1", "SNG", 2); }
    @Test public void psalmAlias() { check("Psalm 23", "PSA", 23); }
    @Test public void noVerseNumber() { check("Romans 8", "ROM", 8); }
    @Test public void caseAndSpacesIgnored() { check("  revelation   21:1 ", "REV", 21); }
    @Test public void unknownBookIsNull() { assertNull(BookReference.parse("Narnia 1:1")); }
    @Test public void nullIsNull() { assertNull(BookReference.parse(null)); }
    @Test public void zeroChapterIsNull() { assertNull(BookReference.parse("John 0:1")); }
}
