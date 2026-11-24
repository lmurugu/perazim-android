package org.perazimchurch.app.domain.model;

/**
 * Single source of truth for Spiritual XP levels. The Profile screen, the Home HUD and the offline assistant must all
 * use these values so they never disagree about a member's level or progress.
 */
public final class SpiritualLevel {

    /** XP needed to advance one level. */
    public static final int XP_PER_LEVEL = 500;

    private SpiritualLevel() {}

    /** Level for the given XP, starting at 1. Negative XP is treated as 0. */
    public static int levelFor(int xp) {
        return Math.max(0, xp) / XP_PER_LEVEL + 1;
    }

    /** XP earned within the current level, from 0 up to XP_PER_LEVEL - 1. */
    public static int xpIntoLevel(int xp) {
        return Math.max(0, xp) % XP_PER_LEVEL;
    }

    /** XP still needed to reach the next level. */
    public static int xpToNextLevel(int xp) {
        return XP_PER_LEVEL - xpIntoLevel(xp);
    }
}
