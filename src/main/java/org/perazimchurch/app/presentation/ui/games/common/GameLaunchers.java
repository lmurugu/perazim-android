package org.perazimchurch.app.presentation.ui.games.common;

import org.perazimchurch.app.presentation.ui.games.FillTheGapScreen;
import org.perazimchurch.app.presentation.ui.games.OldOrNewScreen;
import org.perazimchurch.app.presentation.ui.games.QuizScreen;
import org.perazimchurch.app.presentation.ui.games.VerseBuilderScreen;
import org.perazimchurch.app.presentation.ui.games.WhoSaidItScreen;
import org.perazimchurch.app.presentation.ui.games.LadderScreen;
import org.perazimchurch.app.presentation.ui.games.MemoryScreen;
import org.perazimchurch.app.presentation.ui.games.RoadScreen;
import org.perazimchurch.app.presentation.ui.games.StoryTimelineScreen;
import org.perazimchurch.app.presentation.ui.games.WordSearchScreen;
import org.perazimchurch.app.presentation.ui.games.WhoAmIScreen;
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
        // GameRegistry.register("story_mosaic", activity -> StoryMosaicDialog.show(activity));
        GameRegistry.register("old_or_new", ctx -> new OldOrNewScreen(ctx).start());
        GameRegistry.register("who_said_it", ctx -> new WhoSaidItScreen(ctx).start());
        GameRegistry.register("fill_the_gap", ctx -> new FillTheGapScreen(ctx).start());
        GameRegistry.register("bible_quiz", ctx -> new QuizScreen(ctx).start());
        GameRegistry.register("verse_builder", ctx -> new VerseBuilderScreen(ctx).start());
        GameRegistry.register("who_am_i", WhoAmIScreen::show);
        GameRegistry.register("story_timeline", StoryTimelineScreen::show);
        GameRegistry.register("word_search", WordSearchScreen::show);
        GameRegistry.register("memory_match", MemoryScreen::show);
        GameRegistry.register("ladder_of_faith", LadderScreen::show);
        GameRegistry.register("road_to_jerusalem", RoadScreen::show);
        GameRegistry.register("vine_words", org.perazimchurch.app.presentation.ui.games.vine.VineWordsDialog::show);
        // GameRegistry.register("story_mosaic", ctx -> StoryMosaicDialog.show(ctx.activity(), ctx.ui()));
        GameRegistry.register("lamp_slide", org.perazimchurch.app.presentation.ui.games.lampslide.LampSlideLauncher::open);
        GameRegistry.register("story_mosaic", StoryMosaicDialog::show);
    }
}
