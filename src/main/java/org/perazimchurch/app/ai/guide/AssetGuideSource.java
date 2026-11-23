package org.perazimchurch.app.ai.guide;

import android.content.res.AssetManager;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/** Reads the locked operating files that ship inside the APK under assets/ai/guide. */
final class AssetGuideSource implements GuideDocs.Source {
    static final String ROOT = "ai/guide";
    private final AssetManager assets;

    AssetGuideSource(AssetManager assets) {
        this.assets = assets;
    }

    @Override
    public String read(String path) throws IOException {
        try (InputStream in = assets.open(ROOT + "/" + path)) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] chunk = new byte[4096];
            int n;
            while ((n = in.read(chunk)) != -1) out.write(chunk, 0, n);
            return new String(out.toByteArray(), StandardCharsets.UTF_8);
        }
    }
}
