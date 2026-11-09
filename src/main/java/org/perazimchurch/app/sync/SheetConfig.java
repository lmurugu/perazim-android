package org.perazimchurch.app.sync;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Client-side remote feed URLs are deliberately absent until every feed's
 * publication status, contents and access controls have been reviewed.
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
        private final String defaultUrl;

        Tab(String prefKey) {
            this.prefKey = prefKey;
            this.defaultUrl = "";
        }

        public String getPrefKey() {
            return prefKey;
        }

        public String getDefaultUrl() {
            return defaultUrl;
        }
    }

    private SheetConfig() {}

    private static SharedPreferences getPrefs(@NonNull Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    /**
     * Gets the configured published CSV URL for a specific tab.
     * Checks user SharedPreferences first; falls back to build-time BuildConfig default;
     * returns empty string if unconfigured.
     */
    @NonNull
    public static String getTabUrl(@NonNull Context context, @NonNull Tab tab) {
        if (!org.perazimchurch.app.BuildConfig.REMOTE_SHEET_FEEDS_ENABLED) return "";
        String url = getPrefs(context).getString(tab.getPrefKey(), "");
        if (url == null || url.trim().isEmpty()) {
            url = tab.getDefaultUrl();
        }
        return url != null ? url.trim() : "";
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
