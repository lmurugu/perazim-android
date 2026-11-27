package org.perazimchurch.app.data.interlinear;

import org.perazimchurch.app.domain.model.InterlinearWord;

/** One interlinear word whose English gloss matched a search word, with the verse it sits in. */
public final class GlossMatch {

    public final int bookNumber;
    public final int chapter;
    public final int verse;
    public final InterlinearWord word;

    GlossMatch(int bookNumber, int chapter, int verse, InterlinearWord word) {
        this.bookNumber = bookNumber;
        this.chapter = chapter;
        this.verse = verse;
        this.word = word;
    }
}
