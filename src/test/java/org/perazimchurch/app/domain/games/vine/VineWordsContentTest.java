package org.perazimchurch.app.domain.games.vine;

import org.junit.BeforeClass;
import org.junit.Test;
import org.perazimchurch.app.domain.games.engine.GameSession;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

/**
 * Checks the shipped level file: 60 levels, every grid valid, every target word in its
 * passage, and every level solvable in one run.
 */
public class VineWordsContentTest {

    private static List<VineWordsLevel> levels;

    @BeforeClass public static void load() throws IOException {
        File file = new File("src/main/assets/games/vine_words.json");
        String json = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
        levels = VineWordsLevels.parse(json);
    }

    @Test public void shipsTheAdvertisedNumberOfLevels() {
        assertEquals(VineWordsLevels.LEVEL_COUNT, levels.size());
        for (int i = 0; i < levels.size(); i++) assertEquals(i + 1, levels.get(i).number());
    }

    @Test public void everyLevelIsSolvableInOneRun() {
        for (VineWordsLevel level : levels) {
            VineWordsSession session = new VineWordsSession(level);
            for (String word : level.targetWords()) {
                assertEquals("level " + level.number() + " " + word,
                        GameSession.Outcome.CORRECT, session.answer(word));
            }
            assertTrue("level " + level.number() + " should finish", session.finished());
        }
    }

    @Test public void everyGridIsAValidCrossword() {
        for (VineWordsLevel level : levels) {
            assertTrue("level " + level.number(), level.grid().isValid());
        }
    }

    @Test public void everyTargetAppearsInItsPassage() {
        for (VineWordsLevel level : levels) {
            Set<String> tokens = new HashSet<>();
            for (VineWordsLevel.Verse verse : level.verses()) {
                for (String token : verse.text.split("[^A-Za-z]+")) {
                    if (!token.isEmpty()) tokens.add(token.toUpperCase(Locale.ROOT));
                }
            }
            for (String word : level.targetWords()) {
                assertTrue("level " + level.number() + ": " + word + " not in passage " + level.passage(),
                        tokens.contains(word));
            }
        }
    }

    @Test public void everyTargetCanBeSpelledFromTheWheel() {
        for (VineWordsLevel level : levels) {
            for (String word : level.targetWords()) {
                assertTrue("level " + level.number() + " " + word,
                        VineWordsRules.spellable(word, level.wheel()));
            }
            if (level.bonus() != null) {
                assertTrue("bonus on level " + level.number(),
                        VineWordsRules.spellable(level.bonus(), level.wheel()));
                assertFalse("bonus repeats a target on level " + level.number(),
                        level.targetWords().contains(level.bonus()));
            }
        }
    }

    @Test public void wheelHasAtMostTwentyLettersAndTheRightBands() {
        for (VineWordsLevel level : levels) {
            assertTrue("level " + level.number(), level.wheel().length() <= 20);
            assertTrue("band " + level.band(), level.band() >= 1 && level.band() <= 3);
        }
    }

    @Test public void wheelOrderIsShuffledNotAlphabetical() {
        for (VineWordsLevel level : levels) {
            List<Character> order = level.wheelOrder(level.seed());
            assertEquals(level.wheel().length(), order.size());
            List<Character> again = level.wheelOrder(level.seed());
            assertEquals("same seed, same order", order, again);
            if (order.size() > 3) {
                assertNotEquals("level " + level.number(), level.wheel(), join(order));
            }
        }
    }

    private static String join(List<Character> letters) {
        StringBuilder out = new StringBuilder();
        for (char ch : letters) out.append(ch);
        return out.toString();
    }
}
