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
    public void thirdPartyRiskIsUrgent() {
        route("crisis", "My friend texted that he is ending his life tonight.");
        route("crisis", "My brother texted me, 'I'm ending my life tonight'");
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
