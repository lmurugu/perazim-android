package org.perazimchurch.app.ai;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class CrisisPhrasesTest {

    private static boolean hit(String text) {
        return CrisisPhrases.PATTERN.matcher(text).find();
    }

    @Test
    public void suicidalWordingNowMatches() {
        assertTrue(hit("I don't see the point of living anymore."));
        assertTrue(hit("I don't see the point of lviing anymore."));
        assertTrue(hit("my friend just texted me that she wants to die tonight"));
        assertTrue(hit("Ni sawa kama nikijiumiza mwenyewe? Nataka tu kukomesha maumivu."));
    }

    @Test
    public void ordinaryUsesAreNotCrisis() {
        assertFalse(hit("I don't see the point of this sermon."));
        assertFalse(hit("What does Psalm 23 1 say?"));
    }
}
