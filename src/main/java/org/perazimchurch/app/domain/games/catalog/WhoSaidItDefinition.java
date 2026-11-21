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
    @Override public int levelCount() { return Levels.COUNT; }
    @Override public String levelName(int level) { return "Level " + level; }

    @Override
    public GameSession createSession(int level, long seed) {
        return new WhoSaidItSession(new WhoSaidIt(seed));
    }

    @Override
    public org.perazimchurch.app.domain.games.engine.StarRule starRule() {
        return HintRules.capped(session -> org.perazimchurch.app.domain.games.engine.StarRating.stars(
                session.score(), session.size(), session.lives()));
    }
}
