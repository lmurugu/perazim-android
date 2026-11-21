package org.perazimchurch.app.domain.games.catalog;

import org.perazimchurch.app.domain.games.WhoSaidIt;
import org.perazimchurch.app.domain.games.engine.GameDefinition;
import org.perazimchurch.app.domain.games.engine.GameSession;

/** Adapter for Who Said It?. The answer is the speaker's name, exactly as offered. */
public final class WhoSaidItDefinition implements GameDefinition {
    private final int iconResource;

    public WhoSaidItDefinition(int iconResource) {
        this.iconResource = iconResource;
    }

    @Override public String id() { return "who_said_it"; }
    @Override public String title() { return "Who Said It?"; }
    @Override public String description() {
        return "Read a Bible quotation and choose the person who spoke it.";
    }
    @Override public int iconResource() { return iconResource; }
    @Override public int levelCount() { return 1; }
    @Override public String levelName(int level) { return "Level " + level; }

    @Override
    public GameSession createSession(int level, long seed) {
        final WhoSaidIt game = new WhoSaidIt(seed);
        return new AdvancingSession() {
            @Override protected boolean hasQuestion() { return !game.finished(); }
            @Override protected boolean submit(String value) { return game.answer(value); }
            @Override public int size() { return game.size(); }
            @Override public String prompt() {
                return game.finished() ? null : "Who said: \"" + game.current().quote + "\"";
            }
            @Override public int position() { return game.position(); }
            @Override public boolean finished() { return game.finished(); }
            @Override public int score() { return game.score(); }
        };
    }
}
