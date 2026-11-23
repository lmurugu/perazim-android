package org.perazimchurch.app.ai.reference;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;
import org.junit.Test;

/** A question about the Nicene Creed asks for every stored chunk of it, in order. */
public class ReferenceGuideWordsTest {

    @Test
    public void questionAboutTheNiceneCreedYieldsTheCreedHeadword() {
        List<String> words = ReferenceGuide.words("What does the Nicene Creed say?");
        assertTrue("expected Nicene in " + words, words.contains("Nicene"));
    }

    @Test
    public void creedQuestionsAskForTheWholeCreed() {
        assertTrue(ReferenceGuide.asksForCreed("What does the Nicene Creed say?"));
        assertTrue(ReferenceGuide.asksForCreed("nicene creed"));
    }

    @Test
    public void unrelatedQuestionsDoNotAskForTheCreed() {
        assertFalse(ReferenceGuide.asksForCreed("What does John 3:16 mean?"));
        assertFalse(ReferenceGuide.asksForCreed("Tell me about prayer"));
    }

    @Test
    public void creedCapFitsTheThreeStoredChunks() {
        assertTrue(ReferenceGuide.MAX_CREED_PARTS >= 3);
    }
}
