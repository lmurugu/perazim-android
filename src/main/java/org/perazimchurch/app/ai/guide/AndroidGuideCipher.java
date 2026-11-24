package org.perazimchurch.app.ai.guide;

import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;

import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

/**
 * AES-GCM with a non-exportable key in the Android Keystore, the same pattern as the session token store.
 * Sealed layout: one IV-length byte, the IV, then the ciphertext.
 */
public final class AndroidGuideCipher implements BlobCipher {
    static final String KEY_ALIAS = "org.perazimchurch.app.guide-memory.v1";
    private static final int GCM_TAG_BITS = 128;

    @Override
    public byte[] seal(byte[] plain) throws GeneralSecurityException {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, key());
        byte[] iv = cipher.getIV();
        byte[] body = cipher.doFinal(plain);
        byte[] out = new byte[1 + iv.length + body.length];
        out[0] = (byte) iv.length;
        System.arraycopy(iv, 0, out, 1, iv.length);
        System.arraycopy(body, 0, out, 1 + iv.length, body.length);
        return out;
    }

    @Override
    public byte[] open(byte[] sealed) {
        try {
            if (sealed == null || sealed.length < 2) return null;
            int ivLength = sealed[0];
            if (ivLength <= 0 || sealed.length < 1 + ivLength) return null;
            byte[] iv = Arrays.copyOfRange(sealed, 1, 1 + ivLength);
            byte[] body = Arrays.copyOfRange(sealed, 1 + ivLength, sealed.length);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key(), new GCMParameterSpec(GCM_TAG_BITS, iv));
            return cipher.doFinal(body);
        } catch (Exception e) {
            return null;
        }
    }

    private static SecretKey key() throws GeneralSecurityException {
        try {
            KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
            keyStore.load(null);
            if (!keyStore.containsAlias(KEY_ALIAS)) {
                KeyGenerator generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
                generator.init(new KeyGenParameterSpec.Builder(KEY_ALIAS,
                        KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                        .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                        .setKeySize(256)
                        .build());
                generator.generateKey();
            }
            KeyStore.SecretKeyEntry entry = (KeyStore.SecretKeyEntry) keyStore.getEntry(KEY_ALIAS, null);
            if (entry == null) throw new GeneralSecurityException("guide memory key unavailable");
            return entry.getSecretKey();
        } catch (java.io.IOException | java.security.cert.CertificateException e) {
            throw new GeneralSecurityException("keystore unavailable", e);
        }
    }
}
