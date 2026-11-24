package org.perazimchurch.app.domain.games.catalog;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import org.perazimchurch.app.domain.games.engine.GameDefinition;
import org.perazimchurch.app.domain.games.engine.GameSession;

/**
 * Word Search: find the themed Bible words in an 8 by 8 grid. Twelve levels in three tiers: Easy
 * (1 to 4) hunts four words and offers a hint, Medium (5 to 8) hunts five words with a hint, and
 * Hard (9 to 12) hunts six words with no hint. Each level uses a different theme from the bank.
 */
public final class WordSearchDefinition implements GameDefinition {

    public static final int LEVELS = 12;

    /** Difficulty tier. Easy and Medium offer a hint; Hard does not. */
    public enum Tier { EASY, MEDIUM, HARD }

    private final int iconResource;

    public WordSearchDefinition(int iconResource) { this.iconResource = iconResource; }

    @Override public String id() { return "word_search"; }
    @Override public String title() { return "Word Search"; }
    @Override public String description() { return "Find the Bible words hidden in the letters."; }
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

    /** Words to find on a level: four on Easy, five on Medium, six on Hard. */
    public static int wordsFor(Tier tier) {
        switch (tier) {
            case EASY: return 4;
            case MEDIUM: return 5;
            default: return 6;
        }
    }

    @Override
    public GameSession createSession(int level, long seed) {
        int n = clamp(level);
        Tier tier = tier(n);
        List<WordSearchBank.Theme> themes = new ArrayList<>(WordSearchBank.THEMES);
        Collections.shuffle(themes, new Random(seed));
        WordSearchBank.Theme theme = themes.get((n - 1) % themes.size());
        List<WordSearchBank.Entry> words = new ArrayList<>(theme.words.subList(0, wordsFor(tier)));
        return new WordSearchSession(theme.title, words, tier, seed);
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
