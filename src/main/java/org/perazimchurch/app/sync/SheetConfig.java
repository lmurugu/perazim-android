package org.perazimchurch.app.sync;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.perazimchurch.app.BuildConfig;

/**
 * Where each content tab is downloaded from.
 *
 * By default every tab points at the shared church content feed
 * (BuildConfig.CONTENT_FEED_BASE_URL). A URL saved in preferences overrides it.
 * Nothing is fetched while BuildConfig.REMOTE_SHEET_FEEDS_ENABLED is false.
 */
public final class SheetConfig {

    private static final String PREFS_NAME = "perazim_sheet_config";

    public enum Tab {
        ANNOUNCEMENTS("sheet_url_announcements", "announcements"),
        EVENTS("sheet_url_events", "events"),
        HYMNS("sheet_url_hymns", "hymns"),
        RIDDLES("sheet_url_riddles", "riddles"),
        JOKES("sheet_url_jokes", "jokes"),
        SERMONS("sheet_url_sermons", "sermons"),
        VERSES("sheet_url_verses", "verses"),
        CONFIG("sheet_url_config", "config");

        private final String prefKey;
        private final String feedName;

        Tab(String prefKey, String feedName) {
            this.prefKey = prefKey;
            this.feedName = feedName;
        }

        public String getPrefKey() {
            return prefKey;
        }

        /** Tab name in the shared content feed, e.g. "hymns" for csv/hymns.csv. */
        public String getFeedName() {
            return feedName;
        }

        /**
         * The shared content feed URL for this tab, or "" when no feed is configured.
         * The feed is the website repo's data/ folder, which the website and the
         * Perazim web app also read, so one edit reaches all three.
         */
        public String getDefaultUrl() {
            return defaultUrlFor(BuildConfig.CONTENT_FEED_BASE_URL, feedName);
        }
    }

    /** Builds base + "/csv/" + tab + ".csv"; returns "" for a blank base. */
    @NonNull
    static String defaultUrlFor(@Nullable String baseUrl, @NonNull String feedName) {
        if (baseUrl == null || baseUrl.trim().isEmpty()) return "";
        String base = baseUrl.trim();
        while (base.endsWith("/")) base = base.substring(0, base.length() - 1);
        return base + "/csv/" + feedName + ".csv";
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
        if (!BuildConfig.REMOTE_SHEET_FEEDS_ENABLED) return "";
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
