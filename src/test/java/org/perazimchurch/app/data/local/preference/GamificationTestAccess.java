package org.perazimchurch.app.data.local.preference;

import android.content.SharedPreferences;

/** Test-source-only access; this class is never packaged in the app. */
public final class GamificationTestAccess {
    private GamificationTestAccess() {}

    public static void setPreferences(SharedPreferences preferences) {
        GamificationStore.setPreferencesForTesting(preferences);
    }

    public static boolean completeForDate(String userId, String date, int streak) {
        return GamificationStore.recordDailyRoutineCompletedForDate(null, userId, date, streak);
    }
}
