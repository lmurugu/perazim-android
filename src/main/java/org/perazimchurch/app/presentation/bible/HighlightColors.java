package org.perazimchurch.app.presentation.bible;

import androidx.annotation.Nullable;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * The six verse highlight colours. Names are stored in the database, so they must not change.
 */
public final class HighlightColors {

    /** A highlight colour: stored name, swatch colour and tint used behind highlighted verses. */
    public static final class Colour {
        public final String name;
        public final String swatch;
        public final String tint;

        Colour(String name, String swatch, String tint) {
            this.name = name;
            this.swatch = swatch;
            this.tint = tint;
        }
    }

    public static final List<Colour> ALL = Collections.unmodifiableList(Arrays.asList(
            new Colour("yellow", "#FFE45C", "#38FFE45C"),
            new Colour("green", "#9BE28A", "#389BE28A"),
            new Colour("blue", "#8EC5FF", "#388EC5FF"),
            new Colour("pink", "#F59AC0", "#38F59AC0"),
            new Colour("orange", "#FFB56B", "#38FFB56B"),
            new Colour("purple", "#C9A7FF", "#38C9A7FF")
    ));

    private HighlightColors() {
    }

    /** Muted versions of the six colours, for a calmer highlight. */
    public static final List<Colour> MUTED = Collections.unmodifiableList(Arrays.asList(
            new Colour("muted-yellow", "#D9CB7A", "#30D9CB7A"),
            new Colour("muted-green", "#9FBF97", "#309FBF97"),
            new Colour("muted-blue", "#93AFC9", "#3093AFC9"),
            new Colour("muted-pink", "#C9A0B2", "#30C9A0B2"),
            new Colour("muted-orange", "#D2A682", "#30D2A682"),
            new Colour("muted-purple", "#A89BC4", "#30A89BC4")
    ));

    /**
     * Builds a colour from a "#RRGGBB" value chosen on the spectrum. Returns null when the text
     * is not a six-digit hex colour.
     */
    @Nullable
    public static Colour fromHex(@Nullable String hex) {
        if (hex == null || !hex.matches("#[0-9A-Fa-f]{6}")) {
            return null;
        }
        String upper = hex.toUpperCase(Locale.US);
        return new Colour(upper, upper, "#38" + upper.substring(1));
    }

    /** Returns the colour for a stored value: a named colour or a "#RRGGBB" spectrum colour. */
    @Nullable
    public static Colour byName(@Nullable String name) {
        if (name == null) {
            return null;
        }
        Colour hex = fromHex(name.trim());
        if (hex != null) {
            return hex;
        }
        String clean = name.trim().toLowerCase(Locale.US);
        for (Colour colour : MUTED) {
            if (colour.name.equals(clean)) {
                return colour;
            }
        }
        for (Colour colour : ALL) {
            if (colour.name.equals(clean)) {
                return colour;
            }
        }
        return null;
    }
}
