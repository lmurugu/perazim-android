package org.perazimchurch.app.ai;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class CrisisPhrasesTest {

    private static boolean crisis(String text) {
        return CrisisPhrases.PATTERN.matcher(text).find();
    }

    @Test
    public void englishPhrasesStillMatch() {
        assertTrue(crisis("I want to die"));
        assertTrue(crisis("I think about suicide every day"));
        assertTrue(crisis("I will hurt myself tonight"));
    }

    @Test
    public void swahiliAndShengWishToDieMatch() {
        assertTrue(crisis("nataka kufa"));
        assertTrue(crisis("Nataka kujiua sasa hivi"));
        assertTrue(crisis("najiua leo"));
        assertTrue(crisis("sina sababu ya kuishi tena"));
        assertTrue(crisis("sitaki kuishi tena"));
        assertTrue(crisis("nimechoka kuishi"));
        assertTrue(crisis("nataka kujimaliza"));
    }

    @Test
    public void swahiliSelfHarmMatches() {
        assertTrue(crisis("nataka kujidhuru"));
        assertTrue(crisis("najidhuru kila siku"));
    }

    @Test
    public void swahiliAbuseDisclosuresMatch() {
        assertTrue(crisis("Mume wangu ananipiga kila siku"));
        assertTrue(crisis("ananinyanyasa nyumbani"));
    }

    @Test
    public void everydaySwahiliDoesNotMatch() {
        assertFalse(crisis("nataka chai"));
        assertFalse(crisis("nimechoka baada ya kazi"));
        assertFalse(crisis("Bwana asifiwe, habari yako?"));
        assertFalse(crisis("Nataka kujifunza Biblia"));
        assertFalse(crisis("kufa kwa Yesu msalabani"));
    }

    @Test
    public void wordBoundariesApply() {
        assertFalse(crisis("ananipigia simu"));
        assertFalse(crisis("najiuaje"));
    }
}
