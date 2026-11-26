package org.perazimchurch.app.domain.games;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * Offline Bible word search. The grid is generated from a seed, so a puzzle can be rebuilt
 * on any phone without storing it. Words are Bible names; no copyrighted text is used.
 */
public final class WordSearch {
    public static final int SIZE = 8;
    public static final int WORDS_PER_PUZZLE = 6;

    private static final String[] BANK = {
        "NOAH", "MOSES", "RUTH", "DAVID", "JONAH", "DANIEL", "ELIJAH",
        "JOSEPH", "GIDEON", "SAMSON", "ABRAHAM", "JACOB", "ESTHER", "JOSHUA"
    };
    // Eight directions: right, down, diagonal down-right, and the four reversed forms.
    private static final int[][] DIRS = {{0, 1}, {1, 0}, {1, 1}, {1, -1}, {0, -1}, {-1, 0}, {-1, -1}, {-1, 1}};

    private final char[][] grid = new char[SIZE][SIZE];
    private final List<String> words;
    private final Set<String> found = new HashSet<>();
    private final List<int[][]> cellsByWord = new ArrayList<>();

    public WordSearch(long seed) {
        Random random = new Random(seed);
        List<String> pool = new ArrayList<>(Arrays.asList(BANK));
        Collections.shuffle(pool, random);
        List<String> chosen = new ArrayList<>();
        // Restart the fill if a word cannot be placed; this is rare with 6 words in an 8x8 grid.
        for (int attempt = 0; attempt < 50; attempt++) {
            chosen.clear();
            cellsByWord.clear();
            for (char[] row : grid) Arrays.fill(row, '\0');
            Collections.shuffle(pool, random);
            boolean allPlaced = true;
            for (int i = 0; i < WORDS_PER_PUZZLE && allPlaced; i++) {
                String word = pool.get(i);
                int[][] cells = place(word, random);
                if (cells == null) allPlaced = false;
                else { chosen.add(word); cellsByWord.add(cells); }
            }
            if (allPlaced) break;
        }
        words = Collections.unmodifiableList(chosen);
        for (int r = 0; r < SIZE; r++) {
            for (int c = 0; c < SIZE; c++) {
                if (grid[r][c] == '\0') grid[r][c] = (char) ('A' + random.nextInt(26));
            }
        }
    }

    private int[][] place(String word, Random random) {
        for (int tries = 0; tries < 200; tries++) {
            int[] dir = DIRS[random.nextInt(DIRS.length)];
            int r = random.nextInt(SIZE), c = random.nextInt(SIZE);
            int endR = r + dir[0] * (word.length() - 1), endC = c + dir[1] * (word.length() - 1);
            if (endR < 0 || endR >= SIZE || endC < 0 || endC >= SIZE) continue;
            boolean fits = true;
            for (int i = 0; i < word.length() && fits; i++) {
                int rr = r + dir[0] * i, cc = c + dir[1] * i;
                char existing = grid[rr][cc];
                if (existing != '\0' && existing != word.charAt(i)) fits = false;
            }
            if (!fits) continue;
            int[][] cells = new int[word.length()][2];
            for (int i = 0; i < word.length(); i++) {
                int rr = r + dir[0] * i, cc = c + dir[1] * i;
                grid[rr][cc] = word.charAt(i);
                cells[i] = new int[]{rr, cc};
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
     * Checks a straight selection from one cell to another. Returns the cells of the word that
     * was found, or null when the selection is not a new word.
     */
    public int[][] select(int r1, int c1, int r2, int c2) {
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
