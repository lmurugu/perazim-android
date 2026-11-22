package org.perazimchurch.app.domain.games;

/**
 * Decides when reading counts as the daily routine's Read step (Levi's Q1, 2026-11-21).
 * One completed chapter is enough. Reading earns no extra XP and does not change the streak rules.
 */
public final class RoutineReadRule {
    public static final int CHAPTERS_NEEDED = 1;

    private RoutineReadRule() { }

    public static boolean readStepDone(int chaptersReadToday) {
        return chaptersReadToday >= CHAPTERS_NEEDED;
    }
}
