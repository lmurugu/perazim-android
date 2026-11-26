package org.perazimchurch.app.ai.gaps;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class GapPolicyTest {

    @Test
    public void crisisAndSelfHarmAreNeverLogged() {
        assertFalse(GapPolicy.mayLog("I want to kill myself, what does the Bible say?", "none"));
        assertFalse(GapPolicy.mayLog("Is self-harm forgiven by God?", "none"));
    }

    @Test
    public void abuseAndHarmToOthersAreNeverLogged() {
        assertFalse(GapPolicy.mayLog("My husband beats me. What does the Bible say about this?", "none"));
        assertFalse(GapPolicy.mayLog("Kujiua ni dhambi?", "none"));
        assertFalse(GapPolicy.mayLog("Is it right to kill him for what he did?", "none"));
    }

    @Test
    public void healthAndPrivacyAreNeverLogged() {
        assertFalse(GapPolicy.mayLog("Which medicine should I take for malaria?", "none"));
        assertFalse(GapPolicy.mayLog("What is my password for the members area?", "none"));
        assertFalse(GapPolicy.mayLog("Can you keep my national ID number private?", "none"));
    }

    @Test
    public void firstPersonDistressOnSensitiveTopicsIsNotLogged() {
        assertFalse(GapPolicy.mayLog("I am afraid my husband will leave me, what should I do?", "none"));
        assertFalse(GapPolicy.mayLog("My father died last week, is he with God?", "none"));
    }

    @Test
    public void doctrineOnSensitiveTopicsWithoutSpeakerIsLoggable() {
        assertTrue(GapPolicy.mayLog("What does the Bible teach about divorce?", "none"));
        assertTrue(GapPolicy.mayLog("What does Scripture say about the dead?", "none"));
    }

    @Test
    public void safetyRouteStatusesAreNeverLogged() {
        assertFalse(GapPolicy.mayLog("Is baptism necessary for salvation?", "routed:crisis"));
        assertFalse(GapPolicy.mayLog("Is baptism necessary for salvation?", "safety"));
        assertFalse(GapPolicy.mayLog("Is baptism necessary for salvation?", "routed:safeguarding"));
    }

    @Test
    public void ordinaryDoctrineQuestionIsLoggable() {
        assertTrue(GapPolicy.mayLog("Is baptism necessary for salvation?", "routed:no-go:x"));
    }

    @Test
    public void faithQuestionsAreRecognised() {
        assertTrue(GapPolicy.isFaithQuestion("What does Romans teach about grace?"));
        assertTrue(GapPolicy.isFaithQuestion("Explain tongues in church"));
        assertFalse(GapPolicy.isFaithQuestion("What is the stock price of Safaricom today?"));
    }

    @Test
    public void emptyQuestionIsNotLogged() {
        assertFalse(GapPolicy.mayLog("   ", "none"));
        assertFalse(GapPolicy.mayLog(null, "none"));
    }
}
