package org.perazimchurch.app.presentation.ui.games.common;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;

import org.perazimchurch.app.data.local.PerazimDatabase;
import org.perazimchurch.app.data.local.entity.DailyVerseEntity;
import org.perazimchurch.app.domain.games.engine.GameSession;
import org.perazimchurch.app.domain.games.engine.LevelProgression;
import org.perazimchurch.app.domain.games.engine.StarRating;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.function.Consumer;

/** Shared pieces of the results screen: stars, local progression and the verse of the day. */
public final class GameResults {

    private GameResults() {}

    /** Three stars drawn as filled or hollow glyphs. */
    public static String starLine(int stars) {
        StringBuilder line = new StringBuilder();
        for (int i = 0; i < 3; i++) line.append(i < stars ? "★" : "☆");
        return line.toString();
    }

    /** Scores a finished run on level 1, saves the result on this phone and returns the stars. */
    public static int recordRun(SharedPreferences prefs, String gameId, int score, int size) {
        int stars = StarRating.stars(score, size, GameSession.NO_LIVES);
        new LevelProgression(new PrefsKeyValueStore(prefs)).record(gameId, 1, stars, 1);
        return stars;
    }

    /** Best stars saved for a game on level 1, from 0 to 3. */
    public static int bestStars(SharedPreferences prefs, String gameId) {
        return new LevelProgression(new PrefsKeyValueStore(prefs)).bestStars(gameId, 1);
    }

    /**
     * Loads today's verse from the local database on a background thread and passes it to
     * {@code onLoaded} on the main thread. Passes a short fallback when no verse is stored.
     */
    public static void loadVerseOfDay(Context context, Consumer<String> onLoaded) {
        Context app = context.getApplicationContext();
        Handler main = new Handler(Looper.getMainLooper());
        new Thread(() -> {
            String text = "Verse of the day: keep reading and let the word guide you.";
            try {
                String today = new SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).format(new Date());
                DailyVerseEntity verse = PerazimDatabase.getInstance(app).dailyVerseDao().getVerseForDate(today);
                if (verse != null && verse.isActive() && !TextUtils.isEmpty(verse.getText())) {
                    text = "Verse of the day, " + verse.getReference() + ":\n" + verse.getText();
                }
            } catch (Exception e) {
                android.util.Log.w("GameResults", "Could not load daily verse", e);
            }
            String result = text;
            main.post(() -> onLoaded.accept(result));
        }).start();
    }

    /** Stores level progress in the same preferences file as the game scores. */
    private static final class PrefsKeyValueStore implements org.perazimchurch.app.domain.games.engine.KeyValueStore {
        private final SharedPreferences prefs;

        PrefsKeyValueStore(SharedPreferences prefs) {
            this.prefs = prefs;
        }

        @Override public int getInt(String key, int defaultValue) {
            return prefs.getInt(key, defaultValue);
        }

        @Override public void putInt(String key, int value) {
            prefs.edit().putInt(key, value).apply();
        }
    }
}
