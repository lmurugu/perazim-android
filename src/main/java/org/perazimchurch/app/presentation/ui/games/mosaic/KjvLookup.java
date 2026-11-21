package org.perazimchurch.app.presentation.ui.games.mosaic;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;
import org.perazimchurch.app.data.local.PerazimDatabase;
import org.perazimchurch.app.data.local.dao.BibleDao;
import org.perazimchurch.app.data.local.entity.BibleVerseEntity;
import org.perazimchurch.app.data.local.text.BibleVerseReader;
import org.perazimchurch.app.domain.games.mosaic.VerseReference;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

/**
 * Reads KJV verse text for a caption from the same Bible store the Bible reader uses, through
 * {@link BibleVerseReader}. Read-only. Does not copy the text anywhere. Call it off the main
 * thread. Returns null when the reference is malformed, unknown, or missing, so callers fall back
 * to the story title.
 */
final class KjvLookup {

    private static Map<String, Integer> bookNumbers;

    private KjvLookup() {}

    static String text(Context context, String reference) {
        VerseReference ref = VerseReference.parse(reference);
        if (ref == null) return null;
        try {
            Integer bookNumber = bookNumber(context, ref.book);
            if (bookNumber == null) return null;
            BibleDao dao = PerazimDatabase.getInstance(context).bibleDao();
            BibleVerseEntity verse = BibleVerseReader.create(context, dao)
                    .getVerse("KJV", bookNumber, ref.chapter, ref.verse);
            if (verse == null || verse.getText() == null) return null;
            String text = verse.getText().trim();
            return text.isEmpty() ? null : text;
        } catch (RuntimeException | android.database.sqlite.SQLiteException e) {
            return null;
        }
    }

    private static synchronized Integer bookNumber(Context context, String name) throws Exception {
        if (bookNumbers == null) {
            Map<String, Integer> map = new HashMap<>();
            JSONArray manifest = new JSONArray(read(context, "bible/kjv_manifest.json"));
            for (int i = 0; i < manifest.length(); i++) {
                JSONObject book = manifest.getJSONObject(i);
                map.put(book.getString("name").toLowerCase(), book.getInt("bookNumber"));
            }
            bookNumbers = map;
        }
        return bookNumbers.get(name.toLowerCase());
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
