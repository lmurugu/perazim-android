package org.perazimchurch.app.domain.games.catalog;

import org.perazimchurch.app.domain.games.engine.GameSession;
import org.perazimchurch.app.domain.games.engine.StarRule;

/**
 * Hint rules shared by the five games. A run offers at most {@link #HINTS_PER_RUN} hints, one per
 * question, and they cost nothing. A run that used any hint can earn at most
 * {@link #STAR_CAP_WITH_HINT} stars.
 */
public final class HintRules {
    /** Hints available in one run. */
    public static final int HINTS_PER_RUN = 2;
    /** The most stars a run can earn once a hint has been used. */
    public static final int STAR_CAP_WITH_HINT = 2;

    private HintRules() { }

    /** The book named in a reference such as "1 Samuel 3:5", without the chapter and verse. */
    public static String bookOf(String reference) {
        return reference.replaceAll("\\s++\\d+:\\d+.*$", "");
    }

    /** Wraps a star rule so that a hinted run is capped at {@link #STAR_CAP_WITH_HINT} stars. */
    public static StarRule capped(StarRule base) {
        return session -> session.hintsUsed() > 0
                ? Math.min(STAR_CAP_WITH_HINT, base.stars(session))
                : base.stars(session);
    }
}
