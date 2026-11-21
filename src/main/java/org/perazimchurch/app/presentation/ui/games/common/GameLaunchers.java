package org.perazimchurch.app.presentation.ui.games.common;

import org.perazimchurch.app.presentation.ui.games.mosaic.StoryMosaicDialog;

/**
 * Registers the play screens of games that have their own dialog. Called once from
 * PerazimApplication.onCreate. A game branch adds one {@code GameRegistry.register(...)} line here.
 * See docs/games/ENGINE_USAGE.md.
 */
public final class GameLaunchers {

    private GameLaunchers() {}

    /** Registers every game launcher. Add one line per game. */
    public static void registerAll() {
        GameRegistry.register("story_mosaic", StoryMosaicDialog::show);
    }
}
