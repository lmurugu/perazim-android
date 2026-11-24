package org.perazimchurch.app.ai.library;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Chapter and verse counts for the 66 books, read from the generated index
 * ({@code assets/ai/library/scripture/kjv-verse-counts.json}). A reference is valid only when its book, chapter
 * and verse all exist. Anything else (for example Ezekiel 50:4, or Ezekiel 48:36) is rejected before the guide
 * shows or quotes it. Pure Java apart from org.json, so host tests run without a device.
 */
public final class ScriptureRangeTable {

    private final Map<Integer, int[]> verseCounts;
    private final Map<String, Integer> bookNumbers;

    private ScriptureRangeTable(Map<Integer, int[]> verseCounts, Map<String, Integer> bookNumbers) {
        this.verseCounts = Collections.unmodifiableMap(verseCounts);
        this.bookNumbers = Collections.unmodifiableMap(bookNumbers);
    }

    /** Parses the generated index. Throws if the file is not the expected 66-book table. */
    public static ScriptureRangeTable fromJson(String json) throws JSONException {
        JSONArray books = new JSONObject(json).getJSONArray("books");
        if (books.length() != 66) throw new JSONException("expected 66 books, found " + books.length());
        Map<Integer, int[]> counts = new HashMap<>();
        Map<String, Integer> numbers = new HashMap<>();
        for (int i = 0; i < books.length(); i++) {
            JSONObject book = books.getJSONObject(i);
            int number = book.getInt("book");
            JSONArray chapters = book.getJSONArray("chapterVerseCounts");
            int[] row = new int[chapters.length() + 1];
            for (int c = 1; c <= chapters.length(); c++) row[c] = chapters.getInt(c - 1);
            counts.put(number, row);
            numbers.put(book.getString("name").toLowerCase(java.util.Locale.ROOT), number);
        }
        return new ScriptureRangeTable(counts, numbers);
    }

    /** True when the book (1 to 66), chapter and verse all exist in the table. */
    public boolean isValid(int book, int chapter, int verse) {
        int[] row = verseCounts.get(book);
        if (row == null || chapter < 1 || chapter >= row.length || verse < 1) return false;
        return verse <= row[chapter];
    }

    /** Number of chapters in the book, or 0 when the book is unknown. */
    public int chapterCount(int book) {
        int[] row = verseCounts.get(book);
        return row == null ? 0 : row.length - 1;
    }

    /** Verse count of one chapter, or 0 when the chapter does not exist. */
    public int verseCount(int book, int chapter) {
        int[] row = verseCounts.get(book);
        if (row == null || chapter < 1 || chapter >= row.length) return 0;
        return row[chapter];
    }

    /** Book number for a canonical English name (case-insensitive), or 0 when unknown. */
    public int bookNumber(String canonicalName) {
        if (canonicalName == null) return 0;
        Integer n = bookNumbers.get(canonicalName.trim().toLowerCase(java.util.Locale.ROOT));
        return n == null ? 0 : n;
    }
}
