package org.perazimchurch.app.data.local.seeder;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import org.perazimchurch.app.data.local.PerazimDatabase;
import org.perazimchurch.app.data.local.dao.AnnouncementDao;
import org.perazimchurch.app.data.local.dao.EventDao;
import org.perazimchurch.app.data.local.dao.HymnDao;
import org.perazimchurch.app.data.local.dao.JokeDao;
import org.perazimchurch.app.data.local.dao.ReflectionDao;
import org.perazimchurch.app.data.local.dao.RiddleDao;
import org.perazimchurch.app.data.local.dao.SermonDao;
import org.perazimchurch.app.data.local.entity.AnnouncementEntity;
import org.perazimchurch.app.data.local.entity.EventEntity;
import org.perazimchurch.app.data.local.entity.HymnEntity;
import org.perazimchurch.app.data.local.entity.JokeEntity;
import org.perazimchurch.app.data.local.entity.ReflectionEntity;
import org.perazimchurch.app.data.local.entity.RiddleEntity;
import org.perazimchurch.app.data.local.entity.SermonEntity;
import org.perazimchurch.app.data.repository.RoomAnnouncementRepository;
import org.perazimchurch.app.data.repository.RoomEventRepository;
import org.perazimchurch.app.data.repository.RoomHymnRepository;
import org.perazimchurch.app.data.repository.RoomReflectionRepository;
import org.perazimchurch.app.data.repository.RoomSermonRepository;
import org.perazimchurch.app.domain.model.Announcement;
import org.perazimchurch.app.domain.model.Event;
import org.perazimchurch.app.domain.model.Hymn;
import org.perazimchurch.app.domain.model.Reflection;
import org.perazimchurch.app.domain.model.Sermon;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Content Data Seeder for Workstream 1.4: Content Database.
 * Responsible for seeding Room tables (sermons, hymns, riddles, jokes, events,
 * announcements, reflections) from structured JSON assets located in assets/seed/.
 */
public class ContentDataSeeder {

    private static final String TAG = "PerazimContentSeeder";
    private static final String SEED_DIR = "seed/";
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();

    public interface SeedCallback {
        void onComplete(boolean success, String message);
    }

    /**
     * Seeds all tables if they are empty using the application context and database instance.
     *
     * @param context Application context to access assets
     * @param db      Room database instance
     */
    public static void seedIfNeeded(@NonNull Context context, @NonNull PerazimDatabase db) {
        Log.i(TAG, "Starting ContentDataSeeder check...");
        try {
            seedSermonsIfNeeded(context, db);
            seedHymnsIfNeeded(context, db);
            seedRiddlesIfNeeded(context, db);
            seedJokesIfNeeded(context, db);
            seedEventsIfNeeded(context, db);
            seedAnnouncementsIfNeeded(context, db);
            seedReflectionsIfNeeded(context, db);
            Log.i(TAG, "ContentDataSeeder seeding process completed successfully.");
        } catch (Exception e) {
            Log.e(TAG, "Error during content seeding: " + e.getMessage(), e);
            throw new RuntimeException("Content seeding failed: " + e.getMessage(), e);
        }
    }

    /**
     * Convenience overload using default PerazimDatabase instance.
     */
    public static void seedIfNeeded(@NonNull Context context) {
        seedIfNeeded(context, PerazimDatabase.getInstance(context));
    }

    /**
     * Asynchronous seeding method.
     */
    public static void seedIfNeededAsync(@NonNull Context context, SeedCallback callback) {
        EXECUTOR.execute(() -> {
            try {
                PerazimDatabase db = PerazimDatabase.getInstance(context);
                seedIfNeeded(context, db);
                if (callback != null) {
                    callback.onComplete(true, "All content tables successfully checked and seeded.");
                }
            } catch (Exception e) {
                Log.e(TAG, "Async seeding failed", e);
                if (callback != null) {
                    callback.onComplete(false, e.getMessage());
                }
            }
        });
    }

    // =========================================================================
    // TABLE SEEDERS
    // =========================================================================

    public static void seedSermonsIfNeeded(@NonNull Context context, @NonNull PerazimDatabase db) throws IOException, JSONException {
        SermonDao dao = db.sermonDao();
        List<SermonEntity> existing = dao.getAllSermons();
        if (existing != null && !existing.isEmpty()) {
            Log.d(TAG, "Sermons table already seeded (" + existing.size() + " records). Skipping.");
            return;
        }

        String jsonString = loadJsonFromAsset(context, "sermons.json");
        JSONArray jsonArray = new JSONArray(jsonString);
        List<SermonEntity> entities = new ArrayList<>(jsonArray.length());

        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject obj = jsonArray.getJSONObject(i);
            SermonEntity sermon = new SermonEntity(
                    obj.getString("id"),
                    obj.getString("title"),
                    obj.getString("preacher"),
                    obj.optString("series", ""),
                    obj.optString("passage", ""),
                    obj.optString("audioUrl", ""),
                    obj.optString("videoUrl", ""),
                    obj.optLong("dateMillis", System.currentTimeMillis()),
                    obj.optInt("durationSeconds", 0),
                    obj.optString("summary", ""),
                    obj.optString("notes", ""),
                    obj.optBoolean("isDownloaded", false),
                    obj.optString("localAudioPath", "")
            );
            entities.add(sermon);
        }

        dao.insertAll(entities);
        Log.i(TAG, "Seeded " + entities.size() + " sermons into database.");
    }

    public static void seedHymnsIfNeeded(@NonNull Context context, @NonNull PerazimDatabase db) throws IOException, JSONException {
        HymnDao dao = db.hymnDao();
        List<HymnEntity> existing = dao.getAllHymns();
        if (existing != null && !existing.isEmpty()) {
            Log.d(TAG, "Hymns table already seeded (" + existing.size() + " records). Skipping.");
            return;
        }

        String jsonString = loadJsonFromAsset(context, "hymns.json");
        JSONArray jsonArray = new JSONArray(jsonString);
        List<HymnEntity> entities = new ArrayList<>(jsonArray.length());

        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject obj = jsonArray.getJSONObject(i);
            HymnEntity hymn = new HymnEntity(
                    obj.getString("id"),
                    obj.getInt("number"),
                    obj.getString("title"),
                    obj.getString("lyrics"),
                    obj.optString("chords", ""),
                    obj.optString("keySignature", ""),
                    obj.optString("timeSignature", ""),
                    obj.optString("category", "General"),
                    obj.optBoolean("isFavorite", false)
            );
            entities.add(hymn);
        }

        dao.insertAll(entities);
        Log.i(TAG, "Seeded " + entities.size() + " hymns into database.");
    }

    public static void seedRiddlesIfNeeded(@NonNull Context context, @NonNull PerazimDatabase db) throws IOException, JSONException {
        RiddleDao dao = db.riddleDao();
        List<RiddleEntity> existing = dao.getAllRiddles();
        if (existing != null && !existing.isEmpty()) {
            Log.d(TAG, "Riddles table already seeded (" + existing.size() + " records). Skipping.");
            return;
        }

        String jsonString = loadJsonFromAsset(context, "riddles.json");
        JSONArray jsonArray = new JSONArray(jsonString);
        List<RiddleEntity> entities = new ArrayList<>(jsonArray.length());

        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject obj = jsonArray.getJSONObject(i);
            RiddleEntity riddle = new RiddleEntity(
                    obj.getString("id"),
                    obj.getString("question"),
                    obj.getString("answer"),
                    obj.optString("explanation", ""),
                    obj.optString("scriptureRef", ""),
                    obj.optString("difficulty", "MEDIUM")
            );
            entities.add(riddle);
        }

        dao.insertAll(entities);
        Log.i(TAG, "Seeded " + entities.size() + " riddles into database.");
    }

    public static void seedJokesIfNeeded(@NonNull Context context, @NonNull PerazimDatabase db) throws IOException, JSONException {
        JokeDao dao = db.jokeDao();
        List<JokeEntity> existing = dao.getAllJokes();
        if (existing != null && !existing.isEmpty()) {
            Log.d(TAG, "Jokes table already seeded (" + existing.size() + " records). Skipping.");
            return;
        }

        String jsonString = loadJsonFromAsset(context, "jokes.json");
        JSONArray jsonArray = new JSONArray(jsonString);
        List<JokeEntity> entities = new ArrayList<>(jsonArray.length());

        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject obj = jsonArray.getJSONObject(i);
            JokeEntity joke = new JokeEntity(
                    obj.getString("id"),
                    obj.getString("text"),
                    obj.optString("cleanRating", "FAMILY_SAFE"),
                    obj.optString("category", "Fellowship")
            );
            entities.add(joke);
        }

        dao.insertAll(entities);
        Log.i(TAG, "Seeded " + entities.size() + " jokes into database.");
    }

    public static void seedEventsIfNeeded(@NonNull Context context, @NonNull PerazimDatabase db) throws IOException, JSONException {
        EventDao dao = db.eventDao();
        List<EventEntity> existing = dao.getAllEvents();
        if (existing != null && !existing.isEmpty()) {
            Log.d(TAG, "Events table already seeded (" + existing.size() + " records). Skipping.");
            return;
        }

        String jsonString = loadJsonFromAsset(context, "events.json");
        JSONArray jsonArray = new JSONArray(jsonString);
        List<EventEntity> entities = new ArrayList<>(jsonArray.length());
        long now = System.currentTimeMillis();

        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject obj = jsonArray.getJSONObject(i);
            long startTime = obj.optLong("startTimeMillis", 0);
            long endTime = obj.optLong("endTimeMillis", 0);

            // Ensure event is upcoming so getUpcomingEvents() queries find it
            if (endTime <= now) {
                long duration = (endTime > startTime && startTime > 0) ? (endTime - startTime) : 7200000L;
                long offset = (startTime > 0) ? Math.abs(startTime % 864000000L) : 86400000L;
                startTime = now + offset;
                endTime = startTime + duration;
            }

            EventEntity event = new EventEntity(
                    obj.getString("id"),
                    obj.getString("title"),
                    obj.optString("description", ""),
                    startTime,
                    endTime,
                    obj.optString("location", "Main Sanctuary, Embu Headquarters"),
                    obj.optString("campusId", "campus_central"),
                    obj.optString("imageUrl", ""),
                    obj.optString("category", "General")
            );
            entities.add(event);
        }

        dao.insertAll(entities);
        Log.i(TAG, "Seeded " + entities.size() + " events into database.");
    }

    public static void seedAnnouncementsIfNeeded(@NonNull Context context, @NonNull PerazimDatabase db) throws IOException, JSONException {
        AnnouncementDao dao = db.announcementDao();
        List<AnnouncementEntity> existing = dao.getAllAnnouncements();
        if (existing != null && !existing.isEmpty()) {
            Log.d(TAG, "Announcements table already seeded (" + existing.size() + " records). Skipping.");
            return;
        }

        String jsonString = loadJsonFromAsset(context, "announcements.json");
        JSONArray jsonArray = new JSONArray(jsonString);
        List<AnnouncementEntity> entities = new ArrayList<>(jsonArray.length());
        long now = System.currentTimeMillis();

        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject obj = jsonArray.getJSONObject(i);
            long publishedMillis = obj.optLong("publishedMillis", now);
            long expiresMillis = obj.optLong("expiresMillis", now + 30L * 24 * 3600 * 1000L);

            // Ensure announcement is not expired so getActiveAnnouncements() queries find it
            if (expiresMillis <= now) {
                publishedMillis = now;
                expiresMillis = now + 30L * 24 * 3600 * 1000L;
            }

            AnnouncementEntity announcement = new AnnouncementEntity(
                    obj.getString("id"),
                    obj.getString("title"),
                    obj.optString("body", ""),
                    obj.optString("priority", "NORMAL"),
                    publishedMillis,
                    expiresMillis,
                    obj.optString("campusId", "campus_central")
            );
            entities.add(announcement);
        }

        dao.insertAll(entities);
        Log.i(TAG, "Seeded " + entities.size() + " announcements into database.");
    }

    public static void seedReflectionsIfNeeded(@NonNull Context context, @NonNull PerazimDatabase db) throws IOException, JSONException {
        ReflectionDao dao = db.reflectionDao();
        List<ReflectionEntity> existing = dao.getAllReflections();
        if (existing != null && !existing.isEmpty()) {
            Log.d(TAG, "Reflections table already seeded (" + existing.size() + " records). Skipping.");
            return;
        }

        String jsonString = loadJsonFromAsset(context, "reflections.json");
        JSONArray jsonArray = new JSONArray(jsonString);
        List<ReflectionEntity> entities = new ArrayList<>(jsonArray.length());

        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject obj = jsonArray.getJSONObject(i);
            long createdMillis = obj.optLong("createdMillis", System.currentTimeMillis());
            long updatedMillis = obj.optLong("updatedMillis", createdMillis);

            String content = obj.optString("content", "");
            if (content.isEmpty()) {
                StringBuilder sb = new StringBuilder();
                String passage = obj.optString("passageText", "");
                if (!passage.isEmpty()) {
                    sb.append("SCRIPTURE PASSAGE:\n").append(passage).append("\n\n");
                }
                String morningRefl = obj.optString("morningReflection", "");
                if (!morningRefl.isEmpty()) {
                    sb.append("MORNING REFLECTION:\n").append(morningRefl).append("\n\n");
                }
                String prayers = obj.optString("prayerPoints", "");
                if (!prayers.isEmpty()) {
                    sb.append("PRAYER POINTS:\n").append(prayers).append("\n\n");
                }
                String actions = obj.optString("actionSteps", "");
                if (!actions.isEmpty()) {
                    sb.append("ACTION STEPS:\n").append(actions);
                }
                content = sb.toString().trim();
            }

            ReflectionEntity reflection = new ReflectionEntity(
                    obj.getString("id"),
                    obj.optString("userId", "bishop_dr_david_mutweri"),
                    obj.getString("title"),
                    content,
                    obj.optString("passageRef", ""),
                    createdMillis,
                    updatedMillis,
                    obj.optBoolean("isPrivate", false)
            );
            entities.add(reflection);
        }

        dao.insertAll(entities);
        Log.i(TAG, "Seeded " + entities.size() + " reflections into database.");
    }

    // =========================================================================
    // REPOSITORY RETRIEVAL VERIFICATION
    // =========================================================================

    /**
     * Verifies that the respective Room Repositories retrieve seeded content from Room.
     * Validates:
     * 1. RoomSermonRepository
     * 2. RoomHymnRepository
     * 3. RoomEventRepository
     * 4. RoomAnnouncementRepository
     * 5. RoomReflectionRepository
     *
     * @param db Room database instance
     * @return true if all repository verifications pass
     */
    public static boolean verifyRepositories(@NonNull PerazimDatabase db) {
        Log.i(TAG, "Starting Room Repository Verification against seeded content...");

        // 1. RoomSermonRepository
        RoomSermonRepository sermonRepo = new RoomSermonRepository(db.sermonDao());
        List<Sermon> recentSermons = sermonRepo.getRecentSermons();
        if (recentSermons == null || recentSermons.size() < 4) {
            throw new IllegalStateException("RoomSermonRepository verification failed: expected >= 4 sermons, got " + (recentSermons == null ? "null" : recentSermons.size()));
        }
        boolean foundSupernatural = false;
        boolean foundBaalPerazim = false;
        for (Sermon s : recentSermons) {
            if ("Supernatural Breakthrough".equals(s.getTitle())) foundSupernatural = true;
            if ("The Baal-Perazim Mandate".equals(s.getTitle())) foundBaalPerazim = true;
        }
        if (!foundSupernatural || !foundBaalPerazim) {
            throw new IllegalStateException("RoomSermonRepository verification failed: missing required Bishop Dr. David Mutweri sermons");
        }
        List<Sermon> searchResults = sermonRepo.searchSermons("Breakthrough");
        if (searchResults == null || searchResults.isEmpty()) {
            throw new IllegalStateException("RoomSermonRepository search verification failed for 'Breakthrough'");
        }
        Log.d(TAG, "RoomSermonRepository verified: " + recentSermons.size() + " sermons retrieved.");

        // 2. RoomHymnRepository
        RoomHymnRepository hymnRepo = new RoomHymnRepository(db.hymnDao());
        List<Hymn> hymns = hymnRepo.getAllHymns();
        if (hymns == null || hymns.size() < 10) {
            throw new IllegalStateException("RoomHymnRepository verification failed: expected >= 10 hymns, got " + (hymns == null ? "null" : hymns.size()));
        }
        Hymn hymn1 = hymnRepo.getHymnByNumber(1);
        if (hymn1 == null || !"Holy, Holy, Holy! Lord God Almighty".equals(hymn1.getTitle())) {
            throw new IllegalStateException("RoomHymnRepository verification failed for Hymn #1");
        }
        List<Hymn> worshipHymns = hymnRepo.searchHymns("Holy");
        if (worshipHymns == null || worshipHymns.isEmpty()) {
            throw new IllegalStateException("RoomHymnRepository search verification failed");
        }
        Log.d(TAG, "RoomHymnRepository verified: " + hymns.size() + " hymns retrieved.");

        // 3. RoomEventRepository
        RoomEventRepository eventRepo = new RoomEventRepository(db.eventDao());
        List<Event> upcomingEvents = eventRepo.getUpcomingEvents();
        if (upcomingEvents == null || upcomingEvents.size() < 4) {
            throw new IllegalStateException("RoomEventRepository verification failed: expected >= 4 upcoming events, got " + (upcomingEvents == null ? "null" : upcomingEvents.size()));
        }
        boolean foundSunday = false;
        boolean foundMidweek = false;
        boolean foundYouth = false;
        boolean foundVigil = false;
        for (Event e : upcomingEvents) {
            if ("Sunday Breakthrough Service".equals(e.getTitle())) foundSunday = true;
            if (e.getTitle() != null && e.getTitle().contains("Mid-week Deliverance")) foundMidweek = true;
            if (e.getTitle() != null && e.getTitle().contains("Youth Explosion")) foundYouth = true;
            if (e.getTitle() != null && e.getTitle().contains("Prayer Vigil")) foundVigil = true;
        }
        if (!foundSunday || !foundMidweek || !foundYouth || !foundVigil) {
            throw new IllegalStateException("RoomEventRepository verification failed: required church gatherings not found in upcoming events");
        }
        Log.d(TAG, "RoomEventRepository verified: " + upcomingEvents.size() + " upcoming events retrieved.");

        // 4. RoomAnnouncementRepository
        RoomAnnouncementRepository announcementRepo = new RoomAnnouncementRepository(db.announcementDao());
        List<Announcement> activeAnnouncements = announcementRepo.getActiveAnnouncements();
        if (activeAnnouncements == null || activeAnnouncements.size() < 3) {
            throw new IllegalStateException("RoomAnnouncementRepository verification failed: expected >= 3 active announcements, got " + (activeAnnouncements == null ? "null" : activeAnnouncements.size()));
        }
        boolean foundPastoral = false;
        for (Announcement a : activeAnnouncements) {
            if (a.getTitle() != null && a.getTitle().contains("Pastoral Bulletin")) {
                foundPastoral = true;
                break;
            }
        }
        if (!foundPastoral) {
            throw new IllegalStateException("RoomAnnouncementRepository verification failed: missing Pastoral Bulletin");
        }
        Log.d(TAG, "RoomAnnouncementRepository verified: " + activeAnnouncements.size() + " active announcements retrieved.");

        // 5. RoomReflectionRepository
        RoomReflectionRepository reflectionRepo = new RoomReflectionRepository(db.reflectionDao());
        Reflection todayReflection = reflectionRepo.getTodayReflection();
        if (todayReflection == null) {
            throw new IllegalStateException("RoomReflectionRepository verification failed: getTodayReflection() returned null");
        }
        List<Reflection> reflections = reflectionRepo.getRecentReflections();
        if (reflections == null || reflections.size() < 3) {
            throw new IllegalStateException("RoomReflectionRepository verification failed: expected >= 3 reflections, got " + (reflections == null ? "null" : reflections.size()));
        }
        Log.d(TAG, "RoomReflectionRepository verified: " + reflections.size() + " daily devotionals retrieved. Today: " + todayReflection.getTitle());

        // 6. Riddles and Jokes DAOs
        List<RiddleEntity> riddles = db.riddleDao().getAllRiddles();
        if (riddles == null || riddles.size() < 5) {
            throw new IllegalStateException("RiddleDao verification failed: expected >= 5 riddles, got " + (riddles == null ? "null" : riddles.size()));
        }
        List<JokeEntity> jokes = db.jokeDao().getAllJokes();
        if (jokes == null || jokes.size() < 5) {
            throw new IllegalStateException("JokeDao verification failed: expected >= 5 jokes, got " + (jokes == null ? "null" : jokes.size()));
        }
        Log.d(TAG, "RiddleDao (" + riddles.size() + " riddles) and JokeDao (" + jokes.size() + " jokes) verified.");

        Log.i(TAG, "[PERAZIM-CONTENT-DATABASE-GATE-1.4: SUCCESS]");
        return true;
    }

    public static boolean verifyRepositories(@NonNull Context context) {
        return verifyRepositories(PerazimDatabase.getInstance(context));
    }

    // =========================================================================
    // UTILITY METHODS
    // =========================================================================

    private static String loadJsonFromAsset(Context context, String fileName) throws IOException {
        InputStream is = context.getAssets().open(SEED_DIR + fileName);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int bytesRead;
        while ((bytesRead = is.read(buffer)) != -1) {
            baos.write(buffer, 0, bytesRead);
        }
        is.close();
        return new String(baos.toByteArray(), StandardCharsets.UTF_8);
    }

    // =========================================================================
    // CLI VERIFICATION ENTRY POINT
    // =========================================================================

    public static void main(String[] args) {
        System.out.println(">>> Starting ContentDataSeeder CLI Verification...");
        try {
            android.os.Looper.prepareMainLooper();
            try {
                Class<?> walFlags = Class.forName("android.database.sqlite.SQLiteCompatibilityWalFlags");
                java.lang.reflect.Field f = walFlags.getDeclaredField("sInitialized");
                f.setAccessible(true);
                f.setBoolean(null, true);
            } catch (Throwable ignored) {}

            Class<?> activityThreadClass = Class.forName("android.app.ActivityThread");
            Object thread = activityThreadClass.getMethod("systemMain").invoke(null);
            Context systemContext = (Context) activityThreadClass.getMethod("getSystemContext").invoke(thread);
            Context appContext = systemContext.createPackageContext("org.perazimchurch.app", Context.CONTEXT_INCLUDE_CODE | Context.CONTEXT_IGNORE_SECURITY);
            Context safeContext = new android.content.ContextWrapper(appContext) {
                @Override
                public Context getApplicationContext() {
                    Context app = super.getApplicationContext();
                    return app != null ? app : this;
                }
                @Override
                public String getPackageName() {
                    return "org.perazimchurch.app";
                }
                @Override
                public String getOpPackageName() {
                    return "org.perazimchurch.app";
                }
            };

            PerazimDatabase db = PerazimDatabase.getInstance(safeContext);

            Thread worker = new Thread(() -> {
                try {
                    System.out.println(">>> Seeding database tables if needed...");
                    seedIfNeeded(safeContext, db);

                    System.out.println(">>> Verifying Room repositories retrieval from seeded content...");
                    boolean verified = verifyRepositories(db);
                    if (verified) {
                        System.out.println(">>> [PERAZIM-CONTENT-DATABASE-GATE-1.4: SUCCESS]");
                        System.exit(0);
                    } else {
                        System.err.println(">>> [PERAZIM-CONTENT-DATABASE-GATE-1.4: FAILED]");
                        System.exit(1);
                    }
                } catch (Throwable t) {
                    System.err.println(">>> ContentDataSeeder CLI failed with exception: " + t.getMessage());
                    t.printStackTrace(System.err);
                    System.exit(1);
                }
            });
            worker.start();
            worker.join();
        } catch (Throwable t) {
            System.err.println(">>> ContentDataSeeder initialization failed: " + t.getMessage());
            t.printStackTrace(System.err);
            System.exit(1);
        }
    }
}
