package org.perazimchurch.app.ai.guide;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

/** Test helpers: the real bundled guide files, a temporary vault folder and an AES-GCM test cipher. */
final class GuideTestSupport {
    private GuideTestSupport() {}

    static File guideFolder() {
        File f = new File("src/main/assets/ai/guide");
        if (!f.isDirectory()) throw new IllegalStateException("run tests from the project root");
        return f;
    }

    static GuideDocs.Source bundled() {
        return new GuideDocs.Source() {
            @Override public String read(String path) throws IOException {
                return new String(Files.readAllBytes(new File(guideFolder(), path).toPath()), StandardCharsets.UTF_8);
            }
        };
    }

    static GuideDocs docs() throws IOException {
        return GuideDocs.load(bundled());
    }

    static File tempDir() throws IOException {
        File dir = Files.createTempDirectory("guide-test").toFile();
        dir.deleteOnExit();
        return dir;
    }

    private static SecretKey sharedKey;

    /** One key per test JVM, so a vault can be reopened the way the device reopens it. */
    static synchronized BlobCipher testCipher() throws GeneralSecurityException {
        if (sharedKey == null) sharedKey = KeyGenerator.getInstance("AES").generateKey();
        final SecretKey key = sharedKey;
        return new BlobCipher() {
            private final SecureRandom random = new SecureRandom();
            @Override public byte[] seal(byte[] plain) throws GeneralSecurityException {
                byte[] iv = new byte[12];
                random.nextBytes(iv);
                Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
                c.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(128, iv));
                byte[] body = c.doFinal(plain);
                byte[] out = new byte[iv.length + body.length];
                System.arraycopy(iv, 0, out, 0, iv.length);
                System.arraycopy(body, 0, out, iv.length, body.length);
                return out;
            }
            @Override public byte[] open(byte[] sealed) {
                try {
                    byte[] iv = java.util.Arrays.copyOfRange(sealed, 0, 12);
                    byte[] body = java.util.Arrays.copyOfRange(sealed, 12, sealed.length);
                    Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
                    c.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(128, iv));
                    return c.doFinal(body);
                } catch (Exception e) {
                    return null;
                }
            }
        };
    }
}
