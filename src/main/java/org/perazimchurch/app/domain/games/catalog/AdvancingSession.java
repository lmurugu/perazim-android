package org.perazimchurch.app.domain.games.catalog;

import org.perazimchurch.app.domain.games.engine.GameSession;

/**
 * Wraps a game whose class already advances after each answer. Keeps the existing logic as it is;
 * the adapter only maps it onto {@link GameSession}. No hints and no lives yet.
 */
abstract class AdvancingSession implements GameSession {

    /** Submits an answer to the wrapped game. Returns true when it was right, and it advances. */
    protected abstract boolean submit(String value);

    /** Whether the wrapped game still has a question to answer. */
    protected abstract boolean hasQuestion();

    @Override
    public final Outcome answer(String value) {
        if (!hasQuestion()) return Outcome.IGNORED;
        return submit(value) ? Outcome.CORRECT : Outcome.WRONG;
    }

    @Override
    public final int lives() {
        return NO_LIVES;
    }

    @Override
    public final boolean hintAvailable() {
        return false;
    }

    @Override
    public final String hint() {
        return null;
    }
}
