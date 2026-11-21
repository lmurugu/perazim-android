package org.perazimchurch.app.domain.games.catalog;

import org.perazimchurch.app.domain.games.WordSearch;
import org.perazimchurch.app.domain.games.engine.GameDefinition;
import org.perazimchurch.app.domain.games.engine.GameSession;

/**
 * Adapter for Word Search. An answer is the selection "r1,c1,r2,c2" (zero-based grid cells).
 * A selection that spells a new word is correct; anything else is wrong.
 */
public final class WordSearchDefinition implements GameDefinition {
    private final int iconResource;

    public WordSearchDefinition(int iconResource) { this.iconResource = iconResource; }

    @Override public String id() { return "word_search"; }
    @Override public String title() { return "Word Search"; }
    @Override public String description() { return "Find Bible names hidden in the letters."; }
    @Override public int iconResource() { return iconResource; }
    @Override public int levelCount() { return 1; }
    @Override public String levelName(int level) { return "Level " + level; }

    @Override
    public GameSession createSession(int level, long seed) {
        final WordSearch game = new WordSearch(seed);
        return new GameSession() {
            @Override public int size() { return game.words().size(); }
            @Override public int position() { return Math.min(game.found().size() + 1, size()); }
            @Override public boolean finished() { return game.complete(); }
            @Override public int score() { return game.found().size(); }
            @Override public int lives() { return NO_LIVES; }
            @Override public boolean hintAvailable() { return false; }
            @Override public String hint() { return null; }
            @Override public Outcome answer(String value) {
                if (game.complete()) return Outcome.IGNORED;
                int[] cells = parse(value);
                if (cells == null) return Outcome.IGNORED;
                return game.select(cells[0], cells[1], cells[2], cells[3]) != null
                        ? Outcome.CORRECT : Outcome.WRONG;
            }
        };
    }

    /** Parses "r1,c1,r2,c2" with each value 0 to {@link WordSearch#SIZE} minus one. Null when invalid. */
    static int[] parse(String value) {
        if (value == null) return null;
        String[] parts = value.split(",");
        if (parts.length != 4) return null;
        int[] cells = new int[4];
        try {
            for (int i = 0; i < 4; i++) {
                cells[i] = Integer.parseInt(parts[i].trim());
                if (cells[i] < 0 || cells[i] >= WordSearch.SIZE) return null;
            }
        } catch (NumberFormatException e) {
            return null;
        }
        return cells;
    }
}
