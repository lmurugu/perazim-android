package org.perazimchurch.app.domain.games.catalog;

import org.perazimchurch.app.domain.games.engine.GameSession;

/**
 * Wraps a game whose class already advances after each answer. Keeps the existing logic as it is;
 * the adapter only maps it onto {@link GameSession}. No hints and no lives yet.
 */
abstract class AdvancingSession implements GameSession {

    private final Streak streak = new Streak();
    private int hintCount;
    private boolean hintedThisQuestion;

    /** The right-answer streak for this run. */
    public final Streak streak() {
        return streak;
    }

    /** Submits an answer to the wrapped game. Returns true when it was right, and it advances. */
    protected abstract boolean submit(String value);

    /** Whether the wrapped game still has a question to answer. */
    protected abstract boolean hasQuestion();

    @Override
    public final Outcome answer(String value) {
        if (!hasQuestion()) return Outcome.IGNORED;
        boolean right = submit(value);
        streak.record(right);
        hintedThisQuestion = false;
        return right ? Outcome.CORRECT : Outcome.WRONG;
    }

    @Override
    public final int lives() {
        return NO_LIVES;
    }

    /** The hint for the current question, or null when it has none. */
    protected abstract String hintText();

    @Override
    public final boolean hintAvailable() {
        return hasQuestion() && !hintedThisQuestion && hintCount < HintRules.HINTS_PER_RUN
                && hintText() != null;
    }

    @Override
    public final String hint() {
        if (!hintAvailable()) return null;
        hintedThisQuestion = true;
        hintCount++;
        return hintText();
    }

    @Override
    public final int hintsUsed() {
        return hintCount;
    }
}
