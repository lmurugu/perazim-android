package org.perazimchurch.app.domain.games.engine;

/**
 * One run of a game. A session owns the score, lives and hints for that run and nothing else.
 * Answers are plain strings so every game can share the same results screen.
 */
public interface GameSession {

    /** Number of questions or moves in this run. */
    int size();

    /** 1-based number of the current question, capped at {@link #size()}. */
    int position();

    /** True once every question has been answered. */
    boolean finished();

    /** Points so far. */
    int score();

    /** Lives left, or {@link #NO_LIVES} when the game does not use lives. */
    int lives();

    /** True when a hint can still be shown for the current question. */
    boolean hintAvailable();

    /** The hint text for the current question, or null when none is available. */
    String hint();

    /** The question or clue text a play screen shows, or null when the screen uses its own. */
    default String prompt() { return null; }

    /** Answers the current question. Returns the outcome; the session then moves on or stays. */
    Outcome answer(String value);

    /** Lives value meaning the game is played without lives. */
    int NO_LIVES = -1;

    /**
     * Result of one answer. MOVED is a valid move that neither solves the question nor fails it
     * (for example, sliding a tile to a new square). Existing games never return it.
     */
    enum Outcome { CORRECT, WRONG, IGNORED, MOVED }
}
