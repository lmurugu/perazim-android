package org.perazimchurch.app.domain.games.vine;

import org.junit.Test;
import org.perazimchurch.app.domain.games.engine.GameSession;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class VineWordsSessionTest {

    /** VINE across and NET down share the N. Wheel holds E, I, N, T, V, A, E, N... spelled from the words. */
    private static VineWordsLevel level(String bonus) {
        CrosswordGrid grid = CrosswordGrid.of(3, 4, Arrays.asList(
                new CrosswordGrid.Slot("VINE", 0, 0, true),
                new CrosswordGrid.Slot("NET", 0, 2, false)));
        return new VineWordsLevel(1, 1, "Test 1:1", 42L, Arrays.asList(
                new VineWordsLevel.Verse("Test 1:1", "The vine and the net.")),
                grid, "ENTVIEBA", bonus);
    }

    @Test public void solvingEveryTargetFinishesTheRun() {
        VineWordsSession session = new VineWordsSession(level(null));
        assertEquals(2, session.size());
        assertEquals(GameSession.Outcome.CORRECT, session.answer("vine"));
        assertFalse(session.finished());
        assertEquals(GameSession.Outcome.CORRECT, session.answer("NET"));
        assertTrue(session.finished());
        assertEquals(2, session.score());
        assertEquals(3, session.stars());
    }

    @Test public void swipingAWordTwiceIsIgnoredTheSecondTime() {
        VineWordsSession session = new VineWordsSession(level(null));
        session.answer("VINE");
        assertEquals(GameSession.Outcome.IGNORED, session.answer("VINE"));
        assertEquals(1, session.score());
    }

    @Test public void wordThatMatchesNoSlotIsIgnoredNotPenalised() {
        VineWordsSession session = new VineWordsSession(level(null));
        assertEquals(GameSession.Outcome.IGNORED, session.answer("ZEBRA"));
        assertEquals(0, session.score());
        assertEquals(GameSession.NO_LIVES, session.lives());
    }

    @Test public void swipeNeedingLettersNotOnTheWheelIsIgnored() {
        VineWordsSession session = new VineWordsSession(level(null));
        assertEquals(GameSession.Outcome.IGNORED, session.answer("VINES"));
    }

    @Test public void bonusWordScoresOnceAndNeverFinishesTheRun() {
        VineWordsSession session = new VineWordsSession(level("BEAN"));
        assertEquals(GameSession.Outcome.CORRECT, session.answer("BEAN"));
        assertEquals(GameSession.Outcome.IGNORED, session.answer("BEAN"));
        assertEquals(1, session.score());
        assertFalse(session.finished());
    }

    @Test public void hintRevealsOneLetterPerWordAndCapsStarsAtTwo() {
        VineWordsSession session = new VineWordsSession(level(null));
        assertTrue(session.hintAvailable());
        String first = session.hint();
        assertTrue(first.contains("4 letters"));
        assertEquals('V', session.knownLetter(0, 0));
        assertEquals(1, session.hintsUsed());
        session.hint();
        assertFalse(session.hintAvailable());
        assertNull(session.hint());
        session.answer("VINE");
        session.answer("NET");
        assertEquals("hints cap the star rating at two", 2, session.stars());
    }

    @Test public void solvedWordFillsItsCellsAndSharedLetterShowsOnce() {
        VineWordsSession session = new VineWordsSession(level(null));
        assertEquals(0, session.knownLetter(0, 2));
        session.answer("VINE");
        assertEquals('N', session.knownLetter(0, 2));
        assertEquals("VINE", session.lastSolved());
        session.answer("NET");
        assertEquals('T', session.knownLetter(2, 2));
    }

    @Test public void answersAfterTheRunFinishesAreIgnored() {
        VineWordsSession session = new VineWordsSession(level(null));
        session.answer("VINE");
        session.answer("NET");
        assertEquals(GameSession.Outcome.IGNORED, session.answer("VINE"));
        assertEquals(2, session.position());
    }
}
