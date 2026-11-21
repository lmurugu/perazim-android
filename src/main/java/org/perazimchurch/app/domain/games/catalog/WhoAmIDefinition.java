package org.perazimchurch.app.domain.games.catalog;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import org.perazimchurch.app.domain.games.engine.GameDefinition;
import org.perazimchurch.app.domain.games.engine.GameSession;

/**
 * Who am I?: read a Bible character's clues, then type the name. Twelve levels in three tiers:
 * Easy (1 to 4), Medium (5 to 8) and Hard (9 to 12). Each level uses its own block of people,
 * so a level never repeats a name from another level in the same seed.
 */
public final class WhoAmIDefinition implements GameDefinition {

    public static final int LEVELS = 12;
    public static final int PER_LEVEL = 8;

    /** Difficulty tier. Easy and Medium offer a hint; Hard does not. */
    public enum Tier { EASY, MEDIUM, HARD }

    private final int iconResource;

    public WhoAmIDefinition(int iconResource) { this.iconResource = iconResource; }

    public static WhoAmIDefinition create(int iconResource) { return new WhoAmIDefinition(iconResource); }

    @Override public String id() { return "who_am_i"; }
    @Override public String title() { return "Who am I?"; }
    @Override public String description() { return "Read a Bible character's clues, then type the name."; }
    @Override public int iconResource() { return iconResource; }
    @Override public int levelCount() { return LEVELS; }

    @Override
    public String levelName(int level) {
        int n = clamp(level);
        int within = (n - 1) % 4 + 1;
        return tierLabel(tier(n)) + " " + within;
    }

    /** Tier for a 1-based level, clamped to the valid range. */
    public static Tier tier(int level) {
        int n = clamp(level);
        if (n <= 4) return Tier.EASY;
        if (n <= 8) return Tier.MEDIUM;
        return Tier.HARD;
    }

    @Override
    public GameSession createSession(int level, long seed) {
        int n = clamp(level);
        List<WhoAmIBank.Person> shuffled = new ArrayList<>(WhoAmIBank.PEOPLE);
        Collections.shuffle(shuffled, new Random(seed));
        int from = (n - 1) * PER_LEVEL;
        List<WhoAmIBank.Person> round = new ArrayList<>(shuffled.subList(from, from + PER_LEVEL));
        return new WhoAmISession(round, tier(n));
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
