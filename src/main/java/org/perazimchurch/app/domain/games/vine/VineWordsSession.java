package org.perazimchurch.app.domain.games.vine;

import org.perazimchurch.app.domain.games.engine.GameSession;
import org.perazimchurch.app.domain.games.engine.StarRating;

import java.util.ArrayList;
import java.util.List;

/**
 * One run of Vine Words on one level. Owns the solved words, the bonus word and the letters
 * revealed by hints. Pure Java: the UI draws the grid from {@link #knownLetter(int, int)}.
 *
 * <ul>
 *   <li>A swipe that spells a target word fills its slot ({@link Outcome#CORRECT}).</li>
 *   <li>A swipe that matches no slot is ignored, not penalised ({@link Outcome#IGNORED}).</li>
 *   <li>The bonus word scores a point but never blocks the level.</li>
 *   <li>Hints are free: one letter per target word, each one lowers the star cap to two.</li>
 * </ul>
 */
public final class VineWordsSession implements GameSession {

    private final VineWordsLevel level;
    private final List<CrosswordGrid.Slot> targets;
    private final boolean[] solved;
    private final boolean[] hinted;
    private final char[][] revealed;
    private final boolean[][] known;
    private int solvedCount;
    private int hintsUsed;
    private boolean bonusFound;
    private String lastSolved;

    public VineWordsSession(VineWordsLevel level) {
        this.level = level;
        this.targets = level.grid().slots();
        this.solved = new boolean[targets.size()];
        this.hinted = new boolean[targets.size()];
        this.revealed = new char[level.grid().rows()][level.grid().cols()];
        this.known = new boolean[level.grid().rows()][level.grid().cols()];
    }

    public VineWordsLevel level() { return level; }

    @Override public int size() { return targets.size(); }

    @Override public int position() { return Math.min(solvedCount + 1, targets.size()); }

    @Override public boolean finished() { return solvedCount == targets.size(); }

    /** Solved target words, plus one point for the bonus word. */
    @Override public int score() { return solvedCount + (bonusFound ? 1 : 0); }

    @Override public int lives() { return NO_LIVES; }

    /** A hint is available while some unsolved target word has not had its letter revealed. */
    @Override public boolean hintAvailable() {
        if (finished()) return false;
        for (int i = 0; i < targets.size(); i++) {
            if (!solved[i] && !hinted[i]) return true;
        }
        return false;
    }

    /**
     * Reveals one letter of the first unsolved word that has no hint yet. The revealed letter
     * stays on the grid, and the word still has to be swiped. Returns null when no hint is left.
     */
    @Override public String hint() {
        for (int i = 0; i < targets.size(); i++) {
            if (solved[i] || hinted[i]) continue;
            CrosswordGrid.Slot slot = targets.get(i);
            hinted[i] = true;
            hintsUsed++;
            for (int index = 0; index < slot.word.length(); index++) {
                int row = slot.rowAt(index);
                int col = slot.colAt(index);
                if (!known[row][col]) {
                    known[row][col] = true;
                    revealed[row][col] = slot.word.charAt(index);
                    return "A word has " + slot.word.length() + " letters. Letter " + (index + 1)
                            + " is " + slot.word.charAt(index) + ".";
                }
            }
            return "A word has " + slot.word.length() + " letters, and its letters are already showing.";
        }
        return null;
    }

    /** Number of hints used so far in this run. */
    public int hintsUsed() { return hintsUsed; }

    /** The word a swipe just solved, or null. The UI uses it for the glow. */
    public String lastSolved() { return lastSolved; }

    /** Stars for this run. Hints cap the result at two stars. */
    public int stars() {
        int stars = StarRating.stars(score(), size(), NO_LIVES);
        if (hintsUsed > 0 && stars > 2) stars = 2;
        return stars;
    }

    /**
     * Answers a swipe. Letters are normalised first, so case and spaces do not matter.
     * The same word swiped twice is ignored the second time.
     */
    @Override public Outcome answer(String value) {
        lastSolved = null;
        String word = VineWordsRules.normalise(value);
        if (word.length() < VineWordsRules.MIN_SWIPE || finished()) return Outcome.IGNORED;
        if (!VineWordsRules.spellable(word, level.wheel())) return Outcome.IGNORED;
        for (int i = 0; i < targets.size(); i++) {
            if (targets.get(i).word.equals(word)) {
                if (solved[i]) return Outcome.IGNORED;
                solved[i] = true;
                solvedCount++;
                lastSolved = word;
                fillWord(targets.get(i));
                return Outcome.CORRECT;
            }
        }
        if (word.equals(level.bonus()) && !bonusFound) {
            bonusFound = true;
            return Outcome.CORRECT;
        }
        return Outcome.IGNORED;
    }

    /** The letter shown on a cell, or 0 when the cell is empty or still hidden. */
    public char knownLetter(int row, int col) {
        if (!known[row][col]) return 0;
        if (revealed[row][col] != 0) return revealed[row][col];
        return level.grid().letterAt(row, col);
    }

    /** True when a hint has revealed this cell's letter. */
    public boolean hintedCell(int row, int col) {
        return revealed[row][col] != 0;
    }

    /** True when a cell belongs to a placed word. */
    public boolean isLetterCell(int row, int col) {
        return level.grid().isLetterCell(row, col);
    }

    /** Words the player has solved, for the results screen. */
    public List<String> solvedWords() {
        List<String> words = new ArrayList<>();
        for (int i = 0; i < targets.size(); i++) if (solved[i]) words.add(targets.get(i).word);
        return words;
    }

    private void fillWord(CrosswordGrid.Slot slot) {
        for (int index = 0; index < slot.word.length(); index++) {
            known[slot.rowAt(index)][slot.colAt(index)] = true;
        }
    }
}
