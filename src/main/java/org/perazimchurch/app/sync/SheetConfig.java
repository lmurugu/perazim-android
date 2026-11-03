package org.perazimchurch.app.sync;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Configuration holder for published Google Sheet CSV tab URLs.
 * If URLs are empty or unreachable, the application continues to use
 * the existing local pre-seeded Room data without interruption.
 */
public final class SheetConfig {

    private static final String PREFS_NAME = "perazim_sheet_config";

    public enum Tab {
        ANNOUNCEMENTS("sheet_url_announcements"),
        EVENTS("sheet_url_events"),
        HYMNS("sheet_url_hymns"),
        RIDDLES("sheet_url_riddles"),
        JOKES("sheet_url_jokes"),
        SERMONS("sheet_url_sermons"),
        VERSES("sheet_url_verses"),
        CONFIG("sheet_url_config");

        private final String prefKey;

        Tab(String prefKey) {
            this.prefKey = prefKey;
        }

        public String getPrefKey() {
            return prefKey;
        }
    }

    private SheetConfig() {}

    private static SharedPreferences getPrefs(@NonNull Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    /**
     * Gets the configured published CSV URL for a specific tab.
     * Returns empty string if not configured.
     */
    @NonNull
    public static String getTabUrl(@NonNull Context context, @NonNull Tab tab) {
        return getPrefs(context).getString(tab.getPrefKey(), "");
    }

    /**
     * Updates the configured published CSV URL for a specific tab.
     */
    public static void setTabUrl(@NonNull Context context, @NonNull Tab tab, @Nullable String url) {
        getPrefs(context).edit().putString(tab.getPrefKey(), (url != null) ? url.trim() : "").apply();
    }

    /**
     * Returns true if at least one Google Sheet tab URL has been configured.
     */
    public static boolean hasConfiguredUrls(@NonNull Context context) {
        for (Tab tab : Tab.values()) {
            if (!getTabUrl(context, tab).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Clears all configured sheet URLs (reverting back to local seed data only).
     */
    public static void clearAllUrls(@NonNull Context context) {
        getPrefs(context).edit().clear().apply();
    }
}
