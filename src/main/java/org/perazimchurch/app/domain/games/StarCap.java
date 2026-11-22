package org.perazimchurch.app.domain.games;

/** Limits how many stars one player can earn in one local day. Stops rewards becoming a grind. */
public final class StarCap {
    /** Default daily cap agreed with Levi on 2026-11-21 (Q2). */
    public static final int DEFAULT_DAILY_CAP = 50;

    private StarCap() { }

    /**
     * Returns how many of the {@code requested} stars may be added today, given {@code earnedToday}
     * already counted. Never negative and never above the cap.
     */
    public static int allowed(int earnedToday, int requested, int dailyCap) {
        if (requested <= 0 || dailyCap <= 0) return 0;
        int room = Math.max(0, dailyCap - Math.max(0, earnedToday));
        return Math.min(requested, room);
    }
}
