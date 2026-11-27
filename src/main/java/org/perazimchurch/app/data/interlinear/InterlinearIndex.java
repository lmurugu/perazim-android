package org.perazimchurch.app.data.interlinear;

import android.content.Context;
import android.util.Log;

import androidx.annotation.Nullable;

import org.perazimchurch.app.data.local.seeder.BibleAssets;
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
 * Loads one bundled interlinear asset on first use and keeps it in memory.
 *
 * <p>Lines are {@code book<TAB>chapter<TAB>verse<TAB>cell cell ...}. A cell is
 * {@code original|strongs|morph|transliteration|gloss}; the three-field form {@code original|strongs|morph}
 * from older builds is still read. Spaces inside transliteration and gloss are stored as "_".
 */
final class InterlinearIndex {

    private static final String TAG = "InterlinearIndex";

    private final String asset;
    private final boolean hebrew;
    private Map<String, List<InterlinearWord>> verses;

    InterlinearIndex(String asset, boolean hebrew) {
        this.asset = asset;
        this.hebrew = hebrew;
    }

    synchronized List<InterlinearWord> getVerse(@Nullable Context context, int bookNumber, int chapter, int verse) {
        if (verses == null) {
            verses = load(context);
        }
        List<InterlinearWord> words = verses.get(key(bookNumber, chapter, verse));
        return words != null ? words : Collections.<InterlinearWord>emptyList();
    }

    /** Every verse where a word carries this Strong's number, in reading order. */
    synchronized List<VerseLocation> findOccurrences(@Nullable Context context, int strongsNumber) {
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

    /** Every verse where one of the word's English glosses is this word, in reading order. */
    synchronized List<GlossMatch> findByGloss(@Nullable Context context, String word) {
        List<GlossMatch> found = new ArrayList<>();
        if (word == null || word.trim().isEmpty()) {
            return found;
        }
        if (verses == null) {
            verses = load(context);
        }
        String wanted = word.trim().toLowerCase(java.util.Locale.ROOT);
        for (Map.Entry<String, List<InterlinearWord>> entry : verses.entrySet()) {
            String[] parts = entry.getKey().split("\\.");
            int book = Integer.parseInt(parts[0]);
            int chapter = Integer.parseInt(parts[1]);
            int verse = Integer.parseInt(parts[2]);
            for (InterlinearWord w : entry.getValue()) {
                if (glossHas(w.getGloss(), wanted)) {
                    found.add(new GlossMatch(book, chapter, verse, w));
                }
            }
        }
        Collections.sort(found, new java.util.Comparator<GlossMatch>() {
            @Override
            public int compare(GlossMatch a, GlossMatch b) {
                if (a.bookNumber != b.bookNumber) return Integer.compare(a.bookNumber, b.bookNumber);
                if (a.chapter != b.chapter) return Integer.compare(a.chapter, b.chapter);
                return Integer.compare(a.verse, b.verse);
            }
        });
        return found;
    }

    /** True when one word of the gloss (split on anything that is not a letter) equals the wanted word. */
    static boolean glossHas(String gloss, String wanted) {
        if (gloss == null || gloss.isEmpty()) {
            return false;
        }
        for (String token : gloss.toLowerCase(java.util.Locale.ROOT).split("[^\\p{L}]+")) {
            if (token.equals(wanted)) {
                return true;
            }
        }
        return false;
    }

    static String key(int bookNumber, int chapter, int verse) {
        return bookNumber + "." + chapter + "." + verse;
    }

    /** Parses the space-separated word cells of one verse. */
    List<InterlinearWord> parseWords(String cells) {
        List<InterlinearWord> words = new ArrayList<>();
        for (String cell : cells.trim().split(" ")) {
            String[] fields = cell.split("\\|", -1);
            if ((fields.length != 3 && fields.length != 5) || fields[0].isEmpty()) {
                continue;
            }
            int strongs = 0;
            try {
                strongs = fields[1].isEmpty() ? 0 : Integer.parseInt(fields[1]);
            } catch (NumberFormatException ignored) {
                strongs = 0;
            }
            String morph = fields[2];
            String translit = fields.length == 5 ? display(fields[3]) : "";
            String gloss = fields.length == 5 ? display(fields[4]) : "";
            String parsing = hebrew ? GrammarDescriber.describeHebrew(morph) : GrammarDescriber.describeGreek(morph);
            words.add(new InterlinearWord(fields[0], strongs, morph, translit, gloss, parsing));
        }
        return words;
    }

    private static String display(String stored) {
        if (stored == null || stored.equals("-")) {
            return "";
        }
        return stored.replace('_', ' ');
    }

    private Map<String, List<InterlinearWord>> load(@Nullable Context context) {
        Map<String, List<InterlinearWord>> loaded = new HashMap<>();
        if (context == null) {
            return loaded;
        }
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                BibleAssets.openText(context, asset), StandardCharsets.UTF_8))) {
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
            Log.w(TAG, asset + " could not be read", e);
        }
        return loaded;
    }
}
