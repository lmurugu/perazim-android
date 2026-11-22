package org.perazimchurch.app.domain.games.engine;

/**
 * Tracks which levels each game has unlocked and the best stars earned on each level.
 *
 * <p>Level 1 starts unlocked. Finishing the unlocked level with at least two stars unlocks the
 * next one. Best stars only ever go up. All data is local; the store is injected so this class
 * stays pure Java.
 */
public final class LevelProgression {

    /** Stars needed to pass a level and unlock the next one. */
    public static final int PASS_STARS = 2;

    private final KeyValueStore store;

    public LevelProgression(KeyValueStore store) {
        this.store = store;
    }

    /** Highest level the player may play. Always at least 1. */
    public int unlockedLevel(String gameId) {
        return Math.max(1, store.getInt(unlockKey(gameId), 1));
    }

    /** Best stars earned on a level, 0 when it has not been completed. */
    public int bestStars(String gameId, int level) {
        return store.getInt(starsKey(gameId, level), 0);
    }

    /**
     * Records a finished run.
     *
     * @param levelCount levels the game offers; unlocking never goes past it
     * @return true when this run unlocked a new level
     */
    public boolean record(String gameId, int level, int stars, int levelCount) {
        if (level < 1 || levelCount < 1 || level > levelCount) {
            throw new IllegalArgumentException("level out of range");
        }
        int clampedStars = Math.max(0, Math.min(3, stars));
        if (clampedStars > bestStars(gameId, level)) {
            store.putInt(starsKey(gameId, level), clampedStars);
        }
        int unlocked = unlockedLevel(gameId);
        if (clampedStars >= PASS_STARS && level == unlocked && level < levelCount) {
            store.putInt(unlockKey(gameId), level + 1);
            return true;
        }
        return false;
    }

    private static String unlockKey(String gameId) {
        return "games.unlocked." + gameId;
    }

    private static String starsKey(String gameId, int level) {
        return "games.stars." + gameId + "." + level;
    }
}
