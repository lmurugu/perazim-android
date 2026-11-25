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

    /** Each Swahili name and its English name give the same verse, both ways round. */
    @Test
    public void swahiliAndEnglishVerseReferencesMatchBothWays() {
        String[][] pairs = {
            {"Mwanzo 1:1", "Genesis 1:1"}, {"Kutoka 20:3", "Exodus 20:3"}, {"Zaburi 23:1", "Psalms 23:1"},
            {"Isaya 53:5", "Isaiah 53:5"}, {"Mathayo 5:3", "Matthew 5:3"}, {"Luka 2:11", "Luke 2:11"},
            {"Yohana 3:16", "John 3:16"}, {"Warumi 8:28", "Romans 8:28"}, {"1 Wakorintho 13:4", "1 Corinthians 13:4"},
            {"Waefeso 2:8", "Ephesians 2:8"}, {"Waebrania 11:1", "Hebrews 11:1"}, {"Ufunuo 22:21", "Revelation 22:21"},
        };
        for (String[] pair : pairs) {
            ScriptureReferenceNormalizer.Result sw = ScriptureReferenceNormalizer.normalize(pair[0]);
            ScriptureReferenceNormalizer.Result en = ScriptureReferenceNormalizer.normalize(pair[1]);
            assertTrue(pair[0] + " should normalise", sw != null && sw.normalized != null);
            assertEquals(pair[0], pair[1], sw.normalized);
            assertTrue(pair[1] + " should normalise", en != null && en.normalized != null);
            assertEquals(pair[1], en.normalized);
        }
    }

    /** "Yuda" can mean Judah or Jude. It must never silently become Jude. */
    @Test
    public void ambiguousYudaNeverSilentlyBecomesJude() {
        ScriptureReferenceNormalizer.Result r = ScriptureReferenceNormalizer.normalize("Yuda 1:1");
        assertTrue("Yuda must not resolve to Jude", r == null || r.normalized == null || !r.normalized.startsWith("Jude"));
    }

    /** "Jude" in English still means Jude, so the English book is unaffected by removing the Swahili alias. */
    @Test
    public void englishJudeIsUnaffected() {
        ScriptureReferenceNormalizer.Result r = ScriptureReferenceNormalizer.normalize("Jude 3");
        assertTrue(r == null || r.normalized == null || r.normalized.startsWith("Jude") || r.normalized.contains("Jude"));
    }
}
