package org.perazimchurch.app.domain.games.catalog;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.perazimchurch.app.domain.games.engine.GameSession;

/**
 * One run of Word Search. A correct selection scores with a combo bonus for consecutive finds within
 * this run only. A selection that spells nothing new resets the combo and is shown gently. A hint
 * names the first letter of a word still to find and costs a few points on that word.
 */
public final class WordSearchSession implements GameSession {

    static final int BASE_POINTS = 10;
    static final int COMBO_STEP = 5;
    static final int COMBO_CAP = 5;
    static final int HINT_COST = 3;

    private final String themeTitle;
    private final List<WordSearchBank.Entry> entries;
    private final WordSearchGrid grid;
    private final WordSearchDefinition.Tier tier;
    private int score;
    private int combo;
    private boolean hintShown;
    private String lastFeedback;

    WordSearchSession(String themeTitle, List<WordSearchBank.Entry> entries, WordSearchDefinition.Tier tier, long seed) {
        this.themeTitle = themeTitle;
        this.entries = new ArrayList<>(entries);
        this.tier = tier;
        List<String> words = new ArrayList<>();
        for (WordSearchBank.Entry e : entries) words.add(e.word);
        this.grid = new WordSearchGrid(words, seed);
    }

    /** The grid, for drawing. */
    public WordSearchGrid grid() { return grid; }

    /** Theme shown above the grid. */
    public String themeTitle() { return themeTitle; }

    @Override public int size() { return entries.size(); }
    @Override public int position() { return Math.min(grid.found().size() + 1, size()); }
    @Override public boolean finished() { return grid.complete(); }
    @Override public int score() { return score; }
    @Override public int lives() { return NO_LIVES; }

    @Override
    public boolean hintAvailable() {
        return !finished() && tier != WordSearchDefinition.Tier.HARD && !hintShown;
    }

    @Override
    public String hint() {
        if (!hintAvailable()) return null;
        hintShown = true;
        for (WordSearchBank.Entry e : entries) {
            if (!grid.found().contains(e.word)) return "A word still to find starts with " + e.word.charAt(0) + ".";
        }
        return null;
    }

    @Override
    public String prompt() {
        if (finished()) return null;
        StringBuilder sb = new StringBuilder("Theme: " + themeTitle + ". Find: ");
        for (int i = 0; i < entries.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(entries.get(i).word);
        }
        return sb.append(".").toString();
    }

    /** Explanation with the reference after the last correct find, or the gentle nudge after a miss. */
    public String lastFeedback() { return lastFeedback; }

    /** Current combo streak within this run. */
    int combo() { return combo; }

    /**
     * The answer is "r1,c1,r2,c2": the start and end cell of a straight selection, zero based.
     */
    @Override
    public Outcome answer(String value) {
        if (finished()) return Outcome.IGNORED;
        int[] cells = parse(value);
        if (cells == null) return Outcome.IGNORED;
        Set<String> before = new HashSet<>(grid.found());
        int[][] hit = grid.select(cells[0], cells[1], cells[2], cells[3]);
        if (hit == null) {
            combo = 0;
            lastFeedback = "Not a word yet. Check the letters in a straight line.";
            return Outcome.WRONG;
        }
        WordSearchBank.Entry found = newlyFound(before);
        combo++;
        int bonus = Math.min(combo - 1, COMBO_CAP) * COMBO_STEP;
        int points = BASE_POINTS + bonus - (hintShown ? HINT_COST : 0);
        score += Math.max(1, points);
        hintShown = false;
        lastFeedback = found == null ? "" : found.word + " (" + found.reference + ")";
        return Outcome.CORRECT;
    }

    /** The entry for the word found since {@code before}, or null when none was. */
    private WordSearchBank.Entry newlyFound(Set<String> before) {
        for (String word : grid.found()) {
            if (before.contains(word)) continue;
            for (WordSearchBank.Entry e : entries) {
                if (e.word.equals(word)) return e;
            }
        }
        return null;
    }

    static int[] parse(String value) {
        if (value == null) return null;
        String[] parts = value.split(",");
        if (parts.length != 4) return null;
        int[] cells = new int[4];
        try {
            for (int i = 0; i < 4; i++) {
                cells[i] = Integer.parseInt(parts[i].trim());
                if (cells[i] < 0 || cells[i] >= WordSearchGrid.SIZE) return null;
            }
        } catch (NumberFormatException e) {
            return null;
        }
        return cells;
    }
}
