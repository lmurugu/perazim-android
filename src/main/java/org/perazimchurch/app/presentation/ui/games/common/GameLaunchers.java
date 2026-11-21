package org.perazimchurch.app.presentation.ui.games.common;

/**
 * Registers the play screens of games that have their own dialog. Called once from
 * PerazimApplication.onCreate. A game branch adds one {@code GameRegistry.register(...)} line here.
 * See docs/games/ENGINE_USAGE.md.
 */
public final class GameLaunchers {

    private GameLaunchers() {}

    /** Registers every game launcher. Add one line per game. */
    public static void registerAll() {
        GameRegistry.register("vine_words", org.perazimchurch.app.presentation.ui.games.vine.VineWordsDialog::show);
    }
}
