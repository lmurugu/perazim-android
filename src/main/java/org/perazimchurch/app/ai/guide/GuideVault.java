package org.perazimchurch.app.ai.guide;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/** Encrypted files kept only on this device, in a folder the app owns. Nothing here is uploaded. */
public final class GuideVault {
    private final File dir;
    private final BlobCipher cipher;

    public GuideVault(File dir, BlobCipher cipher) {
        this.dir = dir;
        this.cipher = cipher;
    }

    /** Returns null when the file is missing or cannot be opened. Damaged data is treated as empty. */
    public String read(String name) {
        File file = new File(dir, name);
        if (!file.isFile()) return null;
        try (InputStream in = new java.io.FileInputStream(file)) {
            byte[] sealed = readAll(in, (int) Math.min(file.length(), 4 * 1024 * 1024));
            byte[] plain = cipher.open(sealed);
            return plain == null ? null : new String(plain, StandardCharsets.UTF_8);
        } catch (IOException e) {
            return null;
        }
    }

    public void write(String name, String text) throws IOException {
        try {
            byte[] sealed = cipher.seal(text.getBytes(StandardCharsets.UTF_8));
            if (!dir.isDirectory() && !dir.mkdirs()) throw new IOException("vault folder unavailable");
            File target = new File(dir, name);
            File temp = new File(dir, name + ".tmp");
            try (FileOutputStream out = new FileOutputStream(temp)) {
                out.write(sealed);
                out.flush();
                out.getFD().sync();
            }
            if (target.exists() && !target.delete()) throw new IOException("could not replace " + name);
            if (!temp.renameTo(target)) throw new IOException("could not store " + name);
        } catch (java.security.GeneralSecurityException e) {
            throw new IOException("encryption unavailable", e);
        }
    }

    public void deleteAll() {
        File[] files = dir.listFiles();
        if (files == null) return;
        for (File f : files) {
            if (!f.delete()) f.deleteOnExit();
        }
    }

    private static byte[] readAll(InputStream in, int limit) throws IOException {
        java.io.ByteArrayOutputStream buffer = new java.io.ByteArrayOutputStream();
        byte[] chunk = new byte[8192];
        int total = 0, n;
        while ((n = in.read(chunk)) != -1 && total < limit) {
            buffer.write(chunk, 0, Math.min(n, limit - total));
            total += n;
        }
        return buffer.toByteArray();
    }
}
