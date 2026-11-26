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
        return record(gameId, level, stars, levelCount, stars >= PASS_STARS);
    }

    /** Records the rating and unlocks according to the game's own completion rule. */
    public boolean record(String gameId, int level, int stars, int levelCount, boolean cleared) {
        if (level < 1 || levelCount < 1 || level > levelCount) {
            throw new IllegalArgumentException("level out of range");
        }
        boolean wasCleared = cleared(gameId, level);
        int clampedStars = Math.max(0, Math.min(3, stars));
        if (clampedStars > bestStars(gameId, level)) {
            store.putInt(starsKey(gameId, level), clampedStars);
        }
        if (cleared) store.putInt(clearKey(gameId, level), 1);
        else if (store.getInt(clearKey(gameId, level), 0) == 0) {
            store.putInt(clearKey(gameId, level), wasCleared ? 1 : -1);
        }
        int unlocked = unlockedLevel(gameId);
        if (cleared && level == unlocked && level < levelCount) {
            store.putInt(unlockKey(gameId), level + 1);
            return true;
        }
        return false;
    }

    /** Older two-star results already passed under the original rule. */
    public boolean cleared(String gameId, int level) {
        int explicit = store.getInt(clearKey(gameId, level), 0);
        return explicit == 1 || (explicit == 0 && bestStars(gameId, level) >= PASS_STARS);
    }

    private static String clearKey(String gameId, int level) {
        return "games.cleared." + gameId + "." + level;
    }

    private static String unlockKey(String gameId) {
        return "games.unlocked." + gameId;
    }

    private static String starsKey(String gameId, int level) {
        return "games.stars." + gameId + "." + level;
    }
}
