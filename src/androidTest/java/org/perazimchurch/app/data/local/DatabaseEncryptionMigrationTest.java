package org.perazimchurch.app.data.local;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import android.content.Context;
import android.database.Cursor;

import androidx.room.Room;
import androidx.room.testing.MigrationTestHelper;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import net.zetetic.database.sqlcipher.SQLiteDatabase;
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/** Exercises encryption before Room opens either the shipped seed asset or an installed DB. */
@RunWith(AndroidJUnit4.class)
public class DatabaseEncryptionMigrationTest {
    private static final String ASSET = "database/perazim_database.db";
    private static final String QUEUE_ID = "encrypted-migration-draft";

    @Rule
    public final MigrationTestHelper helper = new MigrationTestHelper(
            InstrumentationRegistry.getInstrumentation(), PerazimDatabase.class);

    @Test
    public void freshInstallEncryptsSeedBeforeRoomOpensIt() throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        String name = uniqueName("encrypted_fresh");
        DatabaseKeyStore keyStore = new DatabaseKeyStore(context, name);
        PerazimDatabase room = null;
        try {
            context.deleteDatabase(name);
            byte[] key = DatabaseEncryptionManager.prepareDatabase(context, name, ASSET);
            File database = context.getDatabasePath(name);
            assertTrue(database.isFile());
            assertFalse("Installed file must not retain the plaintext SQLite header", hasPlaintextHeader(database));

            room = openRoom(context, name, key);
            SupportSQLiteDatabase opened = room.getOpenHelper().getWritableDatabase();
            assertEquals(7, encryptedUserVersion(database, key));
            assertTrue("Seeded Bible rows remain available after encryption", scalarInt(opened, "SELECT COUNT(*) FROM bible_verses") > 0);
        } finally {
            if (room != null) room.close();
            context.deleteDatabase(name);
            keyStore.clearForTesting();
        }
    }

    @Test
    public void plaintextVersion6UpgradeEncryptsAndKeepsEverySyncQueueRow() throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        String name = uniqueName("encrypted_upgrade_v6");
        DatabaseKeyStore keyStore = new DatabaseKeyStore(context, name);
        PerazimDatabase room = null;
        try {
            context.deleteDatabase(name);
            SupportSQLiteDatabase plaintext = helper.createDatabase(name, 6);
            plaintext.execSQL("INSERT INTO sync_queue " +
                            "(id, operationType, entityType, entityId, payloadJson, timestampMillis, status, retryCount, lastError) " +
                            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    new Object[]{QUEUE_ID, "CREATE", "PRAYER", "prayer-encrypted", "{\"content\":\"retained\"}",
                            1700000000000L, "PENDING", 1, "timeout"});
            plaintext.execSQL("INSERT INTO sync_queue " +
                            "(id, operationType, entityType, entityId, payloadJson, timestampMillis, status, retryCount, lastError) " +
                            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    new Object[]{QUEUE_ID + "-second", "UPDATE", "PRAYER", "prayer-second", "{\"content\":\"also retained\"}",
                            1700000000001L, "FAILED", 2, "rejected"});
            plaintext.close();

            byte[] key = DatabaseEncryptionManager.prepareDatabase(context, name, ASSET);
            File database = context.getDatabasePath(name);
            assertFalse("Upgrade must replace the plaintext file with SQLCipher output", hasPlaintextHeader(database));

            room = openRoom(context, name, key);
            SupportSQLiteDatabase encryptedRoom = room.getOpenHelper().getWritableDatabase();
            assertEquals(7, encryptedUserVersion(database, key));
            assertEquals(2, scalarInt(encryptedRoom, "SELECT COUNT(*) FROM sync_queue"));
            assertEquals("{\"content\":\"retained\"}", stringValue(encryptedRoom,
                    "SELECT payloadJson FROM sync_queue WHERE id = '" + QUEUE_ID + "'"));
            assertEquals("FAILED", stringValue(encryptedRoom,
                    "SELECT status FROM sync_queue WHERE id = '" + QUEUE_ID + "-second'"));
            assertFalse("Temporary plaintext backup must be removed after validation",
                    new File(database.getPath() + ".plaintext-backup").exists());
        } finally {
            if (room != null) room.close();
            context.deleteDatabase(name);
            keyStore.clearForTesting();
        }
    }

    @Test
    public void missingKeystoreKeyFailsClosedAndPreservesEncryptedDatabase() throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        String name = uniqueName("encrypted_lost_key");
        DatabaseKeyStore keyStore = new DatabaseKeyStore(context, name);
        try {
            context.deleteDatabase(name);
            DatabaseEncryptionManager.prepareDatabase(context, name, ASSET);
            File database = context.getDatabasePath(name);
            long originalLength = database.length();
            java.security.KeyStore androidKeyStore = java.security.KeyStore.getInstance("AndroidKeyStore");
            androidKeyStore.load(null);
            androidKeyStore.deleteEntry(keyStore.aliasForTesting());

            try {
                DatabaseEncryptionManager.prepareDatabase(context, name, ASSET);
                fail("Missing wrapping key must refuse startup");
            } catch (IllegalStateException expected) {
                assertTrue(expected.getMessage().contains("Android Keystore database key is missing"));
            }
            assertTrue("Encrypted DB must remain in place when its Keystore key is lost", database.isFile());
            assertEquals("The encrypted DB must not be replaced by a fresh plaintext seed", originalLength, database.length());
            assertFalse(hasPlaintextHeader(database));
        } finally {
            context.deleteDatabase(name);
            keyStore.clearForTesting();
        }
    }

    private static PerazimDatabase openRoom(Context context, String name, byte[] key) {
        return Room.databaseBuilder(context, PerazimDatabase.class, name)
                .openHelperFactory(new SupportOpenHelperFactory(key))
                .addMigrations(PerazimDatabase.MIGRATION_4_5,
                        PerazimDatabase.MIGRATION_5_6,
                        PerazimDatabase.MIGRATION_6_7)
                .build();
    }

    private static boolean hasPlaintextHeader(File file) throws IOException {
        byte[] expected = ("SQLite format 3" + '\0').getBytes(StandardCharsets.US_ASCII);
        byte[] actual = new byte[expected.length];
        try (FileInputStream input = new FileInputStream(file)) {
            int read = input.read(actual);
            if (read != expected.length) return false;
        }
        return java.util.Arrays.equals(expected, actual);
    }

    private static int pragmaInt(SupportSQLiteDatabase db, String sql) {
        try (Cursor cursor = db.query(sql)) {
            assertTrue(cursor.moveToFirst());
            return cursor.getInt(0);
        }
    }

    private static int encryptedUserVersion(File database, byte[] key) {
        SQLiteDatabase encrypted = null;
        try {
            encrypted = SQLiteDatabase.openDatabase(database.getAbsolutePath(), key, null,
                    SQLiteDatabase.OPEN_READONLY, null);
            try (Cursor cursor = encrypted.rawQuery("PRAGMA user_version", null)) {
                assertTrue(cursor.moveToFirst());
                return cursor.getInt(0);
            }
        } finally {
            if (encrypted != null) encrypted.close();
        }
    }

    private static int scalarInt(SupportSQLiteDatabase db, String sql) {
        return pragmaInt(db, sql);
    }

    private static String stringValue(SupportSQLiteDatabase db, String sql) {
        try (Cursor cursor = db.query(sql)) {
            assertTrue(cursor.moveToFirst());
            return cursor.getString(0);
        }
    }

    private static String uniqueName(String prefix) {
        return prefix + "_" + UUID.randomUUID().toString().replace("-", "");
    }
}
