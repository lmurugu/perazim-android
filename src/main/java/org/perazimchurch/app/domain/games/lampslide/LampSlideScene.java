package org.perazimchurch.app.domain.games.lampslide;

/** One painting with the verse shown when it is rebuilt. Pure data. */
public final class LampSlideScene {

    private final String id;
    private final String image;
    private final String reference;
    private final String verse;
    private final String prayer;

    /**
     * @param id        stable key, for example "psalm_23_1"
     * @param image     file name of the painting in assets/games/lamp, for example "lamp_psalm_23_1"
     * @param reference Bible reference, for example "Psalm 23:1"
     * @param verse     the KJV text of that reference
     * @param prayer    one short line from the prayer card, may be empty
     */
    public LampSlideScene(String id, String image, String reference, String verse, String prayer) {
        this.id = require(id, "id");
        this.image = require(image, "image");
        this.reference = require(reference, "reference");
        this.verse = require(verse, "verse");
        this.prayer = prayer == null ? "" : prayer;
    }

    public String id() { return id; }
    public String image() { return image; }
    public String reference() { return reference; }
    public String verse() { return verse; }
    public String prayer() { return prayer; }

    private static String require(String value, String field) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value;
    }
}
