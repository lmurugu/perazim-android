package org.perazimchurch.app.ui.theme;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;

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

    // =========================================================================
    // GRADIENT DRAWABLE FACTORIES (Darkened & Muted Palettes)
    // =========================================================================

    /**
     * Vertical darkened background canvas gradient (#2E1038 -> #190720).
     */
    public static GradientDrawable createBackgroundGradient() {
        return new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{Color.parseColor("#2E1038"), Color.parseColor("#190720")}
        );
    }

    /**
     * Top App Bar darkened header gradient (#32123D -> #220A2B) with bottom hairline stroke.
     */
    public static GradientDrawable createTopBarGradient(int strokePx) {
        GradientDrawable gd = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{Color.parseColor("#32123D"), Color.parseColor("#220A2B")}
        );
        if (strokePx > 0) {
            gd.setStroke(strokePx, COLOR_STROKE);
        }
        return gd;
    }

    /**
     * Bottom Navigation Bar darkened gradient (#260C2E -> #18061F) with top hairline stroke.
     */
    public static GradientDrawable createBottomNavGradient(int strokePx) {
        GradientDrawable gd = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{Color.parseColor("#260C2E"), Color.parseColor("#18061F")}
        );
        if (strokePx > 0) {
            gd.setStroke(strokePx, COLOR_STROKE);
        }
        return gd;
    }

    /**
     * Standard card surface gradient (#421950 -> #31113E) with rounded corners and hairline stroke.
     */
    public static GradientDrawable createCardGradient(int radiusPx, int strokePx) {
        GradientDrawable gd = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[]{Color.parseColor("#421950"), Color.parseColor("#31113E")}
        );
        gd.setCornerRadius(radiusPx);
        if (strokePx > 0) {
            gd.setStroke(strokePx, COLOR_STROKE);
        }
        return gd;
    }

    /**
     * Elevated surface 2 gradient (#541F63 -> #3E164A) for search bars, input cards, and chip containers.
     */
    public static GradientDrawable createSurface2Gradient(int radiusPx, int strokePx) {
        GradientDrawable gd = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[]{Color.parseColor("#541F63"), Color.parseColor("#3E164A")}
        );
        gd.setCornerRadius(radiusPx);
        if (strokePx > 0) {
            gd.setStroke(strokePx, COLOR_STROKE);
        }
        return gd;
    }

    /**
     * Canonical single primary CTA warm muted/darkened orange gradient (#E88636 -> #C06119).
     */
    public static GradientDrawable createPrimaryOrangeGradient(int radiusPx) {
        GradientDrawable gd = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[]{Color.parseColor("#E88636"), Color.parseColor("#C06119")}
        );
        gd.setCornerRadius(radiusPx);
        return gd;
    }

    /**
     * Hero card darkened gradient (#4A1B59 -> #2F0D39) for Daily Verse, Livestream hero, and Member identity.
     */
    public static GradientDrawable createHeroGradient(int radiusPx, int strokePx) {
        GradientDrawable gd = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[]{Color.parseColor("#4A1B59"), Color.parseColor("#2F0D39")}
        );
        gd.setCornerRadius(radiusPx);
        if (strokePx > 0) {
            gd.setStroke(strokePx, Color.parseColor("#583B65"));
        }
        return gd;
    }

    /**
     * Darkened dialog background gradient (#3D154A -> #24092C).
     */
    public static GradientDrawable createDialogGradient(int radiusPx, int strokePx) {
        GradientDrawable gd = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{Color.parseColor("#3D154A"), Color.parseColor("#24092C")}
        );
        gd.setCornerRadius(radiusPx);
        if (strokePx > 0) {
            gd.setStroke(strokePx, COLOR_STROKE);
        }
        return gd;
    }

    /**
     * Active filter / nav pill gradient in brand royal purple (#751E8D -> #521264).
     */
    public static GradientDrawable createActivePillGradient(int radiusPx) {
        GradientDrawable gd = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[]{Color.parseColor("#751E8D"), Color.parseColor("#521264")}
        );
        gd.setCornerRadius(radiusPx);
        return gd;
    }

    /**
     * Subtle muted outline pill / button gradient (#361241 -> #280C31).
     */
    public static GradientDrawable createMutedPillGradient(int radiusPx, int strokeColor, int strokePx) {
        GradientDrawable gd = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{Color.parseColor("#361241"), Color.parseColor("#280C31")}
        );
        gd.setCornerRadius(radiusPx);
        if (strokePx > 0) {
            gd.setStroke(strokePx, strokeColor);
        }
        return gd;
    }
}

