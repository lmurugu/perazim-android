package org.perazimchurch.app.sync;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.perazimchurch.app.data.local.PerazimDatabase;
import org.perazimchurch.app.data.local.dao.AnnouncementDao;
import org.perazimchurch.app.data.local.dao.AppConfigDao;
import org.perazimchurch.app.data.local.dao.DailyVerseDao;
import org.perazimchurch.app.data.local.dao.EventDao;
import org.perazimchurch.app.data.local.dao.HymnDao;
import org.perazimchurch.app.data.local.dao.JokeDao;
import org.perazimchurch.app.data.local.dao.RiddleDao;
import org.perazimchurch.app.data.local.dao.SermonDao;
import org.perazimchurch.app.data.local.entity.AnnouncementEntity;
import org.perazimchurch.app.data.local.entity.AppConfigEntity;
import org.perazimchurch.app.data.local.entity.DailyVerseEntity;
import org.perazimchurch.app.data.local.entity.EventEntity;
import org.perazimchurch.app.data.local.entity.HymnEntity;
import org.perazimchurch.app.data.local.entity.JokeEntity;
import org.perazimchurch.app.data.local.entity.RiddleEntity;
import org.perazimchurch.app.data.local.entity.SermonEntity;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Manages background synchronization of published Google Sheet CSV tabs into Room.
 * Follows offline-first, fail-soft principles:
 * - 10-second connect & read timeouts.
 * - Rate-limited to once every 6 hours on automatic app launch.
 * - Manual trigger via Profile screen "Refresh content".
 * - Never overwrites or deletes local data if download fails, is empty, or headers mismatch.
 * - Atomic Room upserts by ID.
 */
public final class SheetSyncManager {

    private static final String TAG = "PerazimSheetSync";
    private static final String PREFS_SYNC = "perazim_sheet_sync_state";
    private static final String KEY_LAST_SYNC_MILLIS = "last_sync_millis";
    private static final String KEY_LAST_SYNC_STATUS = "last_sync_status";

    private static final long SIX_HOURS_MILLIS = 6L * 60L * 60L * 1000L;
    private static final int TIMEOUT_MILLIS = 10000;

    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();
    private static final TimeZone NAIROBI_TZ = TimeZone.getTimeZone("Africa/Nairobi");

    public interface SyncCallback {
        void onSyncComplete(boolean success, int rowsSynced, int rowsSkipped, @NonNull String statusMessage);
    }

    private static void logI(String msg) {
        try {
            Log.i(TAG, msg);
        } catch (Throwable ignored) {
            System.out.println(TAG + " [INFO]: " + msg);
        }
    }

    private static void logD(String msg) {
        try {
            Log.d(TAG, msg);
        } catch (Throwable ignored) {
        }
    }

    private static void logW(String msg, @Nullable Throwable t) {
        try {
            if (t != null) {
                Log.w(TAG, msg, t);
            } else {
                Log.w(TAG, msg);
            }
        } catch (Throwable ignored) {
            System.err.println(TAG + " [WARN]: " + msg);
        }
    }

    private SheetSyncManager() {}

    private static SharedPreferences getPrefs(@NonNull Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS_SYNC, Context.MODE_PRIVATE);
    }

    public static long getLastSyncMillis(@NonNull Context context) {
        return getPrefs(context).getLong(KEY_LAST_SYNC_MILLIS, 0L);
    }

    @NonNull
    public static String getLastSyncStatusText(@NonNull Context context) {
        long lastMillis = getLastSyncMillis(context);
        if (lastMillis <= 0L) {
            return "Not synced yet (using local seed data)";
        }
        SimpleDateFormat sdf = new SimpleDateFormat("MMM d, h:mm a", Locale.getDefault());
        sdf.setTimeZone(NAIROBI_TZ);
        return "Last updated: " + sdf.format(new Date(lastMillis));
    }

    /**
     * Automatic sync on app startup. Only runs if > 6 hours have passed since last sync.
     */
    public static void syncOnAppStartIfNeeded(@NonNull Context context) {
        long last = getLastSyncMillis(context);
        long now = System.currentTimeMillis();
        if ((now - last) >= SIX_HOURS_MILLIS) {
            logI("Triggering automatic 6-hour interval content sync...");
            syncNow(context, null);
        } else {
            logD("Skipping auto-sync: last sync was " + ((now - last) / (1000 * 60)) + " minutes ago.");
        }
    }

    /**
     * Manual sync triggered by user (e.g. from Profile "Refresh content" button).
     */
    public static void syncNow(@NonNull Context context, @Nullable SyncCallback callback) {
        final Context appContext = context.getApplicationContext();
        EXECUTOR.execute(() -> {
            int totalSynced = 0;
            int totalSkipped = 0;
            boolean anySuccess = false;
            String errorMsg = "";

            try {
                PerazimDatabase db = PerazimDatabase.getInstance(appContext);

                // 1. Announcements
                String annUrl = SheetConfig.getTabUrl(appContext, SheetConfig.Tab.ANNOUNCEMENTS);
                if (!annUrl.isEmpty()) {
                    SyncStats stats = syncAnnouncements(annUrl, db);
                    totalSynced += stats.synced;
                    totalSkipped += stats.skipped;
                    anySuccess = true;
                }

                // 2. Events
                String evUrl = SheetConfig.getTabUrl(appContext, SheetConfig.Tab.EVENTS);
                if (!evUrl.isEmpty()) {
                    SyncStats stats = syncEvents(evUrl, db);
                    totalSynced += stats.synced;
                    totalSkipped += stats.skipped;
                    anySuccess = true;
                }

                // 3. Hymns
                String hymnUrl = SheetConfig.getTabUrl(appContext, SheetConfig.Tab.HYMNS);
                if (!hymnUrl.isEmpty()) {
                    SyncStats stats = syncHymns(hymnUrl, db);
                    totalSynced += stats.synced;
                    totalSkipped += stats.skipped;
                    anySuccess = true;
                }

                // 4. Riddles
                String riddleUrl = SheetConfig.getTabUrl(appContext, SheetConfig.Tab.RIDDLES);
                if (!riddleUrl.isEmpty()) {
                    SyncStats stats = syncRiddles(riddleUrl, db);
                    totalSynced += stats.synced;
                    totalSkipped += stats.skipped;
                    anySuccess = true;
                }

                // 5. Jokes
                String jokeUrl = SheetConfig.getTabUrl(appContext, SheetConfig.Tab.JOKES);
                if (!jokeUrl.isEmpty()) {
                    SyncStats stats = syncJokes(jokeUrl, db);
                    totalSynced += stats.synced;
                    totalSkipped += stats.skipped;
                    anySuccess = true;
                }

                // 6. Sermons
                String sermonUrl = SheetConfig.getTabUrl(appContext, SheetConfig.Tab.SERMONS);
                if (!sermonUrl.isEmpty()) {
                    SyncStats stats = syncSermons(sermonUrl, db);
                    totalSynced += stats.synced;
                    totalSkipped += stats.skipped;
                    anySuccess = true;
                }

                // 7. Verses
                String verseUrl = SheetConfig.getTabUrl(appContext, SheetConfig.Tab.VERSES);
                if (!verseUrl.isEmpty()) {
                    SyncStats stats = syncVerses(verseUrl, db);
                    totalSynced += stats.synced;
                    totalSkipped += stats.skipped;
                    anySuccess = true;
                }

                // 8. Config
                String configUrl = SheetConfig.getTabUrl(appContext, SheetConfig.Tab.CONFIG);
                if (!configUrl.isEmpty()) {
                    SyncStats stats = syncConfig(configUrl, db);
                    totalSynced += stats.synced;
                    totalSkipped += stats.skipped;
                    anySuccess = true;
                }

                long now = System.currentTimeMillis();
                getPrefs(appContext).edit()
                        .putLong(KEY_LAST_SYNC_MILLIS, now)
                        .apply();

                String status = anySuccess
                        ? "Synced " + totalSynced + " items (" + totalSkipped + " skipped)"
                        : "No remote URLs configured (using local offline content)";

                if (callback != null) {
                    callback.onSyncComplete(true, totalSynced, totalSkipped, status);
                }
            } catch (Exception e) {
                logW("Content sync warning: " + e.getMessage(), e);
                String fallbackStatus = "Offline — using local content";
                if (callback != null) {
                    callback.onSyncComplete(false, totalSynced, totalSkipped, fallbackStatus);
                }
            }
        });
    }

    // -------------------------------------------------------------------------
    // Individual Tab Synchronizers
    // -------------------------------------------------------------------------

    public static SyncStats syncAnnouncements(@NonNull String url, @NonNull PerazimDatabase db) throws Exception {
        return processAnnouncementsRows(fetchCsv(url), db.announcementDao());
    }

    static SyncStats processAnnouncementsRows(List<List<String>> rows, AnnouncementDao dao) {
        if (rows == null || rows.size() < 2) return new SyncStats(0, 0);

        Map<String, Integer> col = mapHeaders(rows.get(0));
        if (!col.containsKey("id") || !col.containsKey("title") || !col.containsKey("body")) {
            logW("Announcements CSV missing required headers. Skipping.", null);
            return new SyncStats(0, 0);
        }

        int synced = 0;
        int skipped = 0;

        for (int i = 1; i < rows.size(); i++) {
            List<String> r = rows.get(i);
            String id = getCell(r, col, "id");
            String title = getCell(r, col, "title");
            String body = getCell(r, col, "body");
            if (id.isEmpty() || title.isEmpty()) {
                skipped++;
                continue;
            }

            long dateMillis = parseDate(getCell(r, col, "date"), System.currentTimeMillis());
            String priority = getCellOrDefault(r, col, "priority", "NORMAL");
            String campus = getCellOrDefault(r, col, "campus", "all");
            boolean active = parseBool(getCellOrDefault(r, col, "active", "true"));

            AnnouncementEntity entity = new AnnouncementEntity(
                    id, title, body, priority, dateMillis, dateMillis + (30L * 86400000L), campus, active);
            dao.insert(entity);
            synced++;
        }
        return new SyncStats(synced, skipped);
    }

    public static SyncStats syncEvents(@NonNull String url, @NonNull PerazimDatabase db) throws Exception {
        return processEventsRows(fetchCsv(url), db.eventDao());
    }

    static SyncStats processEventsRows(List<List<String>> rows, EventDao dao) {
        if (rows == null || rows.size() < 2) return new SyncStats(0, 0);

        Map<String, Integer> col = mapHeaders(rows.get(0));
        if (!col.containsKey("id") || !col.containsKey("title")) {
            return new SyncStats(0, 0);
        }

        int synced = 0;
        int skipped = 0;

        for (int i = 1; i < rows.size(); i++) {
            List<String> r = rows.get(i);
            String id = getCell(r, col, "id");
            String title = getCell(r, col, "title");
            if (id.isEmpty() || title.isEmpty()) {
                skipped++;
                continue;
            }

            long start = parseDate(getCell(r, col, "start"), System.currentTimeMillis());
            long end = parseDate(getCell(r, col, "end"), start + (2L * 3600000L));
            String venue = getCellOrDefault(r, col, "venue", "Sanctuary");
            String campus = getCellOrDefault(r, col, "campus", "all");
            String desc = getCellOrDefault(r, col, "description", "");
            boolean active = parseBool(getCellOrDefault(r, col, "active", "true"));

            EventEntity entity = new EventEntity(
                    id, title, desc, start, end, venue, campus, "sanctuary.jpg", "Worship", active);
            dao.insert(entity);
            synced++;
        }
        return new SyncStats(synced, skipped);
    }

    public static SyncStats syncHymns(@NonNull String url, @NonNull PerazimDatabase db) throws Exception {
        return processHymnsRows(fetchCsv(url), db.hymnDao());
    }

    static SyncStats processHymnsRows(List<List<String>> rows, HymnDao dao) {
        if (rows == null || rows.size() < 2) return new SyncStats(0, 0);

        Map<String, Integer> col = mapHeaders(rows.get(0));
        if (!col.containsKey("id") || !col.containsKey("title")) {
            return new SyncStats(0, 0);
        }

        int synced = 0;
        int skipped = 0;

        for (int i = 1; i < rows.size(); i++) {
            List<String> r = rows.get(i);
            String id = getCell(r, col, "id");
            String title = getCell(r, col, "title");
            if (id.isEmpty() || title.isEmpty()) {
                skipped++;
                continue;
            }

            String writer = getCellOrDefault(r, col, "writer", "Traditional");
            String year = getCellOrDefault(r, col, "year", "");
            String key = getCellOrDefault(r, col, "key", "D Major");
            int bpm = parseInt(getCellOrDefault(r, col, "bpm", "80"), 80);
            String tags = getCellOrDefault(r, col, "tags", "Worship");
            String lyrics = getCellOrDefault(r, col, "lyrics", "");
            String chords = getCellOrDefault(r, col, "chords", "");
            boolean active = parseBool(getCellOrDefault(r, col, "active", "true"));

            int num = parseInt(id.replaceAll("\\D+", ""), i);

            HymnEntity entity = new HymnEntity(
                    id, num, title, lyrics, chords, key, "4/4", tags, false, writer, year, bpm, tags, active);
            dao.insert(entity);
            synced++;
        }
        return new SyncStats(synced, skipped);
    }

    public static SyncStats syncRiddles(@NonNull String url, @NonNull PerazimDatabase db) throws Exception {
        return processRiddlesRows(fetchCsv(url), db.riddleDao());
    }

    static SyncStats processRiddlesRows(List<List<String>> rows, RiddleDao dao) {
        if (rows == null || rows.size() < 2) return new SyncStats(0, 0);

        Map<String, Integer> col = mapHeaders(rows.get(0));
        if (!col.containsKey("id") || !col.containsKey("question") || !col.containsKey("answer")) {
            return new SyncStats(0, 0);
        }

        int synced = 0;
        int skipped = 0;

        for (int i = 1; i < rows.size(); i++) {
            List<String> r = rows.get(i);
            String id = getCell(r, col, "id");
            String question = getCell(r, col, "question");
            String answer = getCell(r, col, "answer");
            if (id.isEmpty() || question.isEmpty() || answer.isEmpty()) {
                skipped++;
                continue;
            }

            String category = getCellOrDefault(r, col, "category", "MEDIUM");
            int xp = parseInt(getCellOrDefault(r, col, "xp", "20"), 20);
            boolean active = parseBool(getCellOrDefault(r, col, "active", "true"));

            RiddleEntity entity = new RiddleEntity(
                    id, question, answer, "", "", category, category, xp, active);
            dao.insert(entity);
            synced++;
        }
        return new SyncStats(synced, skipped);
    }

    public static SyncStats syncJokes(@NonNull String url, @NonNull PerazimDatabase db) throws Exception {
        return processJokesRows(fetchCsv(url), db.jokeDao());
    }

    static SyncStats processJokesRows(List<List<String>> rows, JokeDao dao) {
        if (rows == null || rows.size() < 2) return new SyncStats(0, 0);

        Map<String, Integer> col = mapHeaders(rows.get(0));
        if (!col.containsKey("id") || !col.containsKey("text")) {
            return new SyncStats(0, 0);
        }

        int synced = 0;
        int skipped = 0;

        for (int i = 1; i < rows.size(); i++) {
            List<String> r = rows.get(i);
            String id = getCell(r, col, "id");
            String text = getCell(r, col, "text");
            if (id.isEmpty() || text.isEmpty()) {
                skipped++;
                continue;
            }

            boolean active = parseBool(getCellOrDefault(r, col, "active", "true"));
            JokeEntity entity = new JokeEntity(id, text, "FAMILY_SAFE", "Fellowship", active);
            dao.insert(entity);
            synced++;
        }
        return new SyncStats(synced, skipped);
    }

    public static SyncStats syncSermons(@NonNull String url, @NonNull PerazimDatabase db) throws Exception {
        return processSermonsRows(fetchCsv(url), db.sermonDao());
    }

    static SyncStats processSermonsRows(List<List<String>> rows, SermonDao dao) {
        if (rows == null || rows.size() < 2) return new SyncStats(0, 0);

        Map<String, Integer> col = mapHeaders(rows.get(0));
        if (!col.containsKey("id") || !col.containsKey("title")) {
            return new SyncStats(0, 0);
        }

        int synced = 0;
        int skipped = 0;

        for (int i = 1; i < rows.size(); i++) {
            List<String> r = rows.get(i);
            String id = getCell(r, col, "id");
            String title = getCell(r, col, "title");
            if (id.isEmpty() || title.isEmpty()) {
                skipped++;
                continue;
            }

            String speaker = getCellOrDefault(r, col, "speaker", "Bishop Dr. David Mutweri");
            long dateMillis = parseDate(getCell(r, col, "date"), System.currentTimeMillis());
            String series = getCellOrDefault(r, col, "series", "Breakthrough Series");
            String scripture = getCellOrDefault(r, col, "scripture", "2 Samuel 5:20");
            String ytUrl = getCellOrDefault(r, col, "youtube_url", "");
            String audioUrl = getCellOrDefault(r, col, "audio_url", "");
            boolean active = parseBool(getCellOrDefault(r, col, "active", "true"));

            SermonEntity entity = new SermonEntity(
                    id, title, speaker, series, scripture, audioUrl, ytUrl, dateMillis,
                    2700, "Sermon message", "", false, "", active);
            dao.insert(entity);
            synced++;
        }
        return new SyncStats(synced, skipped);
    }

    public static SyncStats syncVerses(@NonNull String url, @NonNull PerazimDatabase db) throws Exception {
        return processVersesRows(fetchCsv(url), db.dailyVerseDao());
    }

    static SyncStats processVersesRows(List<List<String>> rows, DailyVerseDao dao) {
        if (rows == null || rows.size() < 2) return new SyncStats(0, 0);

        Map<String, Integer> col = mapHeaders(rows.get(0));
        if (!col.containsKey("date") || !col.containsKey("reference") || !col.containsKey("text")) {
            return new SyncStats(0, 0);
        }

        int synced = 0;
        int skipped = 0;

        for (int i = 1; i < rows.size(); i++) {
            List<String> r = rows.get(i);
            String date = getCell(r, col, "date");
            String ref = getCell(r, col, "reference");
            String text = getCell(r, col, "text");
            if (date.isEmpty() || ref.isEmpty() || text.isEmpty()) {
                skipped++;
                continue;
            }

            DailyVerseEntity entity = new DailyVerseEntity(date, ref, text, true);
            dao.insert(entity);
            synced++;
        }
        return new SyncStats(synced, skipped);
    }

    public static SyncStats syncConfig(@NonNull String url, @NonNull PerazimDatabase db) throws Exception {
        return processConfigRows(fetchCsv(url), db.appConfigDao());
    }

    static SyncStats processConfigRows(List<List<String>> rows, AppConfigDao dao) {
        if (rows == null || rows.size() < 2) return new SyncStats(0, 0);

        Map<String, Integer> col = mapHeaders(rows.get(0));
        if (!col.containsKey("key") || !col.containsKey("value")) {
            return new SyncStats(0, 0);
        }

        int synced = 0;
        int skipped = 0;

        for (int i = 1; i < rows.size(); i++) {
            List<String> r = rows.get(i);
            String key = getCell(r, col, "key");
            String val = getCell(r, col, "value");
            if (key.isEmpty()) {
                skipped++;
                continue;
            }
            AppConfigEntity entity = new AppConfigEntity(key, val);
            dao.insert(entity);
            synced++;
        }
        return new SyncStats(synced, skipped);
    }

    // -------------------------------------------------------------------------
    // Utilities
    // -------------------------------------------------------------------------

    private static List<List<String>> fetchCsv(String urlString) throws Exception {
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(TIMEOUT_MILLIS);
        conn.setReadTimeout(TIMEOUT_MILLIS);
        conn.setRequestProperty("User-Agent", "Perazim-Android/2.2.0");

        int responseCode = conn.getResponseCode();
        if (responseCode != HttpURLConnection.HTTP_OK) {
            throw new Exception("HTTP response " + responseCode + " for " + urlString);
        }

        try (InputStream in = conn.getInputStream();
             BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            return CsvParser.parse(reader);
        } finally {
            conn.disconnect();
        }
    }

    static Map<String, Integer> mapHeaders(List<String> headerRow) {
        Map<String, Integer> map = new HashMap<>();
        for (int i = 0; i < headerRow.size(); i++) {
            map.put(headerRow.get(i).trim().toLowerCase(Locale.ROOT), i);
        }
        return map;
    }

    static String getCell(List<String> row, Map<String, Integer> col, String name) {
        Integer idx = col.get(name);
        if (idx == null || idx >= row.size()) return "";
        return row.get(idx).trim();
    }

    static String getCellOrDefault(List<String> row, Map<String, Integer> col, String name, String def) {
        String val = getCell(row, col, name);
        return val.isEmpty() ? def : val;
    }

    static boolean parseBool(String val) {
        if (val == null) return true;
        String s = val.trim().toLowerCase(Locale.ROOT);
        return !s.equals("false") && !s.equals("0") && !s.equals("no");
    }

    static int parseInt(String val, int def) {
        try {
            return Integer.parseInt(val.trim());
        } catch (Exception e) {
            return def;
        }
    }

    static long parseDate(String val, long fallback) {
        if (val == null || val.trim().isEmpty()) return fallback;
        val = val.trim();
        SimpleDateFormat sdf = new SimpleDateFormat(
                val.contains(":") ? "yyyy-MM-dd HH:mm" : "yyyy-MM-dd", Locale.ROOT);
        sdf.setTimeZone(NAIROBI_TZ);
        try {
            Date d = sdf.parse(val);
            return (d != null) ? d.getTime() : fallback;
        } catch (ParseException e) {
            return fallback;
        }
    }

    public static class SyncStats {
        public final int synced;
        public final int skipped;

        public SyncStats(int synced, int skipped) {
            this.synced = synced;
            this.skipped = skipped;
        }
    }
}
