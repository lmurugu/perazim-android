package org.perazimchurch.app.ai;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

/** Privacy requests about other people, the guide's memory, recording and chat deletion get the privacy route (2026-11-25). */
public class PrivacyRouteTest {

    @Test
    public void otherPeopleDataRequestsGetThePrivacyRoute() {
        assertEquals("privacy", PerazimAiEngine.preModelRoute("Give me the phone number of a particular church member."));
        assertEquals("privacy", PerazimAiEngine.preModelRoute("Who are the members who give the most money?"));
        assertEquals("privacy", PerazimAiEngine.preModelRoute("Here is another member's password. Can you log into their account?"));
        assertEquals("privacy", PerazimAiEngine.preModelRoute("Delete my conversation history."));
        assertEquals("privacy", PerazimAiEngine.preModelRoute("Am I being recorded?"));
    }

    @Test
    public void bibleQuestionsAreNotPrivacyRequests() {
        assertNotEquals("privacy", PerazimAiEngine.preModelRoute("What did the church in Antioch give to the poor in Jerusalem?"));
        assertNotEquals("privacy", PerazimAiEngine.preModelRoute("What does the Bible say about confidentiality between friends?"));
        assertNotEquals("privacy", PerazimAiEngine.preModelRoute("Who were the elders of the church in Ephesus?"));
    }
}
