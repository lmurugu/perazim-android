package org.perazimchurch.app.domain.games.catalog;

import org.perazimchurch.app.domain.games.RoadToJerusalem;
import org.perazimchurch.app.domain.games.engine.GameDefinition;
import org.perazimchurch.app.domain.games.engine.GameSession;

/**
 * Adapter for Road to Jerusalem as a one-race session. Any answer takes the player's turn; the
 * fellow traveller then moves. The run scores 1 when the player reaches Jerusalem first.
 */
public final class RoadDefinition implements GameDefinition {
    private final int iconResource;

    public RoadDefinition(int iconResource) { this.iconResource = iconResource; }

    @Override public String id() { return "road_to_jerusalem"; }
    @Override public String title() { return "Road to Jerusalem"; }
    @Override public String description() {
        return "Roll the die and race a fellow traveller to Jerusalem.";
    }
    @Override public int iconResource() { return iconResource; }
    @Override public int levelCount() { return 1; }
    @Override public String levelName(int level) { return "Level " + level; }

    @Override
    public GameSession createSession(int level, long seed) {
        final RoadToJerusalem game = new RoadToJerusalem(seed);
        return new GameSession() {
            @Override public int size() { return 1; }
            @Override public int position() { return 1; }
            @Override public boolean finished() { return game.finished(); }
            @Override public int score() { return game.playerAt() >= RoadToJerusalem.FINISH ? 1 : 0; }
            @Override public int lives() { return NO_LIVES; }
            @Override public boolean hintAvailable() { return false; }
            @Override public String hint() { return null; }
            @Override public Outcome answer(String value) {
                if (game.finished()) return Outcome.IGNORED;
                RoadToJerusalem.Move move = game.turn(true);
                if (!game.finished()) game.turn(false);
                return move.to > move.from ? Outcome.CORRECT : Outcome.WRONG;
            }
        };
    }
}
