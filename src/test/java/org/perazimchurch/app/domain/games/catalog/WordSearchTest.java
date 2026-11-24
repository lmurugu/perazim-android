package org.perazimchurch.app.domain.games.catalog;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.Test;
import org.perazimchurch.app.domain.games.engine.GameSession;

public class WordSearchTest {

    private static final int[][] DIRS = {{0, 1}, {1, 0}, {1, 1}, {1, -1}, {0, -1}, {-1, 0}, {-1, -1}, {-1, 1}};

    private final WordSearchDefinition game = new WordSearchDefinition(0);

    /** Finds the start and end cell of a word in any straight direction, or null. */
    private static int[] locate(WordSearchGrid grid, String word) {
        int n = WordSearchGrid.SIZE;
        for (int r = 0; r < n; r++) {
            for (int c = 0; c < n; c++) {
                for (int[] d : DIRS) {
                    int endR = r + d[0] * (word.length() - 1), endC = c + d[1] * (word.length() - 1);
                    if (endR < 0 || endR >= n || endC < 0 || endC >= n) continue;
                    boolean match = true;
                    for (int i = 0; i < word.length() && match; i++) {
                        match = grid.letter(r + d[0] * i, c + d[1] * i) == word.charAt(i);
                    }
                    if (match) return new int[] {r, c, endR, endC};
                }
            }
        }
        return null;
    }

    private static String answer(int[] cells) {
        return cells[0] + "," + cells[1] + "," + cells[2] + "," + cells[3];
    }

    @Test public void offersTwelveLevelsInThreeTiers() {
        assertEquals(12, game.levelCount());
        assertEquals("Easy 1", game.levelName(1));
        assertEquals("Medium 1", game.levelName(5));
        assertEquals("Hard 4", game.levelName(12));
        assertEquals(4, WordSearchDefinition.wordsFor(WordSearchDefinition.Tier.EASY));
        assertEquals(6, WordSearchDefinition.wordsFor(WordSearchDefinition.Tier.HARD));
    }

    @Test public void bankHasFortyThemesOfSixValidWords() {
        assertTrue(WordSearchBank.THEMES.size() >= 40);
        Set<String> ids = new HashSet<>();
        for (WordSearchBank.Theme t : WordSearchBank.THEMES) {
            assertTrue("duplicate id " + t.id, ids.add(t.id));
            assertEquals("words in " + t.title, 6, t.words.size());
            Set<String> seen = new HashSet<>();
            for (WordSearchBank.Entry e : t.words) {
                assertTrue("bad word " + e.word, e.word.matches("[A-Z]{3,8}"));
                assertTrue("repeated " + e.word + " in " + t.title, seen.add(e.word));
                assertFalse(e.reference.isEmpty());
            }
        }
    }

    @Test public void everyThemeFitsItsGridForSeveralSeeds() {
        for (WordSearchBank.Theme t : WordSearchBank.THEMES) {
            List<String> words = new ArrayList<>();
            for (WordSearchBank.Entry e : t.words) words.add(e.word);
            for (long seed = 1; seed <= 5; seed++) {
                WordSearchGrid grid = new WordSearchGrid(words, seed);
                for (String w : words) {
                    assertNotNull("missing " + w + " in " + t.title, locate(grid, w));
                }
            }
        }
    }

    @Test public void sameSeedGivesSameGrid() {
        List<String> words = List.of("ARK", "NOAH", "FLOOD");
        WordSearchGrid a = new WordSearchGrid(words, 11L);
        WordSearchGrid b = new WordSearchGrid(words, 11L);
        for (int r = 0; r < WordSearchGrid.SIZE; r++) {
            for (int c = 0; c < WordSearchGrid.SIZE; c++) {
                assertEquals(a.letter(r, c), b.letter(r, c));
            }
        }
    }

    @Test public void findingEveryWordCompletesTheRun() {
        WordSearchSession run = (WordSearchSession) game.createSession(1, 3L);
        assertEquals(4, run.size());
        for (String w : run.grid().words()) {
            int[] cells = locate(run.grid(), w);
            assertEquals(GameSession.Outcome.CORRECT, run.answer(answer(cells)));
        }
        assertTrue(run.finished());
        assertEquals(GameSession.Outcome.IGNORED, run.answer("0,0,0,1"));
        assertNull(run.prompt());
    }

    @Test public void wrongSelectionResetsComboAndIsGentle() {
        WordSearchSession run = (WordSearchSession) game.createSession(1, 3L);
        String first = run.grid().words().get(0);
        assertEquals(GameSession.Outcome.CORRECT, run.answer(answer(locate(run.grid(), first))));
        assertEquals(1, run.combo());
        assertEquals(GameSession.Outcome.WRONG, run.answer("7,7,7,6"));
        if (!run.grid().found().contains(run.grid().words().get(1))) {
            assertEquals(0, run.combo());
        }
        assertTrue(run.lastFeedback().startsWith("Not a word"));
    }

    @Test public void comboBonusStepsUpWithinARun() {
        WordSearchSession run = (WordSearchSession) game.createSession(1, 7L);
        List<String> words = run.grid().words();
        run.answer(answer(locate(run.grid(), words.get(0))));
        assertEquals(WordSearchSession.BASE_POINTS, run.score());
        run.answer(answer(locate(run.grid(), words.get(1))));
        assertEquals(WordSearchSession.BASE_POINTS * 2 + WordSearchSession.COMBO_STEP, run.score());
    }

    @Test public void hintOnEasyAndMediumOnlyNamesAnInitial() {
        WordSearchSession easy = (WordSearchSession) game.createSession(1, 7L);
        assertTrue(easy.hintAvailable());
        String hint = easy.hint();
        assertTrue(hint.contains(easy.grid().words().get(0).substring(0, 1)));
        assertFalse(easy.hintAvailable());
        WordSearchSession hard = (WordSearchSession) game.createSession(12, 7L);
        assertFalse(hard.hintAvailable());
        assertNull(hard.hint());
    }

    @Test public void invalidAnswersAreIgnored() {
        WordSearchSession run = (WordSearchSession) game.createSession(2, 9L);
        assertEquals(GameSession.Outcome.IGNORED, run.answer("1,2,3"));
        assertEquals(GameSession.Outcome.IGNORED, run.answer("1,2,3,9"));
        assertEquals(GameSession.Outcome.IGNORED, run.answer("x,y,z,w"));
        assertEquals(GameSession.Outcome.IGNORED, run.answer(null));
    }
}
