package org.perazimchurch.app.ai;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertNull;

/** Off-topic and referral questions are refused without any model installed (the check runs before the model). */
public class ScopeBeforeInstallTest {

    @Test public void offTopicQuestionIsRefusedWithoutModel() {
        AiResponse r = PerazimAiEngine.deterministicModelRefusal("Which stock should I buy?");
        assertNotNull("off-topic question must be refused before any model check", r);
        assertEquals("I am not authorised to provide medical, legal or financial advice. Please consult a suitably qualified professional.",
                r.getMessageText());
    }

    @Test public void generalQuestionIsRefusedWithChurchWording() {
        AiResponse r = PerazimAiEngine.deterministicModelRefusal("Who won the football match last night?");
        assertNotNull(r);
        assertEquals(PerazimAiEngine.outOfScopeResponse().getMessageText(), r.getMessageText());
    }

    @Test public void membershipDecisionIsReferred() {
        AiResponse r = PerazimAiEngine.deterministicModelRefusal("Am I saved?");
        assertNotNull(r);
        assertEquals(PerazimAiEngine.humanDecisionResponse().getMessageText(), r.getMessageText());
    }

    @Test public void churchQuestionGoesToModel() {
        assertNull("a Bible question must reach the model path", PerazimAiEngine.deterministicModelRefusal("What does Psalm 23 teach about rest?"));
    }

    @Test public void emptyQuestionIsNotHandledHere() {
        assertNull(PerazimAiEngine.deterministicModelRefusal("   "));
    }

    @Test public void greetingsAndCapabilityQuestionsGetBuiltInAnswers() {
        assertNotNull(PerazimAiEngine.builtInAppAnswer("what can you do"));
        assertNotNull(PerazimAiEngine.builtInAppAnswer("Hello!"));
        assertNotNull(PerazimAiEngine.builtInAppAnswer("Who are you?"));
        assertNotNull(PerazimAiEngine.builtInAppAnswer("What is the Bible?"));
    }

    @Test public void shortcutChipsGetUsefulAnswersNotRefusals() {
        assertNotNull(PerazimAiEngine.builtInAppAnswer("📖 Read Scripture"));
        assertNotNull(PerazimAiEngine.builtInAppAnswer("🏛️ Church information"));
        assertNotNull(PerazimAiEngine.builtInAppAnswer("🙏 Prayer and fellowship"));
        assertNotNull(PerazimAiEngine.builtInAppAnswer("📖 Read Scripture (2 Samuel 5)"));
        assertFalse(PerazimAiEngine.builtInAppAnswer("📖 Read Scripture").getMessageText().contains("designed specifically"));
    }

    @Test public void builtInRoutingDoesNotOpenTheRefusalGate() {
        assertNull(PerazimAiEngine.builtInAppAnswer("Which stock should I buy?"));
        assertNull(PerazimAiEngine.builtInAppAnswer("What does Psalm 23 teach about rest?"));
        assertNotNull(PerazimAiEngine.deterministicModelRefusal("Which stock should I buy?"));
    }
}
