package org.perazimchurch.app.ai.guide;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * The user skill: reads mood and explicit style requests from a single question and keeps only the result.
 * The question text itself is never stored.
 */
public final class UserSkill {
    private static final List<String> POSITIVE = Arrays.asList("thank", "thanks", "helpful", "great", "blessed",
            "amen", "appreciate", "good", "love", "wonderful", "asante", "barikiwa", "nice");
    private static final List<String> NEGATIVE = Arrays.asList("useless", "wrong", "confused", "unhelpful", "bad",
            "annoying", "slow", "terrible", "stupid", "robot", "frustrating", "angry", "tired");

    private UserSkill() {}

    /** Mood of one message from -1 (negative) to 1 (positive). Zero when no clear signal. */
    public static double mood(String question) {
        List<String> words = GuideText.words(question);
        int pos = 0, neg = 0;
        for (String w : words) {
            if (POSITIVE.contains(w)) pos++;
            if (NEGATIVE.contains(w)) neg++;
        }
        if (pos + neg == 0) return 0.0;
        return (double) (pos - neg) / (pos + neg);
    }

    /** Explicit requests like "shorter please" or "explain it simply". Each entry is a "key=value" pair. */
    public static List<String> preferences(String question) {
        String t = " " + GuideText.normalise(question) + " ";
        List<String> out = new ArrayList<>();
        if (has(t, "shorter", "brief", "short answer", "one line", "yes or no", "too long", "be short", "in short"))
            out.add("length=short");
        if (has(t, "more detail", "in detail", "explain more", "longer", "elaborate", "full answer"))
            out.add("length=medium");
        if (has(t, "simple", "simply", "easy words", "for a child", "like i am", "kids", "plain english"))
            out.add("reading=simple");
        if (has(t, "formal", "respectful tone"))
            out.add("tone=formal");
        if (has(t, "warmer", "gentle", "kind words", "encouraging"))
            out.add("tone=warm");
        if (has(t, "straight to the point", "plain", "direct answer", "just tell me"))
            out.add("tone=plain");
        return out;
    }

    private static boolean has(String paddedText, String... phrases) {
        for (String p : phrases) {
            if (paddedText.contains(" " + p + " ")) return true;
        }
        return false;
    }
}
