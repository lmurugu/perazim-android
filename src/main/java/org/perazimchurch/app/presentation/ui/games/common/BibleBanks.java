package org.perazimchurch.app.presentation.ui.games.common;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.perazimchurch.app.domain.games.BibleGames;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Loads the bundled Bible Quiz and Verse Builder banks from assets/games and filters them by
 * difficulty. The files are built and checked against KJV by scripts/games/content, so the app
 * only reads them. Works offline.
 */
public final class BibleBanks {

    /** Difficulty keys as they appear in the bank files. */
    public static final String[] DIFFICULTIES = {"easy", "medium", "hard"};

    private BibleBanks() { }

    /** Label shown on the difficulty buttons. */
    public static String label(String difficulty) {
        return Character.toUpperCase(difficulty.charAt(0)) + difficulty.substring(1);
    }

    /** Multiple-choice questions at one difficulty. */
    public static List<BibleGames.Card> quiz(Context context, String difficulty) {
        List<BibleGames.Card> cards = new ArrayList<>();
        try {
            JSONArray items = new JSONObject(read(context, "bible_trivia.json")).getJSONArray("items");
            for (int i = 0; i < items.length(); i++) {
                JSONObject item = items.getJSONObject(i);
                if (!difficulty.equals(item.getString("difficulty"))) continue;
                List<String> options = new ArrayList<>();
                JSONArray choices = item.getJSONArray("options");
                for (int j = 0; j < choices.length(); j++) options.add(choices.getString(j));
                cards.add(BibleGames.trivia(
                        item.getString("question"),
                        item.getString("answer"),
                        item.getString("reference"),
                        item.getString("explanation"),
                        item.getString("hint"),
                        options));
            }
        } catch (JSONException e) {
            throw new IllegalStateException("Bible quiz bank is malformed", e);
        }
        return cards;
    }

    /** Verses for word-order play at one difficulty. */
    public static List<BibleGames.Card> verses(Context context, String difficulty) {
        List<BibleGames.Card> cards = new ArrayList<>();
        try {
            JSONArray items = new JSONObject(read(context, "verse_builder.json")).getJSONArray("items");
            for (int i = 0; i < items.length(); i++) {
                JSONObject item = items.getJSONObject(i);
                if (!difficulty.equals(item.getString("difficulty"))) continue;
                cards.add(BibleGames.verse(item.getString("text"), item.getString("reference")));
            }
        } catch (JSONException e) {
            throw new IllegalStateException("Verse bank is malformed", e);
        }
        return cards;
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
