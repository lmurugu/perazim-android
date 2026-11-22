package org.perazimchurch.app.domain.games;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Turns a citation such as "1 Samuel 3:4" into the reader's book id and chapter.
 * Book ids are the ones the Bible reader and database use (GEN, 1SA, JHN and so on).
 * Pure domain logic with no Android types.
 */
public final class BookReference {
    /** A parsed citation. */
    public static final class Target {
        public final String bookId;
        public final int chapter;

        Target(String bookId, int chapter) {
            this.bookId = bookId;
            this.chapter = chapter;
        }
    }

    private static final List<String> NAMES = Arrays.asList(
            "Genesis", "Exodus", "Leviticus", "Numbers", "Deuteronomy", "Joshua", "Judges", "Ruth",
            "1 Samuel", "2 Samuel", "1 Kings", "2 Kings", "1 Chronicles", "2 Chronicles", "Ezra", "Nehemiah",
            "Esther", "Job", "Psalms", "Proverbs", "Ecclesiastes", "Song of Solomon", "Isaiah", "Jeremiah",
            "Lamentations", "Ezekiel", "Daniel", "Hosea", "Joel", "Amos", "Obadiah", "Jonah", "Micah", "Nahum",
            "Habakkuk", "Zephaniah", "Haggai", "Zechariah", "Malachi", "Matthew", "Mark", "Luke", "John", "Acts",
            "Romans", "1 Corinthians", "2 Corinthians", "Galatians", "Ephesians", "Philippians", "Colossians",
            "1 Thessalonians", "2 Thessalonians", "1 Timothy", "2 Timothy", "Titus", "Philemon", "Hebrews",
            "James", "1 Peter", "2 Peter", "1 John", "2 John", "3 John", "Jude", "Revelation");

    private static final List<String> IDS = Arrays.asList(
            "GEN", "EXO", "LEV", "NUM", "DEU", "JOS", "JDG", "RUT", "1SA", "2SA", "1KI", "2KI", "1CH", "2CH",
            "EZR", "NEH", "EST", "JOB", "PSA", "PRO", "ECC", "SNG", "ISA", "JER", "LAM", "EZK", "DAN", "HOS",
            "JOL", "AMO", "OBA", "JON", "MIC", "NAM", "HAB", "ZEP", "HAG", "ZEC", "MAL", "MAT", "MRK", "LUK",
            "JHN", "ACT", "ROM", "1CO", "2CO", "GAL", "EPH", "PHP", "COL", "1TH", "2TH", "1TI", "2TI", "TIT",
            "PHM", "HEB", "JAS", "1PE", "2PE", "1JN", "2JN", "3JN", "JUD", "REV");

    private static final Map<String, String> BY_NAME = new HashMap<>();
    static {
        for (int i = 0; i < NAMES.size(); i++) BY_NAME.put(key(NAMES.get(i)), IDS.get(i));
        BY_NAME.put("psalm", "PSA");
        BY_NAME.put("song of songs", "SNG");
        BY_NAME.put("canticles", "SNG");
    }

    private static final Pattern CITATION = Pattern.compile("^\\s*((?:[1-3]\\s+)?[A-Za-z][A-Za-z ]*?)\\s+(\\d{1,3})(?::\\d+.*)?\\s*$");

    private BookReference() { }

    /** Returns the book id and chapter for a citation, or null when the book is not recognised. */
    public static Target parse(String citation) {
        if (citation == null) return null;
        Matcher m = CITATION.matcher(citation);
        if (!m.matches()) return null;
        String bookId = BY_NAME.get(key(m.group(1)));
        if (bookId == null) return null;
        int chapter = Integer.parseInt(m.group(2));
        if (chapter < 1) return null;
        return new Target(bookId, chapter);
    }

    private static String key(String name) {
        return name.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }
}
