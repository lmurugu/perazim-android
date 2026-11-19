package org.perazimchurch.app.domain.model;

/** One word of an interlinear verse: the original text, its Strong's lemma number and its morphology code. */
public class InterlinearWord {
    private final String original;
    private final int strongsNumber;
    private final String morphology;

    public InterlinearWord(String original, int strongsNumber, String morphology) {
        this.original = original;
        this.strongsNumber = strongsNumber;
        this.morphology = morphology;
    }

    public String getOriginal() { return original; }

    /** Strong's number (for example 3056 for λογος), or 0 when none is recorded. */
    public int getStrongsNumber() { return strongsNumber; }

    /** Robinson-style morphology code such as "N-NSM", or an empty string. */
    public String getMorphology() { return morphology; }
}
