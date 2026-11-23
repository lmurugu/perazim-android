package org.perazimchurch.app.domain.model;

import org.perazimchurch.app.data.interlinear.GrammarDescriber;

/**
 * One word of an interlinear verse: the original text, its transliteration, a short English gloss,
 * its Strong's lemma number, the raw morphology code and a plain-English parsing.
 */
public class InterlinearWord {
    private final String original;
    private final int strongsNumber;
    private final String morphology;
    private final String transliteration;
    private final String gloss;
    private final String parsing;

    public InterlinearWord(String original, int strongsNumber, String morphology) {
        this(original, strongsNumber, morphology, "", "", GrammarDescriber.describeGreek(morphology));
    }

    public InterlinearWord(String original, int strongsNumber, String morphology,
                           String transliteration, String gloss, String parsing) {
        this.original = original;
        this.strongsNumber = strongsNumber;
        this.morphology = morphology;
        this.transliteration = transliteration;
        this.gloss = gloss;
        this.parsing = parsing;
    }

    public String getOriginal() { return original; }

    /** Strong's number (for example 3056 for λογος), or 0 when none is recorded. */
    public int getStrongsNumber() { return strongsNumber; }

    /** Source morphology code, such as "N-NSM" (Greek) or "HVqp3ms" (Hebrew), or an empty string. */
    public String getMorphology() { return morphology; }

    /** Transliteration of the word, for example "logos" or "bere'siyt"; empty when not bundled. */
    public String getTransliteration() { return transliteration; }

    /** Short English meaning from the Strong's entry, for example "word"; empty when unknown. */
    public String getGloss() { return gloss; }

    /** Plain-English parsing, for example "Noun · masculine singular · nominative". */
    public String getParsing() { return parsing; }
}
