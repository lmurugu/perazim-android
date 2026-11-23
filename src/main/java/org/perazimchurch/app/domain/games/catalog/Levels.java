package org.perazimchurch.app.domain.games.catalog;

/**
 * The level layout shared by the five games: twelve numbered levels in three bands of four.
 * Levels 1 to 4 are easy, 5 to 8 medium and 9 to 12 hard. A level always gets the same seed, so
 * a level plays the same five questions each time it is replayed.
 */
public final class Levels {
    /** Numbered levels per game. */
    public static final int COUNT = 12;
    /** Levels in each band. */
    public static final int PER_BAND = 4;
    /** Band keys, in order. */
    public static final String[] BANDS = {"easy", "medium", "hard"};

    private Levels() { }

    /** The band key for a level. Levels outside 1 to 12 are clamped. */
    public static String band(int level) {
        return BANDS[Math.min(BANDS.length - 1, (clamp(level) - 1) / PER_BAND)];
    }

    /** The fixed seed for a level, so each level has the same questions every time. */
    public static long seed(int level) {
        return 7919L * clamp(level);
    }

    /** The level number kept inside 1 to {@link #COUNT}. */
    public static int clamp(int level) {
        return Math.max(1, Math.min(COUNT, level));
    }
}
