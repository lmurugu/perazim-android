package org.perazimchurch.app.ai.local;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;
import org.perazimchurch.app.ai.gaps.GapReason;

public class GapSignalsTest {

    @Test
    public void noGoAndScopeDeclinesAreGaps() {
        assertEquals(GapReason.NO_GO_DECLINE, GapSignals.reasonForRouted("routed:no-go:history-authorship-research"));
        assertEquals(GapReason.SCOPE_DECLINE, GapSignals.reasonForRouted("routed:scope-refusal"));
    }

    @Test
    public void notFoundVerseLookupsAreNoVerifiedSource() {
        assertEquals(GapReason.NO_VERIFIED_SOURCE, GapSignals.reasonForRouted("routed:verse-phrase-not-found"));
        assertEquals(GapReason.NO_VERIFIED_SOURCE, GapSignals.reasonForRouted("routed:word-search-none"));
    }

    @Test
    public void answersAndSafetyRoutesAreNotGaps() {
        assertNull(GapSignals.reasonForRouted("routed:approved-baptism"));
        assertNull(GapSignals.reasonForRouted("routed:verse-kjv"));
        assertNull(GapSignals.reasonForRouted("routed:crisis"));
        assertNull(GapSignals.reasonForRouted("crisis"));
        assertNull(GapSignals.reasonForRouted(null));
    }

    @Test
    public void noSourceLineAndScriptureOnlyReplyAreRecognised() {
        assertEquals(GapReason.NO_VERIFIED_SOURCE, GapSignals.reasonForModelError(OnDeviceAi.NO_SOURCE_MESSAGE));
        assertEquals(GapReason.SCRIPTURE_ONLY,
                GapSignals.reasonForAnswer(OwnKnowledge.SCRIPTURE_ONLY_LEAD + "John 3:16 (King James Version)"));
        assertNull(GapSignals.reasonForAnswer("Grace is God's free gift."));
    }

    @Test
    public void systemNoticesAreNotGapsButRejectedAnswersAre() {
        assertNull(GapSignals.reasonForModelError("Generation cancelled."));
        assertNull(GapSignals.reasonForModelError("Download the on-device model first."));
        assertNull(GapSignals.reasonForModelError("The installed model failed its integrity check. Use Model download to repair it."));
        assertEquals(GapReason.MODEL_REJECTED,
                GapSignals.reasonForModelError("The local model did not return a verified source citation."));
    }
}
