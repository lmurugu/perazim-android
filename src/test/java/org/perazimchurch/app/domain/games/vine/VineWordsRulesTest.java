package org.perazimchurch.app.domain.games.vine;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class VineWordsRulesTest {

    @Test public void normaliseKeepsLettersInUpperCase() {
        assertEquals("VINE", VineWordsRules.normalise(" v-i n e! "));
        assertEquals("", VineWordsRules.normalise(null));
        assertEquals("", VineWordsRules.normalise("123"));
    }

    @Test public void spellableCountsRepeatedLetters() {
        assertTrue(VineWordsRules.spellable("BEAN", "BEANS"));
        assertFalse("needs two E", VineWordsRules.spellable("BEE", "BEAN"));
        assertTrue(VineWordsRules.spellable("EEL", "EELS"));
    }

    @Test public void emptyWordIsSpellableOnlyTrivially() {
        assertTrue(VineWordsRules.spellable("", "ABC"));
    }
}
