package org.perazimchurch.app.data.interlinear;

import android.content.Context;

import androidx.annotation.Nullable;

import org.perazimchurch.app.domain.model.InterlinearWord;
import org.perazimchurch.app.domain.model.VerseLocation;

import java.util.List;

/**
 * Greek New Testament interlinear ({@code assets/bible/interlinear/greek_nt.tsv.gz}).
 * Words carry transliteration, a short gloss, the Strong's number and a plain-English parsing.
 */
public final class GreekInterlinearStore {

    static final String ASSET = "bible/interlinear/greek_nt.tsv.gz";
    private static final InterlinearIndex INDEX = new InterlinearIndex(ASSET, false);

    private GreekInterlinearStore() {
    }

    /** Words of one verse, in Greek order, or an empty list when the verse is not in the interlinear. */
    public static List<InterlinearWord> getVerse(@Nullable Context context, int bookNumber, int chapter, int verse) {
        return INDEX.getVerse(context, bookNumber, chapter, verse);
    }

    /** New Testament verses where a word carries this Strong's number, for the offline concordance. */
    public static List<VerseLocation> findOccurrences(@Nullable Context context, int strongsNumber) {
        return INDEX.findOccurrences(context, strongsNumber);
    }

    /** Parses space-separated word cells. Used by tests and by the loader. */
    public static List<InterlinearWord> parseWords(String cells) {
        return INDEX.parseWords(cells);
    }

    /** Verses where one of the word's English glosses is this word, for the Bible search overview. */
    public static List<GlossMatch> findByGloss(@Nullable Context context, String word) {
        return INDEX.findByGloss(context, word);
    }
}
