package org.perazimchurch.app.data.local.session;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.UUID;

@RunWith(AndroidJUnit4.class)
public class SessionTokenStoreTest {
    private Context context;
    private String suffix;
    private String encryptedPrefsName;
    private String legacyPrefsName;
    private String keyAlias;
    private SharedPreferences encryptedPrefs;
    private SharedPreferences legacyPrefs;
    private SessionTokenStore store;

    @Before
    public void setUp() {
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        suffix = UUID.randomUUID().toString().replace("-", "");
        encryptedPrefsName = "phase_b_session_token_test_" + suffix;
        legacyPrefsName = "phase_b_legacy_session_test_" + suffix;
        keyAlias = "org.perazimchurch.app.phaseb.test." + suffix;
        encryptedPrefs = context.getSharedPreferences(encryptedPrefsName, Context.MODE_PRIVATE);
        legacyPrefs = context.getSharedPreferences(legacyPrefsName, Context.MODE_PRIVATE);
        store = new SessionTokenStore(context, encryptedPrefsName, keyAlias);
    }

    @After
    public void tearDown() {
        if (store != null) store.clear();
        if (legacyPrefs != null) legacyPrefs.edit().clear().commit();
    }

    @Test
    public void migratesLegacyTokenOnlyAfterEncryptedReadbackAndRemovesPlaintext() {
        String token = "synthetic-token-" + suffix;
        legacyPrefs.edit().putString("auth_token", token).commit();

        assertEquals(token, store.readOrMigrateLegacy(legacyPrefs, "auth_token", "synthetic-user", "https://api.example.test"));
        assertFalse("legacy plaintext should be removed after verified encrypted persistence",
                legacyPrefs.contains("auth_token"));
        assertTrue(store.hasRecord());
        assertTrue("ciphertext must not equal the legacy token",
                !token.equals(encryptedPrefs.getString("ciphertext", null)));
        assertEquals(token, store.read("synthetic-user", "https://api.example.test"));
    }

    @Test
    public void tokenIsBoundToAccountAndNormalizedServerOrigin() {
        String token = "synthetic-token-" + suffix;
        assertTrue(store.write(token, "synthetic-user", "https://api.example.test"));

        assertNull(store.read("different-user", "https://api.example.test"));
        assertNull(store.read("synthetic-user", "https://other.example.test"));
        assertEquals(token, store.read("synthetic-user", "https://api.example.test"));
        assertEquals("https://api.example.test",
                SessionTokenStore.normalizeOrigin("HTTPS://API.EXAMPLE.TEST:443/v2?x=1#frag"));
    }

    @Test
    public void tamperedCiphertextFailsClosedWithoutLegacyFallback() {
        String legacyToken = "synthetic-legacy-token-" + suffix;
        legacyPrefs.edit().putString("auth_token", legacyToken).commit();
        assertEquals(legacyToken,
                store.readOrMigrateLegacy(legacyPrefs, "auth_token", "synthetic-user", "http://127.0.0.1:3000"));

        String ciphertext = encryptedPrefs.getString("ciphertext", null);
        assertTrue(ciphertext != null && !ciphertext.isEmpty());
        encryptedPrefs.edit().putString("ciphertext",
                (ciphertext.startsWith("A") ? "B" : "A") + ciphertext.substring(1)).commit();

        // Simulate a stale plaintext copy from an older install. The failed encrypted read must
        // not make the same call eligible to fall back to it.
        legacyPrefs.edit().putString("auth_token", "stale-legacy-" + suffix).commit();
        assertNull("a damaged encrypted record must not trigger plaintext recovery",
                store.readOrMigrateLegacy(legacyPrefs, "auth_token", "synthetic-user", "http://127.0.0.1:3000"));
        assertNull("repeated reads must keep failing closed after tampering",
                store.readOrMigrateLegacy(legacyPrefs, "auth_token", "synthetic-user", "http://127.0.0.1:3000"));
    }

    @Test
    public void blockedLegacyFallbackDoesNotReturnAnOlderEncryptedToken() {
        String oldToken = "synthetic-old-token-" + suffix;
        assertTrue(store.write(oldToken, "synthetic-user", "https://api.example.test"));
        legacyPrefs.edit().putString("auth_token", "synthetic-legacy-" + suffix).commit();
        assertTrue(store.blockLegacyMigration());

        assertNull(store.readOrMigrateLegacy(legacyPrefs, "auth_token", "synthetic-user", "https://api.example.test"));
        String newToken = "synthetic-new-token-" + suffix;
        assertTrue(store.write(newToken, "synthetic-user", "https://api.example.test"));
        assertNull(store.readOrMigrateLegacy(legacyPrefs, "auth_token", "synthetic-user", "https://api.example.test"));
        assertTrue(store.unblockLegacyMigration());
        assertEquals(newToken, store.readOrMigrateLegacy(legacyPrefs, "auth_token", "synthetic-user", "https://api.example.test"));
    }
}
