package org.perazimchurch.app.domain.games.catalog;

import org.perazimchurch.app.domain.games.FillTheGap;
import org.perazimchurch.app.domain.games.engine.GameDefinition;
import org.perazimchurch.app.domain.games.engine.GameSession;

/** Adapter for Fill the Gap. The answer is the missing word. */
public final class FillTheGapDefinition implements GameDefinition {
    private final int iconResource;

    public FillTheGapDefinition(int iconResource) {
        this.iconResource = iconResource;
    }

    @Override public String id() { return "fill_the_gap"; }
    @Override public String title() { return "Fill the Gap"; }
    @Override public String description() {
        return "A famous verse has one word missing. Choose the word that completes it.";
    }
    @Override public int iconResource() { return iconResource; }
    @Override public int levelCount() { return 1; }
    @Override public String levelName(int level) { return "Level " + level; }

    @Override
    public GameSession createSession(int level, long seed) {
        final FillTheGap game = new FillTheGap(seed);
        return new AdvancingSession() {
            @Override protected boolean hasQuestion() { return !game.finished(); }
            @Override protected boolean submit(String value) { return game.answer(value); }
            @Override public int size() { return game.size(); }
            @Override public int position() { return game.position(); }
            @Override public boolean finished() { return game.finished(); }
            @Override public int score() { return game.score(); }
        };
    }
}
