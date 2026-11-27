package org.perazimchurch.app.ai.search;

import java.util.Collections;
import java.util.List;

/**
 * The AI overview shown above Bible search results. Every field comes from verified sources: the KJV text and the
 * bundled interlinear and Strong's lexicon. Nothing here is written by a model, so nothing is invented.
 */
public final class SearchOverview {

    /** One Hebrew or Greek root behind an English word of the query. */
    public static final class RootNote {
        public final String term;
        public final String strongsCode;
        public final String original;
        public final String transliteration;
        public final String meaning;
        /** Other verses where the same Greek or Hebrew word is used, up to three, in canonical order. */
        public final List<VerseRef> alsoIn;

        public RootNote(String term, String strongsCode, String original, String transliteration, String meaning,
                        List<VerseRef> alsoIn) {
            this.term = term;
            this.strongsCode = strongsCode;
            this.original = original;
            this.transliteration = transliteration;
            this.meaning = meaning;
            this.alsoIn = Collections.unmodifiableList(alsoIn);
        }
    }

    /** A verse the overview cites. The UI opens the reader at this verse when it is tapped. */
    public static final class VerseRef {
        public final int bookNumber;
        public final int chapter;
        public final int verse;
        public final String kjvText;

        public VerseRef(int bookNumber, int chapter, int verse, String kjvText) {
            this.bookNumber = bookNumber;
            this.chapter = chapter;
            this.verse = verse;
            this.kjvText = kjvText;
        }
    }

    public final List<String> terms;
    public final List<RootNote> roots;
    public final List<VerseRef> verses;
    /** Every verse that matched, including those not listed in {@link #verses}. */
    public final int totalVerses;

    public SearchOverview(List<String> terms, List<RootNote> roots, List<VerseRef> verses, int totalVerses) {
        this.terms = Collections.unmodifiableList(terms);
        this.roots = Collections.unmodifiableList(roots);
        this.verses = Collections.unmodifiableList(verses);
        this.totalVerses = totalVerses;
    }

    public boolean isEmpty() {
        return verses.isEmpty();
    }
}
