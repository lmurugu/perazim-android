package org.perazimchurch.app.ai.library;

import android.content.Context;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Verbatim King James Version text from the full bundled KJV (assets/bible/text/KJV.tsv.gz, 31,102 verses, the same
 * file the Bible reader and the knowledge library use). A verse that is not in the file is reported as missing,
 * never filled in from memory.
 */
public final class KjvVerseText {
    static final String KJV_TSV = "bible/text/KJV.tsv.gz";
    static final String MANIFEST = "bible/kjv_manifest.json";

    /** In canonical order, as the file is read. */
    private final Map<String, String> verses = new LinkedHashMap<>();
    private final Map<String, Integer> bookNumbers = new HashMap<>();
    private final Map<Integer, String> bookNames = new HashMap<>();

    /** One verse found by a phrase search: its reference label and its verbatim text. */
    public static final class Found {
        public final String label;
        public final String text;

        Found(String label, String text) {
            this.label = label;
            this.text = text;
        }
    }

    private KjvVerseText() {}

    /** Parses the TSV (bookNumber, chapter, verse, text per line) and the book manifest. */
    public static KjvVerseText fromTsv(InputStream tsv, String manifestJson) throws IOException, JSONException {
        KjvVerseText out = new KjvVerseText();
        JSONArray books = new JSONArray(manifestJson);
        for (int i = 0; i < books.length(); i++) {
            JSONObject b = books.getJSONObject(i);
            out.bookNumbers.put(b.getString("name").toLowerCase(Locale.ROOT), b.getInt("bookNumber"));
            out.bookNames.put(b.getInt("bookNumber"), b.getString("name"));
        }
        try (BufferedReader in = new BufferedReader(new InputStreamReader(tsv, StandardCharsets.UTF_8))) {
            String line;
            while ((line = in.readLine()) != null) {
                String[] f = line.split("\t", 4);
                if (f.length < 4) continue;
                out.verses.put(key(Integer.parseInt(f[0]), Integer.parseInt(f[1]), Integer.parseInt(f[2])), f[3]);
            }
        }
        return out;
    }

    private static KjvVerseText cached;

    /** Loads the bundled full KJV once. Returns an empty table if the files cannot be read. */
    public static synchronized KjvVerseText load(Context context) {
        if (cached != null) return cached;
        try (InputStream tsv = org.perazimchurch.app.data.local.seeder.BibleAssets.openText(context, KJV_TSV)) {
            cached = fromTsv(tsv, read(context, MANIFEST));
        } catch (Exception e) {
            cached = empty();
        }
        return cached;
    }

    /** Parses the same files from the source tree, for the JVM tests and the evaluation harness. */
    public static KjvVerseText fromFiles(java.io.File tsvGz, java.io.File manifest) throws IOException, JSONException {
        try (InputStream in = new java.util.zip.GZIPInputStream(new java.io.FileInputStream(tsvGz))) {
            return fromTsv(in, new String(java.nio.file.Files.readAllBytes(manifest.toPath()),
                    StandardCharsets.UTF_8));
        }
    }

    private static KjvVerseText empty() {
        try {
            return fromTsv(new java.io.ByteArrayInputStream(new byte[0]), "[]");
        } catch (Exception impossible) {
            throw new IllegalStateException(impossible);
        }
    }

    /** The book number for a canonical name, or -1 when the name is not in the manifest. */
    public int bookNumber(String name) {
        if (name == null) return -1;
        Integer n = bookNumbers.get(name.toLowerCase(Locale.ROOT));
        return n == null ? -1 : n;
    }

    /**
     * Verses whose text contains the phrase, in canonical order, with their verbatim KJV text. Words are compared
     * ignoring case and punctuation. Returns an empty list when the phrase is blank or not in the file.
     */
    public List<Found> findPhrase(String phrase, int limit) {
        List<Found> out = new ArrayList<>();
        String needle = normalise(phrase);
        if (needle.trim().isEmpty() || limit <= 0) return out;
        for (Map.Entry<String, String> e : verses.entrySet()) {
            if (!normalise(e.getValue()).contains(needle)) continue;
            String[] k = e.getKey().split(":");
            String book = bookNames.get(Integer.parseInt(k[0]));
            out.add(new Found((book == null ? k[0] : book) + " " + k[1] + ":" + k[2], e.getValue()));
            if (out.size() >= limit) break;
        }
        return out;
    }

    /** Lowercase words separated by single spaces, with a space at each end, so phrases match whole words only. */
    static String normalise(String text) {
        if (text == null) return " ";
        return " " + text.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", " ").trim() + " ";
    }

    /** The verse text as written in the file, or null when the verse is not in the file. */
    public String verse(int book, int chapter, int verse) {
        return verses.get(key(book, chapter, verse));
    }

    private static String key(int book, int chapter, int verse) {
        return book + ":" + chapter + ":" + verse;
    }

    private static String read(Context context, String path) throws IOException {
        try (InputStream in = context.getAssets().open(path)) {
            return new String(org.perazimchurch.app.ai.IoCompat.readAll(in), StandardCharsets.UTF_8);
        }
    }
}
