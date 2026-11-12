package org.perazimchurch.app.ai.local;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class OfflineScriptureLookupTest {
    @Test public void recognizesSingleVersesAndRanges() {
        assertTrue(OfflineScriptureLookup.matchesRequest("John 3:16"));
        assertTrue(OfflineScriptureLookup.matchesRequest("Please show me 1 John 4:7–9 (KJV)."));
        assertTrue(OfflineScriptureLookup.matchesRequest("Look up Song of Solomon 2:1"));
    }

    @Test public void rejectsOtherTranslationsAndCrossChapterRanges() {
        assertFalse(OfflineScriptureLookup.matchesRequest("John 3:16 ESV"));
        assertFalse(OfflineScriptureLookup.matchesRequest("John 3:16–4:2"));
    }
}
