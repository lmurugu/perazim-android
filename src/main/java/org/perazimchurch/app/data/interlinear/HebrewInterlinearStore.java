package org.perazimchurch.app.data.interlinear;

import android.content.Context;

import androidx.annotation.Nullable;

import org.perazimchurch.app.domain.model.InterlinearWord;

import java.util.List;

/**
 * Hebrew Old Testament interlinear ({@code assets/bible/interlinear/hebrew_ot.tsv.gz}), from the
 * Open Scriptures Hebrew Bible (CC BY 4.0). Words carry transliteration, a short gloss, the Strong's
 * number and a plain-English parsing.
 */
public final class HebrewInterlinearStore {

    static final String ASSET = "bible/interlinear/hebrew_ot.tsv.gz";
    private static final InterlinearIndex INDEX = new InterlinearIndex(ASSET, true);

    private HebrewInterlinearStore() {
    }

    /** Words of one Old Testament verse, in Hebrew order, or an empty list when none is bundled. */
    public static List<InterlinearWord> getVerse(@Nullable Context context, int bookNumber, int chapter, int verse) {
        return INDEX.getVerse(context, bookNumber, chapter, verse);
    }

    /** Verses where one of the word's English glosses is this word, for the Bible search overview. */
    public static List<GlossMatch> findByGloss(@Nullable Context context, String word) {
        return INDEX.findByGloss(context, word);
    }
}
