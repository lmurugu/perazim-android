package org.perazimchurch.app.domain.games.catalog;

import org.perazimchurch.app.domain.games.engine.GameDefinition;
import org.perazimchurch.app.domain.games.engine.StarRule;
import org.perazimchurch.app.domain.games.engine.GameSession;
import org.perazimchurch.app.domain.games.mosaic.MosaicBoard;
import org.perazimchurch.app.domain.games.mosaic.MosaicPuzzles;
import org.perazimchurch.app.domain.games.mosaic.MosaicStars;

/**
 * Story Mosaic. One level per puzzle, in story order across the shared chapters. Levels 1 to 10
 * are guided (a hint is shown); later levels are quiet. Works offline, no ads, no score upload.
 */
public final class StoryMosaicDefinition implements GameDefinition {

    /** Levels with a hint button. */
    public static final int GUIDED_LEVELS = 10;

    private final int iconResource;

    public StoryMosaicDefinition(int iconResource) {
        this.iconResource = iconResource;
    }

    @Override public String id() { return "story_mosaic"; }
    @Override public String title() { return "Story Mosaic"; }
    @Override public String description() {
        return "Put the Bible scene together, piece by piece, and read the story behind it.";
    }
    @Override public int iconResource() { return iconResource; }
    @Override public int levelCount() { return MosaicPuzzles.all().size(); }
    @Override public String levelName(int level) { return "Level " + level; }

    /** Stars follow the Story Mosaic rule: 3 with no wrong drops, 2 with at most a third wrong. */
    @Override
    public StarRule starRule() { return MosaicStars.RULE; }

    @Override
    public GameSession createSession(int level, long seed) {
        int clamped = Math.max(1, Math.min(level, levelCount()));
        MosaicPuzzles.Puzzle puzzle = MosaicPuzzles.at(clamped - 1);
        return new StoryMosaicSession(puzzle, MosaicBoard.sideForLevel(clamped),
                clamped <= GUIDED_LEVELS, seed);
    }
}
