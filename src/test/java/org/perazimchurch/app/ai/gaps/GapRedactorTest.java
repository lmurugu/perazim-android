package org.perazimchurch.app.ai.gaps;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class GapRedactorTest {

    @Test
    public void emailPhoneAndIdNumbersAreRemoved() {
        String out = GapRedactor.redact("Email me at grace.w@example.com or call 0712 345 678. My ID is 23456789. Is baptism needed?");
        assertFalse(out, out.contains("@"));
        assertFalse(out, out.contains("0712"));
        assertFalse(out, out.contains("23456789"));
        assertTrue(out, out.contains("[email]"));
        assertTrue(out, out.contains("[phone]"));
        assertTrue(out, out.contains("[number]"));
        assertTrue(out, out.contains("Is baptism needed?"));
    }

    @Test
    public void namesAfterIntroductionsAreReplaced() {
        String out = GapRedactor.redact("My name is Grace Wambui. Does the Bible teach tithing?");
        assertFalse(out, out.contains("Wambui"));
        assertTrue(out, out.startsWith("My name is [name]"));
        assertTrue(out, out.contains("Does the Bible teach tithing?"));
    }

    @Test
    public void titledNamesAreReplacedButKeepTheTitle() {
        String out = GapRedactor.redact("Pastor Kamau said grace is a gift. What does Romans 3:24 say?");
        assertFalse(out, out.contains("Kamau"));
        assertTrue(out, out.contains("Pastor [name]"));
        assertTrue(out, out.contains("Romans 3:24"));
    }

    @Test
    public void capitalisedNamesInTheMiddleOfASentenceAreReplaced() {
        String out = GapRedactor.redact("Please explain what Njeri taught about grace in church");
        assertFalse(out, out.contains("Njeri"));
        assertTrue(out, out.contains("grace"));
    }

    @Test
    public void bibleAndFaithWordsAreKept() {
        String q = "What does John 3:16 say about Holy Spirit baptism, and what did Peter and Mary believe?";
        assertEquals(q, GapRedactor.redact(q));
    }

    @Test
    public void shortVerseNumbersAreKept() {
        assertEquals("Explain Psalm 119:105 and Genesis 1:1", GapRedactor.redact("Explain Psalm 119:105 and Genesis 1:1"));
    }

    @Test
    public void emptyAndNullGiveNothing() {
        assertNull(GapRedactor.redact(null));
        assertNull(GapRedactor.redact("   "));
    }

    @Test
    public void longQuestionsAreCapped() {
        StringBuilder long300 = new StringBuilder();
        for (int i = 0; i < 80; i++) long300.append("grace ");
        String out = GapRedactor.redact(long300.toString());
        assertTrue(out.length() <= GapRedactor.MAX_CHARS);
    }

    @Test
    public void normalizedKeyIgnoresCaseAndPunctuation() {
        assertEquals(GapRedactor.normalizedKey("Is baptism needed?"), GapRedactor.normalizedKey("  is BAPTISM needed "));
    }
}
