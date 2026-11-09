package org.perazimchurch.app.ai.remote;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class BibleContextExtractorTest {
    @Test
    public void missingScriptureContextDoesNotInventFallbackQuotes() {
        assertTrue(BibleContextExtractor.getFallbackPassages().isEmpty());
    }
}
