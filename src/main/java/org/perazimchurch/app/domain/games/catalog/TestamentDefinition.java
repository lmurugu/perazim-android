package org.perazimchurch.app.domain.games.catalog;

import org.perazimchurch.app.domain.games.TestamentSort;
import org.perazimchurch.app.domain.games.engine.GameDefinition;
import org.perazimchurch.app.domain.games.engine.GameSession;

/** Adapter for Old or New?. Answers are "OLD" or "NEW". */
public final class TestamentDefinition implements GameDefinition {
    private final int iconResource;

    public TestamentDefinition(int iconResource) {
        this.iconResource = iconResource;
    }

    @Override public String id() { return "old_or_new"; }
    @Override public String title() { return "Old or New?"; }
    @Override public String description() {
        return "Place each Bible book in the Old or New Testament.";
    }
    @Override public int iconResource() { return iconResource; }
    @Override public int levelCount() { return 1; }
    @Override public String levelName(int level) { return "Level " + level; }

    @Override
    public GameSession createSession(int level, long seed) {
        return new TestamentSession(new TestamentSort(seed));
    }
}
