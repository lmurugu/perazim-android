package org.perazimchurch.app.games;

import org.junit.Test;
import org.perazimchurch.app.domain.games.BibleGames;
import org.perazimchurch.app.domain.games.BibleGames.Card;
import org.perazimchurch.app.domain.games.BibleGames.Mode;
import org.perazimchurch.app.domain.games.LadderOfFaith;
import org.perazimchurch.app.domain.games.LadderOfFaith.Event;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.*;

public class LadderOfFaithTest {

    @Test public void bankIsLargeAndEveryQuestionIsWellFormed() {
        List<Card> bank = BibleGames.cards(Mode.LADDER);
        assertTrue("bank too small: " + bank.size(), bank.size() >= 250);
        Set<String> prompts = new HashSet<>();
        for (Card card : bank) {
            assertTrue("duplicate question: " + card.prompt, prompts.add(card.prompt));
            assertEquals("options for: " + card.prompt, 4, card.options.size());
            assertEquals("repeated option for: " + card.prompt, 4, new HashSet<>(card.options).size());
            assertTrue("answer not among options: " + card.prompt, card.options.contains(card.answer));
            assertFalse("empty reference: " + card.prompt, card.reference.trim().isEmpty());
            assertFalse("empty explanation: " + card.prompt, card.explanation.trim().isEmpty());
        }
    }

    @Test public void runHasTwentyUniqueRungsAndSameSeedRepeats() {
        List<String> first = prompts(new LadderOfFaith(42L));
        List<String> again = prompts(new LadderOfFaith(42L));
        assertEquals(LadderOfFaith.RUNGS, first.size());
        assertEquals(LadderOfFaith.RUNGS, new HashSet<>(first).size());
        assertEquals(first, again);
    }

    @Test public void differentSeedsGiveDifferentRuns() {
        assertNotEquals(prompts(new LadderOfFaith(1L)), prompts(new LadderOfFaith(2L)));
    }

    @Test public void difficultyRisesAcrossTheLadder() {
        assertEquals(1, LadderOfFaith.tierOf(1));
        assertEquals(1, LadderOfFaith.tierOf(7));
        assertEquals(2, LadderOfFaith.tierOf(8));
        assertEquals(2, LadderOfFaith.tierOf(14));
        assertEquals(3, LadderOfFaith.tierOf(15));
        assertEquals(3, LadderOfFaith.tierOf(20));
    }

    private static List<String> prompts(LadderOfFaith run) {
        List<String> result = new ArrayList<>();
        while (!run.finished()) {
            result.add(run.current().prompt);
            run.answer(run.current().answer);
            if (!run.finished()) run.next();
        }
        return result;
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
