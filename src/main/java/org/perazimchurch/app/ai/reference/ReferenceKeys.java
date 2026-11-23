package org.perazimchurch.app.ai.reference;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Pure helpers for verse keys and book names used by the reference index. No Android types, so host tests run.
 *
 * A verse key is {@code book * 1_000_000 + chapter * 1000 + verse}. Keys sort in canonical order, and the
 * same key layout is used by the reference database (see scripts/reference/build_reference_db.py).
 */
public final class ReferenceKeys {

    /** Canonical English book names, Genesis = 1 to Revelation = 66. */
    public static final List<String> BOOK_NAMES = Arrays.asList(
            "Genesis", "Exodus", "Leviticus", "Numbers", "Deuteronomy", "Joshua", "Judges", "Ruth",
            "1 Samuel", "2 Samuel", "1 Kings", "2 Kings", "1 Chronicles", "2 Chronicles", "Ezra", "Nehemiah",
            "Esther", "Job", "Psalms", "Proverbs", "Ecclesiastes", "Song of Solomon", "Isaiah", "Jeremiah",
            "Lamentations", "Ezekiel", "Daniel", "Hosea", "Joel", "Amos", "Obadiah", "Jonah", "Micah", "Nahum",
            "Habakkuk", "Zephaniah", "Haggai", "Zechariah", "Malachi", "Matthew", "Mark", "Luke", "John", "Acts",
            "Romans", "1 Corinthians", "2 Corinthians", "Galatians", "Ephesians", "Philippians", "Colossians",
            "1 Thessalonians", "2 Thessalonians", "1 Timothy", "2 Timothy", "Titus", "Philemon", "Hebrews",
            "James", "1 Peter", "2 Peter", "1 John", "2 John", "3 John", "Jude", "Revelation");

    /** Common alternative names and abbreviations. Lower-case keys. */
    private static final Map<String, Integer> ALIASES = new HashMap<>();
    static {
        alias(1, "gen", "genesis");
        alias(2, "ex", "exo", "exod", "exodus");
        alias(3, "lev", "leviticus");
        alias(4, "num", "numbers");
        alias(5, "deut", "dt", "deuteronomy");
        alias(6, "josh", "joshua");
        alias(7, "judg", "judges");
        alias(9, "1 sam", "1sam", "first samuel");
        alias(10, "2 sam", "2sam", "second samuel");
        alias(11, "1 kings", "1 kgs", "1kgs");
        alias(12, "2 kings", "2 kgs", "2kgs");
        alias(18, "job");
        alias(19, "ps", "psa", "psalm", "psalms");
        alias(20, "prov", "proverb", "proverbs");
        alias(22, "song", "canticles", "song of songs", "song of solomon");
        alias(23, "isa", "isaiah");
        alias(24, "jer", "jeremiah");
        alias(26, "ezek", "ezekiel");
        alias(40, "mt", "matt", "matthew");
        alias(41, "mk", "mark");
        alias(42, "lk", "luke");
        alias(43, "jn", "john");
        alias(45, "rom", "romans");
        alias(46, "1 cor", "1cor", "first corinthians");
        alias(47, "2 cor", "2cor", "second corinthians");
        alias(58, "heb", "hebrews");
        alias(59, "jas", "james");
        alias(60, "1 pet", "1pet", "first peter");
        alias(62, "1 jn", "1jn", "1 john", "first john");
        alias(66, "rev", "revelations", "revelation");
    }

    private static final Pattern CHAPTER_VERSE = Pattern.compile("(?<![\\p{L}\\p{N}])(\\d{1,3}):(\\d{1,3})(?!\\d)");

    private ReferenceKeys() {}

    private static void alias(int book, String... names) {
        for (String name : names) ALIASES.put(name, book);
    }

    /** Returns the verse key, or -1 when the book, chapter or verse is out of range. */
    public static int verseKey(int book, int chapter, int verse) {
        if (book < 1 || book > 66 || chapter < 1 || chapter > 999 || verse < 1 || verse > 999) return -1;
        return book * 1_000_000 + chapter * 1000 + verse;
    }

    /** Book number for a name or common abbreviation, or 0 when unknown. */
    public static int bookNumber(String name) {
        if (name == null) return 0;
        String key = name.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
        if (key.isEmpty()) return 0;
        for (int i = 0; i < BOOK_NAMES.size(); i++) {
            if (BOOK_NAMES.get(i).toLowerCase(Locale.ROOT).equals(key)) return i + 1;
        }
        Integer alias = ALIASES.get(key);
        return alias == null ? 0 : alias;
    }

    /** Chapter headword used by the commentaries, such as "Genesis 1". Returns "" for an invalid key. */
    public static String chapterHeadword(int key) {
        if (key <= 0) return "";
        int book = key / 1_000_000;
        int chapter = (key / 1000) % 1000;
        if (book < 1 || book > 66 || chapter < 1) return "";
        return BOOK_NAMES.get(book - 1) + " " + chapter;
    }

    /** Canonical display form for a verse key, such as "John 3:16". Returns "" for an invalid key. */
    public static String format(int key) {
        if (key <= 0) return "";
        int book = key / 1_000_000;
        int chapter = (key / 1000) % 1000;
        int verse = key % 1000;
        if (book < 1 || book > 66) return "";
        return BOOK_NAMES.get(book - 1) + " " + chapter + ":" + verse;
    }

    /**
     * Finds the first "chapter:verse" in a question and the book name directly before it. The longest run of
     * up to four words ending before the chapter and verse that names a book wins, so "What does Romans 8:28
     * mean" resolves to Romans 8:28. Returns -1 when no reference is present.
     */
    public static int parseReference(String question) {
        if (question == null) return -1;
        Matcher m = CHAPTER_VERSE.matcher(question);
        if (!m.find()) return -1;
        String before = question.substring(0, m.start()).trim();
        String[] words = before.isEmpty() ? new String[0] : before.split("\\s+");
        for (int length = Math.min(4, words.length); length >= 1; length--) {
            StringBuilder name = new StringBuilder();
            for (int i = words.length - length; i < words.length; i++) {
                if (name.length() > 0) name.append(' ');
                name.append(words[i].replaceAll("[^\\p{L}\\p{N}]", ""));
            }
            int book = bookNumber(name.toString());
            if (book > 0) {
                int chapter = Integer.parseInt(m.group(1));
                int verse = Integer.parseInt(m.group(2));
                return verseKey(book, chapter, verse);
            }
        }
        return -1;
    }
}
