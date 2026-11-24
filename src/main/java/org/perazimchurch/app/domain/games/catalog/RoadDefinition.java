package org.perazimchurch.app.domain.games.catalog;

import org.perazimchurch.app.domain.games.engine.GameDefinition;
import org.perazimchurch.app.domain.games.engine.GameSession;

/**
 * Road to Jerusalem: twelve levels in three tiers. Each run is six Bible stages on the road to
 * Jerusalem. Easy levels (1 to 4) use well-known places, Medium (5 to 8) use distances and routes,
 * and Hard (9 to 12) use exact words from the text.
 */
public final class RoadDefinition implements GameDefinition {

    public static final int LEVELS = 12;

    /** Difficulty tier of a level. */
    public enum Tier { EASY, MEDIUM, HARD }

    private final int iconResource;

    public RoadDefinition(int iconResource) { this.iconResource = iconResource; }

    @Override public String id() { return "road_to_jerusalem"; }
    @Override public String title() { return "Road to Jerusalem"; }
    @Override public String description() { return "Six Bible stages on the road to Jerusalem, with three tiers."; }
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

    /** The bank rows for a tier. */
    static String[] rowsFor(Tier tier) {
        switch (tier) {
            case EASY: return RoadBank.EASY;
            case MEDIUM: return RoadBank.MEDIUM;
            default: return RoadBank.HARD;
        }
    }

    @Override
    public GameSession createSession(int level, long seed) {
        return new RoadSession(rowsFor(tier(level)), seed);
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
