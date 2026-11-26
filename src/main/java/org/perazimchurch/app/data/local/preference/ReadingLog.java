package org.perazimchurch.app.data.local.preference;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;

/**
 * Counts chapters the member opened in the Bible reader, per local day, on this phone only.
 * Used to complete the routine's Read step (Levi's Q1). Nothing is sent to the server.
 */
public final class ReadingLog {
    private static final String PREFS = "perazim_reading_log";
    private static final String KEY_PREFIX = "chapters_";

    private ReadingLog() { }

    public static void recordChapter(@NonNull Context context) {
        SharedPreferences prefs = context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String key = KEY_PREFIX + GamificationStore.getTodayDateNairobi();
        prefs.edit().putInt(key, prefs.getInt(key, 0) + 1).apply();
    }

    public static int chaptersToday(@NonNull Context context) {
        SharedPreferences prefs = context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        return prefs.getInt(KEY_PREFIX + GamificationStore.getTodayDateNairobi(), 0);
    }
}
