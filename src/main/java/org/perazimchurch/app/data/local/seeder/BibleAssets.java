package org.perazimchurch.app.data.local.seeder;

import android.content.Context;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;

/**
 * Opens bundled Bible text assets. The build may store a ".gz" asset already decompressed, so
 * this checks the gzip signature and reads either form.
 */
public final class BibleAssets {

    private BibleAssets() {
    }

    public static InputStream openText(Context context, String assetPath) throws IOException {
        InputStream opened;
        try {
            opened = context.getAssets().open(assetPath);
        } catch (IOException missing) {
            // The build may store the asset without its ".gz" suffix; read that copy instead.
            if (!assetPath.endsWith(".gz")) {
                throw missing;
            }
            opened = context.getAssets().open(assetPath.substring(0, assetPath.length() - 3));
        }
        InputStream raw = new BufferedInputStream(opened);
        raw.mark(2);
        int first = raw.read();
        int second = raw.read();
        raw.reset();
        if (first == 0x1f && second == 0x8b) {
            return new GZIPInputStream(raw);
        }
        return raw;
    }
}
