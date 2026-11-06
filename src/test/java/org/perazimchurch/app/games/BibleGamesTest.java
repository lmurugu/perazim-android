package org.perazimchurch.app.games;

import org.junit.Test;
import org.perazimchurch.app.domain.games.BibleGames;
import org.perazimchurch.app.domain.games.BibleGames.Card;
import org.perazimchurch.app.domain.games.BibleGames.Mode;
import org.perazimchurch.app.domain.games.BibleGames.Round;
import org.perazimchurch.app.domain.games.BibleGames.VersePuzzle;

import java.util.HashSet;
import java.util.List;

import static org.junit.Assert.*;

public class BibleGamesTest {
    @Test public void roundRequiresAnswerBeforeAdvancing() {
        Round round = new Round(Mode.QUIZ, 1);
        assertFalse(round.next());
        assertEquals(1, round.position());
        assertEquals(0, round.score());
    }

    @Test public void fiveAnswersFinishRoundWithoutDuplicateScore() {
        Round round = new Round(Mode.QUIZ, 2);
        for (int i = 0; i < 5; i++) {
            String answer = round.current().answer;
            assertTrue(round.answer(answer));
            assertFalse(round.answer(answer));
            assertEquals(i + 1, round.score());
            assertTrue(round.next());
        }
        assertTrue(round.finished());
        assertFalse(round.answer("anything"));
        assertFalse(round.next());
    }

    @Test public void wrongAnswerCannotBeChangedForExtraCredit() {
        Round round = new Round(Mode.WHO_AM_I, 3);
        String correct = round.current().answer;
        assertFalse(round.answer("not a character"));
        assertTrue(round.answered());
        assertFalse(round.correct());
        assertFalse(round.answer(correct));
        assertEquals(0, round.score());
    }

    @Test public void eachRoundHasFiveDistinctCardsAndSeededReplay() {
        for (Mode mode : Mode.values()) {
            Round first = new Round(mode, 10);
            Round replay = new Round(mode, 10);
            HashSet<Card> unique = new HashSet<>();
            for (int i = 0; i < first.size(); i++) {
                assertSame(first.current(), replay.current());
                unique.add(first.current());
                first.answer(""); replay.answer("");
                first.next(); replay.next();
            }
            assertEquals(5, unique.size());
        }
    }

    @Test public void characterAliasesIgnoreCaseAndSpaces() {
        Card moses = BibleGames.cards(Mode.WHO_AM_I).get(0);
        assertTrue(moses.accepts("  MUSA  "));
        assertTrue(moses.accepts("Moses!"));
        assertFalse(moses.accepts("Moses and Aaron"));
    }

    @Test public void allQuizChoicesContainTheCorrectAnswer() {
        for (Card card : BibleGames.cards(Mode.QUIZ)) {
            assertEquals(4, card.options.size());
            assertTrue(card.options.contains(card.answer));
            assertFalse(card.reference.isEmpty());
            assertFalse(card.explanation.isEmpty());
        }
    }

    @Test public void puzzleKeepsRepeatedWordsAsDistinctTiles() {
        List<Card> verses = BibleGames.cards(Mode.VERSE_ORDER);
        Card verse = verses.get(verses.size() - 1);
        VersePuzzle puzzle = new VersePuzzle(verse, 12);
        List<String> words = verse.words();
        assertEquals(words.size(), new HashSet<>(puzzle.tiles()).size());
        int firstA = words.indexOf("a");
        int secondA = words.lastIndexOf("a");
        assertNotEquals(firstA, secondA);
        assertTrue(puzzle.choose(firstA));
        assertFalse(puzzle.choose(firstA));
        assertTrue(puzzle.choose(secondA));
        assertFalse(puzzle.choose(-1));
        assertFalse(puzzle.choose(words.size()));
    }

    @Test public void puzzleCanBeSolvedAndUndoAllowsReusingTile() {
        Card verse = BibleGames.cards(Mode.VERSE_ORDER).get(0);
        VersePuzzle puzzle = new VersePuzzle(verse, 6);
        assertFalse(puzzle.complete());
        for (int i = 0; i < verse.words().size(); i++) assertTrue(puzzle.choose(i));
        assertTrue(puzzle.complete());
        assertEquals(verse.answer, puzzle.sentence());
        assertTrue(verse.accepts(puzzle.sentence()));
        puzzle.undo();
        assertFalse(puzzle.complete());
        assertTrue(puzzle.choose(verse.words().size() - 1));
        puzzle.clear();
        assertEquals("", puzzle.sentence());
    }

    @Test public void puzzleStartsShuffledAndDoesNotExposeMutableOrder() {
        Card verse = BibleGames.cards(Mode.VERSE_ORDER).get(0);
        VersePuzzle puzzle = new VersePuzzle(verse, 6);
        boolean shuffled = false;
        for (int i = 0; i < puzzle.tiles().size(); i++) if (puzzle.tiles().get(i) != i) shuffled = true;
        assertTrue(shuffled);
        try { puzzle.tiles().clear(); fail("immutable order expected"); }
        catch (UnsupportedOperationException expected) { }
    }

    @Test public void answerNormalizationAcceptsPunctuationButNotWrongWordOrder() {
        Card verse = BibleGames.cards(Mode.VERSE_ORDER).get(0);
        assertTrue(verse.accepts("the lord is my shepherd i shall not want"));
        assertFalse(verse.accepts("my shepherd is the lord i shall not want"));
        assertFalse(verse.accepts(null));
    }
}
