package org.perazimchurch.app.domain.games.catalog;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * An 8 by 8 word search built from a seed. Words go in straight lines in any of eight directions.
 * The same seed and words always give the same grid, so a run can be rebuilt without storing it.
 */
public final class WordSearchGrid {

    public static final int SIZE = 8;
    private static final int[][] DIRS = {{0, 1}, {1, 0}, {1, 1}, {1, -1}, {0, -1}, {-1, 0}, {-1, -1}, {-1, 1}};

    private final char[][] grid = new char[SIZE][SIZE];
    private final List<String> words;
    private final List<int[][]> cellsByWord = new ArrayList<>();
    private final Set<String> found = new HashSet<>();

    /** Places every word. Throws IllegalStateException if the words cannot fit (never for the bank). */
    public WordSearchGrid(List<String> words, long seed) {
        Random random = new Random(seed);
        for (int attempt = 0; attempt < 500; attempt++) {
            clear();
            boolean allPlaced = true;
            for (String word : words) {
                int[][] cells = place(word, random);
                if (cells == null) {
                    allPlaced = false;
                    break;
                }
                cellsByWord.add(cells);
            }
            if (allPlaced) {
                this.words = Collections.unmodifiableList(new ArrayList<>(words));
                fillRandom(random);
                return;
            }
        }
        throw new IllegalStateException("could not place words: " + words);
    }

    private void clear() {
        for (char[] row : grid) java.util.Arrays.fill(row, '\0');
        cellsByWord.clear();
    }

    private void fillRandom(Random random) {
        for (int r = 0; r < SIZE; r++) {
            for (int c = 0; c < SIZE; c++) {
                if (grid[r][c] == '\0') grid[r][c] = (char) ('A' + random.nextInt(26));
            }
        }
    }

    private int[][] place(String word, Random random) {
        for (int tries = 0; tries < 300; tries++) {
            int[] dir = DIRS[random.nextInt(DIRS.length)];
            int r = random.nextInt(SIZE), c = random.nextInt(SIZE);
            int endR = r + dir[0] * (word.length() - 1), endC = c + dir[1] * (word.length() - 1);
            if (endR < 0 || endR >= SIZE || endC < 0 || endC >= SIZE) continue;
            boolean fits = true;
            for (int i = 0; i < word.length() && fits; i++) {
                char existing = grid[r + dir[0] * i][c + dir[1] * i];
                if (existing != '\0' && existing != word.charAt(i)) fits = false;
            }
            if (!fits) continue;
            int[][] cells = new int[word.length()][2];
            for (int i = 0; i < word.length(); i++) {
                int rr = r + dir[0] * i, cc = c + dir[1] * i;
                grid[rr][cc] = word.charAt(i);
                cells[i] = new int[] {rr, cc};
            }
            return cells;
        }
        return null;
    }

    public char letter(int row, int col) { return grid[row][col]; }
    public List<String> words() { return words; }
    public Set<String> found() { return Collections.unmodifiableSet(found); }
    public boolean complete() { return found.size() == words.size(); }

    /**
     * Checks a straight selection from one cell to another (either direction). Returns the cells of
     * the word it spells when that word is still to be found, or null otherwise.
     */
    public int[][] select(int r1, int c1, int r2, int c2) {
        if (r1 < 0 || c1 < 0 || r2 < 0 || c2 < 0 || r1 >= SIZE || c1 >= SIZE || r2 >= SIZE || c2 >= SIZE) return null;
        int dr = Integer.signum(r2 - r1), dc = Integer.signum(c2 - c1);
        int len = Math.max(Math.abs(r2 - r1), Math.abs(c2 - c1)) + 1;
        boolean straight = (r1 == r2) || (c1 == c2) || (Math.abs(r2 - r1) == Math.abs(c2 - c1));
        if (!straight) return null;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < len; i++) sb.append(grid[r1 + dr * i][c1 + dc * i]);
        String selected = sb.toString();
        for (int w = 0; w < words.size(); w++) {
            String word = words.get(w);
            if (found.contains(word)) continue;
            if (word.equals(selected) || new StringBuilder(word).reverse().toString().equals(selected)) {
                found.add(word);
                return cellsByWord.get(w);
            }
        }
        return null;
    }
}
