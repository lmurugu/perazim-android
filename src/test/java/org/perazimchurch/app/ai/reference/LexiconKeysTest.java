package org.perazimchurch.app.ai.reference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

public class LexiconKeysTest {

    private static final String AGAPE = "αγάπη";
    private static final String AB = "אב";

    @Test
    public void strongsNumberIsPaddedToItsCode() {
        List<LexiconKeys.Key> keys = LexiconKeys.parse("What does G26 mean?");
        assertEquals(1, keys.size());
        assertTrue(keys.get(0).strongs);
        assertEquals("G0026", keys.get(0).value);
    }

    @Test
    public void hebrewNumberIsReadWithOrWithoutSpace() {
        assertEquals("H0430", LexiconKeys.parse("Explain H430 in Genesis 1:1").get(0).value);
        assertEquals("H0430", LexiconKeys.parse("Explain h 430").get(0).value);
    }

    @Test
    public void numbersOutsideTheStrongsRangeAreIgnored() {
        assertTrue(LexiconKeys.parse("G5625").isEmpty());
        assertTrue(LexiconKeys.parse("H8675").isEmpty());
        assertTrue(LexiconKeys.parse("G0").isEmpty());
    }

    @Test
    public void chapterAndVerseNumbersAreNotStrongsNumbers() {
        assertTrue(LexiconKeys.parse("Read John 3:16 and Psalm 23").isEmpty());
    }

    @Test
    public void lettersAttachedToNumbersAreNotStrongsNumbers() {
        assertTrue(LexiconKeys.parse("What is H2O?").isEmpty());
        assertTrue(LexiconKeys.parse("Chapter G3rd").isEmpty());
    }

    @Test
    public void greekAndHebrewWordsAreExactLemmaKeys() {
        List<LexiconKeys.Key> greek = LexiconKeys.parse("What does " + AGAPE + " mean?");
        assertEquals(1, greek.size());
        assertFalse(greek.get(0).strongs);
        assertEquals(AGAPE, greek.get(0).value);
        assertEquals(AB, LexiconKeys.parse("Meaning of " + AB).get(0).value);
    }

    @Test
    public void keysAreDeduplicatedAndCapped() {
        assertEquals(1, LexiconKeys.parse("G26 and G26 again").size());
        assertEquals(LexiconKeys.MAX_KEYS, LexiconKeys.parse("G26 G27 G28 G29").size());
    }

    @Test
    public void plainEnglishQuestionHasNoKeys() {
        assertTrue(LexiconKeys.parse("How should I pray about my job?").isEmpty());
        assertTrue(LexiconKeys.parse(null).isEmpty());
        assertTrue(LexiconKeys.parse("   ").isEmpty());
    }
}
