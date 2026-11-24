package org.perazimchurch.app.domain.games.catalog;

import org.junit.Test;
import org.perazimchurch.app.domain.games.engine.GameSession;
import org.perazimchurch.app.domain.games.engine.StarRule;

import static org.junit.Assert.assertEquals;

public class HintRulesTest {

    /** A session stub with just the two values the cap reads. */
    private static GameSession session(int hints) {
        return new GameSession() {
            @Override public Outcome answer(String value) { return Outcome.IGNORED; }
            @Override public int lives() { return NO_LIVES; }
            @Override public boolean hintAvailable() { return false; }
            @Override public String hint() { return null; }
            @Override public int hintsUsed() { return hints; }
            @Override public int score() { return 0; }
            @Override public int size() { return 5; }
            @Override public int position() { return 5; }
            @Override public boolean finished() { return true; }
        };
    }

    @Test
    public void unhintedRunKeepsTheBaseStars() {
        StarRule base = s -> 3;
        assertEquals(3, HintRules.capped(base).stars(session(0)));
    }

    @Test
    public void hintedRunIsCappedAtTwoStars() {
        StarRule base = s -> 3;
        assertEquals(2, HintRules.capped(base).stars(session(1)));
        assertEquals(1, HintRules.capped(s -> 1).stars(session(2)));
    }
}
