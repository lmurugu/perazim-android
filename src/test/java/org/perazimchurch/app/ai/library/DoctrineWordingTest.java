package org.perazimchurch.app.ai.library;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import org.junit.Test;

public class DoctrineWordingTest {

    /** Internal placeholders must never reach a member. The alternative views are not written yet. */
    @Test
    public void placeholderSentencesAreRemovedFromApprovedWording() {
        String view = "Working view under review: pre-tribulation premillennialism, with the future turning of national Israel. "
                + "Other positions are described fairly and labelled as alternatives. No alternative text is written yet.";
        String out = DoctrineLibrary.withoutPlaceholders(view);
        assertFalse(out.contains("No alternative text"));
        assertFalse(out.contains("labelled as alternatives"));
        assertEquals("Working view under review: pre-tribulation premillennialism, with the future turning of national Israel.",
                out);
    }
}
