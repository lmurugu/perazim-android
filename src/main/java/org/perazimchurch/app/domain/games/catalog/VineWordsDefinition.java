package org.perazimchurch.app.domain.games.catalog;

import org.perazimchurch.app.domain.games.engine.GameDefinition;
import org.perazimchurch.app.domain.games.engine.GameSession;
import org.perazimchurch.app.domain.games.vine.VineWordsLevel;
import org.perazimchurch.app.domain.games.vine.VineWordsLevels;
import org.perazimchurch.app.domain.games.vine.VineWordsSession;

import java.util.List;
import java.util.function.Supplier;

/** Catalog entry for Vine Words. The level file is read lazily, so the Games list stays cheap to build. */
public final class VineWordsDefinition implements GameDefinition {
    private final int iconResource;
    private final Supplier<List<VineWordsLevel>> levels;

    public VineWordsDefinition(int iconResource, Supplier<List<VineWordsLevel>> levels) {
        this.iconResource = iconResource;
        this.levels = levels;
    }

    @Override public String id() { return "vine_words"; }
    @Override public String title() { return "Vine Words"; }
    @Override public String description() {
        return "Swipe letters on a wheel to fill a Bible crossword. Each level is built from one passage.";
    }
    @Override public int iconResource() { return iconResource; }
    @Override public int levelCount() { return VineWordsLevels.LEVEL_COUNT; }
    @Override public String levelName(int level) { return "Level " + level; }

    @Override
    public GameSession createSession(int level, long seed) {
        List<VineWordsLevel> all = levels.get();
        int index = Math.max(1, Math.min(level, all.size())) - 1;
        return new VineWordsSession(all.get(index));
    }
}
