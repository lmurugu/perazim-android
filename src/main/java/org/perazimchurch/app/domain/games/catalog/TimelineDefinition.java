package org.perazimchurch.app.domain.games.catalog;

import org.perazimchurch.app.domain.games.TimelineSort;
import org.perazimchurch.app.domain.games.engine.GameDefinition;
import org.perazimchurch.app.domain.games.engine.GameSession;

/** Adapter for Story Timeline with six events. An answer is the shuffled index of the event tapped. */
public final class TimelineDefinition implements GameDefinition {
    private static final int EVENTS = 6;
    private final int iconResource;

    public TimelineDefinition(int iconResource) { this.iconResource = iconResource; }

    @Override public String id() { return "story_timeline"; }
    @Override public String title() { return "Story Timeline"; }
    @Override public String description() { return "Tap the Bible events in the order they happen in the story."; }
    @Override public int iconResource() { return iconResource; }
    @Override public int levelCount() { return 1; }
    @Override public String levelName(int level) { return "Level " + level; }

    @Override
    public GameSession createSession(int level, long seed) {
        final TimelineSort game = new TimelineSort(seed, EVENTS);
        return new GameSession() {
            @Override public int size() { return game.ordered().size(); }
            @Override public int position() { return Math.min(game.placed() + 1, size()); }
            @Override public boolean finished() { return game.complete(); }
            @Override public int score() { return game.placed(); }
            @Override public int lives() { return NO_LIVES; }
            @Override public boolean hintAvailable() { return false; }
            @Override public String hint() { return null; }
            @Override public Outcome answer(String value) {
                if (game.complete()) return Outcome.IGNORED;
                int index;
                try {
                    index = Integer.parseInt(value == null ? "" : value.trim());
                } catch (NumberFormatException e) {
                    return Outcome.IGNORED;
                }
                if (index < 0 || index >= game.shuffled().size()) return Outcome.IGNORED;
                return game.tap(index) ? Outcome.CORRECT : Outcome.WRONG;
            }
        };
    }
}
