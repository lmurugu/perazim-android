package org.perazimchurch.app.ai.reference;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class ReferenceKeysTest {

    @Test
    public void verseKeysSortInCanonicalOrder() {
        assertEquals(1001001, ReferenceKeys.verseKey(1, 1, 1));
        assertEquals(43003016, ReferenceKeys.verseKey(43, 3, 16));
        assertTrue(ReferenceKeys.verseKey(43, 3, 16) < ReferenceKeys.verseKey(43, 3, 17));
        assertTrue(ReferenceKeys.verseKey(43, 9, 1) < ReferenceKeys.verseKey(43, 10, 1));
        assertTrue(ReferenceKeys.verseKey(43, 10, 1) < ReferenceKeys.verseKey(44, 1, 1));
    }

    @Test
    public void invalidKeysAreRejected() {
        assertEquals(-1, ReferenceKeys.verseKey(0, 1, 1));
        assertEquals(-1, ReferenceKeys.verseKey(67, 1, 1));
        assertEquals(-1, ReferenceKeys.verseKey(1, 0, 1));
        assertEquals(-1, ReferenceKeys.verseKey(1, 1, 1000));
    }

    @Test
    public void bookNamesAndAliasesResolve() {
        assertEquals(43, ReferenceKeys.bookNumber("John"));
        assertEquals(43, ReferenceKeys.bookNumber("  john "));
        assertEquals(43, ReferenceKeys.bookNumber("Jn"));
        assertEquals(62, ReferenceKeys.bookNumber("1 John"));
        assertEquals(62, ReferenceKeys.bookNumber("1 Jn"));
        assertEquals(22, ReferenceKeys.bookNumber("Song of Solomon"));
        assertEquals(19, ReferenceKeys.bookNumber("Psalm"));
        assertEquals(0, ReferenceKeys.bookNumber("Gospel"));
        assertEquals(0, ReferenceKeys.bookNumber(null));
    }

    @Test
    public void formatsCanonicalReferences() {
        assertEquals("John 3:16", ReferenceKeys.format(43003016));
        assertEquals("1 John 2:1", ReferenceKeys.format(62002001));
        assertEquals("", ReferenceKeys.format(0));
    }

    @Test
    public void parsesReferencesInsideQuestions() {
        assertEquals(45008028, ReferenceKeys.parseReference("What does Romans 8:28 mean?"));
        assertEquals(62002001, ReferenceKeys.parseReference("Explain 1 John 2:1 to me"));
        assertEquals(22002004, ReferenceKeys.parseReference("read Song of Solomon 2:4"));
        assertEquals(43003016, ReferenceKeys.parseReference("John 3:16"));
    }

    @Test
    public void unknownOrMissingReferencesReturnNegativeOne() {
        assertEquals(-1, ReferenceKeys.parseReference("What is grace?"));
        assertEquals(-1, ReferenceKeys.parseReference("Tell me about Gospel 3:16"));
        assertEquals(-1, ReferenceKeys.parseReference(null));
        assertEquals(-1, ReferenceKeys.parseReference("John 3:1000"));
    }

    private static void assertTrue(boolean value) {
        org.junit.Assert.assertTrue(value);
    }

    @Test
    public void chapterHeadwordMatchesCommentaryLabels() {
        assertEquals("Genesis 1", ReferenceKeys.chapterHeadword(ReferenceKeys.verseKey(1, 1, 1)));
        assertEquals("1 Corinthians 13", ReferenceKeys.chapterHeadword(ReferenceKeys.verseKey(46, 13, 4)));
        assertEquals("", ReferenceKeys.chapterHeadword(0));
    }
}
