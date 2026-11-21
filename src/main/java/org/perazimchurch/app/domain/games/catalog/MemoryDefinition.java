package org.perazimchurch.app.domain.games.catalog;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import org.perazimchurch.app.domain.games.engine.GameDefinition;
import org.perazimchurch.app.domain.games.engine.GameSession;

/**
 * Memory Match: flip two cards at a time and find the matching pair. Twelve levels in three tiers:
 * Easy (1 to 4) uses three pairs and offers a hint, Medium (5 to 8) uses four pairs with a hint, and
 * Hard (9 to 12) uses five pairs with no hint. Each level draws a different pair set from the bank.
 */
public final class MemoryDefinition implements GameDefinition {

    public static final int LEVELS = 12;

    /** Difficulty tier. Easy and Medium offer a hint; Hard does not. */
    public enum Tier { EASY, MEDIUM, HARD }

    private final int iconResource;

    public MemoryDefinition(int iconResource) { this.iconResource = iconResource; }

    @Override public String id() { return "memory_match"; }
    @Override public String title() { return "Memory Match"; }
    @Override public String description() { return "Flip two tiles at a time and match the Bible pairs."; }
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

    /** Pairs on a level: three on Easy, four on Medium, five on Hard. */
    public static int pairsFor(Tier tier) {
        switch (tier) {
            case EASY: return 3;
            case MEDIUM: return 4;
            default: return 5;
        }
    }

    @Override
    public GameSession createSession(int level, long seed) {
        int n = clamp(level);
        Tier tier = tier(n);
        List<MemoryBank.PairSet> sets = new ArrayList<>(MemoryBank.SETS);
        Collections.shuffle(sets, new Random(seed));
        MemoryBank.PairSet set = sets.get((n - 1) % sets.size());
        List<MemoryBank.Pair> pairs = new ArrayList<>(set.pairs);
        Collections.shuffle(pairs, new Random(seed + n));
        return new MemorySession(pairs.subList(0, pairsFor(tier)), tier, seed);
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
