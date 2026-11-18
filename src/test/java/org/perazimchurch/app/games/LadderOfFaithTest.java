package org.perazimchurch.app.games;

import org.junit.Test;
import org.perazimchurch.app.domain.games.BibleGames;
import org.perazimchurch.app.domain.games.BibleGames.Card;
import org.perazimchurch.app.domain.games.BibleGames.Mode;
import org.perazimchurch.app.domain.games.LadderOfFaith;
import org.perazimchurch.app.domain.games.LadderOfFaith.Event;

import java.util.List;

import static org.junit.Assert.*;

public class LadderOfFaithTest {

    @Test public void everyRungHasAnswerAmongFourOptions() {
        List<Card> rungs = BibleGames.cards(Mode.LADDER);
        assertEquals(LadderOfFaith.RUNGS, rungs.size());
        for (Card card : rungs) {
            assertEquals("options for: " + card.prompt, 4, card.options.size());
            assertTrue("answer not among options: " + card.prompt, card.options.contains(card.answer));
            assertFalse("empty reference: " + card.prompt, card.reference.isEmpty());
        }
    }

    @Test public void correctAnswerAdvancesAndScores() {
        LadderOfFaith run = new LadderOfFaith();
        assertTrue(run.answer(run.current().answer));
        assertEquals(1, run.score());
        assertTrue(run.next());
        assertEquals(2, run.position());
        assertFalse(run.answered());
    }

    @Test public void nextRequiresAnswer() {
        LadderOfFaith run = new LadderOfFaith();
        assertFalse(run.next());
        assertEquals(1, run.position());
    }

    @Test public void wrongAnswerCostsLifeAndAnswerCannotBeRepeated() {
        LadderOfFaith run = new LadderOfFaith();
        assertFalse(run.answer("definitely not an option"));
        assertEquals(LadderOfFaith.LIVES - 1, run.lives());
        assertFalse(run.answer(run.current().answer));
        assertEquals(0, run.score());
    }

    @Test public void threeWrongAnswersEndRun() {
        LadderOfFaith run = new LadderOfFaith();
        for (int i = 0; i < LadderOfFaith.LIVES; i++) {
            run.answer("wrong");
            if (!run.finished()) run.next();
        }
        assertTrue(run.finished());
        assertEquals(0, run.lives());
    }

    @Test public void milestonesAreRungsFiveTenFifteenTwenty() {
        assertTrue(LadderOfFaith.isMilestone(5));
        assertTrue(LadderOfFaith.isMilestone(20));
        assertFalse(LadderOfFaith.isMilestone(6));
        assertFalse(LadderOfFaith.isMilestone(1));
    }

    @Test public void correctMilestoneEmitsCelebrationEvent() {
        LadderOfFaith run = new LadderOfFaith();
        for (int i = 0; i < 4; i++) { run.answer(run.current().answer); run.next(); }
        run.drainEvents();
        run.answer(run.current().answer);
        assertTrue(run.milestone());
        assertTrue(run.drainEvents().contains(Event.MILESTONE));
    }

    @Test public void fiftyFiftyHidesTwoWrongOptionsOnce() {
        LadderOfFaith run = new LadderOfFaith();
        assertTrue(run.useFiftyFifty());
        List<String> hidden = run.hiddenOptions();
        assertEquals(2, hidden.size());
        assertFalse(hidden.contains(run.current().answer));
        assertFalse(run.useFiftyFifty());
    }

    @Test public void peekAndPassAreOneTimeLifelines() {
        LadderOfFaith run = new LadderOfFaith();
        assertTrue(run.usePeek());
        assertTrue(run.peeked());
        assertFalse(run.usePeek());

        assertTrue(run.usePass());
        assertEquals(2, run.position());
        assertEquals(LadderOfFaith.LIVES, run.lives());
        assertFalse(run.usePass());
    }

    @Test public void lifelinesCannotBeUsedAfterAnswering() {
        LadderOfFaith run = new LadderOfFaith();
        run.answer(run.current().answer);
        assertFalse(run.useFiftyFifty());
        assertFalse(run.usePeek());
        assertFalse(run.usePass());
    }

    @Test public void completingTopRungFinishesRun() {
        LadderOfFaith run = new LadderOfFaith();
        for (int i = 0; i < LadderOfFaith.RUNGS; i++) {
            assertTrue(run.answer(run.current().answer));
            if (i < LadderOfFaith.RUNGS - 1) assertTrue(run.next());
        }
        assertTrue(run.finished());
        assertEquals(LadderOfFaith.RUNGS, run.score());
        assertTrue(run.drainEvents().contains(Event.RUN_COMPLETE));
        assertFalse(run.next());
    }

    @Test public void drainEventsClearsQueue() {
        LadderOfFaith run = new LadderOfFaith();
        run.answer("wrong");
        assertTrue(run.drainEvents().contains(Event.LIFE_LOST));
        assertTrue(run.drainEvents().isEmpty());
    }
}
