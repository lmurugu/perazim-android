package org.perazimchurch.app.domain.games.catalog;

import org.perazimchurch.app.domain.games.LadderOfFaith;
import org.perazimchurch.app.domain.games.engine.GameDefinition;
import org.perazimchurch.app.domain.games.engine.GameSession;

/** Adapter for Ladder of Faith: 20 rungs with three lives. Lifelines and hints come later. */
public final class LadderDefinition implements GameDefinition {
    private final int iconResource;

    public LadderDefinition(int iconResource) { this.iconResource = iconResource; }

    @Override public String id() { return "ladder_of_faith"; }
    @Override public String title() { return "Ladder of Faith"; }
    @Override public String description() { return "Climb 20 rungs from easy to hard with three lives."; }
    @Override public int iconResource() { return iconResource; }
    @Override public int levelCount() { return 1; }
    @Override public String levelName(int level) { return "Level " + level; }

    @Override
    public GameSession createSession(int level, long seed) {
        final LadderOfFaith ladder = new LadderOfFaith(seed);
        return new GameSession() {
            @Override public int size() { return LadderOfFaith.RUNGS; }
            @Override public int position() { return Math.min(ladder.position(), LadderOfFaith.RUNGS); }
            @Override public boolean finished() { return ladder.finished(); }
            @Override public int score() { return ladder.score(); }
            @Override public int lives() { return ladder.lives(); }
            @Override public boolean hintAvailable() { return false; }
            @Override public String hint() { return null; }
            @Override public Outcome answer(String value) {
                if (ladder.finished() || ladder.answered()) return Outcome.IGNORED;
                boolean right = ladder.answer(value);
                ladder.next();
                return right ? Outcome.CORRECT : Outcome.WRONG;
            }
        };
    }
}
