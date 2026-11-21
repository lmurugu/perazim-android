package org.perazimchurch.app.domain.games.mosaic;

import org.perazimchurch.app.domain.games.engine.GameSession;
import org.perazimchurch.app.domain.games.engine.StarRule;

/**
 * Story Mosaic's star rule. Score is pieces placed without a wrong drop, so wrong drops are
 * {@code size - score}. 3 stars: no wrong drops. 2 stars: at most one piece in three was dropped
 * wrong. 1 star: anything else. Pure Java.
 */
public final class MosaicStars implements StarRule {

    public static final MosaicStars RULE = new MosaicStars();

    @Override
    public int stars(GameSession session) {
        return starsFor(session.score(), session.size());
    }

    /** The rule on plain numbers, so it can be tested without a session. */
    public static int starsFor(int score, int size) {
        if (size < 1) throw new IllegalArgumentException("size must be at least 1");
        int clamped = Math.max(0, Math.min(score, size));
        int wrong = size - clamped;
        if (wrong == 0) return 3;
        if (wrong * 3 <= size) return 2;
        return 1;
    }
}
