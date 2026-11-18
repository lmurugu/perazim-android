package org.perazimchurch.app.domain.games;

/** Daily play streak for Bible games. Days are whole local days counted from the epoch. */
public final class GameStreak {
    private GameStreak() { }

    /**
     * Returns the streak after playing on {@code today}.
     * Same day keeps the streak, the next day adds one, and any gap starts again at one.
     */
    public static int next(long lastDay, long today, int streak) {
        if (lastDay == today) return Math.max(1, streak);
        if (lastDay == today - 1) return streak + 1;
        return 1;
    }
}
