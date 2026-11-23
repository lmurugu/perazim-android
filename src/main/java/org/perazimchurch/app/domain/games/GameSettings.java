package org.perazimchurch.app.domain.games;

/**
 * Game settings the church can tune without an app update. Every value has a built-in default,
 * so the app works offline or before the first download. Out-of-range values fall back to the default.
 */
public final class GameSettings {
    public static final int DEFAULT_DAILY_STAR_CAP = 50;
    public static final int DEFAULT_UNSEEN_PERCENT = 50;
    public static final int DEFAULT_MISSED_PERCENT = 30;

    public final int dailyStarCap;
    public final int unseenPercent;
    public final int missedPercent;

    private GameSettings(int dailyStarCap, int unseenPercent, int missedPercent) {
        this.dailyStarCap = dailyStarCap;
        this.unseenPercent = unseenPercent;
        this.missedPercent = missedPercent;
    }

    /** The built-in defaults. */
    public static GameSettings defaults() {
        return new GameSettings(DEFAULT_DAILY_STAR_CAP, DEFAULT_UNSEEN_PERCENT, DEFAULT_MISSED_PERCENT);
    }

    /**
     * Builds settings from values that may be missing (pass null) or out of range. A cap must be 0 to 200.
     * The two question shares must each be 0 to 100 and add up to 100 or less; otherwise both use defaults.
     */
    public static GameSettings of(Integer dailyStarCap, Integer unseenPercent, Integer missedPercent) {
        int cap = dailyStarCap != null && dailyStarCap >= 0 && dailyStarCap <= 200 ? dailyStarCap : DEFAULT_DAILY_STAR_CAP;
        int unseen = DEFAULT_UNSEEN_PERCENT, missed = DEFAULT_MISSED_PERCENT;
        if (unseenPercent != null && missedPercent != null
                && unseenPercent >= 0 && missedPercent >= 0 && unseenPercent + missedPercent <= 100) {
            unseen = unseenPercent;
            missed = missedPercent;
        }
        return new GameSettings(cap, unseen, missed);
    }
}
