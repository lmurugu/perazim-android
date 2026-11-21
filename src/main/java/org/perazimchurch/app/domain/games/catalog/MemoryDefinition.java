package org.perazimchurch.app.domain.games.catalog;

import org.perazimchurch.app.domain.games.MemoryMatch;
import org.perazimchurch.app.domain.games.engine.GameDefinition;
import org.perazimchurch.app.domain.games.engine.GameSession;

/** Adapter for Memory Match with six pairs. An answer is the tile index to turn over. */
public final class MemoryDefinition implements GameDefinition {
    private static final int PAIRS = 6;
    private final int iconResource;

    public MemoryDefinition(int iconResource) { this.iconResource = iconResource; }

    @Override public String id() { return "memory_match"; }
    @Override public String title() { return "Memory Match"; }
    @Override public String description() { return "Flip two tiles at a time and match the Bible symbols."; }
    @Override public int iconResource() { return iconResource; }
    @Override public int levelCount() { return 1; }
    @Override public String levelName(int level) { return "Level " + level; }

    @Override
    public GameSession createSession(int level, long seed) {
        final MemoryMatch game = new MemoryMatch(seed, PAIRS);
        return new GameSession() {
            @Override public int size() { return PAIRS; }
            @Override public int position() { return Math.min(score() + 1, PAIRS); }
            @Override public boolean finished() { return game.complete(); }
            @Override public int score() {
                int matched = 0;
                for (MemoryMatch.Tile tile : game.tiles()) if (tile.matched) matched++;
                return matched / 2;
            }
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
                if (index < 0 || index >= game.tiles().size()) return Outcome.IGNORED;
                if (game.mismatchShowing()) game.hideMismatch();
                int before = score();
                if (!game.flip(index)) return Outcome.IGNORED;
                if (score() > before) return Outcome.CORRECT;
                return game.mismatchShowing() ? Outcome.WRONG : Outcome.IGNORED;
            }
        };
    }
}
