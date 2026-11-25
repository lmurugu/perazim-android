package org.perazimchurch.app.ai;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/** Context for crisis wording (Levi, 2026-11-25): real warnings stay urgent; denial and literary questions get context. */
public class CrisisContextTest {

    private static void route(String expected, String question) {
        assertEquals(question, expected, PerazimAiEngine.preModelRoute(question));
    }

    @Test
    public void firstPersonIntentIsUrgent() {
        route("crisis", "I'm thinking of ending my life.");
        route("crisis", "I will end my life tonight.");
        route("crisis", "I'm thinking of ending my life because my wife left and my marriage is destroyed.");
    }

    @Test
    public void thirdPartyRiskGetsThirdPartyGuidance() {
        route("crisis-third-party", "My friend texted that he is ending his life tonight.");
        route("crisis-third-party", "My brother texted me, 'I'm ending my life tonight'");
    }

    @Test
    public void abuseGetsViolenceSafeguardingNotSuicidePlanQuestion() {
        route("violence-safeguarding", "My husband beats me.");
        route("violence-safeguarding", "I'm not thinking of ending my life, but my husband threatened me.");
        assertEquals("topic-violence", PerazimAiEngine.safetyRoute("What does the Bible say about domestic violence?"));
    }

    @Test
    public void abuseWithSuicidalWordsGetsBothRisks() {
        route("crisis-and-violence", "I'm thinking of ending my life because my husband beats me.");
    }

    @Test
    public void denialGetsContextNotFullCrisis() {
        route("crisis-denial", "I'm not thinking of ending my life.");
    }

    @Test
    public void literaryQuestionsGetExplanation() {
        route("crisis-literary", "What does 'ending my life' mean in this poem?");
        route("crisis-literary", "A character in my novel says he wants to end his life.");
    }
}
