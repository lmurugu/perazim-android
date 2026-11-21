package org.perazimchurch.app.domain.games.catalog;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import org.perazimchurch.app.domain.games.engine.GameSession;

/**
 * One run of Memory Match. Cards are shuffled by seed. The first flip is a move that neither wins
 * nor fails; the second flip either makes a pair (correct) or misses (wrong, and the cards turn back).
 * Combo bonuses build on consecutive pairs within this run only. The hint names the first letter of
 * a pair that is still face down.
 */
public final class MemorySession implements GameSession {

    static final int BASE_POINTS = 10;
    static final int COMBO_STEP = 5;
    static final int COMBO_CAP = 5;
    static final int HINT_COST = 3;

    private final List<MemoryBank.Pair> pairs;
    private final List<Card> cards = new ArrayList<>();
    private final boolean[] matched;
    private final MemoryDefinition.Tier tier;
    private int firstFlip = -1;
    private int score;
    private int combo;
    private int pairsFound;
    private boolean hintShown;
    private String lastFeedback;

    /** One face-down card: which pair it belongs to and the word on it. */
    public static final class Card {
        public final int pair;
        public final String label;

        Card(int pair, String label) {
            this.pair = pair;
            this.label = label;
        }
    }

    MemorySession(List<MemoryBank.Pair> pairs, MemoryDefinition.Tier tier, long seed) {
        this.pairs = new ArrayList<>(pairs);
        this.tier = tier;
        for (int i = 0; i < this.pairs.size(); i++) {
            cards.add(new Card(i, this.pairs.get(i).a));
            cards.add(new Card(i, this.pairs.get(i).b));
        }
        Collections.shuffle(cards, new Random(seed));
        matched = new boolean[cards.size()];
    }

    /** The cards in play, shuffled. */
    public List<Card> cards() { return Collections.unmodifiableList(cards); }

    /** Whether the card at this index has been matched. */
    public boolean isMatched(int index) { return matched[index]; }

    /** Feedback after the last pair attempt, or null before the first attempt. */
    public String lastFeedback() { return lastFeedback; }

    /** Current combo streak within this run. */
    int combo() { return combo; }

    @Override public int size() { return pairs.size(); }
    @Override public int position() { return Math.min(pairsFound + 1, size()); }
    @Override public boolean finished() { return pairsFound == pairs.size(); }
    @Override public int score() { return score; }
    @Override public int lives() { return NO_LIVES; }

    @Override
    public boolean hintAvailable() {
        return !finished() && tier != MemoryDefinition.Tier.HARD && !hintShown;
    }

    @Override
    public String hint() {
        if (!hintAvailable()) return null;
        hintShown = true;
        for (int i = 0; i < cards.size(); i++) {
            if (!matched[i]) {
                String label = cards.get(i).label;
                return "A pair still face down starts with " + label.charAt(0) + ".";
            }
        }
        return null;
    }

    @Override
    public String prompt() {
        return finished() ? null : "Flip two cards at a time to find the matching pairs.";
    }

    /** Flips the card at this index (as text). The first flip returns MOVED. */
    @Override
    public Outcome answer(String value) {
        if (finished()) return Outcome.IGNORED;
        int index;
        try {
            index = Integer.parseInt(value == null ? "" : value.trim());
        } catch (NumberFormatException e) {
            return Outcome.IGNORED;
        }
        if (index < 0 || index >= cards.size() || matched[index] || index == firstFlip) return Outcome.IGNORED;
        if (firstFlip < 0) {
            firstFlip = index;
            return Outcome.MOVED;
        }
        int first = firstFlip;
        firstFlip = -1;
        Card a = cards.get(first);
        Card b = cards.get(index);
        if (a.pair == b.pair) {
            matched[first] = true;
            matched[index] = true;
            pairsFound++;
            combo++;
            int bonus = Math.min(combo - 1, COMBO_CAP) * COMBO_STEP;
            int points = BASE_POINTS + bonus - (hintShown ? HINT_COST : 0);
            score += Math.max(1, points);
            hintShown = false;
            MemoryBank.Pair pair = pairs.get(a.pair);
            lastFeedback = pair.a + " and " + pair.b + " (" + pair.reference + ")";
            return Outcome.CORRECT;
        }
        combo = 0;
        lastFeedback = "Not a pair. Try to remember where they were.";
        return Outcome.WRONG;
    }
}
