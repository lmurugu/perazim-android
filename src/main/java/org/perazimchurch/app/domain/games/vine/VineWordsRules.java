package org.perazimchurch.app.domain.games.vine;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/** Word rules for Vine Words: how a swipe is read, and whether a word can be spelled from the wheel. */
public final class VineWordsRules {

    /** Shortest word a player can swipe. */
    public static final int MIN_SWIPE = 2;

    private VineWordsRules() {}

    /** Keeps letters only, in upper case, so "Vine " and "vine" compare equal. */
    public static String normalise(String swipe) {
        if (swipe == null) return "";
        StringBuilder out = new StringBuilder(swipe.length());
        for (int i = 0; i < swipe.length(); i++) {
            char ch = Character.toUpperCase(swipe.charAt(i));
            if (ch >= 'A' && ch <= 'Z') out.append(ch);
        }
        return out.toString().toUpperCase(Locale.ROOT);
    }

    /** True when every letter of {@code word} is on the wheel, counting repeats. */
    public static boolean spellable(String word, String wheel) {
        Map<Character, Integer> available = counts(wheel);
        for (Map.Entry<Character, Integer> need : counts(word).entrySet()) {
            if (available.getOrDefault(need.getKey(), 0) < need.getValue()) return false;
        }
        return true;
    }

    /** Letter counts of a string, keyed by character. */
    public static Map<Character, Integer> counts(String text) {
        Map<Character, Integer> counts = new HashMap<>();
        for (int i = 0; i < text.length(); i++) {
            counts.merge(text.charAt(i), 1, Integer::sum);
        }
        return counts;
    }
}
