package org.perazimchurch.app.data.interlinear;

import android.content.Context;
import android.util.Log;

import androidx.annotation.Nullable;

import org.perazimchurch.app.domain.model.InterlinearWord;
import org.perazimchurch.app.domain.model.VerseLocation;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;

/**
 * Reads the bundled Greek New Testament interlinear ({@code assets/bible/interlinear/greek_nt.tsv.gz}).
 *
 * <p>The file is loaded once, on first use, and kept in memory. Lines are
 * {@code book<TAB>chapter<TAB>verse<TAB>word|strongs|morph word|strongs|morph ...}.
 */
public final class GreekInterlinearStore {

    private static final String TAG = "GreekInterlinearStore";
    static final String ASSET = "bible/interlinear/greek_nt.tsv.gz";

    private static Map<String, List<InterlinearWord>> verses;

    private GreekInterlinearStore() {
    }

    /** Words of one verse, in Greek order, or an empty list when the verse is not in the interlinear. */
    public static synchronized List<InterlinearWord> getVerse(@Nullable Context context, int bookNumber,
                                                              int chapter, int verse) {
        if (verses == null) {
            verses = load(context);
        }
        List<InterlinearWord> words = verses.get(key(bookNumber, chapter, verse));
        return words != null ? words : Collections.<InterlinearWord>emptyList();
    }

    /**
     * Every New Testament verse in the interlinear where a word carries this Strong's number,
     * in reading order. Used for the offline concordance.
     */
    public static synchronized List<VerseLocation> findOccurrences(@Nullable Context context, int strongsNumber) {
        if (verses == null) {
            verses = load(context);
        }
        List<VerseLocation> found = new ArrayList<>();
        for (Map.Entry<String, List<InterlinearWord>> entry : verses.entrySet()) {
            for (InterlinearWord word : entry.getValue()) {
                if (word.getStrongsNumber() == strongsNumber) {
                    String[] parts = entry.getKey().split("\\.");
                    found.add(new VerseLocation(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]),
                            Integer.parseInt(parts[2])));
                    break;
                }
            }
        }
        Collections.sort(found);
        return found;
    }

    static String key(int bookNumber, int chapter, int verse) {
        return bookNumber + "." + chapter + "." + verse;
    }

    private static Map<String, List<InterlinearWord>> load(@Nullable Context context) {
        Map<String, List<InterlinearWord>> loaded = new HashMap<>();
        if (context == null) {
            return loaded;
        }
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                new GZIPInputStream(context.getAssets().open(ASSET)), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split("\t", 4);
                if (parts.length != 4) {
                    continue;
                }
                try {
                    int book = Integer.parseInt(parts[0]);
                    int chapter = Integer.parseInt(parts[1]);
                    int verse = Integer.parseInt(parts[2]);
                    loaded.put(key(book, chapter, verse), parseWords(parts[3]));
                } catch (NumberFormatException ignored) {
                    // A damaged line is skipped; the rest of the interlinear still loads.
                }
            }
        } catch (IOException e) {
            Log.w(TAG, "Greek interlinear asset could not be read", e);
        }
        return loaded;
    }

    /** Parses the space-separated word cells of one verse. */
    public static List<InterlinearWord> parseWords(String cells) {
        List<InterlinearWord> words = new ArrayList<>();
        for (String cell : cells.trim().split(" ")) {
            String[] fields = cell.split("\\|", -1);
            if (fields.length != 3 || fields[0].isEmpty()) {
                continue;
            }
            int strongs = 0;
            try {
                strongs = fields[1].isEmpty() ? 0 : Integer.parseInt(fields[1]);
            } catch (NumberFormatException ignored) {
                strongs = 0;
            }
            words.add(new InterlinearWord(fields[0], strongs, fields[2]));
        }
        return words;
    }
}
