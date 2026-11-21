package org.perazimchurch.app.domain.games.engine;

/**
 * One game as the Games tab sees it. Pure Java, no Android types, so every game can be
 * described and tested the same way. The UI reads the metadata to draw a card and calls
 * {@link #createSession} to start a run.
 *
 * <p>Games work offline. Nothing here uploads scores or personal data.
 */
public interface GameDefinition {

    /** Stable key used for local progress and art names, for example "who_said_it". Never change it. */
    String id();

    /** Title shown on the card and the play screen. */
    String title();

    /** One short line shown on the card. */
    String description();

    /**
     * Drawable resource for the card icon, or 0 when the game has no art yet. The art thread
     * delivers assets as {@code res/drawable-nodpi/game_<id>.png}.
     */
    int iconResource();

    /** How many difficulty levels the game offers. Always at least 1. */
    int levelCount();

    /** Name of a level, for example "Level 2". {@code level} runs from 1 to {@link #levelCount()}. */
    String levelName(int level);

    /**
     * Starts a run at a difficulty level.
     *
     * @param level 1-based level, clamped to the range of {@link #levelCount()} by the caller
     * @param seed  seed for shuffling; the same seed gives the same run
     */
    GameSession createSession(int level, long seed);
}
