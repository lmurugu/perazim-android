package org.perazimchurch.app.presentation.ui.games.common;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;

import org.perazimchurch.app.data.local.PerazimDatabase;
import org.perazimchurch.app.data.local.entity.DailyVerseEntity;
import org.perazimchurch.app.domain.games.engine.GameDefinition;
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

    /**
     * The one call a game makes when its run ends. Rates the finished session (lives included),
     * saves the stars for this level, and returns them so the results screen can show them.
     * Use {@code levelCount} 1 for games that have no levels yet. See docs/games/ENGINE_USAGE.md.
     */
    public static int finishRun(SharedPreferences prefs, String gameId, int level, int levelCount, GameSession session) {
        int stars = StarRating.stars(session.score(), session.size(), session.lives());
        new LevelProgression(new PrefsKeyValueStore(prefs)).record(gameId, level, stars, levelCount);
        return stars;
    }

    /**
     * Ends a run for a catalog game and uses that game's own star rule ({@link GameDefinition#starRule()}).
     * Saves the stars for the level and returns them.
     */
    public static int finishRun(SharedPreferences prefs, String storedId, GameDefinition game, int level, GameSession session) {
        int stars = game.starRule().stars(session);
        new LevelProgression(new PrefsKeyValueStore(prefs)).record(storedId, level, stars, game.levelCount());
        return stars;
    }

    /** Save the result and unlock together, before any animation or navigation. */
    public static boolean saveLevel(SharedPreferences prefs, String storedId, int level, int count,
                                    int stars, boolean cleared, String result) {
        SharedPreferences.Editor editor = prefs.edit();
        LevelProgression progression = new LevelProgression(new PrefsKeyValueStore(prefs, editor));
        boolean unlocked = progression.record(storedId, level, stars, count, cleared);
        editor.putString("games.lastResult." + storedId + "." + level, result).apply();
        return unlocked;
    }

    public static int clearedCount(SharedPreferences prefs, String storedId, int count, boolean openChapters) {
        LevelProgression progression = new LevelProgression(new PrefsKeyValueStore(prefs));
        int cleared = 0;
        for (int level = 1; level <= count; level++) {
            if (progression.cleared(storedId, level)
                    || (openChapters && progression.bestStars(storedId, level) > 0)) cleared++;
        }
        return cleared;
    }

    /** Old one-round result screens could save a passing score before they had a next level. */
    public static void carryForwardPassedLevels(SharedPreferences prefs, String storedId, int count) {
        LevelProgression progress = new LevelProgression(new PrefsKeyValueStore(prefs));
        int level = progress.unlockedLevel(storedId);
        while (level < count && progress.cleared(storedId, level)) {
            progress.record(storedId, level, progress.bestStars(storedId, level), count, true);
            level++;
        }
    }

    public static String lastLevelResult(SharedPreferences prefs, String storedId, int level) {
        return prefs.getString("games.lastResult." + storedId + "." + level, "");
    }

    /** The highest level unlocked for a stored id (1 when nothing is unlocked yet). */
    public static int unlockedLevel(SharedPreferences prefs, String storedId) {
        return new LevelProgression(new PrefsKeyValueStore(prefs)).unlockedLevel(storedId);
    }

    /** Best stars saved for a stored id on one level, from 0 to 3. */
    public static int bestStars(SharedPreferences prefs, String storedId, int level) {
        return new LevelProgression(new PrefsKeyValueStore(prefs)).bestStars(storedId, level);
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
        private final SharedPreferences.Editor editor;

        PrefsKeyValueStore(SharedPreferences prefs) { this(prefs, null); }

        private PrefsKeyValueStore(SharedPreferences prefs, SharedPreferences.Editor editor) {
            this.prefs = prefs;
            this.editor = editor;
        }

        @Override public int getInt(String key, int defaultValue) {
            return prefs.getInt(key, defaultValue);
        }

        @Override public void putInt(String key, int value) {
            if (editor != null) editor.putInt(key, value);
            else prefs.edit().putInt(key, value).apply();
        }
    }
}
