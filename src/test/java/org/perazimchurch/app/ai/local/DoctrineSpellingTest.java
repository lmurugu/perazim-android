package org.perazimchurch.app.ai.local;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/** Typo tolerance for doctrine and common faith words (round 49 gap fill). */
public class DoctrineSpellingTest {

    @Test
    public void correctsCommonFaithMisspellings() {
        assertEquals("Can I think about the antichrist?",
                GuideTurnRouter.correctDoctrineSpelling("Can I tihnk about the Antichrsit?"));
        assertEquals("Ask the same question three times using different wording.",
                GuideTurnRouter.correctDoctrineSpelling("Ask the same question three tmies using different wording."));
        assertEquals("explain the causes",
                GuideTurnRouter.correctDoctrineSpelling("Epxlain the causes"));
        assertEquals("a scientific theory that appears to contradict the Bible",
                GuideTurnRouter.correctDoctrineSpelling("a scientific theory that apepars to contradict the Bible"));
        assertEquals("Does the Bible teach once saved, always saved?",
                GuideTurnRouter.correctDoctrineSpelling("Does the Bible teach once svaed, always saved?"));
    }

    @Test
    public void leavesCorrectWordsAlone() {
        assertEquals("What does the Bible say about the times?",
                GuideTurnRouter.correctDoctrineSpelling("What does the Bible say about the times?"));
    }
}
