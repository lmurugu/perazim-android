package org.perazimchurch.app.ai.local;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class PreSigningEvidenceNamesTest {

    /** FW-001 and friends: "so", "on", "no" and "job" in ordinary sentences must not name a dictionary headword. */
    @Test
    public void ordinaryWordsDoNotMatchDictionaryHeadwords() {
        assertFalse(PreSigningEvidence.properNames("quick q: i've been so angry at my brother").contains("so"));
        assertFalse(PreSigningEvidence.properNames("i'm afraid about money and losing my job").contains("job"));
        assertFalse(PreSigningEvidence.properNames("is it a sin to support a political party").contains("sin"));
    }

    @Test
    public void capitalisedNamesMatch() {
        assertTrue(PreSigningEvidence.properNames("Did Paul write Hebrews?").contains("paul"));
    }
}
