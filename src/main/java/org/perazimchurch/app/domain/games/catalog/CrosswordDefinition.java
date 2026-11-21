package org.perazimchurch.app.domain.games.catalog;

import org.perazimchurch.app.domain.games.MiniCrossword;
import org.perazimchurch.app.domain.games.engine.GameDefinition;
import org.perazimchurch.app.domain.games.engine.GameSession;

/**
 * Adapter for Mini Crossword. An answer is "row,col,letter" for one cell. A correct letter in a
 * letter cell counts once; the run is finished when every letter cell is filled correctly.
 */
public final class CrosswordDefinition implements GameDefinition {
    private final int iconResource;

    public CrosswordDefinition(int iconResource) { this.iconResource = iconResource; }

    @Override public String id() { return "mini_crossword"; }
    @Override public String title() { return "Mini Crossword"; }
    @Override public String description() { return "A five-by-five Bible crossword with four clues that cross."; }
    @Override public int iconResource() { return iconResource; }
    @Override public int levelCount() { return 1; }
    @Override public String levelName(int level) { return "Level " + level; }

    @Override
    public GameSession createSession(int level, long seed) {
        final MiniCrossword game = new MiniCrossword();
        final String[][] entries = new String[MiniCrossword.SIZE][MiniCrossword.SIZE];
        int cells = 0;
        for (int r = 0; r < MiniCrossword.SIZE; r++) {
            for (int c = 0; c < MiniCrossword.SIZE; c++) if (game.isLetter(r, c)) cells++;
        }
        final int total = cells;
        return new GameSession() {
            @Override public int size() { return total; }
            @Override public int position() { return Math.min(score() + 1, total); }
            @Override public boolean finished() { return game.solved(entries); }
            @Override public int score() {
                int correct = 0;
                for (int r = 0; r < MiniCrossword.SIZE; r++) {
                    for (int c = 0; c < MiniCrossword.SIZE; c++) {
                        if (game.isLetter(r, c) && game.matches(r, c, entries[r][c])) correct++;
                    }
                }
                return correct;
            }
            @Override public int lives() { return NO_LIVES; }
            @Override public boolean hintAvailable() { return false; }
            @Override public String hint() { return null; }
            @Override public Outcome answer(String value) {
                if (finished()) return Outcome.IGNORED;
                String[] parts = value == null ? new String[0] : value.split(",");
                if (parts.length != 3) return Outcome.IGNORED;
                int row;
                int col;
                try {
                    row = Integer.parseInt(parts[0].trim());
                    col = Integer.parseInt(parts[1].trim());
                } catch (NumberFormatException e) {
                    return Outcome.IGNORED;
                }
                if (row < 0 || row >= MiniCrossword.SIZE || col < 0 || col >= MiniCrossword.SIZE) return Outcome.IGNORED;
                if (!game.isLetter(row, col)) return Outcome.IGNORED;
                String letter = parts[2].trim();
                if (game.matches(row, col, letter)) {
                    entries[row][col] = letter.toUpperCase();
                    return Outcome.CORRECT;
                }
                return Outcome.WRONG;
            }
        };
    }
}
