package org.perazimchurch.app.ui.theme;

import android.graphics.Color;

/**
 * Design System Contract for Perazim Mission Church.
 * Single source of truth for canonical brand colors, typography, spacing, and elevation tokens.
 * Compliant with Phase 0.3 of the Perazim Android Master Build Guidebook V2.
 */
public final class PerazimTheme {

    private PerazimTheme() {
        // Prevent instantiation
    }

    // =========================================================================
    // CANONICAL BRAND COLORS (Phase 0.3 Contract)
    // =========================================================================

    /** Canonical Primary Royal Purple */
    public static final int COLOR_PRIMARY_PURPLE = Color.parseColor("#681A7D");

    /** Canonical Accent Warm Amber / Orange */
    public static final int COLOR_ACCENT_ORANGE = Color.parseColor("#E17D2F");

    /** Canonical Warm Cream */
    public static final int COLOR_WARM_CREAM = Color.parseColor("#FAF6EE");

    /** Canonical Purple Deep */
    public static final int COLOR_PURPLE_DEEP = Color.parseColor("#2D0938");

    /** Canonical Obsidian Dark */
    public static final int COLOR_OBSIDIAN = Color.parseColor("#140319");

    /** Canonical Pure White */
    public static final int COLOR_WHITE = Color.parseColor("#FFFFFF");

    /** Canonical Dark Text (High Contrast) */
    public static final int COLOR_TEXT_DARK = Color.parseColor("#1A1A1A");

    /** Canonical Muted Text (Secondary Content) */
    public static final int COLOR_TEXT_MUTED = Color.parseColor("#666666");

    // =========================================================================
    // SUPPORTING BRAND & SYSTEM COLORS
    // =========================================================================

    /** 3D Bevel Shadow for Purple Surfaces */
    public static final int COLOR_PURPLE_SHADOW = Color.parseColor("#450E53");

    /** Soft Purple for Accents and Secondary Highlights */
    public static final int COLOR_PURPLE_SOFT = Color.parseColor("#C896D8");

    /** Pastel Purple Tint for Badges and Pill Backgrounds */
    public static final int COLOR_PURPLE_TINT = Color.parseColor("#F5ECF7");

    /** Card Border & Divider Grey */
    public static final int COLOR_BORDER_GREY = Color.parseColor("#E7D5EC");

    /** Neutral Canvas Background */
    public static final int COLOR_BG_NEUTRAL = Color.parseColor("#FAF7FB");

    /** Dark Amber for Contrast Text */
    public static final int COLOR_ORANGE_DARK = Color.parseColor("#78350A");

    /** 3D Bevel Shadow for Orange CTAs */
    public static final int COLOR_ORANGE_SHADOW = Color.parseColor("#98450B");

    /** Amber Pill Border */
    public static final int COLOR_ORANGE_BORDER = Color.parseColor("#F9DFCC");

    /** Amber Pastel Tint */
    public static final int COLOR_ORANGE_TINT = Color.parseColor("#FDF5EF");

    /** Success Emerald Green */
    public static final int COLOR_SUCCESS = Color.parseColor("#10B981");

    /** Error / Warning Crimson */
    public static final int COLOR_ERROR = Color.parseColor("#EF4444");

    // =========================================================================
    // SPACING GUIDELINES (dp)
    // =========================================================================

    public static final int SPACING_2XS = 2;
    public static final int SPACING_XS = 4;
    public static final int SPACING_SM = 8;
    public static final int SPACING_MD = 12;
    public static final int SPACING_LG = 16;
    public static final int SPACING_XL = 20;
    public static final int SPACING_2XL = 24;
    public static final int SPACING_3XL = 32;

    // =========================================================================
    // TYPOGRAPHY GUIDELINES (sp)
    // =========================================================================

    public static final float TEXT_SIZE_MICRO = 9f;
    public static final float TEXT_SIZE_CAPTION = 11f;
    public static final float TEXT_SIZE_BODY_SM = 12f;
    public static final float TEXT_SIZE_BODY = 14f;
    public static final float TEXT_SIZE_SUBTITLE = 16f;
    public static final float TEXT_SIZE_TITLE = 18f;
    public static final float TEXT_SIZE_HEADLINE = 22f;
    public static final float TEXT_SIZE_HERO = 26f;

    // =========================================================================
    // CORNER RADII (dp)
    // =========================================================================

    public static final int RADIUS_SM = 6;
    public static final int RADIUS_MD = 10;
    public static final int RADIUS_LG = 14;
    public static final int RADIUS_XL = 20;
    public static final int RADIUS_PILL = 999;
}
