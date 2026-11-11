package org.perazimchurch.app.data.local;

import android.content.Context;
import android.database.Cursor;
import android.util.Log;

import androidx.annotation.NonNull;

import net.zetetic.database.sqlcipher.SQLiteDatabase;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/** Prepares the Room file as SQLCipher before Room opens it. */
public final class DatabaseEncryptionManager {
    private static final String TAG = "DatabaseEncryption";
    private static final String SQLITE_HEADER = "SQLite format 3" + '\0';
    private static final Object LOCK = new Object();

    private DatabaseEncryptionManager() { }

    /**
     * Encrypts a fresh asset or a legacy plaintext database in place. Existing plaintext data is
     * retained until the exported encrypted copy has passed SQLCipher integrity validation.
     */
    @NonNull
    public static byte[] prepareDatabase(@NonNull Context context,
                                         @NonNull String databaseName,
                                         @NonNull String assetPath) {
        synchronized (LOCK) {
            System.loadLibrary("sqlcipher");
            Context app = context.getApplicationContext();
            File database = app.getDatabasePath(databaseName);
            File directory = database.getParentFile();
            if (directory == null || (!directory.exists() && !directory.mkdirs())) {
                throw new IllegalStateException("Could not access the private database directory");
            }

            DatabaseKeyStore keyStore = new DatabaseKeyStore(app, databaseName);
            File plaintextBackup = new File(database.getPath() + ".plaintext-backup");
            restoreInterruptedPlaintextMigration(database, plaintextBackup);

            if (database.exists() && !isPlaintextSqlite(database)) {
                byte[] key = keyStore.getExistingKey();
                if (key == null) {
                    throw new IllegalStateException("Encrypted database exists but its Android Keystore key record is missing; refusing plaintext fallback");
                }
                validateEncryptedDatabase(database, key);
                securelyDeletePlaintext(plaintextBackup);
                securelyDeleteSidecars(plaintextBackup);
                return key;
            }

            byte[] key = keyStore.getOrCreateKey();
            File plaintext = null;
            File encrypted = new File(directory, ".perazim_cipher_" + UUID.randomUUID() + ".tmp");
            try {
                if (!database.exists()) {
                    plaintext = File.createTempFile(".perazim_plain_", ".tmp", directory);
                    copyAsset(app, assetPath, plaintext);
                } else {
                    plaintext = database;
                }

                exportPlaintextToEncrypted(plaintext, encrypted, key);
                validateEncryptedDatabase(encrypted, key);
                installEncryptedCopy(database, encrypted, plaintextBackup, key);
                securelyDeletePlaintext(plaintextBackup);
                securelyDeleteSidecars(plaintextBackup);
                securelyDeleteSidecars(database);
                return key;
            } catch (Exception e) {
                if (e instanceof IllegalStateException) throw (IllegalStateException) e;
                throw new IllegalStateException("Could not encrypt the local database; plaintext was not opened by Room", e);
            } finally {
                if (plaintext != null && !plaintext.equals(database)) securelyDeletePlaintext(plaintext);
                securelyDeleteSidecars(plaintext);
                securelyDeletePlaintext(encrypted);
                securelyDeleteSidecars(encrypted);
            }
        }
    }

    private static void restoreInterruptedPlaintextMigration(File database, File backup) {
        if (!database.exists() && backup.exists() && !backup.renameTo(database)) {
            throw new IllegalStateException("Could not restore the retained plaintext database after an interrupted encryption; no data was discarded");
        }
    }

    private static boolean isPlaintextSqlite(File database) {
        if (!database.isFile() || database.length() < SQLITE_HEADER.length()) return false;
        byte[] header = new byte[SQLITE_HEADER.length()];
        try (FileInputStream input = new FileInputStream(database)) {
            int read = input.read(header);
            return read == header.length && SQLITE_HEADER.equals(new String(header, StandardCharsets.US_ASCII));
        } catch (IOException e) {
            throw new IllegalStateException("Could not inspect the local database format", e);
        }
    }

    private static void copyAsset(Context context, String assetPath, File destination) throws IOException {
        try (InputStream input = context.getAssets().open(assetPath);
             FileOutputStream output = new FileOutputStream(destination, false)) {
            byte[] buffer = new byte[32 * 1024];
            int count;
            while ((count = input.read(buffer)) >= 0) {
                if (count > 0) output.write(buffer, 0, count);
            }
            output.getFD().sync();
        }
    }

    private static void exportPlaintextToEncrypted(File plaintext, File encrypted, byte[] key) {
        SQLiteDatabase source = null;
        try {
            // Android's SQLCipher ATTACH path requires an existing private file here.
            // It will initialize this empty file using the supplied database key.
            try (FileOutputStream ignored = new FileOutputStream(encrypted, false)) {
                // Create the destination before ATTACH attempts to open it.
            }
            // SQLCipher opens the source SQLite file without a passphrase, then exports its
            // contents into an attached database keyed with the Keystore-wrapped passphrase.
            source = SQLiteDatabase.openDatabase(plaintext.getAbsolutePath(), null,
                    SQLiteDatabase.OPEN_READWRITE);
            source.execSQL("ATTACH DATABASE ? AS encrypted KEY ?",
                    new Object[]{encrypted.getAbsolutePath(), key});
            source.rawExecSQL("SELECT sqlcipher_export('encrypted')");
            // sqlcipher_export copies tables and indexes but leaves SQLite's user_version at 0.
            // Room uses this pragma to select the registered migration chain.
            int userVersion = readUserVersion(source);
            source.execSQL("DETACH DATABASE encrypted");
            setEncryptedUserVersion(encrypted, key, userVersion);
        } catch (Exception e) {
            throw new IllegalStateException("SQLCipher could not export the plaintext database", e);
        } finally {
            if (source != null) source.close();
        }
    }

    private static void setEncryptedUserVersion(File database, byte[] key, int version) {
        SQLiteDatabase encrypted = null;
        try {
            encrypted = SQLiteDatabase.openDatabase(database.getAbsolutePath(), key, null,
                    SQLiteDatabase.OPEN_READWRITE, null);
            encrypted.setVersion(version);
        } catch (Exception e) {
            throw new IllegalStateException("Could not preserve the Room schema version in the encrypted database", e);
        } finally {
            if (encrypted != null) encrypted.close();
        }
    }

    private static int readUserVersion(SQLiteDatabase database) {
        try (Cursor cursor = database.rawQuery("PRAGMA user_version", null)) {
            if (!cursor.moveToFirst()) {
                throw new IllegalStateException("Plaintext database has no readable Room schema version");
            }
            int version = cursor.getInt(0);
            if (version < 1) {
                throw new IllegalStateException("Plaintext database has no valid Room schema version");
            }
            return version;
        }
    }

    private static void validateEncryptedDatabase(File database, byte[] key) {
        SQLiteDatabase encrypted = null;
        try {
            encrypted = SQLiteDatabase.openDatabase(database.getAbsolutePath(), key, null,
                    SQLiteDatabase.OPEN_READONLY, null);
            try (Cursor cursor = encrypted.rawQuery("PRAGMA integrity_check", null)) {
                if (!cursor.moveToFirst() || !"ok".equalsIgnoreCase(cursor.getString(0))) {
                    throw new IllegalStateException("Encrypted database integrity check failed");
                }
            }
            try (Cursor cursor = encrypted.rawQuery("PRAGMA user_version", null)) {
                if (!cursor.moveToFirst() || cursor.getInt(0) < 1) {
                    throw new IllegalStateException("Encrypted database has no valid Room schema version");
                }
            }
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("Android Keystore key could not open and verify the SQLCipher database; refusing plaintext fallback", e);
        } finally {
            if (encrypted != null) encrypted.close();
        }
    }

    private static void installEncryptedCopy(File database, File encrypted, File plaintextBackup, byte[] key) {
        boolean movedOriginal = false;
        if (database.exists()) {
            if (plaintextBackup.exists()) {
                throw new IllegalStateException("A retained plaintext backup already exists; refusing to overwrite it");
            }
            if (!database.renameTo(plaintextBackup)) {
                throw new IllegalStateException("Could not retain the original plaintext database before conversion");
            }
            movedOriginal = true;
        }
        if (!encrypted.renameTo(database)) {
            if (movedOriginal && !plaintextBackup.renameTo(database)) {
                throw new IllegalStateException("Encrypted database install failed and the original database could not be restored");
            }
            throw new IllegalStateException("Could not atomically install the validated encrypted database");
        }
        try {
            validateEncryptedDatabase(database, key);
        } catch (RuntimeException validationError) {
            securelyDeletePlaintext(database);
            if (movedOriginal && !plaintextBackup.renameTo(database)) {
                throw new IllegalStateException("Final encrypted database validation failed and the original could not be restored", validationError);
            }
            throw validationError;
        }
    }

    /** Overwrites, syncs, and unlinks a private plaintext temporary file. */
    private static void securelyDeletePlaintext(File file) {
        if (file == null || !file.exists() || !file.isFile()) return;
        try (RandomAccessFile output = new RandomAccessFile(file, "rw")) {
            byte[] zeros = new byte[32 * 1024];
            long remaining = output.length();
            output.seek(0);
            while (remaining > 0) {
                int count = (int) Math.min(zeros.length, remaining);
                output.write(zeros, 0, count);
                remaining -= count;
            }
            output.getFD().sync();
        } catch (IOException e) {
            Log.e(TAG, "Could not overwrite a plaintext database temporary file", e);
            throw new IllegalStateException("Could not securely remove plaintext database data", e);
        }
        if (!file.delete() && file.exists()) {
            throw new IllegalStateException("Could not unlink a plaintext database temporary file");
        }
    }

    private static void securelyDeleteSidecars(File database) {
        if (database == null) return;
        securelyDeletePlaintext(new File(database.getPath() + "-wal"));
        securelyDeletePlaintext(new File(database.getPath() + "-shm"));
        securelyDeletePlaintext(new File(database.getPath() + "-journal"));
    }
}
