package org.perazimchurch.app.domain.games;

import java.util.Collections;
import java.util.List;

/**
 * Mini Bible crossword: a fixed 5x5 grid with four answers that cross correctly.
 * '#' marks a black square. Answers are names and short words from the KJV story.
 */
public final class MiniCrossword {
    public static final int SIZE = 5;

    private static final String[] GRID = {
        "MOSES",
        "A###A",
        "N###U",
        "N###L",
        "ABEL#"
    };

    public static final class Clue {
        public final String direction;
        public final int number;
        public final String text;
        public final String answer;
        Clue(String direction, int number, String text, String answer) {
            this.direction = direction;
            this.number = number;
            this.text = text;
            this.answer = answer;
        }
    }

    private static final List<Clue> CLUES = Collections.unmodifiableList(List.of(
        new Clue("Across", 1, "The leader who brought Israel out of Egypt (5)", "MOSES"),
        new Clue("Across", 4, "Keeper of sheep, murdered by his brother (4)", "ABEL"),
        new Clue("Down", 1, "Bread from heaven in the wilderness (5)", "MANNA"),
        new Clue("Down", 2, "The first king of Israel (4)", "SAUL")));

    public List<Clue> clues() { return CLUES; }

    public boolean isLetter(int row, int col) { return GRID[row].charAt(col) != '#'; }

    public char answer(int row, int col) { return GRID[row].charAt(col); }

    /** True when the typed letter matches this cell (case-insensitive). Black squares never match. */
    public boolean matches(int row, int col, String typed) {
        if (!isLetter(row, col) || typed == null || typed.length() != 1) return false;
        return Character.toUpperCase(typed.charAt(0)) == answer(row, col);
    }

    /** True when every letter cell is filled correctly. */
    public boolean solved(String[][] entries) {
        for (int r = 0; r < SIZE; r++) {
            for (int c = 0; c < SIZE; c++) {
                if (isLetter(r, c) && !matches(r, c, entries[r][c])) return false;
            }
        }
        return true;
    }
}
