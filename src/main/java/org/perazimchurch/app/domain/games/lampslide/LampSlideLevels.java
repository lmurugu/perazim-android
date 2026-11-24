package org.perazimchurch.app.domain.games.lampslide;

/** Level rules for Lamp Slide: 3 by 3 for levels 1 to 10, 4 by 4 for 11 to 25, 5 by 5 for 26 to 40. */
public final class LampSlideLevels {

    public static final int LEVEL_COUNT = 40;

    private LampSlideLevels() {}

    /** Board side for a 1-based level. Levels outside 1 to 40 are clamped. */
    public static int boardSize(int level) {
        int clamped = Math.max(1, Math.min(LEVEL_COUNT, level));
        if (clamped <= 10) return 3;
        if (clamped <= 25) return 4;
        return 5;
    }
}
