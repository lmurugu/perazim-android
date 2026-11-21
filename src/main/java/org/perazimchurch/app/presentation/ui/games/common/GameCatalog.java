package org.perazimchurch.app.presentation.ui.games.common;

import org.perazimchurch.app.R;
import org.perazimchurch.app.domain.games.BibleGames;
import org.perazimchurch.app.domain.games.catalog.FillTheGapDefinition;
import org.perazimchurch.app.domain.games.catalog.LampSlideDefinition;
import org.perazimchurch.app.domain.games.catalog.LadderDefinition;
import org.perazimchurch.app.domain.games.catalog.MemoryDefinition;
import org.perazimchurch.app.domain.games.catalog.VineWordsDefinition;
import org.perazimchurch.app.domain.games.catalog.RoadDefinition;
import org.perazimchurch.app.domain.games.catalog.RoundDefinition;
import org.perazimchurch.app.domain.games.catalog.StoryMosaicDefinition;
import org.perazimchurch.app.domain.games.catalog.TestamentDefinition;
import org.perazimchurch.app.domain.games.catalog.TimelineDefinition;
import org.perazimchurch.app.domain.games.catalog.WhoAmIDefinition;
import org.perazimchurch.app.domain.games.catalog.WhoSaidItDefinition;
import org.perazimchurch.app.domain.games.catalog.WordSearchDefinition;
import org.perazimchurch.app.domain.games.engine.GameDefinition;
import org.perazimchurch.app.presentation.ui.games.vine.VineWordsContent;
import org.perazimchurch.app.presentation.ui.games.lampslide.LampSlideScenes;

import java.util.ArrayList;
import java.util.List;

/**
 * Every game on the Games tab and on Home, in one list. Adding a game here makes it appear on
 * both screens. Icons are Android resources, so the list lives in the app layer.
 */
public final class GameCatalog {

    private GameCatalog() {}

    public static List<GameDefinition> all() {
        List<GameDefinition> games = new ArrayList<>();
        games.add(new RoundDefinition("bible_quiz", "Bible Quiz",
                "Five questions, four choices each. Discover the story behind every answer.",
                R.drawable.game_art_quiz, BibleGames.Mode.QUIZ));
        games.add(WhoAmIDefinition.create(R.drawable.game_art_who_am_i));
        games.add(new RoundDefinition("verse_builder", "Verse Builder",
                "Tap shuffled word tiles to rebuild a short KJV verse.",
                R.drawable.game_art_verse_builder, BibleGames.Mode.VERSE_ORDER));
        games.add(new LadderDefinition(R.drawable.game_art_ladder));
        games.add(new WordSearchDefinition(R.drawable.game_art_word_search));
        games.add(new MemoryDefinition(R.drawable.game_art_memory_match));
        games.add(new TimelineDefinition(R.drawable.game_art_timeline));
        // Vine Words replaces Mini Crossword. Icon: game_vine_words.
        games.add(new VineWordsDefinition(R.drawable.game_vine_words, VineWordsContent::levels));
        games.add(new RoadDefinition(R.drawable.ic_game_road));
        games.add(new TestamentDefinition(R.drawable.game_art_old_or_new));
        games.add(new WhoSaidItDefinition(R.drawable.game_art_who_said_it));
        games.add(new FillTheGapDefinition(R.drawable.game_art_verse_builder));
        games.add(new LampSlideDefinition(R.drawable.game_lamp_slide, LampSlideScenes::bundled));
        games.add(new StoryMosaicDefinition(R.drawable.game_story_mosaic));
        return games;
    }
}
