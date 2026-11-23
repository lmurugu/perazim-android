package org.perazimchurch.app.ai.reference;

/** One row from the reference database, with the metadata needed to label it. Immutable. */
public final class ReferenceHit {
    public static final String KIND_DICTIONARY = "DICTIONARY";
    public static final String KIND_CROSS_REFERENCE = "CROSS_REFERENCE";

    public final String kind;
    public final String sourceId;
    public final String title;
    public final String author;
    public final String licence;
    public final String theologyTag;
    /** Headword for a dictionary entry, or the verse the cross-reference is about. */
    public final String headword;
    /** Dictionary body text, or the referenced verse for a cross-reference. */
    public final String text;
    public final int votes;

    public ReferenceHit(String kind, String sourceId, String title, String author, String licence,
                        String theologyTag, String headword, String text, int votes) {
        this.kind = kind;
        this.sourceId = sourceId;
        this.title = title;
        this.author = author;
        this.licence = licence;
        this.theologyTag = theologyTag;
        this.headword = headword;
        this.text = text;
        this.votes = votes;
    }
}
