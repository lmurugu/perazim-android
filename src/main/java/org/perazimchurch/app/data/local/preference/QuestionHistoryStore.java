package org.perazimchurch.app.data.local.preference;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;

import org.perazimchurch.app.domain.games.ReviewScheduler;
import org.perazimchurch.app.domain.games.ReviewScheduler.Record;

import java.util.HashMap;
import java.util.Map;

/**
 * Per-question results kept only on this phone, used by {@link ReviewScheduler} to choose the next questions.
 * Keys are "game|prompt"; values are "correct,missed,lastSeenDay". Nothing is sent to the server.
 */
public final class QuestionHistoryStore {
    private static final String PREFS = "perazim_question_history";
    public static final String LADDER = "ladder";
    private static final long MILLIS_PER_DAY = 86_400_000L;

    private QuestionHistoryStore() { }

    /** Records for one game, keyed by question prompt. Unreadable entries are skipped. */
    @NonNull
    public static Map<String, Record> load(@NonNull Context context, @NonNull String game) {
        Map<String, Record> out = new HashMap<>();
        try {
            SharedPreferences prefs = prefs(context);
            String prefix = game + "|";
            for (Map.Entry<String, ?> entry : prefs.getAll().entrySet()) {
                if (!entry.getKey().startsWith(prefix) || !(entry.getValue() instanceof String)) continue;
                Record record = parse((String) entry.getValue());
                if (record != null) out.put(entry.getKey().substring(prefix.length()), record);
            }
        } catch (RuntimeException ignored) {
            // Storage unavailable: play with no history, which is the same as a first run.
        }
        return out;
    }

    /**
     * Read-only view of every game's records, keyed by game then by prompt. Used by the guide to show which questions
     * the member misses most. Returns an empty map when storage is unavailable.
     */
    @NonNull
    public static Map<String, Map<String, Record>> loadAll(@NonNull Context context) {
        Map<String, Map<String, Record>> out = new HashMap<>();
        try {
            for (Map.Entry<String, ?> entry : prefs(context).getAll().entrySet()) {
                if (!(entry.getValue() instanceof String)) continue;
                int split = entry.getKey().indexOf('|');
                if (split <= 0) continue;
                Record record = parse((String) entry.getValue());
                if (record == null) continue;
                String game = entry.getKey().substring(0, split);
                Map<String, Record> byPrompt = out.get(game);
                if (byPrompt == null) {
                    byPrompt = new HashMap<>();
                    out.put(game, byPrompt);
                }
                byPrompt.put(entry.getKey().substring(split + 1), record);
            }
        } catch (RuntimeException ignored) {
            // Storage unavailable: the guide sees no history, the same as a first run.
        }
        return out;
    }

    /** Adds one answer for a question to its record. */
    public static void record(@NonNull Context context, @NonNull String game, @NonNull String prompt, boolean correct) {
        try {
            SharedPreferences prefs = prefs(context);
            String key = game + "|" + prompt;
            Record previous = parse(prefs.getString(key, null));
            Record next = ReviewScheduler.afterAnswer(previous, correct, System.currentTimeMillis() / MILLIS_PER_DAY);
            prefs.edit().putString(key, next.correct + "," + next.missed + "," + next.lastSeenDay).apply();
        } catch (RuntimeException ignored) {
            // Best effort: a lost answer only means the question is scheduled as before.
        }
    }

    private static Record parse(String value) {
        if (value == null) return null;
        String[] parts = value.split(",");
        if (parts.length != 3) return null;
        try {
            return new Record(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]), Long.parseLong(parts[2]));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
}
