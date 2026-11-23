package org.perazimchurch.app.ai.appcontext;

import org.junit.Test;
import org.perazimchurch.app.ai.AiResponse;
import org.perazimchurch.app.ai.appcontext.AppContextComposer.Reply;
import org.perazimchurch.app.ai.appcontext.AppContextRouter.Route;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/** Answers built from compact facts. Synthetic data only; no member records. */
public class AppContextComposerTest {

    private static Route route(String question) {
        Route route = AppContextRouter.route(question);
        assertNotNull(route);
        return route;
    }

    private static AppSnapshot sample() {
        AppSnapshot s = new AppSnapshot();
        s.games.add(new AppSnapshot.Game("Bible Quiz", 2, 2, 3));
        s.games.add(new AppSnapshot.Game("Word Search", 0, 1, 3));
        s.missedQuestions.add(new AppSnapshot.MissedQuestion("Ladder of Faith", "Who built the ark?", 4, 1));
        s.noteCount = 2;
        s.latestNotePreview = "Grace is a gift, not a wage.";
        s.latestNotePassage = new AppSnapshot.Passage("ROM", "Romans", 3, 24);
        s.highlightCount = 5;
        s.favouriteCount = 1;
        s.favouriteVerses.add(new AppSnapshot.Passage("PSA", "Psalms", 23, 1));
        s.lastReading = new AppSnapshot.Passage("2SA", "2 Samuel", 5, 0);
        s.chaptersToday = 2;
        s.recentSermons.add("Walking in the Light");
        s.savedSermonCount = 1;
        s.myPostCount = 3;
        s.recentPublicPosts.add("Prayer for the campus");
        s.joinedGroups.add("Men of Faith");
        s.upcomingEvents.add("Youth night (28 Nov)");
        s.streak = 6;
        s.lifetimeRoutineDays = 20;
        s.routineDoneToday = false;
        return s;
    }

    @Test
    public void gamesAnswerNamesProgressAndMissedQuestions() {
        Reply reply = AppContextComposer.compose(route("What are my game scores?"), sample(), "What are my game scores?");
        assertTrue(reply.text.contains("Bible Quiz 2 of 3 stars, level 2 open"));
        assertTrue(reply.text.contains("\"Who built the ark?\" (missed 4, right 1)"));
        assertTrue(reply.text.contains("replaying those games"));
        assertEquals(AiResponse.INTENT_OPEN_GAMES, reply.actionIntent);
        assertEquals("🎮 Open Games", reply.actionLabel);
    }

    @Test
    public void notesPreferTheirPassageWhenAskedAboutNotes() {
        String question = "Show my Bible notes";
        Reply reply = AppContextComposer.compose(route(question), sample(), question);
        assertTrue(reply.text.contains("You have 2 Bible notes. Latest: \"Grace is a gift, not a wage.\""));
        assertEquals(AiResponse.INTENT_OPEN_BIBLE, reply.actionIntent);
        assertEquals("ROM|3", reply.actionPayload);
        assertEquals("📖 Open Romans 3:24", reply.actionLabel);
    }

    @Test
    public void biblePlainQuestionOpensLastReading() {
        String question = "Where did I stop reading?";
        Reply reply = AppContextComposer.compose(route(question), sample(), question);
        assertTrue(reply.text.contains("You last read 2 Samuel 5. Chapters read today: 2."));
        assertEquals("2SA|5", reply.actionPayload);
    }

    @Test
    public void routineAnswerUsesStreakAndOpensProfileTab() {
        Reply reply = AppContextComposer.compose(route("What is my streak?"), sample(), "What is my streak?");
        assertTrue(reply.text.contains("Your streak is 6 days."));
        assertTrue(reply.text.contains("Today's routine is not finished yet"));
        assertEquals(AiResponse.INTENT_OPEN_PROFILE, reply.actionIntent);
    }

    @Test
    public void emptyStoresSayWhatIsMissingWithoutInventingData() {
        AppSnapshot empty = new AppSnapshot();
        Reply reply = AppContextComposer.compose(route("What are my game scores?"), empty, "What are my game scores?");
        assertTrue(reply.text.contains("You have not played any games on this phone yet."));
        assertFalse(reply.text.contains("stars"));
        Reply notes = AppContextComposer.compose(route("Show my Bible notes"), empty, "Show my Bible notes");
        assertTrue(notes.text.contains("You have no Bible notes yet."));
        assertEquals(AiResponse.INTENT_OPEN_BIBLE, notes.actionIntent);
        assertEquals(null, notes.actionPayload);
    }

    @Test
    public void communityAnswerUsesOnlyPublicPostsGroupsAndEvents() {
        Reply groups = AppContextComposer.compose(route("What groups have I joined?"), sample(), "What groups have I joined?");
        assertTrue(groups.text.contains("Groups you have joined: Men of Faith."));
        assertFalse("a groups question does not repeat the post count", groups.text.contains("shared 3 posts"));
        assertEquals(AiResponse.INTENT_NAVIGATE_FELLOWSHIP, groups.actionIntent);

        Reply all = AppContextComposer.compose(route("How is my community activity?"), sample(),
                "How is my community activity?");
        assertTrue(all.text.contains("You have shared 3 posts."));
        assertTrue(all.text.contains("Groups you have joined: Men of Faith."));
        assertTrue(all.text.contains("Coming up: Youth night (28 Nov)."));
        assertTrue(all.text.contains("Latest public posts: Prayer for the campus."));
        assertEquals(AiResponse.INTENT_NAVIGATE_FELLOWSHIP, all.actionIntent);
    }

    @Test
    public void bibleReadingQuestionsShowOnlyTheReadingPart() {
        Reply reply = AppContextComposer.compose(route("Where did I stop reading?"), sample(), "Where did I stop reading?");
        assertTrue(reply.text.contains("You last read 2 Samuel 5."));
        assertFalse(reply.text.contains("Bible note"));
    }

    @Test
    public void privateMessagesAreRefusedWithoutAnyAction() {
        Reply reply = AppContextComposer.compose(route("Can you read my messages?"), sample(), "Can you read my messages?");
        assertTrue(reply.text.contains("I cannot see your personal messages or private chats"));
        assertFalse(reply.hasAction());
        assertFalse(reply.text.contains("Bible Quiz"));
    }

    @Test
    public void disabledReplyNamesTheSwitchAndReadsNothing() {
        Reply reply = AppContextComposer.disabled();
        assertTrue(reply.text.contains("Choose your guide"));
        assertFalse(reply.hasAction());
    }

    @Test
    public void everyDataReplyEndsWithThePrivacyLine() {
        Reply reply = AppContextComposer.compose(route("What is my streak?"), sample(), "What is my streak?");
        assertTrue(reply.text.endsWith(AppContextComposer.FOOTER));
    }

    @Test
    public void longAnswersStayWithinTheLimit() {
        AppSnapshot s = sample();
        for (int i = 0; i < 40; i++) s.joinedGroups.add("Group number " + i + " with a long name for testing");
        Reply reply = AppContextComposer.compose(route("What groups have I joined?"), s, "What groups have I joined?");
        assertTrue(reply.text.length() <= AppContextComposer.MAX_CHARS + AppContextComposer.FOOTER.length() + 1);
    }

    @Test
    public void summaryMentionsEachAreaInOneLine() {
        Reply reply = AppContextComposer.compose(route("How am I doing in the app?"), sample(), "How am I doing in the app?");
        assertTrue(reply.text.contains("Bible Quiz 2 of 3 stars"));
        assertTrue(reply.text.contains("Your streak is 6 days."));
        assertFalse(reply.hasAction());
        assertEquals(Arrays.asList("What are my game stars?", "Show my Bible notes"), reply.chips);
    }

    @Test
    public void clipKeepsPromptsShortAndCleanWhitespace() {
        assertEquals("a b", AppContextComposer.clip("a   b", 10));
        assertEquals("abcdefg...", AppContextComposer.clip("abcdefghijklmnop", 10));
    }
}
