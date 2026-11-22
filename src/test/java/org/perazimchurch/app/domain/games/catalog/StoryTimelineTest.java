package org.perazimchurch.app.domain.games.catalog;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.Test;
import org.perazimchurch.app.domain.games.engine.GameSession;

public class StoryTimelineTest {

    private final TimelineDefinition game = new TimelineDefinition(0);

    private static List<StoryChapterBank.Event> firstFour() {
        return StoryChapterBank.CHAPTERS.get(0).events.subList(0, 4);
    }

    @Test public void offersTwelveLevelsInThreeTiers() {
        assertEquals(12, game.levelCount());
        assertEquals("Easy 1", game.levelName(1));
        assertEquals("Medium 1", game.levelName(5));
        assertEquals("Hard 4", game.levelName(12));
        assertEquals(TimelineDefinition.Tier.HARD, TimelineDefinition.tier(9));
        assertEquals(4, TimelineDefinition.eventsFor(TimelineDefinition.Tier.EASY));
        assertEquals(5, TimelineDefinition.eventsFor(TimelineDefinition.Tier.HARD));
    }

    @Test public void chapterBankIsLargeAndInTextOrder() {
        assertTrue(StoryChapterBank.CHAPTERS.size() >= 40);
        Set<String> ids = new HashSet<>();
        for (StoryChapterBank.Chapter c : StoryChapterBank.CHAPTERS) {
            assertTrue("duplicate id " + c.id, ids.add(c.id));
            assertTrue("fewer than 4 events: " + c.title, c.events.size() >= 4);
            Set<String> titles = new HashSet<>();
            for (StoryChapterBank.Event e : c.events) {
                assertTrue("duplicate event in " + c.title, titles.add(e.title));
                assertFalse(e.reference.isEmpty());
            }
        }
    }

    @Test public void bookNamesComeFromReferences() {
        assertEquals("Genesis", StoryTimelineSession.bookOf("Genesis 1:1"));
        assertEquals("1 Samuel", StoryTimelineSession.bookOf("1 Samuel 17:50"));
    }

    @Test public void tappingInOrderFinishesTheRun() {
        StoryTimelineSession run = new StoryTimelineSession(firstFour(), TimelineDefinition.Tier.EASY, 9L);
        assertEquals(4, run.size());
        for (StoryChapterBank.Event e : firstFour()) {
            assertNotNull(run.prompt());
            assertEquals(GameSession.Outcome.CORRECT, run.answer(String.valueOf(run.shuffled().indexOf(e))));
        }
        assertTrue(run.finished());
        assertNull(run.prompt());
        assertEquals(GameSession.Outcome.IGNORED, run.answer("0"));
    }

    @Test public void wrongTapDoesNotAdvanceAndResetsCombo() {
        StoryTimelineSession run = new StoryTimelineSession(firstFour(), TimelineDefinition.Tier.EASY, 9L);
        int first = run.shuffled().indexOf(firstFour().get(0));
        int second = run.shuffled().indexOf(firstFour().get(1));
        assertEquals(GameSession.Outcome.CORRECT, run.answer(String.valueOf(first)));
        assertEquals(GameSession.Outcome.WRONG, run.answer(String.valueOf(first)));
        assertEquals(2, run.position());
        assertEquals(0, run.combo());
        assertEquals(GameSession.Outcome.CORRECT, run.answer(String.valueOf(second)));
        assertTrue(run.lastFeedback().contains(firstFour().get(1).reference));
    }

    @Test public void comboBonusGrowsWithinARun() {
        StoryTimelineSession run = new StoryTimelineSession(firstFour(), TimelineDefinition.Tier.HARD, 9L);
        run.answer(String.valueOf(run.shuffled().indexOf(firstFour().get(0))));
        assertEquals(StoryTimelineSession.BASE_POINTS, run.score());
        run.answer(String.valueOf(run.shuffled().indexOf(firstFour().get(1))));
        assertEquals(StoryTimelineSession.BASE_POINTS * 2 + StoryTimelineSession.COMBO_STEP, run.score());
    }

    @Test public void hintsOnEasyAndMediumOnlyAndNamesTheBook() {
        StoryTimelineSession easy = new StoryTimelineSession(firstFour(), TimelineDefinition.Tier.EASY, 9L);
        assertTrue(easy.hintAvailable());
        assertEquals("Look in " + StoryTimelineSession.bookOf(firstFour().get(0).reference) + ".", easy.hint());
        assertFalse(easy.hintAvailable());
        StoryTimelineSession hard = new StoryTimelineSession(firstFour(), TimelineDefinition.Tier.HARD, 9L);
        assertFalse(hard.hintAvailable());
        assertNull(hard.hint());
    }

    @Test public void everyLevelBuildsAShuffledRunOfTheRightSize() {
        for (int level = 1; level <= game.levelCount(); level++) {
            GameSession session = game.createSession(level, 3L);
            int expected = TimelineDefinition.eventsFor(TimelineDefinition.tier(level));
            assertEquals(expected, session.size());
            assertEquals(expected, ((StoryTimelineSession) session).shuffled().size());
        }
    }

    @Test public void invalidIndexIsIgnored() {
        StoryTimelineSession run = new StoryTimelineSession(firstFour(), TimelineDefinition.Tier.EASY, 9L);
        assertEquals(GameSession.Outcome.IGNORED, run.answer("99"));
        assertEquals(GameSession.Outcome.IGNORED, run.answer("x"));
    }
}
