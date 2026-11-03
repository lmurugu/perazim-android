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
    // MANDATORY QUIET DARK COLOR SYSTEM (§1 Contract)
    // =========================================================================

    // Purple Family
    /** App Background Canvas (#2A0F33) */
    public static final int COLOR_BG_DEEP       = Color.parseColor("#2A0F33");
    /** Cards and Content Containers (#3D1749) */
    public static final int COLOR_SURFACE       = Color.parseColor("#3D1749");
    /** Elevated Surfaces, Text Inputs, Search Bars (#4F1E5C) */
    public static final int COLOR_SURFACE_2     = Color.parseColor("#4F1E5C");
    /** Inactive Nav, Secondary Icons, Outline Borders (#8B5A9A) */
    public static final int COLOR_PURPLE_MUTED  = Color.parseColor("#8B5A9A");
    /** Active Nav, Links, Selected Chips (#681A7D) */
    public static final int COLOR_PURPLE_BRAND  = Color.parseColor("#681A7D");
    /** Soft Labels and Supporting Text (#A67BB3) */
    public static final int COLOR_PURPLE_SOFT   = Color.parseColor("#A67BB3");
    /** Selection Wash with 20% alpha */
    public static final int COLOR_PURPLE_GHOST  = Color.parseColor("#33681A7D");

    // Orange Family
    /** ONLY the Single Primary CTA on a screen (#E17D2F) */
    public static final int COLOR_ORANGE_BRAND  = Color.parseColor("#E17D2F");
    /** Rare Secondary Accent (#C4894F) */
    public static final int COLOR_ORANGE_MUTED  = Color.parseColor("#C4894F");
    /** Pressed / Focus on Orange CTA (#E8A86A) */
    public static final int COLOR_ORANGE_SOFT   = Color.parseColor("#E8A86A");
    /** Orange Selection Wash with 24% alpha */
    public static final int COLOR_ORANGE_GHOST  = Color.parseColor("#3DE17D2F");

    // Neutrals
    /** Primary High-Contrast Text on Dark Surfaces (#F5F0F7) */
    public static final int COLOR_TEXT_PRIMARY   = Color.parseColor("#F5F0F7");
    /** Secondary Subtitle / Metadata Text (#B9A8C0) */
    public static final int COLOR_TEXT_SECONDARY = Color.parseColor("#B9A8C0");
    /** Disabled Text and Placeholders (#6E5C78) */
    public static final int COLOR_TEXT_DISABLED  = Color.parseColor("#6E5C78");
    /** Card Borders and Structural Dividers (#4A3554) */
    public static final int COLOR_STROKE         = Color.parseColor("#4A3554");

    // =========================================================================
    // LEGACY ALIASES (Backwards Compatibility)
    // =========================================================================

    public static final int COLOR_PRIMARY_PURPLE = COLOR_PURPLE_BRAND;
    public static final int COLOR_ACCENT_ORANGE  = COLOR_ORANGE_BRAND;
    public static final int COLOR_WARM_CREAM     = Color.parseColor("#FAF6EE");
    public static final int COLOR_PURPLE_DEEP    = COLOR_BG_DEEP;
    public static final int COLOR_OBSIDIAN       = Color.parseColor("#140319");
    public static final int COLOR_WHITE          = Color.parseColor("#FFFFFF");
    public static final int COLOR_TEXT_DARK      = Color.parseColor("#1A1A1A");
    public static final int COLOR_TEXT_MUTED     = COLOR_TEXT_SECONDARY;
    public static final int COLOR_PURPLE_SHADOW  = Color.parseColor("#450E53");
    public static final int COLOR_PURPLE_TINT    = COLOR_PURPLE_GHOST;
    public static final int COLOR_BORDER_GREY    = COLOR_STROKE;
    public static final int COLOR_BG_NEUTRAL     = COLOR_BG_DEEP;
    public static final int COLOR_ORANGE_DARK    = Color.parseColor("#78350A");
    public static final int COLOR_ORANGE_SHADOW  = Color.parseColor("#98450B");
    public static final int COLOR_ORANGE_BORDER  = COLOR_ORANGE_MUTED;
    public static final int COLOR_ORANGE_TINT    = COLOR_ORANGE_GHOST;
    public static final int COLOR_SUCCESS        = Color.parseColor("#10B981");
    public static final int COLOR_ERROR          = Color.parseColor("#EF4444");

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
