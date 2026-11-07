package org.perazimchurch.app.data.local.session;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.net.URI;
import java.security.KeyStore;
import java.util.Locale;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

/** Encrypted, account- and server-bound storage for an authentication token. */
public final class SessionTokenStore {
    public static final String DEFAULT_PREFERENCES = "perazim_session_token_v1";
    public static final String DEFAULT_KEY_ALIAS = "org.perazimchurch.app.session-token.v1";

    private static final String CIPHERTEXT = "ciphertext";
    private static final String IV = "iv";
    private static final String ACCOUNT = "account";
    private static final String ORIGIN = "origin";
    private static final String LEGACY_MIGRATION_BLOCKED = "legacy_migration_blocked";
    private static final int GCM_TAG_BITS = 128;

    private final SharedPreferences encryptedPrefs;
    private final String keyAlias;

    public SessionTokenStore(@NonNull Context context) {
        this(context, DEFAULT_PREFERENCES, DEFAULT_KEY_ALIAS);
    }

    /** The names are injectable so instrumentation tests can own and clean their data. */
    public SessionTokenStore(@NonNull Context context,
                             @NonNull String preferencesName,
                             @NonNull String keyAlias) {
        Context appContext = context.getApplicationContext();
        this.encryptedPrefs = appContext.getSharedPreferences(preferencesName, Context.MODE_PRIVATE);
        this.keyAlias = keyAlias;
    }

    /** True when an encrypted record exists, even if its binding does not match. */
    public synchronized boolean hasRecord() {
        return encryptedPrefs.contains(CIPHERTEXT) || encryptedPrefs.contains(IV)
                || encryptedPrefs.contains(ACCOUNT) || encryptedPrefs.contains(ORIGIN);
    }

    /** Returns null for a missing or differently bound record; cryptographic failures fail closed. */
    @Nullable
    public synchronized String read(@NonNull String accountId, @NonNull String normalizedOrigin) {
        if (!encryptedPrefs.contains(CIPHERTEXT)) return null;
        String storedAccount = encryptedPrefs.getString(ACCOUNT, null);
        String storedOrigin = encryptedPrefs.getString(ORIGIN, null);
        String encodedIv = encryptedPrefs.getString(IV, null);
        String encodedCiphertext = encryptedPrefs.getString(CIPHERTEXT, null);
        if (storedAccount == null || storedOrigin == null || encodedIv == null || encodedCiphertext == null) {
            return null;
        }
        if (!accountId.equals(storedAccount) || !normalizedOrigin.equals(storedOrigin)) return null;
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(),
                    new GCMParameterSpec(GCM_TAG_BITS, Base64.decode(encodedIv, Base64.NO_WRAP)));
            cipher.updateAAD(aad(accountId, normalizedOrigin));
            return new String(cipher.doFinal(Base64.decode(encodedCiphertext, Base64.NO_WRAP)),
                    java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception e) {
            return null;
        }
    }

    /** Stores and cryptographically verifies a token before reporting success. */
    public synchronized boolean write(@NonNull String token,
                                      @NonNull String accountId,
                                      @NonNull String normalizedOrigin) {
        if (token.isEmpty() || accountId.isEmpty() || normalizedOrigin.isEmpty()) return false;
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey());
            cipher.updateAAD(aad(accountId, normalizedOrigin));
            byte[] ciphertext = cipher.doFinal(token.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            boolean committed = encryptedPrefs.edit()
                    .putString(ACCOUNT, accountId)
                    .putString(ORIGIN, normalizedOrigin)
                    .putString(IV, Base64.encodeToString(cipher.getIV(), Base64.NO_WRAP))
                    .putString(CIPHERTEXT, Base64.encodeToString(ciphertext, Base64.NO_WRAP))
                    .commit();
            if (!committed) return false;
            String verified = read(accountId, normalizedOrigin);
            if (!token.equals(verified)) {
                return false;
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /** Migrates one legacy plaintext preference only after encrypted write/readback succeeds. */
    @Nullable
    public synchronized String migrateLegacy(@NonNull SharedPreferences legacyPrefs,
                                             @NonNull String legacyKey,
                                             @NonNull String accountId,
                                             @NonNull String normalizedOrigin) {
        if (hasRecord()) return null;
        String legacyToken = legacyPrefs.getString(legacyKey, null);
        if (legacyToken == null || legacyToken.trim().isEmpty()) return null;
        if (!write(legacyToken, accountId, normalizedOrigin)) return null;
        if (!legacyToken.equals(read(accountId, normalizedOrigin))) return null;
        if (!legacyPrefs.edit().remove(legacyKey).commit()) return null;
        return legacyToken;
    }

    /** Reads the bound record first and permits legacy migration only when no record existed. */
    @Nullable
    public synchronized String readOrMigrateLegacy(@NonNull SharedPreferences legacyPrefs,
                                                   @NonNull String legacyKey,
                                                   @NonNull String accountId,
                                                   @NonNull String normalizedOrigin) {
        if (encryptedPrefs.getBoolean(LEGACY_MIGRATION_BLOCKED, false)) return null;
        boolean hadEncryptedRecord = hasRecord();
        String token = read(accountId, normalizedOrigin);
        if (token != null) {
            if (legacyPrefs.contains(legacyKey) && !legacyPrefs.edit().remove(legacyKey).commit()) return null;
            return token;
        }
        if (hadEncryptedRecord) return null;
        return migrateLegacy(legacyPrefs, legacyKey, accountId, normalizedOrigin);
    }

    /** Prevents a failed new-token write from later recovering a stale plaintext token. */
    public synchronized boolean blockLegacyMigration() {
        return encryptedPrefs.edit().putBoolean(LEGACY_MIGRATION_BLOCKED, true).commit();
    }

    public synchronized boolean unblockLegacyMigration() {
        return encryptedPrefs.edit().remove(LEGACY_MIGRATION_BLOCKED).commit();
    }

    public synchronized boolean clear() {
        boolean removed = encryptedPrefs.edit().clear().commit();
        if (removed) {
            try {
                KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
                keyStore.load(null);
                if (keyStore.containsAlias(keyAlias)) keyStore.deleteEntry(keyAlias);
            } catch (Exception ignored) {
                // No token is returned after clear; stale unusable key material is harmless.
            }
        }
        return removed;
    }

    /** Converts an absolute server URL to its origin, excluding path, query, and fragment. */
    @NonNull
    public static String normalizeOrigin(@NonNull String baseUrl) {
        try {
            URI uri = URI.create(baseUrl.trim());
            String scheme = uri.getScheme();
            String host = uri.getHost();
            if (scheme == null || host == null || uri.getRawUserInfo() != null) {
                throw new IllegalArgumentException("Server URL must have a scheme and host");
            }
            scheme = scheme.toLowerCase(Locale.ROOT);
            if (!"http".equals(scheme) && !"https".equals(scheme)) {
                throw new IllegalArgumentException("Server URL must use HTTP or HTTPS");
            }
            host = host.toLowerCase(Locale.ROOT);
            int port = uri.getPort();
            if (("http".equals(scheme) && port == 80) || ("https".equals(scheme) && port == 443)) port = -1;
            if (port < -1 || port > 65535) throw new IllegalArgumentException("Invalid server port");
            return scheme + "://" + host + (port == -1 ? "" : ":" + port);
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("Invalid server URL", e);
        }
    }

    private SecretKey getOrCreateKey() throws Exception {
        KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
        keyStore.load(null);
        java.security.Key existing = keyStore.getKey(keyAlias, null);
        if (existing instanceof SecretKey) return (SecretKey) existing;
        KeyGenerator generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
        generator.init(new KeyGenParameterSpec.Builder(keyAlias,
                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .build());
        return generator.generateKey();
    }

    private static byte[] aad(String accountId, String origin) {
        return (accountId + "\n" + origin).getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }
}
