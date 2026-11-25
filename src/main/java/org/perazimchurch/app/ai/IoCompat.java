package org.perazimchurch.app.ai;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Reads a whole stream on every supported Android version. {@code InputStream.readAllBytes()} is only
 * available from API 33, and the app supports API 24 and up.
 */
public final class IoCompat {
    private IoCompat() {}

    public static byte[] readAll(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int read;
        while ((read = in.read(buffer)) != -1) {
            out.write(buffer, 0, read);
        }
        return out.toByteArray();
    }
}
