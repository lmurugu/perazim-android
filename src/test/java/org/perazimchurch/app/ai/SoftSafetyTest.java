package org.perazimchurch.app.ai;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Two-step safety check for vague exhaustion. A warm check first; crisis wording, or a yes or a plan after the check,
 * escalates to the full response. Each conversation starts with no pending check.
 */
public class SoftSafetyTest {

    @Before
    public void startConversation() {
        SoftSafety.reset();
    }

    @Test
    public void swahiliVagueExhaustionGivesWarmCheck() {
        assertEquals(SoftSafety.Verdict.SOFT_CHECK, SoftSafety.judge("Nimechoka na maisha"));
        assertTrue(SoftSafety.isPending());
    }

    @Test
    public void englishVagueExhaustionGivesWarmCheck() {
        assertEquals(SoftSafety.Verdict.SOFT_CHECK, SoftSafety.judge("I'm tired of life"));
        assertEquals(SoftSafety.Verdict.SOFT_CHECK, SoftSafety.judge("I am tired of my life"));
    }

    @Test
    public void checkTextAsksDirectlyAndCarriesVerifiedNumbers() {
        String text = SoftSafety.checkText();
        assertTrue(text, text.contains("harming yourself or ending your life"));
        assertTrue(text, text.contains("999"));
        assertTrue(text, text.contains("112"));
        assertTrue(text, text.contains("922"));
        assertTrue(text, text.contains("116"));
        assertTrue(text, text.contains("Befrienders Kenya +254 722 178 177"));
    }

    @Test
    public void explicitCrisisWordingGoesStraightToFullResponse() {
        assertEquals(SoftSafety.Verdict.CRISIS, SoftSafety.judge("I want to die"));
        assertFalse(SoftSafety.isPending());
    }

    @Test
    public void yesAfterCheckEscalatesAcrossTurns() {
        assertEquals(SoftSafety.Verdict.SOFT_CHECK, SoftSafety.judge("nimechoka na maisha"));
        assertEquals(SoftSafety.Verdict.CRISIS, SoftSafety.judge("yes"));
        assertFalse(SoftSafety.isPending());
    }

    @Test
    public void swahiliYesAfterCheckEscalates() {
        assertEquals(SoftSafety.Verdict.SOFT_CHECK, SoftSafety.judge("nimechoka na maisha"));
        assertEquals(SoftSafety.Verdict.CRISIS, SoftSafety.judge("Ndiyo"));
    }

    @Test
    public void planOrMeansAfterCheckEscalates() {
        assertEquals(SoftSafety.Verdict.SOFT_CHECK, SoftSafety.judge("I'm tired of life"));
        assertEquals(SoftSafety.Verdict.CRISIS, SoftSafety.judge("I have a plan with pills"));
    }

    @Test
    public void nonCommittalMessageKeepsCheckPending() {
        assertEquals(SoftSafety.Verdict.SOFT_CHECK, SoftSafety.judge("nimechoka na maisha"));
        assertEquals(SoftSafety.Verdict.NONE, SoftSafety.judge("What does the Bible say about rest?"));
        assertTrue(SoftSafety.isPending());
        assertEquals(SoftSafety.Verdict.CRISIS, SoftSafety.judge("yes"));
    }

    @Test
    public void yesWithoutCheckIsNotEscalated() {
        assertEquals(SoftSafety.Verdict.NONE, SoftSafety.judge("yes"));
        assertFalse(SoftSafety.isPending());
    }

    @Test
    public void resetStartsNewConversationWithNoPendingCheck() {
        assertEquals(SoftSafety.Verdict.SOFT_CHECK, SoftSafety.judge("nimechoka na maisha"));
        SoftSafety.reset();
        assertEquals(SoftSafety.Verdict.NONE, SoftSafety.judge("yes"));
    }

    @Test
    public void repeatedMessageGivesSameVerdictWithoutChangingState() {
        assertEquals(SoftSafety.Verdict.SOFT_CHECK, SoftSafety.judge("nimechoka na maisha"));
        assertEquals(SoftSafety.Verdict.SOFT_CHECK, SoftSafety.judge("nimechoka na maisha"));
        assertEquals(SoftSafety.Verdict.CRISIS, SoftSafety.judge("yes"));
        assertEquals(SoftSafety.Verdict.CRISIS, SoftSafety.judge("yes"));
    }

    @Test
    public void unrelatedQuestionIsNotSafetyRouted() {
        assertEquals(SoftSafety.Verdict.NONE, SoftSafety.judge("What does John 3:16 mean?"));
        assertFalse(SoftSafety.isPending());
    }
}
