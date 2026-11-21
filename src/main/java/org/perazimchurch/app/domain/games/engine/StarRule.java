package org.perazimchurch.app.domain.games.engine;

/**
 * How a finished run becomes 0 to 3 stars. Each game can override its rule through
 * {@link GameDefinition#starRule()}; the default is {@link StarRating#stars}.
 */
public interface StarRule {
    /** Stars for a finished run. {@code lives} is {@link GameSession#NO_LIVES} when the game has none. */
    int stars(int score, int size, int lives);
}
