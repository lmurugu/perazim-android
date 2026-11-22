package org.perazimchurch.app.presentation.ui.games.common;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.perazimchurch.app.domain.games.FillTheGap;
import org.perazimchurch.app.domain.games.TestamentSort;
import org.perazimchurch.app.domain.games.WhoSaidIt;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Loads the bundled banks for Who Said It?, Fill the Gap and Old or New? from assets/games and
 * splits them into the three bands used by the level layout. The files are checked against KJV by
 * scripts/games/content. Bands are ranks: quotations and verses are sorted by length and cut into
 * thirds, so easy means short. Old or New? uses the entry kind: books are easy, people medium and
 * events hard. Works offline.
 */
public final class GameBanks {

    private GameBanks() { }

    /** All speakers in the quotation bank, used as wrong choices. */
    public static List<String> speakers(Context context) {
        try {
            JSONArray speakers = new JSONObject(read(context, "who_said_it.json")).getJSONArray("speakers");
            List<String> names = new ArrayList<>();
            for (int i = 0; i < speakers.length(); i++) names.add(speakers.getString(i));
            return names;
        } catch (JSONException e) {
            throw new IllegalStateException("Who said it bank is malformed", e);
        }
    }

    /** Quotations in one band, by length. */
    public static List<WhoSaidIt.Quote> quotes(Context context, String band) {
        List<WhoSaidIt.Quote> all = new ArrayList<>();
        try {
            JSONArray items = new JSONObject(read(context, "who_said_it.json")).getJSONArray("items");
            List<JSONObject> rows = new ArrayList<>();
            for (int i = 0; i < items.length(); i++) rows.add(items.getJSONObject(i));
            List<JSONObject> band3 = bandRows(rows, band, "quote");
            for (JSONObject item : band3) {
                all.add(new WhoSaidIt.Quote(item.getString("quote"), item.getString("reference"), item.getString("speaker")));
            }
        } catch (JSONException e) {
            throw new IllegalStateException("Who said it bank is malformed", e);
        }
        return all;
    }

    /** Verses with one word left out, in one band, by length. */
    public static List<FillTheGap.Verse> verses(Context context, String band) {
        List<FillTheGap.Verse> verses = new ArrayList<>();
        try {
            JSONArray items = new JSONObject(read(context, "fill_the_gap.json")).getJSONArray("items");
            List<JSONObject> rows = new ArrayList<>();
            for (int i = 0; i < items.length(); i++) rows.add(items.getJSONObject(i));
            for (JSONObject item : bandRows(rows, band, "text")) {
                verses.add(verse(item));
            }
        } catch (JSONException e) {
            throw new IllegalStateException("Fill the gap bank is malformed", e);
        }
        return verses;
    }

    /** Books, people or events for one band of Old or New?. Easy is books, medium people, hard events. */
    public static List<TestamentSort.Book> testamentEntries(Context context, String band) {
        String kind = "easy".equals(band) ? "book" : "medium".equals(band) ? "person" : "event";
        List<TestamentSort.Book> books = new ArrayList<>();
        try {
            JSONArray items = new JSONObject(read(context, "old_or_new.json")).getJSONArray("items");
            for (int i = 0; i < items.length(); i++) {
                JSONObject item = items.getJSONObject(i);
                if (!kind.equals(item.getString("kind"))) continue;
                books.add(new TestamentSort.Book(
                        item.getString("name"),
                        TestamentSort.Testament.valueOf(item.getString("testament")),
                        item.getString("fact")));
            }
        } catch (JSONException e) {
            throw new IllegalStateException("Old or new bank is malformed", e);
        }
        return books;
    }

    /** Rows in one of three length ranks, sorted by the text field then by id. */
    private static List<JSONObject> bandRows(List<JSONObject> rows, String band, String textField) throws JSONException {
        List<JSONObject> sorted = new ArrayList<>(rows);
        Collections.sort(sorted, Comparator
                .comparingInt((JSONObject o) -> wordCount(o.optString(textField)))
                .thenComparing(o -> o.optString("id")));
        int total = sorted.size();
        int index = "easy".equals(band) ? 0 : "medium".equals(band) ? 1 : 2;
        List<JSONObject> out = new ArrayList<>();
        for (int i = 0; i < total; i++) {
            if (i * 3 / total == index) out.add(sorted.get(i));
        }
        return out;
    }

    /** Builds a verse with its word at {@code gapIndex} left out. Trailing punctuation stays after the gap. */
    private static FillTheGap.Verse verse(JSONObject item) throws JSONException {
        String[] words = item.getString("text").split(" ");
        int gap = item.getInt("gapIndex");
        StringBuilder before = new StringBuilder();
        for (int i = 0; i < gap; i++) before.append(words[i]).append(' ');
        String token = words[gap];
        String punctuation = token.replaceAll("^.*?([.,;:!?”’\"']*)$", "$1");
        StringBuilder after = new StringBuilder(punctuation);
        for (int i = gap + 1; i < words.length; i++) after.append(' ').append(words[i]);
        List<String> wrong = new ArrayList<>();
        JSONArray choices = item.getJSONArray("choices");
        for (int i = 0; i < choices.length(); i++) {
            String choice = choices.getString(i);
            if (!choice.equals(item.getString("answer"))) wrong.add(choice);
        }
        return new FillTheGap.Verse(before.toString(), item.getString("answer"), after.toString(),
                item.getString("reference"), wrong.toArray(new String[0]));
    }

    private static int wordCount(String text) {
        String trimmed = text.trim();
        return trimmed.isEmpty() ? 0 : trimmed.split("\\s+").length;
    }

    private static String read(Context context, String name) {
        try (InputStream in = context.getAssets().open("games/" + name);
             BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            StringBuilder text = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) text.append(line).append('\n');
            return text.toString();
        } catch (IOException e) {
            throw new IllegalStateException("Missing game bank " + name, e);
        }
    }
}
