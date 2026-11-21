package org.perazimchurch.app.domain.games.vine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A crossword laid out on a fixed rectangle. Pure Java, no Android types.
 *
 * <p>A grid is valid when every run of two or more letters, read across or down, is exactly one
 * placed word. That rule rules out words that touch without crossing, parallel overlaps, and
 * two words that share a cell with different letters.
 */
public final class CrosswordGrid {

    /** One placed word: its letters start at (row, col) and run across or down. */
    public static final class Slot {
        public final String word;
        public final int row;
        public final int col;
        public final boolean across;

        public Slot(String word, int row, int col, boolean across) {
            this.word = word;
            this.row = row;
            this.col = col;
            this.across = across;
        }

        /** Row of the letter at {@code index} inside the word. */
        public int rowAt(int index) { return across ? row : row + index; }

        /** Column of the letter at {@code index} inside the word. */
        public int colAt(int index) { return across ? col + index : col; }
    }

    private final int rows;
    private final int cols;
    private final List<Slot> slots;
    private final Map<Long, Character> letters;

    private CrosswordGrid(int rows, int cols, List<Slot> slots, Map<Long, Character> letters) {
        this.rows = rows;
        this.cols = cols;
        this.slots = Collections.unmodifiableList(slots);
        this.letters = letters;
    }

    /**
     * Builds the letter map for slots on a rows-by-cols grid.
     *
     * @throws IllegalArgumentException when a slot leaves the grid or two slots disagree on a cell
     */
    public static CrosswordGrid of(int rows, int cols, List<Slot> slots) {
        if (rows < 1 || cols < 1) throw new IllegalArgumentException("grid must be at least 1x1");
        Map<Long, Character> letters = new HashMap<>();
        for (Slot slot : slots) {
            if (slot.word.isEmpty()) throw new IllegalArgumentException("empty word");
            for (int i = 0; i < slot.word.length(); i++) {
                int r = slot.rowAt(i);
                int c = slot.colAt(i);
                if (r < 0 || c < 0 || r >= rows || c >= cols) {
                    throw new IllegalArgumentException(slot.word + " leaves the grid");
                }
                char letter = slot.word.charAt(i);
                Character existing = letters.putIfAbsent(key(r, c), letter);
                if (existing != null && existing != letter) {
                    throw new IllegalArgumentException(slot.word + " clashes at row " + r + ", column " + c);
                }
            }
        }
        return new CrosswordGrid(rows, cols, new ArrayList<>(slots), letters);
    }

    public int rows() { return rows; }
    public int cols() { return cols; }
    public List<Slot> slots() { return slots; }

    /** The letter at a cell, or 0 when the cell is empty (no word covers it). */
    public char letterAt(int row, int col) {
        Character letter = letters.get(key(row, col));
        return letter == null ? 0 : letter;
    }

    /** True when the cell belongs to at least one placed word. */
    public boolean isLetterCell(int row, int col) {
        return letters.containsKey(key(row, col));
    }

    /** Number of cells that belong to at least one word. */
    public int letterCellCount() {
        return letters.size();
    }

    /** True when the placed words form a valid crossword (see class comment). */
    public boolean isValid() {
        java.util.Set<String> found = new java.util.HashSet<>();
        for (long k : letters.keySet()) {
            int row = (int) (k >> 32);
            int col = (int) k;
            for (boolean across : new boolean[] {true, false}) {
                int dr = across ? 0 : 1;
                int dc = across ? 1 : 0;
                if (letters.containsKey(key(row - dr, col - dc))) continue;
                StringBuilder word = new StringBuilder();
                int r = row;
                int c = col;
                while (letters.containsKey(key(r, c))) {
                    word.append(letters.get(key(r, c)));
                    r += dr;
                    c += dc;
                }
                if (word.length() >= 2) found.add(word + "@" + row + "," + col + "," + across);
            }
        }
        java.util.Set<String> expected = new java.util.HashSet<>();
        for (Slot slot : slots) {
            expected.add(slot.word + "@" + slot.row + "," + slot.col + "," + slot.across);
        }
        return found.equals(expected);
    }

    static long key(int row, int col) {
        return ((long) row << 32) | (col & 0xffffffffL);
    }
}
