package org.perazimchurch.app.ui.theme;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;

/**
 * Frosted-glass look: surfaces are translucent white over the deep purple canvas with light borders.
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
    public static int COLOR_BG_DEEP       = Color.parseColor("#2A0F33");
    /** Opaque fallbacks for dialogs, popups and full-screen containers where glass would show through */
    public static int COLOR_SURFACE_SOLID   = Color.parseColor("#3D1749");
    public static int COLOR_SURFACE_2_SOLID = Color.parseColor("#3D1749");
    /** Cards and Content Containers (#3D1749) */
    public static int COLOR_SURFACE       = Color.parseColor("#26FFFFFF");
    /** Elevated Surfaces, Text Inputs, Search Bars (#4F1E5C) */
    public static int COLOR_SURFACE_2     = Color.parseColor("#33FFFFFF");
    /** Inactive Nav, Secondary Icons, Outline Borders (#8B5A9A) */
    public static int COLOR_PURPLE_MUTED  = Color.parseColor("#8B5A9A");
    /** Active Nav, Links, Selected Chips (#681A7D) */
    public static int COLOR_PURPLE_BRAND  = Color.parseColor("#681A7D");
    /** Soft Labels and Supporting Text (#A67BB3) */
    public static int COLOR_PURPLE_SOFT   = Color.parseColor("#A67BB3");
    /** Selection Wash with 20% alpha */
    public static int COLOR_PURPLE_GHOST  = Color.parseColor("#33681A7D");

    // Orange Family
    /** ONLY the Single Primary CTA on a screen (#E17D2F) */
    public static int COLOR_ORANGE_BRAND  = Color.parseColor("#E17D2F");
    /** Rare Secondary Accent (#C4894F) */
    public static int COLOR_ORANGE_MUTED  = Color.parseColor("#E88636");
    /** Pressed / Focus on Orange CTA (#E8A86A) */
    public static int COLOR_ORANGE_SOFT   = Color.parseColor("#F8C894");
    /** Orange Selection Wash with 24% alpha */
    public static int COLOR_ORANGE_GHOST  = Color.parseColor("#3DE17D2F");

    // Neutrals
    /** Primary High-Contrast Text on Dark Surfaces (#F5F0F7) */
    public static int COLOR_TEXT_PRIMARY   = Color.parseColor("#F5F0F7");
    /** Secondary Subtitle / Metadata Text (#B9A8C0) */
    public static int COLOR_TEXT_SECONDARY = Color.parseColor("#B9A8C0");
    /** Disabled Text and Placeholders (#6E5C78) */
    public static int COLOR_TEXT_DISABLED  = Color.parseColor("#6E5C78");
    /** Card Borders and Structural Dividers (#4A3554) */
    public static int COLOR_STROKE         = Color.parseColor("#33FFFFFF");

    // =========================================================================
    // LEGACY ALIASES (Backwards Compatibility)
    // =========================================================================

    public static int COLOR_PRIMARY_PURPLE = COLOR_PURPLE_BRAND;
    public static int COLOR_ACCENT_ORANGE  = COLOR_ORANGE_BRAND;
    public static int COLOR_WARM_CREAM     = Color.parseColor("#F5F0F7");
    public static int COLOR_PURPLE_DEEP    = COLOR_BG_DEEP;
    public static int COLOR_OBSIDIAN       = Color.parseColor("#2A0F33");
    public static int COLOR_WHITE          = Color.parseColor("#FFFFFF");
    public static int COLOR_TEXT_DARK      = Color.parseColor("#2A0F33");
    public static int COLOR_TEXT_MUTED     = COLOR_TEXT_SECONDARY;
    public static int COLOR_PURPLE_SHADOW  = Color.parseColor("#2A0F33");
    public static int COLOR_PURPLE_TINT    = COLOR_PURPLE_GHOST;
    public static int COLOR_BORDER_GREY    = COLOR_STROKE;
    public static int COLOR_BG_NEUTRAL     = COLOR_BG_DEEP;
    public static int COLOR_ORANGE_DARK    = Color.parseColor("#C06119");
    public static int COLOR_ORANGE_SHADOW  = Color.parseColor("#C06119");
    public static int COLOR_ORANGE_BORDER  = COLOR_ORANGE_MUTED;
    public static int COLOR_ORANGE_TINT    = COLOR_ORANGE_GHOST;
    public static int COLOR_SUCCESS        = Color.parseColor("#8A2BA3"); // no Bible green: nearest Bible purple
    
    // =========================================================================
    // LIGHT / DARK MODE
    // =========================================================================

    private static final String PREFS = "perazim_theme";
    private static final String KEY_LIGHT = "light_mode"; // older saved choice: true = light, false = dark
    private static final String KEY_CHOICE = "theme_choice";

    /** The three appearance choices. New installs start on SYSTEM, which follows the phone. */
    public static final int CHOICE_SYSTEM = 0;
    public static final int CHOICE_LIGHT = 1;
    public static final int CHOICE_DARK = 2;

    private static boolean sLight = false;

    /** Resolves the saved choice into the active mode. Call before any screen is built. */
    public static void init(android.content.Context context) {
        applyPreference(context);
    }

    public static boolean isLight() {
        return sLight;
    }

    /** The choice the person made: SYSTEM, LIGHT or DARK. Older installs keep their saved light or dark pick. */
    public static int savedChoice(android.content.Context context) {
        android.content.SharedPreferences prefs = context.getApplicationContext()
                .getSharedPreferences(PREFS, android.content.Context.MODE_PRIVATE);
        if (prefs.contains(KEY_CHOICE)) {
            return prefs.getInt(KEY_CHOICE, CHOICE_SYSTEM);
        }
        if (prefs.contains(KEY_LIGHT)) {
            return prefs.getBoolean(KEY_LIGHT, false) ? CHOICE_LIGHT : CHOICE_DARK;
        }
        return CHOICE_SYSTEM;
    }

    public static void saveChoice(android.content.Context context, int choice) {
        context.getApplicationContext()
                .getSharedPreferences(PREFS, android.content.Context.MODE_PRIVATE)
                .edit().putInt(KEY_CHOICE, choice).apply();
    }

    public static String choiceName(int choice) {
        return choice == CHOICE_LIGHT ? "Light" : choice == CHOICE_DARK ? "Dark" : "System";
    }

    /** Applies the saved choice. With SYSTEM, it follows the phone's current light or dark setting. */
    public static void applyPreference(android.content.Context context) {
        int choice = savedChoice(context);
        boolean phoneNight = (context.getResources().getConfiguration().uiMode
                & android.content.res.Configuration.UI_MODE_NIGHT_MASK)
                == android.content.res.Configuration.UI_MODE_NIGHT_YES;
        applyMode(choice == CHOICE_LIGHT || (choice == CHOICE_SYSTEM && !phoneNight));
    }

    /** Swaps the mode-dependent color tokens. Dark is purple frosted glass; light is pale lavender glass. */
    public static void applyMode(boolean light) {
        sLight = light;
        COLOR_BG_DEEP = Color.parseColor(light ? "#F6EEFA" : "#2A0F33");
        COLOR_SURFACE = Color.parseColor(light ? "#E6FFFFFF" : "#3D1749");
        COLOR_SURFACE_2 = Color.parseColor(light ? "#CCFFFFFF" : "#1FFFFFFF");
        COLOR_SURFACE_SOLID = Color.parseColor(light ? "#FFFFFF" : "#3D1749");
        COLOR_PURPLE_MUTED = Color.parseColor(light ? "#5A3A66" : "#CDBBD6");
        COLOR_PURPLE_SOFT = Color.parseColor(light ? "#5A3A66" : "#CDBBD6");
        COLOR_TEXT_PRIMARY = Color.parseColor(light ? "#2A0F33" : "#F5F0F7");
        COLOR_TEXT_SECONDARY = Color.parseColor(light ? "#5A3A66" : "#CDBBD6");
        COLOR_TEXT_DISABLED = Color.parseColor(light ? "#8C5A3A66" : "#8CCDBBD6"); // Bible muted at 55%
        COLOR_ORANGE_SOFT = Color.parseColor(light ? "#8A4A12" : "#F8C894"); // Bible accent
        COLOR_SURFACE_2_SOLID = Color.parseColor(light ? "#F8F1FB" : "#3D1749"); // Bible card and canvas
        COLOR_STROKE = Color.parseColor(light ? "#F2FFFFFF" : "#4A3554");
        // Aliases that mirror the tokens above
        COLOR_BG_NEUTRAL = COLOR_BG_DEEP;
        COLOR_TEXT_MUTED = COLOR_TEXT_SECONDARY;
        COLOR_BORDER_GREY = COLOR_STROKE;
        COLOR_PURPLE_DEEP = COLOR_TEXT_PRIMARY; // text-on-glass alias follows the active foreground colour
    }

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
    public static final int RADIUS_LG = 22;
    public static final int RADIUS_XL = 28;
    public static final int RADIUS_PILL = 999;

    // =========================================================================
    // GRADIENT DRAWABLE FACTORIES (Darkened & Muted Palettes)
    // =========================================================================

    /**
     * Vertical darkened background canvas gradient (#2E1038 -> #190720).
     */
    private static GradientDrawable createBaseBackground() {
        return new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                sLight ? new int[]{Color.parseColor("#F8F1FB"), Color.parseColor("#EFE0F6"), Color.parseColor("#E6D2F0")} : new int[]{Color.parseColor("#2A0F33"), Color.parseColor("#160520")}
        );
    }

    /** Page background: the base gradient plus two soft glow orbs (purple top-left, orange lower-right). */
    public static android.graphics.drawable.Drawable createBackgroundGradient() {
        return new GlowBackground(new android.graphics.drawable.Drawable[]{
                createBaseBackground(),
                // Mockup glows: purple top-left rgba(139,50,170,0.75), orange lower-right rgba(225,125,47,0.45),
                // on a dark #2A0F33 -> #160520 base so the bottom of the page stays deep, as drawn.
                createGlowOrb(0.20f, 0.13f, 0.62f, sLight ? "#66C58BDB" : "#BF8B32AA"),
                createGlowOrb(0.87f, 0.62f, 0.58f, sLight ? "#4DF3B37A" : "#73E17D2F"),
                createGlowOrb(0.15f, 0.88f, 0.85f, sLight ? "#40B070C8" : "#00000000")
        });
    }

    /** Marker type so screens and {@link GlassPolish} can tell the glowing page background apart. */
    public static final class GlowBackground extends android.graphics.drawable.LayerDrawable {
        GlowBackground(android.graphics.drawable.Drawable[] layers) {
            super(layers);
        }
    }

    private static GradientDrawable createGlowOrb(float cx, float cy, float radiusOfWidth, String centerColor) {
        GradientDrawable gd = new GradientDrawable();
        gd.setGradientType(GradientDrawable.RADIAL_GRADIENT);
        gd.setGradientCenter(cx, cy);
        gd.setGradientRadius(android.content.res.Resources.getSystem().getDisplayMetrics().widthPixels * radiusOfWidth);
        gd.setColors(new int[]{Color.parseColor(centerColor), Color.TRANSPARENT});
        return gd;
    }

    /** Frosted glass pill (floating nav bar): translucent fill, light border, large rounded corners. */
    public static GradientDrawable createGlassPill(int radiusPx, int strokePx) {
        GradientDrawable gd = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                sLight ? new int[]{Color.parseColor("#E6FFFFFF"), Color.parseColor("#B3FFFFFF")}
                       : new int[]{Color.parseColor("#33FFFFFF"), Color.parseColor("#1AFFFFFF")});
        gd.setCornerRadius(radiusPx);
        gd.setStroke(strokePx, Color.parseColor(sLight ? "#40681A7D" : "#40FFFFFF"));
        return gd;
    }

    /** Raised purple circle used for the centre Give button. */
    public static GradientDrawable createGiveOrb() {
        GradientDrawable gd = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{Color.parseColor("#8A2BA3"), Color.parseColor("#681A7D")});
        gd.setShape(GradientDrawable.OVAL);
        gd.setStroke(2, Color.parseColor("#66FFFFFF"));
        return gd;
    }

    /**
     * Top App Bar darkened header gradient (#32123D -> #220A2B) with bottom hairline stroke.
     */
    public static GradientDrawable createTopBarGradient(int strokePx) {
        GradientDrawable gd = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                sLight ? new int[]{Color.parseColor("#CCFFFFFF"), Color.parseColor("#99FFFFFF")} : new int[]{Color.parseColor("#33FFFFFF"), Color.parseColor("#14FFFFFF")}
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
                sLight ? new int[]{Color.parseColor("#D9FFFFFF"), Color.parseColor("#A6FFFFFF")} : new int[]{Color.parseColor("#2EFFFFFF"), Color.parseColor("#14FFFFFF")}
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
                sLight ? new int[]{Color.parseColor("#C7FFFFFF"), Color.parseColor("#B3FFFFFF")} : new int[]{Color.parseColor("#3D1749"), Color.parseColor("#3D1749")}
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
                sLight ? new int[]{Color.parseColor("#CCFFFFFF"), Color.parseColor("#99FFFFFF")} : new int[]{Color.parseColor("#1FFFFFFF"), Color.parseColor("#1FFFFFFF")}
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
                GradientDrawable.Orientation.TOP_BOTTOM,
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
                sLight ? new int[]{Color.parseColor("#66B070C8"), Color.parseColor("#33E17D2F")} : new int[]{Color.parseColor("#668B32AA"), Color.parseColor("#33E17D2F")}
        );
        gd.setCornerRadius(radiusPx);
        if (strokePx > 0) {
            gd.setStroke(strokePx, Color.parseColor(sLight ? "#40681A7D" : "#47FFFFFF"));
        }
        return gd;
    }

    /**
     * Darkened dialog background gradient (#3D154A -> #24092C).
     */
    public static GradientDrawable createDialogGradient(int radiusPx, int strokePx) {
        GradientDrawable gd = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                sLight ? new int[]{Color.parseColor("#FFFFFFFF"), Color.parseColor("#FFF3E8F8")} : new int[]{Color.parseColor("#3D1749"), Color.parseColor("#3D1749")}
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
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{Color.parseColor("#8A2BA3"), Color.parseColor("#681A7D")}
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
                sLight ? new int[]{Color.parseColor("#B3FFFFFF"), Color.parseColor("#80FFFFFF")} : new int[]{Color.parseColor("#26FFFFFF"), Color.parseColor("#14FFFFFF")}
        );
        gd.setCornerRadius(radiusPx);
        if (strokePx > 0) {
            gd.setStroke(strokePx, strokeColor);
        }
        return gd;
    }
}

