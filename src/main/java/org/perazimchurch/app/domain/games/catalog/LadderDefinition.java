package org.perazimchurch.app.domain.games.catalog;

import org.perazimchurch.app.domain.games.LadderOfFaith;
import org.perazimchurch.app.domain.games.engine.GameDefinition;
import org.perazimchurch.app.domain.games.engine.GameSession;

/**
 * Ladder of Faith: climb twenty rungs with three lives, in twelve levels across three tiers.
 * Easy levels (1 to 4) put most rungs on easy questions, Medium (5 to 8) balances the mix and
 * Hard (9 to 12) puts most rungs on hard questions. The one hint is the Verse Peek lifeline.
 */
public final class LadderDefinition implements GameDefinition {

    public static final int LEVELS = 12;

    /** Difficulty tier of a level. */
    public enum Tier { EASY, MEDIUM, HARD }

    private final int iconResource;

    public LadderDefinition(int iconResource) { this.iconResource = iconResource; }

    @Override public String id() { return "ladder_of_faith"; }
    @Override public String title() { return "Ladder of Faith"; }
    @Override public String description() { return "Climb 20 rungs from easy to hard with three lives."; }
    @Override public int iconResource() { return iconResource; }
    @Override public int levelCount() { return LEVELS; }

    @Override
    public String levelName(int level) {
        int n = clamp(level);
        return tierLabel(tier(n)) + " " + ((n - 1) % 4 + 1);
    }

    /** Tier for a 1-based level, clamped to the valid range. */
    public static Tier tier(int level) {
        int n = clamp(level);
        if (n <= 4) return Tier.EASY;
        if (n <= 8) return Tier.MEDIUM;
        return Tier.HARD;
    }

    /** Easy rungs in a run at this tier. Hard rungs are whatever is left of the twenty. */
    public static int easyRungsFor(Tier tier) {
        switch (tier) {
            case EASY: return 10;
            case MEDIUM: return 6;
            default: return 3;
        }
    }

    /** Medium rungs in a run at this tier. */
    public static int mediumRungsFor(Tier tier) {
        switch (tier) {
            case EASY: return 6;
            case MEDIUM: return 8;
            default: return 7;
        }
    }

    @Override
    public GameSession createSession(int level, long seed) {
        Tier tier = tier(level);
        return new LadderSession(new LadderOfFaith(seed, easyRungsFor(tier), mediumRungsFor(tier)), seed);
    }

    static int clamp(int level) {
        return Math.max(1, Math.min(LEVELS, level));
    }

    private static String tierLabel(Tier tier) {
        switch (tier) {
            case EASY: return "Easy";
            case MEDIUM: return "Medium";
            default: return "Hard";
        }
    }
}
