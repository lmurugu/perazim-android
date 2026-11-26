package org.perazimchurch.app.ai;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

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

    private static final String[] EMERGENCY_ROUTES = {"crisis", "crisis-third-party", "crisis-and-violence", "violence-safeguarding"};

    private static String text(String route) {
        return PerazimAiEngine.preModelResponse(route, null).getMessageText();
    }

    @Test
    public void swahiliAbuseGoesToSafeguarding() {
        route("violence-safeguarding", "Mume wangu ananipiga kila siku.");
        route("violence-safeguarding", "Mke wangu ananinyanyasa nyumbani.");
        route("violence-safeguarding", "Ananitishia kuniua nyumbani.");
    }

    @Test
    public void swahiliAbuseWithSpeakerSuicidalWordsGetsBothRisks() {
        route("crisis-and-violence", "Mume wangu ananipiga, na mimi nataka kufa.");
    }

    @Test
    public void personalSuicidalIntentGetsSuicideResponse() {
        route("crisis", "I'm thinking of ending my life because my wife left.");
        route("crisis", "I want to end my life tonight.");
    }

    @Test
    public void thirdPartySuicideNeverAsksReporterForOwnPlan() {
        route("crisis-third-party", "My friend texted that he is ending his life tonight.");
        String response = text("crisis-third-party");
        assertFalse(response, response.contains("Do you have a plan"));
        assertFalse(response, response.contains("Are you safe right now"));
        assertTrue(response, response.contains("where they are"));
        assertTrue(response, response.contains("999 or 112"));
    }

    @Test
    public void spouseAbuseNeverAsksSuicidePlanQuestion() {
        for (String route : new String[] {"violence-safeguarding"}) {
            String response = text(route);
            assertFalse(response, response.contains("end your life"));
            assertFalse(response, response.contains("Do you have a plan"));
            assertTrue(response, response.contains("immediate danger from someone else"));
        }
    }

    @Test
    public void domesticViolenceTheologyIsNotEmergencyRoute() {
        String q = "What does the Bible teach about domestic violence?";
        assertEquals("topic-violence", PerazimAiEngine.safetyRoute(q));
        String route = PerazimAiEngine.preModelRoute(q);
        for (String emergency : EMERGENCY_ROUTES) {
            assertFalse(q + " -> " + route, emergency.equals(route));
        }
    }

    @Test
    public void bothRisksAreAddressedTogether() {
        String response = text("crisis-and-violence");
        assertTrue(response, response.contains("two dangers"));
        assertTrue(response, response.contains("immediate danger from someone else"));
        assertTrue(response, response.contains("ending your life"));
    }

    @Test
    public void staleGbvWarningIsGone() {
        for (String route : new String[] {"crisis", "crisis-third-party", "crisis-and-violence", "violence-safeguarding", "safeguarding"}) {
            assertFalse(route, text(route).contains("may not be current"));
        }
    }

    @Test
    public void leaveAloneAdviceIsQualifiedForSafety() {
        String response = text("crisis");
        assertFalse(response, response.contains("do not leave them alone"));
        assertTrue(response, response.contains("safe for you to do so"));
        assertTrue(response, response.contains("Do not put yourself or anyone else in danger"));
    }

    @Test
    public void thirdPartySuicideWithHelpQuestionStaysThirdParty() {
        route("crisis-third-party", "My brother texted me, 'I'm ending my life tonight'. What should I do?");
    }

}
