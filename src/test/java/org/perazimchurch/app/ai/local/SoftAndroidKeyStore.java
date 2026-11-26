package org.perazimchurch.app.ai.local;

import android.security.keystore.KeyGenParameterSpec;

import java.io.InputStream;
import java.io.OutputStream;
import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.KeyStoreSpi;
import java.security.Provider;
import java.security.SecureRandom;
import java.security.Security;
import java.security.cert.Certificate;
import java.security.spec.AlgorithmParameterSpec;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.KeyGenerator;
import javax.crypto.spec.SecretKeySpec;

/**
 * Test-only software stand-in for the Android Keystore. The app uses it only for local,
 * non-sensitive keys (an install-state MAC key and cache encryption). The package signature is
 * still verified against the bundled public trust key; this provider does not touch that check.
 * Registered by test code only. Production never uses it.
 */
final class SoftAndroidKeyStore {

    private static final Map<String, Key> KEYS = new HashMap<>();

    private SoftAndroidKeyStore() {}

    static synchronized void install() {
        if (Security.getProvider("AndroidKeyStore") == null) {
            Security.insertProviderAt(new Provider("AndroidKeyStore", 1.0, "software test provider") {
                {
                    put("KeyStore.AndroidKeyStore", SoftKeyStoreSpi.class.getName());
                    put("KeyGenerator.HmacSHA256", SoftKeyGeneratorSpi.class.getName());
                    put("KeyGenerator.AES", SoftKeyGeneratorSpi.class.getName());
                }
            }, 1);
        }
    }

    static synchronized void uninstall() {
        Security.removeProvider("AndroidKeyStore");
    }

    static synchronized Key get(String alias) { return KEYS.get(alias); }

    static synchronized void put(String alias, Key key) { KEYS.put(alias, key); }

    public static final class SoftKeyStoreSpi extends KeyStoreSpi {
        @Override public Key engineGetKey(String alias, char[] password) { return get(alias); }
        @Override public Certificate[] engineGetCertificateChain(String alias) { return null; }
        @Override public Certificate engineGetCertificate(String alias) { return null; }
        @Override public Date engineGetCreationDate(String alias) { return new Date(); }
        @Override public void engineSetKeyEntry(String alias, Key key, char[] password, Certificate[] chain) { put(alias, key); }
        @Override public void engineSetKeyEntry(String alias, byte[] key, Certificate[] chain) throws KeyStoreException {
            throw new KeyStoreException("not supported by test provider");
        }
        @Override public void engineSetCertificateEntry(String alias, Certificate cert) { }
        @Override public void engineDeleteEntry(String alias) { synchronized (SoftAndroidKeyStore.class) { KEYS.remove(alias); } }
        @Override public synchronized Enumeration<String> engineAliases() {
            synchronized (SoftAndroidKeyStore.class) { return Collections.enumeration(new java.util.ArrayList<>(KEYS.keySet())); }
        }
        @Override public boolean engineContainsAlias(String alias) { return get(alias) != null; }
        @Override public int engineSize() { synchronized (SoftAndroidKeyStore.class) { return KEYS.size(); } }
        @Override public boolean engineIsKeyEntry(String alias) { return get(alias) != null; }
        @Override public boolean engineIsCertificateEntry(String alias) { return false; }
        @Override public String engineGetCertificateAlias(Certificate cert) { return null; }
        @Override public void engineStore(OutputStream stream, char[] password) { }
        @Override public void engineLoad(InputStream stream, char[] password) { }
    }

    public static final class SoftKeyGeneratorSpi extends javax.crypto.KeyGeneratorSpi {
        private String alias;
        private int bits = 256;
        private final SecureRandom random = new SecureRandom();

        @Override protected void engineInit(SecureRandom random) { }
        @Override protected void engineInit(int keysize, SecureRandom random) { this.bits = keysize; }
        @Override protected void engineInit(AlgorithmParameterSpec params, SecureRandom random) {
            if (params instanceof KeyGenParameterSpec) {
                alias = ((KeyGenParameterSpec) params).getKeystoreAlias();
            }
        }
        @Override protected javax.crypto.SecretKey engineGenerateKey() {
            byte[] raw = new byte[bits / 8];
            random.nextBytes(raw);
            SecretKeySpec key = new SecretKeySpec(raw, "HmacSHA256");
            if (alias != null) put(alias, key);
            return key;
        }
    }
}
