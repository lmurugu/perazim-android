package org.perazimchurch.app.data.interlinear;

import android.content.Context;
import android.util.Log;

import androidx.annotation.Nullable;

import org.perazimchurch.app.domain.model.StrongsEntry;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.GZIPInputStream;

/**
 * Offline Strong's dictionary ({@code assets/bible/strongs/strongs.tsv.gz}), loaded on first use.
 * Lines are {@code code<TAB>original<TAB>transliteration<TAB>definition}.
 */
public final class StrongsStore {

    private static final String TAG = "StrongsStore";
    static final String ASSET = "bible/strongs/strongs.tsv.gz";

    private static Map<String, StrongsEntry> entries;

    private StrongsStore() {
    }

    @Nullable
    public static synchronized StrongsEntry getEntry(@Nullable Context context, String code) {
        if (entries == null) {
            entries = load(context);
        }
        return code == null ? null : entries.get(code.toUpperCase(java.util.Locale.US));
    }

    private static Map<String, StrongsEntry> load(@Nullable Context context) {
        Map<String, StrongsEntry> loaded = new HashMap<>();
        if (context == null) {
            return loaded;
        }
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                new GZIPInputStream(context.getAssets().open(ASSET)), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                StrongsEntry entry = parseLine(line);
                if (entry != null) {
                    loaded.put(entry.getCode(), entry);
                }
            }
        } catch (IOException e) {
            Log.w(TAG, "Strong's dictionary asset could not be read", e);
        }
        return loaded;
    }

    /** Parses one TSV line, or returns null when it is malformed. */
    @Nullable
    public static StrongsEntry parseLine(String line) {
        if (line == null) {
            return null;
        }
        String[] parts = line.split("\t", 4);
        if (parts.length != 4 || parts[0].isEmpty()) {
            return null;
        }
        return new StrongsEntry(parts[0], parts[1], parts[2], parts[3]);
    }
}
