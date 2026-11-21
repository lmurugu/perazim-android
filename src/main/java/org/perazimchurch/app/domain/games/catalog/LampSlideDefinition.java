package org.perazimchurch.app.domain.games.catalog;

import org.perazimchurch.app.domain.games.engine.GameDefinition;
import org.perazimchurch.app.domain.games.engine.GameSession;
import org.perazimchurch.app.domain.games.lampslide.LampSlideLevels;
import org.perazimchurch.app.domain.games.lampslide.LampSlidePuzzle;
import org.perazimchurch.app.domain.games.lampslide.LampSlideScene;
import org.perazimchurch.app.domain.games.lampslide.LampSlideSession;

import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

/**
 * Lamp Slide for the Games tab. The scene list comes from a supplier so the bundled JSON can be
 * read by the app layer. Works offline, no ads, no score upload.
 */
public final class LampSlideDefinition implements GameDefinition {

    private final int iconResource;
    private final Supplier<List<LampSlideScene>> scenes;

    public LampSlideDefinition(int iconResource, Supplier<List<LampSlideScene>> scenes) {
        this.iconResource = iconResource;
        this.scenes = scenes;
    }

    @Override public String id() { return "lamp_slide"; }
    @Override public String title() { return "Lamp Slide"; }
    @Override public String description() {
        return "Slide the tiles to rebuild a painting. Finish the picture to read its verse.";
    }
    @Override public int iconResource() { return iconResource; }
    @Override public int levelCount() { return LampSlideLevels.LEVEL_COUNT; }
    @Override public String levelName(int level) { return "Level " + level; }

    @Override
    public GameSession createSession(int level, long seed) {
        int clamped = Math.max(1, Math.min(LampSlideLevels.LEVEL_COUNT, level));
        int size = LampSlideLevels.boardSize(clamped);
        LampSlidePuzzle puzzle = LampSlidePuzzle.scrambled(size, seed * 31L + clamped);
        List<LampSlideScene> list = scenes.get();
        if (list == null || list.isEmpty()) {
            list = Collections.singletonList(new LampSlideScene("fallback", "ic_game_puzzle",
                    "Psalm 119:105", "Thy word is a lamp unto my feet, and a light unto my path.", ""));
        }
        LampSlideScene scene = list.get((int) Math.floorMod(seed + clamped, (long) list.size()));
        return new LampSlideSession(puzzle, scene);
    }
}
