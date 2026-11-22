package org.perazimchurch.app.games;

import org.junit.Test;
import org.perazimchurch.app.domain.games.mosaic.VerseReference;

import static org.junit.Assert.*;

public class VerseReferenceTest {
    @Test public void parsesSimpleAndNumberedBooks() {
        VerseReference a = VerseReference.parse("Genesis 6:22");
        assertEquals("Genesis", a.book);
        assertEquals(6, a.chapter);
        assertEquals(22, a.verse);
        VerseReference b = VerseReference.parse("1 Samuel 16:13");
        assertEquals("1 Samuel", b.book);
        assertEquals(16, b.chapter);
        assertEquals(13, b.verse);
    }

    @Test public void rejectsMalformedText() {
        assertNull(VerseReference.parse(null));
        assertNull(VerseReference.parse("Genesis"));
        assertNull(VerseReference.parse("Genesis 6"));
        assertNull(VerseReference.parse("Genesis 0:1"));
        assertNull(VerseReference.parse("Genesis x:1"));
    }
}
