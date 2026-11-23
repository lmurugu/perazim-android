package org.perazimchurch.app.ai.guide;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Text helpers shared by the guide. Pure Java so the logic can be unit-tested without Android. */
public final class GuideText {
    private GuideText() {}

    /** Lower-case, curly quotes straightened, punctuation removed, single spaces. */
    public static String normalise(String text) {
        if (text == null) return "";
        String t = text.toLowerCase(Locale.ROOT)
                .replace('’', '\'').replace('‘', '\'')
                .replace('“', '"').replace('”', '"');
        StringBuilder out = new StringBuilder(t.length());
        for (int i = 0; i < t.length(); i++) {
            char c = t.charAt(i);
            if (Character.isLetterOrDigit(c)) out.append(c);
            else out.append(' ');
        }
        return out.toString().replaceAll("\\s+", " ").trim();
    }

    /** True when the normalised phrase occurs as whole words inside the normalised text. */
    public static boolean hasPhrase(String normalisedText, String normalisedPhrase) {
        if (normalisedText.isEmpty() || normalisedPhrase.isEmpty()) return false;
        return (" " + normalisedText + " ").contains(" " + normalisedPhrase + " ");
    }

    public static List<String> words(String text) {
        List<String> out = new ArrayList<>();
        String n = normalise(text);
        if (n.isEmpty()) return out;
        for (String w : n.split(" ")) if (!w.isEmpty()) out.add(w);
        return out;
    }

    /** Removes control characters and clips to a maximum length at a word boundary. */
    public static String clean(String text, int max) {
        if (text == null) return "";
        String t = text.replaceAll("[\\p{Cntrl}]+", " ").replaceAll("\\s+", " ").trim();
        if (t.length() <= max) return t;
        int cut = t.lastIndexOf(' ', max);
        return (cut > max / 2 ? t.substring(0, cut) : t.substring(0, max)).trim();
    }
}
