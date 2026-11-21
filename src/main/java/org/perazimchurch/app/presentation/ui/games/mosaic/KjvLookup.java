package org.perazimchurch.app.presentation.ui.games.mosaic;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;
import org.perazimchurch.app.domain.games.mosaic.VerseReference;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

/**
 * Looks up KJV verse text from the bundled assets for a reference such as "Genesis 6:22".
 * Loads the files once. Returns null when the verse is not in the bundle, so callers can fall
 * back to the story title.
 */
final class KjvLookup {

    private static Map<String, String> bookIds;
    private static Map<String, String> verses;

    private KjvLookup() {}

    static synchronized String text(Context context, String reference) {
        VerseReference ref = VerseReference.parse(reference);
        if (ref == null) return null;
        try {
            load(context);
        } catch (Exception e) {
            return null;
        }
        String bookId = bookIds.get(ref.book.toLowerCase());
        if (bookId == null) return null;
        return verses.get(bookId + "." + ref.chapter + "." + ref.verse);
    }

    private static void load(Context context) throws Exception {
        if (verses != null) return;
        Map<String, String> ids = new HashMap<>();
        JSONArray manifest = new JSONArray(read(context, "bible/kjv_manifest.json"));
        for (int i = 0; i < manifest.length(); i++) {
            JSONObject book = manifest.getJSONObject(i);
            ids.put(book.getString("name").toLowerCase(), book.getString("id"));
        }
        Map<String, String> map = new HashMap<>();
        JSONArray rows = new JSONArray(read(context, "bible/kjv_verses.json"));
        for (int i = 0; i < rows.length(); i++) {
            JSONObject row = rows.getJSONObject(i);
            map.put(row.getString("bookId") + "." + row.getInt("chapterNumber") + "."
                    + row.getInt("verseNumber"), row.getString("text"));
        }
        bookIds = ids;
        verses = map;
    }

    private static String read(Context context, String path) throws Exception {
        StringBuilder out = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(context.getAssets().open(path), "UTF-8"))) {
            String line;
            while ((line = reader.readLine()) != null) out.append(line).append('\n');
        }
        return out.toString();
    }
}
