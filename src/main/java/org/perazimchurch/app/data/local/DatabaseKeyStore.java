package org.perazimchurch.app.data.local;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

/** Wraps a random SQLCipher passphrase with a non-exportable Android Keystore AES key. */
public final class DatabaseKeyStore {
    private static final String KEYSTORE = "AndroidKeyStore";
    private static final String CIPHER = "AES/GCM/NoPadding";
    private static final String PREFIX = "org.perazimchurch.app.database-key.v1.";
    private static final String PREFS_PREFIX = "perazim_database_key_v1_";
    private static final String VALUE = "wrapped_passphrase";
    private static final String IV = "iv";
    private static final int TAG_BITS = 128;
    private static final int KEY_BYTES = 32;

    private final SharedPreferences prefs;
    private final String alias;
    private final byte[] aad;

    public DatabaseKeyStore(@NonNull Context context, @NonNull String databaseName) {
        Context app = context.getApplicationContext();
        String identity = identity(databaseName);
        this.prefs = app.getSharedPreferences(PREFS_PREFIX + identity, Context.MODE_PRIVATE);
        this.alias = PREFIX + identity;
        this.aad = databaseName.getBytes(StandardCharsets.UTF_8);
    }

    /** Returns null only when no wrapped key record exists. A lost Keystore key fails closed. */
    @Nullable
    public synchronized byte[] getExistingKey() {
        boolean hasValue = prefs.contains(VALUE);
        boolean hasIv = prefs.contains(IV);
        if (!hasValue && !hasIv) return null;
        if (!hasValue || !hasIv) throw new IllegalStateException("Database key record is incomplete; encrypted database cannot be opened safely");
        try {
            KeyStore keyStore = KeyStore.getInstance(KEYSTORE);
            keyStore.load(null);
            if (!keyStore.containsAlias(alias)) {
                throw new IllegalStateException("Android Keystore database key is missing; refusing to open or replace the encrypted database");
            }
            Cipher cipher = Cipher.getInstance(CIPHER);
            cipher.init(Cipher.DECRYPT_MODE, (SecretKey) keyStore.getKey(alias, null),
                    new GCMParameterSpec(TAG_BITS, Base64.decode(prefs.getString(IV, ""), Base64.NO_WRAP)));
            cipher.updateAAD(aad);
            byte[] key = cipher.doFinal(Base64.decode(prefs.getString(VALUE, ""), Base64.NO_WRAP));
            if (key.length != KEY_BYTES) throw new IllegalStateException("Stored database key has an invalid length");
            return key;
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("Android Keystore could not unwrap the database key; encrypted data is preserved and startup is refused", e);
        }
    }

    /** Creates the wrapped database passphrase only for a new or verified plaintext database. */
    @NonNull
    public synchronized byte[] getOrCreateKey() {
        byte[] existing = getExistingKey();
        if (existing != null) return existing;
        try {
            KeyStore keyStore = KeyStore.getInstance(KEYSTORE);
            keyStore.load(null);
            if (keyStore.containsAlias(alias)) keyStore.deleteEntry(alias);
            KeyGenerator generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE);
            generator.init(new KeyGenParameterSpec.Builder(alias,
                    KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setRandomizedEncryptionRequired(true)
                    .build());
            SecretKey wrappingKey = generator.generateKey();
            byte[] passphrase = new byte[KEY_BYTES];
            new SecureRandom().nextBytes(passphrase);
            Cipher cipher = Cipher.getInstance(CIPHER);
            cipher.init(Cipher.ENCRYPT_MODE, wrappingKey);
            cipher.updateAAD(aad);
            byte[] wrapped = cipher.doFinal(passphrase);
            boolean saved = prefs.edit()
                    .putString(IV, Base64.encodeToString(cipher.getIV(), Base64.NO_WRAP))
                    .putString(VALUE, Base64.encodeToString(wrapped, Base64.NO_WRAP))
                    .commit();
            if (!saved) throw new IllegalStateException("Could not persist the wrapped database key");
            byte[] verified = getExistingKey();
            if (verified == null || !MessageDigest.isEqual(passphrase, verified)) {
                throw new IllegalStateException("Wrapped database key verification failed");
            }
            return verified;
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("Could not create a protected Android Keystore database key", e);
        }
    }

    private static String identity(String databaseName) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(databaseName.getBytes(StandardCharsets.UTF_8));
            return Base64.encodeToString(digest, Base64.URL_SAFE | Base64.NO_WRAP | Base64.NO_PADDING).substring(0, 22);
        } catch (Exception e) {
            throw new IllegalStateException("Could not identify the local database key", e);
        }
    }

    String aliasForTesting() { return alias; }

    void clearForTesting() {
        prefs.edit().clear().commit();
        try {
            KeyStore keyStore = KeyStore.getInstance(KEYSTORE);
            keyStore.load(null);
            if (keyStore.containsAlias(alias)) keyStore.deleteEntry(alias);
        } catch (Exception e) {
            throw new IllegalStateException("Could not clean test database key material", e);
        }
    }
}
