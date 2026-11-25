package org.perazimchurch.app.ai.library;

import android.content.Context;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Verbatim King James Version text from the bundled verse file (assets/bible). Only verses that are in the file are
 * available; a missing verse is reported as missing, never filled in from memory.
 */
public final class KjvVerseText {
    private final Map<String, String> verses = new HashMap<>();
    private final Map<String, Integer> bookNumbers = new HashMap<>();

    private KjvVerseText() {}

    /** Parses the verse file and the book manifest. */
    public static KjvVerseText fromJson(String versesJson, String manifestJson) throws JSONException {
        KjvVerseText out = new KjvVerseText();
        JSONArray books = new JSONArray(manifestJson);
        for (int i = 0; i < books.length(); i++) {
            JSONObject b = books.getJSONObject(i);
            out.bookNumbers.put(b.getString("name").toLowerCase(Locale.ROOT), b.getInt("bookNumber"));
        }
        JSONArray verses = new JSONArray(versesJson);
        for (int i = 0; i < verses.length(); i++) {
            JSONObject v = verses.getJSONObject(i);
            out.verses.put(key(v.getInt("bookNumber"), v.getInt("chapterNumber"), v.getInt("verseNumber")),
                    v.getString("text"));
        }
        return out;
    }

    private static KjvVerseText cached;

    /** Loads the bundled files once. Returns an empty table if they cannot be read. */
    public static synchronized KjvVerseText load(Context context) {
        if (cached != null) return cached;
        try {
            cached = fromJson(read(context, "bible/kjv_verses.json"), read(context, "bible/kjv_manifest.json"));
        } catch (Exception e) {
            try {
                cached = fromJson("[]", "[]");
            } catch (JSONException impossible) {
                throw new IllegalStateException(impossible);
            }
        }
        return cached;
    }

    /** The book number for a canonical name, or -1 when the name is not in the manifest. */
    public int bookNumber(String name) {
        if (name == null) return -1;
        Integer n = bookNumbers.get(name.toLowerCase(Locale.ROOT));
        return n == null ? -1 : n;
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
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
