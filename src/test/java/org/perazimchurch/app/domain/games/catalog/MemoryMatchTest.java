package org.perazimchurch.app.domain.games.catalog;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.Test;
import org.perazimchurch.app.domain.games.engine.GameSession;

public class MemoryMatchTest {

    private final MemoryDefinition game = new MemoryDefinition(0);

    /** The two card indexes that belong to a pair. */
    private static List<Integer> cardsOfPair(MemorySession run, int pair) {
        List<Integer> out = new ArrayList<>();
        for (int i = 0; i < run.cards().size(); i++) {
            if (run.cards().get(i).pair == pair) out.add(i);
        }
        return out;
    }

    /** Flips both cards of a pair and returns the points it earned. */
    private static int matchPair(MemorySession run, int pair) {
        List<Integer> cards = cardsOfPair(run, pair);
        int before = run.score();
        run.answer(String.valueOf(cards.get(0)));
        run.answer(String.valueOf(cards.get(1)));
        return run.score() - before;
    }

    @Test public void offersTwelveLevelsInThreeTiers() {
        assertEquals(12, game.levelCount());
        assertEquals("Easy 1", game.levelName(1));
        assertEquals("Medium 1", game.levelName(5));
        assertEquals("Hard 4", game.levelName(12));
        assertEquals(3, MemoryDefinition.pairsFor(MemoryDefinition.Tier.EASY));
        assertEquals(5, MemoryDefinition.pairsFor(MemoryDefinition.Tier.HARD));
    }

    @Test public void bankHasEnoughSetsOfDistinctWords() {
        assertTrue(MemoryBank.SETS.size() >= MemoryDefinition.LEVELS);
        Set<String> ids = new HashSet<>();
        for (MemoryBank.PairSet s : MemoryBank.SETS) {
            assertTrue("duplicate id " + s.id, ids.add(s.id));
            assertTrue("fewer than 5 pairs: " + s.title, s.pairs.size() >= 5);
            Set<String> words = new HashSet<>();
            for (MemoryBank.Pair p : s.pairs) {
                assertFalse(p.a.equals(p.b));
                assertTrue("repeated " + p.a + " in " + s.title, words.add(p.a));
                assertTrue("repeated " + p.b + " in " + s.title, words.add(p.b));
                assertFalse(p.reference.isEmpty());
            }
        }
    }

    @Test public void firstFlipMovesAndMatchingSecondFlipIsCorrect() {
        MemorySession run = (MemorySession) game.createSession(1, 5L);
        assertEquals(3, run.size());
        List<Integer> cards = cardsOfPair(run, 0);
        assertEquals(GameSession.Outcome.MOVED, run.answer(String.valueOf(cards.get(0))));
        assertEquals(GameSession.Outcome.CORRECT, run.answer(String.valueOf(cards.get(1))));
        assertTrue(run.isMatched(cards.get(0)) && run.isMatched(cards.get(1)));
        assertEquals(2, run.position());
        assertTrue(run.lastFeedback().contains("("));
    }

    @Test public void mismatchIsWrongAndResetsCombo() {
        MemorySession run = (MemorySession) game.createSession(1, 5L);
        matchPair(run, 0);
        assertEquals(1, run.combo());
        int a = cardsOfPair(run, 1).get(0);
        int b = cardsOfPair(run, 2).get(0);
        assertEquals(GameSession.Outcome.MOVED, run.answer(String.valueOf(a)));
        assertEquals(GameSession.Outcome.WRONG, run.answer(String.valueOf(b)));
        assertFalse(run.isMatched(a));
        assertEquals(0, run.combo());
        assertFalse(run.finished());
    }

    @Test public void flippingTheSameOrAMatchedCardIsIgnored() {
        MemorySession run = (MemorySession) game.createSession(2, 5L);
        run.answer("0");
        assertEquals(GameSession.Outcome.IGNORED, run.answer("0"));
        assertEquals(GameSession.Outcome.IGNORED, run.answer("99"));
        assertEquals(GameSession.Outcome.IGNORED, run.answer("x"));
    }

    @Test public void everyPairCanBeFoundAndTheRunFinishes() {
        MemorySession run = (MemorySession) game.createSession(9, 13L);
        for (int pair = 0; pair < run.size(); pair++) {
            assertTrue(matchPair(run, pair) > 0);
        }
        assertTrue(run.finished());
        assertNull(run.prompt());
        assertEquals(GameSession.Outcome.IGNORED, run.answer("0"));
    }

    @Test public void comboBonusGrowsAcrossPairs() {
        MemorySession run = (MemorySession) game.createSession(1, 21L);
        assertEquals(MemorySession.BASE_POINTS, matchPair(run, 0));
        assertEquals(MemorySession.BASE_POINTS + MemorySession.COMBO_STEP, matchPair(run, 1));
    }

    @Test public void hintsOnEasyAndMediumOnly() {
        MemorySession easy = (MemorySession) game.createSession(1, 3L);
        assertTrue(easy.hintAvailable());
        assertTrue(easy.hint().startsWith("A pair still face down starts with "));
        assertFalse(easy.hintAvailable());
        MemorySession hard = (MemorySession) game.createSession(12, 3L);
        assertFalse(hard.hintAvailable());
        assertNull(hard.hint());
    }
}
