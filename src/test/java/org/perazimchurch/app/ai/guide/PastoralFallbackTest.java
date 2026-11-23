package org.perazimchurch.app.ai.guide;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class PastoralFallbackTest {

    @Test
    public void pastoralQuestionsGetTheFixedCitedVerse() throws Exception {
        assertTrue(PastoralFallback.applies(GuideTestSupport.docs().index(),
                "I'm so afraid about money and losing my job"));
    }

    @Test
    public void otherQuestionsDoNotGetTheFallback() throws Exception {
        assertFalse(PastoralFallback.applies(GuideTestSupport.docs().index(), "What time is the service?"));
        assertFalse(PastoralFallback.applies(GuideTestSupport.docs().index(), "   "));
        assertFalse(PastoralFallback.applies(null, "I am afraid"));
    }

    @Test
    public void fallbackAnswerCitesTheKingJamesVerseWordForWord() {
        assertEquals("Psalm 34:18", PastoralFallback.LOCATION);
        assertEquals("The LORD is nigh unto them that are of a broken heart; and saveth such as be of a contrite spirit.",
                PastoralFallback.VERSE);
        String answer = PastoralFallback.answer();
        assertTrue(answer, answer.startsWith("Psalm 34:18 (King James Version): “The LORD is nigh unto them"));
        assertTrue(answer, answer.contains("Bishop or a trusted leader"));
    }
}
