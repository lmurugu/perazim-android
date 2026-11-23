package org.perazimchurch.app.domain.games.catalog;

import org.junit.Test;
import org.perazimchurch.app.domain.games.BibleGames;
import org.perazimchurch.app.domain.games.engine.GameSession;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class RoundSessionsTest {

    @Test
    public void quizChoicesIncludeTheAnswerAndKeepTheSameOrderOnRedraw() {
        long seed = 7L;
        String answer = new BibleGames.Round(BibleGames.Mode.QUIZ, seed).current().answer;
        QuizSession session = new QuizSession(new BibleGames.Round(BibleGames.Mode.QUIZ, seed), seed);
        assertTrue(session.choices().contains(answer));
        assertEquals(session.choices(), session.choices());
        assertNotNull(session.prompt());
        assertEquals(1, session.position());
    }

    @Test
    public void verseBuilderAcceptsTheVerseBuiltInOrder() {
        VerseBuilderSession session = new VerseBuilderSession(new BibleGames.Round(BibleGames.Mode.VERSE_ORDER, 3L), 3L);
        BibleGames.VersePuzzle puzzle = session.puzzle();
        assertNotNull(puzzle);
        int words = puzzle.tiles().size();
        for (int i = 0; i < words; i++) assertTrue(puzzle.choose(i));
        assertTrue(puzzle.complete());
        GameSession.Outcome outcome = session.answer(puzzle.sentence());
        assertEquals(GameSession.Outcome.CORRECT, outcome);
        assertNotNull(session.lastCard());
        assertEquals("the session has moved on to the second verse", 2, session.position());
    }

    @Test
    public void verseBuilderRejectsTheVerseBuiltInReverse() {
        VerseBuilderSession session = new VerseBuilderSession(new BibleGames.Round(BibleGames.Mode.VERSE_ORDER, 3L), 3L);
        BibleGames.VersePuzzle puzzle = session.puzzle();
        for (int i = puzzle.tiles().size() - 1; i >= 0; i--) puzzle.choose(i);
        assertEquals(GameSession.Outcome.WRONG, session.answer(puzzle.sentence()));
    }
}
