package org.perazimchurch.app.domain.model;

/** One Strong's dictionary entry, for example G3056 or H430. */
public class StrongsEntry {
    private final String code;
    private final String original;
    private final String transliteration;
    private final String definition;

    public StrongsEntry(String code, String original, String transliteration, String definition) {
        this.code = code;
        this.original = original;
        this.transliteration = transliteration;
        this.definition = definition;
    }

    /** Code such as "G3056" (Greek) or "H430" (Hebrew). */
    public String getCode() { return code; }

    /** Original-script word; empty for Greek entries, whose letters come from the interlinear. */
    public String getOriginal() { return original; }

    public String getTransliteration() { return transliteration; }

    public String getDefinition() { return definition; }
}
