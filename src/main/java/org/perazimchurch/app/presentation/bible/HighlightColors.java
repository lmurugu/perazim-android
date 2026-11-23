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

    /** Bold, saturated colours: a strong red, orange, yellow and so on, for highlights that must stand out. */
    public static final List<Colour> BOLD = Collections.unmodifiableList(Arrays.asList(
            new Colour("red", "#E53935", "#66E53935"),
            new Colour("bold-orange", "#FB8C00", "#66FB8C00"),
            new Colour("bold-yellow", "#FDD835", "#66FDD835"),
            new Colour("bold-green", "#43A047", "#6643A047"),
            new Colour("teal", "#00897B", "#6600897B"),
            new Colour("bold-blue", "#1E88E5", "#661E88E5"),
            new Colour("indigo", "#3949AB", "#663949AB"),
            new Colour("bold-purple", "#8E24AA", "#668E24AA"),
            new Colour("magenta", "#D81B60", "#66D81B60")
    ));

    private static final String[] HUE_BASE = {"#E53935", "#FB8C00", "#FDD835", "#43A047", "#00897B",
            "#1E88E5", "#3949AB", "#8E24AA", "#D81B60"};
    private static final String[] HUE_NAME = {"red", "orange", "yellow", "green", "teal", "blue", "indigo", "purple", "pink"};

    /**
     * Three shades per hue, from washed out (light) to darker (dark): rows are light, mid and dark,
     * each row has the nine hues in the same order as HUE_NAME.
     */
    public static final List<List<Colour>> GRADIENT_ROWS = buildGradient();

    private static List<List<Colour>> buildGradient() {
        String[] shade = {"light", "mid", "dark"};
        List<List<Colour>> rows = new java.util.ArrayList<>();
        for (int k = 0; k < shade.length; k++) {
            List<Colour> row = new java.util.ArrayList<>();
            for (int h = 0; h < HUE_BASE.length; h++) {
                String hex = k == 0 ? mix(HUE_BASE[h], 0xFF, 0.55f)
                        : k == 1 ? HUE_BASE[h]
                        : mix(HUE_BASE[h], 0x00, 0.35f);
                row.add(new Colour(HUE_NAME[h] + "-" + shade[k], hex, "#66" + hex.substring(1)));
            }
            rows.add(Collections.unmodifiableList(row));
        }
        return Collections.unmodifiableList(rows);
    }

    /** Moves a colour toward white (channel 0xFF) or black (0x00) by the given fraction. */
    private static String mix(String hex, int target, float amount) {
        int r = Integer.parseInt(hex.substring(1, 3), 16);
        int g = Integer.parseInt(hex.substring(3, 5), 16);
        int b = Integer.parseInt(hex.substring(5, 7), 16);
        r = Math.round(r + (target - r) * amount);
        g = Math.round(g + (target - g) * amount);
        b = Math.round(b + (target - b) * amount);
        return String.format(Locale.US, "#%02X%02X%02X", r, g, b);
    }

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
        for (Colour colour : BOLD) {
            if (colour.name.equals(clean)) {
                return colour;
            }
        }
        for (List<Colour> row : GRADIENT_ROWS) {
            for (Colour colour : row) {
                if (colour.name.equals(clean)) {
                    return colour;
                }
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
