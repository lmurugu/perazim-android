package org.perazimchurch.app.ai.reference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import org.junit.Test;

public class ReferenceGuideProperNamesTest {

    /** Ordinary words such as "so", "on" and "no" must not be looked up as dictionary names. */
    @Test
    public void lowercaseOrdinaryWordsAreNotNames() {
        assertEquals(Arrays.asList(), ReferenceGuide.properNames("I've been so angry at my brother"));
        assertFalse(ReferenceGuide.properNames("I keep failing the same sin, and I feel no hope").contains("Sin"));
    }

    @Test
    public void capitalisedNamesAreLookedUp() {
        assertTrue(ReferenceGuide.properNames("Did Paul write Hebrews?").contains("Paul"));
        assertTrue(ReferenceGuide.properNames("Did Paul write Hebrews?").contains("Hebrews"));
    }
}
