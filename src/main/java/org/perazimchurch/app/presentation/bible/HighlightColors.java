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

    /** Returns the colour with this stored name, or null when the name is unknown or empty. */
    @Nullable
    public static Colour byName(@Nullable String name) {
        if (name == null) {
            return null;
        }
        String clean = name.trim().toLowerCase(Locale.US);
        for (Colour colour : ALL) {
            if (colour.name.equals(clean)) {
                return colour;
            }
        }
        return null;
    }
}
