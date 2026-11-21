package org.perazimchurch.app.domain.games.vine;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/** Reads the Vine Words level file. Fails loudly on a malformed file rather than guessing. */
public final class VineWordsLevels {

    /** Number of levels the game offers. The content script writes exactly this many. */
    public static final int LEVEL_COUNT = 60;

    private VineWordsLevels() {}

    /**
     * Parses the JSON text of {@code vine_words.json}.
     *
     * @throws IllegalArgumentException when the file is not a valid level set
     */
    public static List<VineWordsLevel> parse(String json) {
        try {
            JSONObject document = new JSONObject(json);
            JSONArray array = document.getJSONArray("levels");
            List<VineWordsLevel> levels = new ArrayList<>(array.length());
            for (int i = 0; i < array.length(); i++) {
                levels.add(readLevel(array.getJSONObject(i)));
            }
            return levels;
        } catch (JSONException e) {
            throw new IllegalArgumentException("Bad Vine Words level file: " + e.getMessage(), e);
        }
    }

    private static VineWordsLevel readLevel(JSONObject level) throws JSONException {
        List<VineWordsLevel.Verse> verses = new ArrayList<>();
        JSONArray verseArray = level.getJSONArray("verses");
        for (int i = 0; i < verseArray.length(); i++) {
            JSONObject verse = verseArray.getJSONObject(i);
            verses.add(new VineWordsLevel.Verse(verse.getString("ref"), verse.getString("text")));
        }
        JSONArray wordArray = level.getJSONArray("words");
        List<CrosswordGrid.Slot> slots = new ArrayList<>();
        for (int i = 0; i < wordArray.length(); i++) {
            JSONObject word = wordArray.getJSONObject(i);
            slots.add(new CrosswordGrid.Slot(word.getString("word"), word.getInt("row"),
                    word.getInt("col"), word.getBoolean("across")));
        }
        CrosswordGrid grid = CrosswordGrid.of(level.getInt("rows"), level.getInt("cols"), slots);
        String bonus = level.isNull("bonus") ? null : level.getString("bonus");
        return new VineWordsLevel(level.getInt("level"), level.getInt("band"), level.getString("passage"),
                level.getLong("seed"), verses, grid, level.getString("wheel"), bonus);
    }
}
