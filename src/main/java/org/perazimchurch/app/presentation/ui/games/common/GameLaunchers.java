package org.perazimchurch.app.presentation.ui.games.common;

import org.perazimchurch.app.presentation.ui.games.LadderScreen;
import org.perazimchurch.app.presentation.ui.games.MemoryScreen;
import org.perazimchurch.app.presentation.ui.games.StoryTimelineScreen;
import org.perazimchurch.app.presentation.ui.games.WordSearchScreen;
import org.perazimchurch.app.presentation.ui.games.WhoAmIScreen;

/**
 * Registers the play screens of games that have their own dialog. Called once from
 * PerazimApplication.onCreate. A game branch adds one {@code GameRegistry.register(...)} line here.
 * See docs/games/ENGINE_USAGE.md.
 */
public final class GameLaunchers {

    private GameLaunchers() {}

    /** Registers every game launcher. Add one line per game. */
    public static void registerAll() {
        GameRegistry.register("who_am_i", WhoAmIScreen::show);
        GameRegistry.register("story_timeline", StoryTimelineScreen::show);
        GameRegistry.register("word_search", WordSearchScreen::show);
        GameRegistry.register("memory_match", MemoryScreen::show);
        GameRegistry.register("ladder_of_faith", LadderScreen::show);
    }
}
