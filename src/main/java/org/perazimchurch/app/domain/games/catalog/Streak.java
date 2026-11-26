package org.perazimchurch.app.domain.games.catalog;

/**
 * Counts right answers in a row within one run. A wrong answer resets it. Every
 * {@link #BONUS_EVERY} right answers in a row adds {@link #BONUS_POINTS} to the run score.
 * Pure Java, so the rule is covered by plain unit tests.
 */
public final class Streak {
    /** Right answers in a row needed for each bonus. */
    public static final int BONUS_EVERY = 5;
    /** Points added to the run score for each bonus. */
    public static final int BONUS_POINTS = 2;

    private int current;
    private int best;
    private int bonuses;
    private boolean grew;
    private boolean bonusAwarded;

    /** Records one answer. A right answer grows the streak; a wrong one resets it. */
    public void record(boolean correct) {
        grew = false;
        bonusAwarded = false;
        if (!correct) {
            current = 0;
            return;
        }
        current++;
        grew = true;
        best = Math.max(best, current);
        if (current % BONUS_EVERY == 0) {
            bonuses++;
            bonusAwarded = true;
        }
    }

    /** Right answers in a row right now. */
    public int current() { return current; }

    /** The longest run of right answers in this session. */
    public int best() { return best; }

    /** Points earned from streak bonuses so far in this run. */
    public int bonusPoints() { return bonuses * BONUS_POINTS; }

    /** True when the last answer grew the streak. */
    public boolean grew() { return grew; }

    /** True when the last answer earned a streak bonus. */
    public boolean bonusAwarded() { return bonusAwarded; }
}
