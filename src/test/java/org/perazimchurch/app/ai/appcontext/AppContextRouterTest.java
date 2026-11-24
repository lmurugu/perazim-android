package org.perazimchurch.app.ai.appcontext;

import org.junit.Test;
import org.perazimchurch.app.ai.appcontext.AppContextRouter.Route;
import org.perazimchurch.app.ai.appcontext.AppContextRouter.Topic;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/** Routing rules for app-context questions. Synthetic sentences only; no member data. */
public class AppContextRouterTest {

    @Test
    public void personalGameQuestionsRouteToGames() {
        assertRoute("What are my game scores?", Topic.GAMES);
        assertRoute("Which questions do I miss most?", Topic.GAMES);
        assertRoute("Did I get the ladder of faith questions wrong?", Topic.GAMES);
        assertRoute("Why am I failing in the games?", Topic.GAMES);
    }

    @Test
    public void personalBibleQuestionsRouteToBible() {
        assertRoute("Show my Bible notes", Topic.BIBLE);
        assertRoute("What verses have I bookmarked?", Topic.BIBLE);
        assertRoute("Where did I stop reading?", Topic.BIBLE);
        assertRoute("How many highlights do I have in my verses?", Topic.BIBLE);
    }

    @Test
    public void sermonsCommunityAndRoutineRoute() {
        assertRoute("Show my recent sermons", Topic.SERMONS);
        assertRoute("What groups have I joined?", Topic.COMMUNITY);
        assertRoute("How many posts have I shared in the community?", Topic.COMMUNITY);
        assertRoute("What is my streak?", Topic.ROUTINE);
        assertRoute("Did I keep my routine today?", Topic.ROUTINE);
    }

    @Test
    public void summaryQuestionsRouteToSummary() {
        assertRoute("How am I doing in the app?", Topic.SUMMARY);
        assertRoute("Give me a summary of my activity", Topic.SUMMARY);
    }

    @Test
    public void personalMessagesAreBlockedAtRouting() {
        Route route = AppContextRouter.route("Can you read my messages to the pastor?");
        assertNotNull(route);
        assertEquals(Topic.PRIVATE_BLOCKED, route.topics.get(0));
        assertTrue(AppContextRouter.route("Show me my chats").has(Topic.PRIVATE_BLOCKED));
        assertTrue(AppContextRouter.route("Do you see my direct messages?").has(Topic.PRIVATE_BLOCKED));
    }

    @Test
    public void doctrinalAndGeneralQuestionsDoNotRoute() {
        // Generic words need "my" so scripture and doctrine questions stay with the Bible and doctrine guides.
        assertNull(AppContextRouter.route("What does this verse mean in its context?"));
        assertNull(AppContextRouter.route("Explain the verses about grace in Romans 8"));
        assertNull(AppContextRouter.route("Do I have to speak in tongues to be filled with the Holy Spirit?"));
        assertNull(AppContextRouter.route("Did I sin by being angry?"));
        assertNull(AppContextRouter.route("Did I answer the questions in the sermon well?"));
        assertNull(AppContextRouter.route("Who won the football match yesterday?"));
        assertNull(AppContextRouter.route("I ate a pineapple"));
    }

    @Test
    public void appHelpQuestionsStayWithAppHelp() {
        // Asking how the app works is app help, not a question about the member's own data.
        assertNull(AppContextRouter.route("Where is the sermon button in the app?"));
        assertNull(AppContextRouter.route("How do I highlight a verse?"));
        assertNull(AppContextRouter.route("Where is the Bible tab?"));
    }

    @Test
    public void navigationWordsAreRecognised() {
        assertTrue(AppContextRouter.route("Open my notes").navigate);
        assertTrue(AppContextRouter.route("Take me to my sermons").navigate);
        assertFalse(AppContextRouter.route("What is my streak?").navigate);
    }

    @Test
    public void emptyOrNullQuestionsDoNotRoute() {
        assertNull(AppContextRouter.route(null));
        assertNull(AppContextRouter.route("   "));
        assertNull(AppContextRouter.route("!!!"));
    }

    @Test
    public void normaliseKeepsWholeWordsAndApostrophes() {
        assertEquals(" what's my streak ", AppContextRouter.normalise("What’s my streak?"));
    }

    private static void assertRoute(String question, Topic expected) {
        Route route = AppContextRouter.route(question);
        assertNotNull("expected a route for: " + question, route);
        assertTrue("expected " + expected + " for: " + question + " but got " + route.topics,
                route.has(expected));
    }
}
