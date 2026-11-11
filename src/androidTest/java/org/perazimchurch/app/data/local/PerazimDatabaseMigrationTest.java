package org.perazimchurch.app.data.local;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;

import androidx.room.Room;
import androidx.room.migration.Migration;
import androidx.room.testing.MigrationTestHelper;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.SupportSQLiteOpenHelper;
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.perazimchurch.app.data.local.preference.GamificationStore;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Room migrations use authentic compiler-exported v4/v5 schemas and synthetic rows.
 * The bundled v5 asset is copied into the isolated validation package's private DB
 * directory before opening it; the tracked asset is only read.
 */
@RunWith(AndroidJUnit4.class)
public class PerazimDatabaseMigrationTest {

    private static final String FIXTURE_USER_ID = "migration-fixture-user";
    private static final String FIXTURE_VERSE_ID = "migration-fixture-verse";
    private static final String FIXTURE_MESSAGE_ID = "migration-fixture-message";
    private static final String FIXTURE_QUEUE_ID = "migration-fixture-draft";

    @Rule
    public final MigrationTestHelper helper = new MigrationTestHelper(
            InstrumentationRegistry.getInstrumentation(), PerazimDatabase.class);

    @Test
    public void migration4To5PreservesUserBibleNotesAndPendingDraft() throws IOException {
        assertMigrationAndRows(4, 5, PerazimDatabase.MIGRATION_4_5);
    }

    @Test
    public void migration5To6PreservesRowsAndAddsMessageMediaDefault() throws IOException {
        assertMigrationAndRows(5, 6, PerazimDatabase.MIGRATION_5_6);
    }

    @Test
    public void migration6To7PreservesQueuePayloadAndAddsStableSequenceAndBackoff() throws IOException {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        String databaseName = uniqueName("queue_6_to_7");
        try {
            SupportSQLiteDatabase before = helper.createDatabase(databaseName, 6);
            seedMessageAndDraft(before);
            before.execSQL("INSERT INTO sync_queue " +
                            "(id, operationType, entityType, entityId, payloadJson, timestampMillis, status, retryCount, lastError) " +
                            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    new Object[]{"migration-fixture-second-draft", "UPDATE", "message", "message-2",
                            "{\"body\":\"second draft\"}", 1700000004L, "pending", 1, "timeout"});
            before.close();

            SupportSQLiteDatabase after = helper.runMigrationsAndValidate(
                    databaseName, 7, true, PerazimDatabase.MIGRATION_6_7);
            assertEquals(2, scalarInt(after, "SELECT COUNT(*) FROM sync_queue"));
            assertEquals("{\"body\":\"synthetic draft\"}", firstString(after,
                    "SELECT payloadJson FROM sync_queue WHERE id = '" + FIXTURE_QUEUE_ID + "'"));
            assertEquals("{\"body\":\"second draft\"}", firstString(after,
                    "SELECT payloadJson FROM sync_queue WHERE id = 'migration-fixture-second-draft'"));
            assertEquals(0, scalarInt(after, "SELECT COUNT(*) FROM sync_queue WHERE nextAttemptAt != 0"));
            assertTrue("Migrated rows receive monotonically increasing enqueue sequence",
                    scalarInt(after, "SELECT MAX(sequenceNumber) FROM sync_queue") >
                            scalarInt(after, "SELECT MIN(sequenceNumber) FROM sync_queue"));
            after.close();
        } finally {
            context.deleteDatabase(databaseName);
        }
    }

    @Test
    public void migration4To6RunsBothRoomMigrationsAndPreservesRows() throws IOException {
        assertMigrationAndRows(4, 6,
                PerazimDatabase.MIGRATION_4_5,
                PerazimDatabase.MIGRATION_5_6);
    }

    @Test
    public void bundledVersion5AssetMigratesTo6WithoutLosingBibleContent() throws IOException {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        String databaseName = uniqueName("bundled_asset_v5");
        File databaseFile = context.getDatabasePath(databaseName);
        SharedPreferences preferences = context.getSharedPreferences(
                "perazim_gamification", Context.MODE_PRIVATE);
        String xpKey = GamificationStore.KEY_PREFIX_XP + FIXTURE_USER_ID;
        String graceKey = GamificationStore.KEY_PREFIX_GRACE + FIXTURE_USER_ID;
        preferences.edit().putInt(xpKey, 73).putInt(graceKey, 0).commit();
        PerazimDatabase upgraded = null;

        try {
            copyBundledDatabase(context, databaseFile);
            SupportSQLiteOpenHelper rawHelper = openAtExistingVersion(context, databaseName, 5);
            SupportSQLiteDatabase before = rawHelper.getWritableDatabase();

            assertEquals(5, pragmaInt(before, "user_version"));
            assertEquals(0, scalarInt(before, "SELECT COUNT(*) FROM users"));
            Map<String, Integer> translationCountsBefore = translationCounts(before);
            assertTrue("Bundled Bible fixture should contain all supported translations",
                    translationCountsBefore.size() >= 5);

            String verseId = firstString(before, "SELECT id FROM bible_verses ORDER BY id LIMIT 1");
            before.execSQL("UPDATE bible_verses SET isFavorite = 1, note = ? WHERE id = ?",
                    new Object[]{"synthetic migration note", verseId});
            seedUser(before);
            seedMessageAndDraft(before);
            rawHelper.close();

            upgraded = Room.databaseBuilder(context, PerazimDatabase.class, databaseName)
                    .addMigrations(PerazimDatabase.MIGRATION_5_6, PerazimDatabase.MIGRATION_6_7)
                    .build();
            SupportSQLiteDatabase after = upgraded.getOpenHelper().getWritableDatabase();

            assertEquals(7, pragmaInt(after, "user_version"));
            assertEquals(translationCountsBefore, translationCounts(after));
            assertFixtureRowsPreserved(after, verseId, "synthetic migration note");
            assertEquals("", firstString(after,
                    "SELECT mediaUrl FROM messages WHERE id = '" + FIXTURE_MESSAGE_ID + "'"));
            assertEquals(73, preferences.getInt(xpKey, -1));
            assertEquals(0, preferences.getInt(graceKey, -1));
        } finally {
            if (upgraded != null) upgraded.close();
            context.deleteDatabase(databaseName);
            preferences.edit().remove(xpKey).remove(graceKey).commit();
        }
    }

    @Test
    public void closedVersion6DatabaseCopyRestoresAndValidatesWithRows() throws IOException {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        String sourceName = uniqueName("backup_source_v6");
        String restoredName = uniqueName("backup_restored_v6");
        SharedPreferences preferences = context.getSharedPreferences(
                "perazim_gamification", Context.MODE_PRIVATE);
        String xpKey = GamificationStore.KEY_PREFIX_XP + FIXTURE_USER_ID;
        String graceKey = GamificationStore.KEY_PREFIX_GRACE + FIXTURE_USER_ID;
        preferences.edit().putInt(xpKey, 73).putInt(graceKey, 0).commit();
        PerazimDatabase source = null;
        PerazimDatabase restored = null;
        try {
            source = Room.databaseBuilder(context, PerazimDatabase.class, sourceName).build();
            SupportSQLiteDatabase sourceDb = source.getOpenHelper().getWritableDatabase();
            seedUser(sourceDb);
            seedVerse(sourceDb);
            seedMessageAndDraft(sourceDb);
            try (Cursor checkpoint = sourceDb.query("PRAGMA wal_checkpoint(FULL)")) {
                assertTrue("Checkpoint should return its status row", checkpoint.moveToFirst());
                assertEquals("Checkpoint should complete without a busy writer", 0,
                        checkpoint.getInt(0));
            }
            source.close();
            source = null;

            File sourceFile = context.getDatabasePath(sourceName);
            File restoredFile = context.getDatabasePath(restoredName);
            File walFile = new File(sourceFile.getPath() + "-wal");
            assertTrue("Closed source database should have no uncheckpointed WAL",
                    !walFile.exists() || walFile.length() == 0);
            copyDatabaseFile(sourceFile, restoredFile);

            restored = Room.databaseBuilder(context, PerazimDatabase.class, restoredName).build();
            SupportSQLiteDatabase restoredDb = restored.getOpenHelper().getWritableDatabase();
            assertEquals(7, pragmaInt(restoredDb, "user_version"));
            assertFixtureRowsPreserved(restoredDb, FIXTURE_VERSE_ID, "synthetic private note");
            assertEquals("Database copy must not replace the separate preferences file", 73,
                    preferences.getInt(xpKey, -1));
            assertEquals(0, preferences.getInt(graceKey, -1));
        } finally {
            if (source != null) source.close();
            if (restored != null) restored.close();
            context.deleteDatabase(sourceName);
            context.deleteDatabase(restoredName);
            preferences.edit().remove(xpKey).remove(graceKey).commit();
        }
    }

    private void assertMigrationAndRows(int startVersion, int targetVersion,
                                        Migration... migrations) throws IOException {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        String databaseName = uniqueName("schema_" + startVersion + "_to_" + targetVersion);
        try {
            SupportSQLiteDatabase before = helper.createDatabase(databaseName, startVersion);
            seedUser(before);
            seedVerse(before);
            seedMessageAndDraft(before);
            before.close();

            SupportSQLiteDatabase after = helper.runMigrationsAndValidate(
                    databaseName, targetVersion, true, migrations);
            assertFixtureRowsPreserved(after, FIXTURE_VERSE_ID, "synthetic private note");
            if (targetVersion >= 6) {
                assertEquals("", firstString(after,
                        "SELECT mediaUrl FROM messages WHERE id = '" + FIXTURE_MESSAGE_ID + "'"));
            }
            after.close();
        } finally {
            context.deleteDatabase(databaseName);
        }
    }

    private static void seedUser(SupportSQLiteDatabase db) {
        db.execSQL("INSERT INTO users "
                        + "(id, name, email, phone, role, campusId, avatarUrl, createdAt, updatedAt, "
                        + "isActive, streakCount, spiritualXp, gracePoints, streakFrozen) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                new Object[]{FIXTURE_USER_ID, "Synthetic Member", "fixture@example.invalid",
                        "0000000000", "member", "fixture-campus", "", 1700000000L, 1700000001L,
                        1, 4, 125, 0, 0});
    }

    private static void seedVerse(SupportSQLiteDatabase db) {
        db.execSQL("INSERT INTO bible_verses "
                        + "(id, translationId, chapterId, bookNumber, chapterNumber, verseNumber, "
                        + "text, isFavorite, note) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                new Object[]{FIXTURE_VERSE_ID, "KJV", "fixture-chapter", 1, 1, 1,
                        "Synthetic verse fixture", 1, "synthetic private note"});
    }

    private static void seedMessageAndDraft(SupportSQLiteDatabase db) {
        db.execSQL("INSERT INTO messages "
                        + "(id, conversationId, senderId, senderName, content, sentMillis, "
                        + "deliveryStatus, syncStatus) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                new Object[]{FIXTURE_MESSAGE_ID, "fixture-conversation", FIXTURE_USER_ID,
                        "Synthetic Member", "Synthetic message body", 1700000002L, "pending", "pending"});
        db.execSQL("INSERT INTO sync_queue "
                        + "(id, operationType, entityType, entityId, payloadJson, timestampMillis, "
                        + "status, retryCount, lastError) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                new Object[]{FIXTURE_QUEUE_ID, "CREATE", "message", FIXTURE_MESSAGE_ID,
                        "{\"body\":\"synthetic draft\"}", 1700000003L, "pending", 0, ""});
    }

    private static void assertFixtureRowsPreserved(SupportSQLiteDatabase db,
                                                   String verseId, String expectedNote) {
        assertEquals("Synthetic Member", firstString(db,
                "SELECT name FROM users WHERE id = '" + FIXTURE_USER_ID + "'"));
        assertEquals(125, scalarInt(db,
                "SELECT spiritualXp FROM users WHERE id = '" + FIXTURE_USER_ID + "'"));
        assertEquals(0, scalarInt(db,
                "SELECT gracePoints FROM users WHERE id = '" + FIXTURE_USER_ID + "'"));
        assertEquals(expectedNote, firstString(db,
                "SELECT note FROM bible_verses WHERE id = '" + verseId + "'"));
        assertEquals(1, scalarInt(db,
                "SELECT isFavorite FROM bible_verses WHERE id = '" + verseId + "'"));
        assertEquals("Synthetic message body", firstString(db,
                "SELECT content FROM messages WHERE id = '" + FIXTURE_MESSAGE_ID + "'"));
        assertEquals("{\"body\":\"synthetic draft\"}", firstString(db,
                "SELECT payloadJson FROM sync_queue WHERE id = '" + FIXTURE_QUEUE_ID + "'"));
        assertEquals("pending", firstString(db,
                "SELECT status FROM sync_queue WHERE id = '" + FIXTURE_QUEUE_ID + "'"));
    }

    private static SupportSQLiteOpenHelper openAtExistingVersion(Context context,
                                                                  String databaseName,
                                                                  int version) {
        SupportSQLiteOpenHelper.Callback callback = new SupportSQLiteOpenHelper.Callback(version) {
            @Override
            public void onCreate(SupportSQLiteDatabase db) {
                throw new AssertionError("The copied database asset must already exist");
            }

            @Override
            public void onUpgrade(SupportSQLiteDatabase db, int oldVersion, int newVersion) {
                throw new AssertionError("The fixture must remain at its recorded schema version");
            }
        };
        SupportSQLiteOpenHelper.Configuration configuration =
                SupportSQLiteOpenHelper.Configuration.builder(context)
                        .name(databaseName)
                        .callback(callback)
                        .build();
        return new FrameworkSQLiteOpenHelperFactory().create(configuration);
    }

    private static Map<String, Integer> translationCounts(SupportSQLiteDatabase db) {
        Map<String, Integer> result = new HashMap<>();
        try (Cursor cursor = db.query(
                "SELECT translationId, COUNT(*) FROM bible_verses "
                        + "GROUP BY translationId ORDER BY translationId")) {
            while (cursor.moveToNext()) {
                result.put(cursor.getString(0), cursor.getInt(1));
            }
        }
        return result;
    }

    private static void copyBundledDatabase(Context context, File destination) throws IOException {
        File parent = destination.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            throw new IOException("Could not create isolated validation database directory");
        }
        try (InputStream input = context.getAssets().open("database/perazim_database.db");
             OutputStream output = new java.io.FileOutputStream(destination)) {
            byte[] buffer = new byte[8192];
            int count;
            while ((count = input.read(buffer)) != -1) {
                output.write(buffer, 0, count);
            }
        }
    }

    private static void copyDatabaseFile(File source, File destination) throws IOException {
        File parent = destination.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            throw new IOException("Could not create isolated backup destination");
        }
        try (InputStream input = new java.io.FileInputStream(source);
             OutputStream output = new java.io.FileOutputStream(destination)) {
            byte[] buffer = new byte[8192];
            int count;
            while ((count = input.read(buffer)) != -1) {
                output.write(buffer, 0, count);
            }
        }
    }

    private static int pragmaInt(SupportSQLiteDatabase db, String pragma) {
        return scalarInt(db, "PRAGMA " + pragma);
    }

    private static int scalarInt(SupportSQLiteDatabase db, String sql) {
        try (Cursor cursor = db.query(sql)) {
            if (!cursor.moveToFirst()) {
                throw new AssertionError("Expected a result for: " + sql);
            }
            return cursor.getInt(0);
        }
    }

    private static String firstString(SupportSQLiteDatabase db, String sql) {
        try (Cursor cursor = db.query(sql)) {
            if (!cursor.moveToFirst()) {
                throw new AssertionError("Expected a row for: " + sql);
            }
            String value = cursor.getString(0);
            return value == null ? "" : value;
        }
    }

    private static String uniqueName(String prefix) {
        return "migration_" + prefix + "_" + UUID.randomUUID().toString().replace("-", "");
    }
}
