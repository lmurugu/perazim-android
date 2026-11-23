package org.perazimchurch.app.presentation.ui.games.vine;

import android.content.Context;

import org.perazimchurch.app.domain.games.vine.VineWordsLevel;
import org.perazimchurch.app.domain.games.vine.VineWordsLevels;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

/** Reads {@code assets/games/vine_words.json} once and keeps the parsed levels. */
public final class VineWordsContent {

    private static final String ASSET = "games/vine_words.json";
    private static Context appContext;
    private static List<VineWordsLevel> cache;

    private VineWordsContent() {}

    /** Called once from PerazimApplication.onCreate. */
    public static void attach(Context context) {
        appContext = context.getApplicationContext();
    }

    /** The parsed level set. Throws IllegalStateException if called before {@link #attach}. */
    public static synchronized List<VineWordsLevel> levels() {
        if (cache != null) return cache;
        if (appContext == null) throw new IllegalStateException("VineWordsContent.attach was not called");
        try (InputStream in = appContext.getAssets().open(ASSET)) {
            byte[] bytes = new byte[in.available()];
            int read = 0;
            while (read < bytes.length) {
                int n = in.read(bytes, read, bytes.length - read);
                if (n < 0) break;
                read += n;
            }
            cache = VineWordsLevels.parse(new String(bytes, 0, read, StandardCharsets.UTF_8));
            return cache;
        } catch (IOException e) {
            throw new IllegalStateException("Could not read " + ASSET, e);
        }
    }
}
