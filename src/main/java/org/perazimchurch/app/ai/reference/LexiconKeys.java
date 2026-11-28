package org.perazimchurch.app.ai.reference;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The original-language words a question names: a Strong's number (G26 or H430) or a Greek or Hebrew word typed in its
 * own script. Pure string work with no Android dependency, so it is tested on the JVM.
 */
public final class LexiconKeys {

    static final int MAX_KEYS = 2;
    static final int MAX_GREEK_STRONGS = 5624;
    static final int MAX_HEBREW_STRONGS = 8674;
    private static final Pattern STRONGS = Pattern.compile(
            "(?<![\\p{L}\\p{N}])([GgHh])\\s?(\\d{1,4})(?![\\p{L}\\p{N}])");
    private static final Pattern ORIGINAL_WORD = Pattern.compile(
            "[\\p{IsGreek}\\p{IsHebrew}][\\p{IsGreek}\\p{IsHebrew}\\p{M}]*");

    private LexiconKeys() {}

    /** One lookup key: a Strong's code such as G0026, or a word in the original script. */
    public static final class Key {
        public final boolean strongs;
        public final String value;

        Key(boolean strongs, String value) {
            this.strongs = strongs;
            this.value = value;
        }
    }

    /** Keys named by the question, Strong's numbers first, at most {@link #MAX_KEYS}. */
    public static List<Key> parse(String question) {
        List<Key> keys = new ArrayList<>();
        if (question == null || question.trim().isEmpty()) return keys;
        Set<String> seen = new LinkedHashSet<>();
        Matcher numbers = STRONGS.matcher(question);
        while (numbers.find() && keys.size() < MAX_KEYS) {
            char testament = Character.toUpperCase(numbers.group(1).charAt(0));
            int number = Integer.parseInt(numbers.group(2));
            int limit = testament == 'G' ? MAX_GREEK_STRONGS : MAX_HEBREW_STRONGS;
            if (number < 1 || number > limit) continue;
            String code = testament + String.format(Locale.ROOT, "%04d", number);
            if (seen.add(code)) keys.add(new Key(true, code));
        }
        Matcher words = ORIGINAL_WORD.matcher(Normalizer.normalize(question, Normalizer.Form.NFC));
        while (words.find() && keys.size() < MAX_KEYS) {
            String word = words.group();
            if (seen.add(word)) keys.add(new Key(false, word));
        }
        return keys;
    }
}
