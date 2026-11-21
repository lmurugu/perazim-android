package org.perazimchurch.app.data.local.preference;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;

import org.perazimchurch.app.domain.games.GameSettings;

/**
 * Caches the game settings last downloaded from the server. Until a download succeeds the built-in
 * defaults are used, so the games work offline and on a first launch.
 */
public final class GameSettingsStore {
    private static final String PREFS = "perazim_game_settings";
    private static final String KEY_CAP = "daily_star_cap";
    private static final String KEY_UNSEEN = "unseen_percent";
    private static final String KEY_MISSED = "missed_percent";

    private GameSettingsStore() { }

    @NonNull
    public static GameSettings load(@NonNull Context context) {
        SharedPreferences prefs = context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        return GameSettings.of(
                prefs.contains(KEY_CAP) ? prefs.getInt(KEY_CAP, 0) : null,
                prefs.contains(KEY_UNSEEN) ? prefs.getInt(KEY_UNSEEN, 0) : null,
                prefs.contains(KEY_MISSED) ? prefs.getInt(KEY_MISSED, 0) : null);
    }

    public static void save(@NonNull Context context, int dailyStarCap, int unseenPercent, int missedPercent) {
        GameSettings checked = GameSettings.of(dailyStarCap, unseenPercent, missedPercent);
        context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putInt(KEY_CAP, checked.dailyStarCap)
                .putInt(KEY_UNSEEN, checked.unseenPercent)
                .putInt(KEY_MISSED, checked.missedPercent)
                .apply();
    }
}
