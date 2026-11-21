package org.perazimchurch.app.domain.games.engine;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;
import org.junit.Test;

public class LevelProgressionTest {

    /** In-memory store so the test runs without Android. */
    private static final class MemoryStore implements KeyValueStore {
        final Map<String, Integer> values = new HashMap<>();

        @Override public int getInt(String key, int defaultValue) {
            Integer value = values.get(key);
            return value == null ? defaultValue : value;
        }

        @Override public void putInt(String key, int value) {
            values.put(key, value);
        }
    }

    private final LevelProgression progression = new LevelProgression(new MemoryStore());

    @Test
    public void levelOneIsUnlockedFromTheStart() {
        assertEquals(1, progression.unlockedLevel("quiz"));
        assertEquals(0, progression.bestStars("quiz", 1));
    }

    @Test
    public void passingLevelOneUnlocksLevelTwo() {
        assertTrue(progression.record("quiz", 1, 2, 3));
        assertEquals(2, progression.unlockedLevel("quiz"));
    }

    @Test
    public void oneStarDoesNotUnlockTheNextLevel() {
        assertFalse(progression.record("quiz", 1, 1, 3));
        assertEquals(1, progression.unlockedLevel("quiz"));
    }

    @Test
    public void bestStarsOnlyGoUp() {
        progression.record("quiz", 1, 3, 3);
        progression.record("quiz", 1, 1, 3);
        assertEquals(3, progression.bestStars("quiz", 1));
    }

    @Test
    public void unlockNeverGoesPastTheLastLevel() {
        progression.record("quiz", 1, 3, 2);
        progression.record("quiz", 2, 3, 2);
        assertEquals(2, progression.unlockedLevel("quiz"));
    }

    @Test
    public void playingALockedLevelDoesNotUnlockAnything() {
        assertFalse(progression.record("quiz", 2, 3, 3));
        assertEquals(1, progression.unlockedLevel("quiz"));
    }

    @Test
    public void gamesKeepSeparateProgress() {
        progression.record("quiz", 1, 3, 3);
        assertEquals(1, progression.unlockedLevel("who_said_it"));
        assertEquals(0, progression.bestStars("who_said_it", 1));
    }

    @Test(expected = IllegalArgumentException.class)
    public void levelOutsideTheGameIsRejected() {
        progression.record("quiz", 4, 3, 3);
    }
}
