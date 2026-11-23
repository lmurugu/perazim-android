package org.perazimchurch.app.ai.reference;

import static org.junit.Assert.assertTrue;

import java.util.List;
import org.junit.Test;

/** The creed lookup works because a question about the Nicene Creed yields the headword "Nicene". */
public class ReferenceGuideWordsTest {

    @Test
    public void questionAboutTheNiceneCreedYieldsTheCreedHeadword() {
        List<String> words = ReferenceGuide.words("What does the Nicene Creed say?");
        assertTrue("expected Nicene in " + words, words.contains("Nicene"));
    }
}
