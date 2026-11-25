package org.perazimchurch.app.ai;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** A crisis reply never waits behind an earlier request, and a stuck request releases after the stale limit. */
public class GuideSendGateTest {

    private static final long START = 1_000_000L;

    @Test
    public void idleChatAcceptsAnyMessage() {
        assertTrue(GuideSendGate.mayAccept(false, 0L, START, false));
    }

    @Test
    public void ordinaryMessageWaitsWhileAnEarlierRequestRuns() {
        assertFalse(GuideSendGate.mayAccept(true, START, START + 5_000L, false));
    }

    @Test
    public void safetyMessageIsAcceptedWhileAnEarlierRequestRuns() {
        assertTrue(GuideSendGate.mayAccept(true, START, START + 5_000L, true));
    }

    @Test
    public void stuckRequestReleasesAfterTheStaleLimit() {
        assertFalse(GuideSendGate.mayAccept(true, START, START + GuideSendGate.STALE_AFTER_MS - 1, false));
        assertTrue(GuideSendGate.mayAccept(true, START, START + GuideSendGate.STALE_AFTER_MS, false));
    }

    @Test
    public void crisisMessageAfterAnotherCrisisReplyIsAccepted() {
        // The phone case: a crisis message must still get its reply even if an earlier request still holds the lock.
        assertTrue(PerazimAiEngine.safetyFirstResponse("My brother texted me Im ending my life tonight. What should I do?") != null);
        assertTrue(GuideSendGate.mayAccept(true, START, START + 1_000L, true));
    }
}
