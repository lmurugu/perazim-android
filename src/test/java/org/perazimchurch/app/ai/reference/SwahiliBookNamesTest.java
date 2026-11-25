package org.perazimchurch.app.ai.reference;

import org.junit.Test;
import org.perazimchurch.app.ai.local.ScriptureReferenceNormalizer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/** Swahili book names (Biblia Takatifu) map to the same canonical books as the English names. */
public class SwahiliBookNamesTest {

    @Test
    public void swahiliNamesMapToCanonicalBookNumbers() {
        assertEquals(1, ReferenceKeys.bookNumber("Mwanzo"));
        assertEquals(19, ReferenceKeys.bookNumber("Zaburi"));
        assertEquals(40, ReferenceKeys.bookNumber("Mathayo"));
        assertEquals(43, ReferenceKeys.bookNumber("Yohana"));
        assertEquals(62, ReferenceKeys.bookNumber("1 Yohana"));
        assertEquals(66, ReferenceKeys.bookNumber("Ufunuo"));
    }

    @Test
    public void englishNamesStillMapToTheSameBooks() {
        assertEquals(ReferenceKeys.bookNumber("Psalms"), ReferenceKeys.bookNumber("Zaburi"));
        assertEquals(ReferenceKeys.bookNumber("John"), ReferenceKeys.bookNumber("Yohana"));
    }

    @Test
    public void swahiliVerseReferenceNormalisesToEnglish() {
        ScriptureReferenceNormalizer.Result r = ScriptureReferenceNormalizer.normalize("Yohana 1:1 inasema nini");
        assertTrue(r != null && r.normalized != null);
        assertEquals("John 1:1", r.normalized);
    }

    @Test
    public void swahiliPsalmNormalisesToEnglish() {
        ScriptureReferenceNormalizer.Result r = ScriptureReferenceNormalizer.normalize("Zaburi 23:1");
        assertTrue(r != null && r.normalized != null);
        assertEquals("Psalms 23:1", r.normalized);
    }
}
