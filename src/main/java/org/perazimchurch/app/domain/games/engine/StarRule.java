package org.perazimchurch.app.domain.games.engine;

/**
 * How a finished run becomes 0 to 3 stars. A game overrides its rule through
 * {@link GameDefinition#starRule()}. The rule sees the whole finished session, so it can read
 * the score, size, lives and {@link GameSession#hintsUsed()}.
 */
public interface StarRule {
    /** Stars for a finished session, from 0 to 3. */
    int stars(GameSession session);
}
