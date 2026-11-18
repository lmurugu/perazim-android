package org.perazimchurch.app.ui.theme;

import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;

/**
 * Frosted-glass drawables shared by every screen.
 *
 * <p>Any old "fill colour" a screen asks for is mapped to one of three glass looks:
 * <ul>
 *     <li>{@link Kind#ORANGE}: the warm primary call to action, solid enough for white text.</li>
 *     <li>{@link Kind#TINTED}: a saturated colour (brand purple, green, red) as see-through tinted glass.</li>
 *     <li>{@link Kind#FROSTED}: anything pale, white or near-black becomes clear frosted glass.</li>
 * </ul>
 * Every glass shape has a light rim and a soft sheen across its top half, which is what reads as glass.
 */
public final class Glass {

    public enum Kind { FROSTED, TINTED, ORANGE }

    /** A glass background that remembers what kind of glass it is, so text on it can be checked. */
    public static final class GlassLayer extends LayerDrawable {
        public final Kind kind;
        public final int tint;

        GlassLayer(Drawable[] layers, Kind kind, int tint) {
            super(layers);
            this.kind = kind;
            this.tint = tint;
        }
    }

    private Glass() {
    }

    private static float density = Resources.getSystem().getDisplayMetrics().density;

    /** The activity's density, which {@link UiScale} may have changed from the system's. */
    public static void useDensity(float activityDensity) {
        if (activityDensity > 0f) {
            density = activityDensity;
        }
    }

    public static float dp(float value) {
        return value * density;
    }

    /** Works out which glass look an old fill colour should become. */
    public static Kind kindOf(int color) {
        if (Color.alpha(color) < 0x80) {
            return Kind.FROSTED;
        }
        float[] hsv = new float[3];
        Color.colorToHSV(color, hsv);
        float hue = hsv[0];
        float sat = hsv[1];
        float val = hsv[2];
        if (sat < 0.28f || val < 0.30f) {
            return Kind.FROSTED;
        }
        if (hue >= 12f && hue <= 45f && val > 0.55f) {
            return Kind.ORANGE;
        }
        return Kind.TINTED;
    }

    /** Glass for a card or panel. */
    public static GlassLayer card(int requestedFill, float radiusPx) {
        return build(kindOf(requestedFill), requestedFill, radiusPx, true);
    }

    /** Glass for a button or chip: always a full pill. */
    public static GlassLayer button(int requestedFill) {
        return build(kindOf(requestedFill), requestedFill, dp(999), true);
    }

    public static GlassLayer build(Kind kind, int tint, float radiusPx, boolean rim) {
        boolean light = PerazimTheme.isLight();
        GradientDrawable body;
        int rimColor;
        int sheenTop;
        switch (kind) {
            case ORANGE:
                body = gradient(new int[]{Color.parseColor("#F5A05A"), Color.parseColor("#E17D2F"), Color.parseColor("#C8611B")});
                rimColor = Color.parseColor("#80FFE2C8");
                sheenTop = Color.parseColor("#55FFFFFF");
                break;
            case TINTED:
                int top = withAlpha(lighten(tint, 0.22f), light ? 0xF2 : 0xE0);
                int bottom = withAlpha(tint, light ? 0xE6 : 0xB8);
                body = gradient(new int[]{top, bottom});
                rimColor = Color.parseColor(light ? "#80FFFFFF" : "#66FFFFFF");
                sheenTop = Color.parseColor("#4DFFFFFF");
                break;
            case FROSTED:
            default:
                body = gradient(light
                        ? new int[]{Color.parseColor("#E6FFFFFF"), Color.parseColor("#A6FFFFFF")}
                        : new int[]{Color.parseColor("#38FFFFFF"), Color.parseColor("#12FFFFFF")});
                rimColor = Color.parseColor(light ? "#4D8A2BA3" : "#4DFFFFFF");
                sheenTop = Color.parseColor(light ? "#00FFFFFF" : "#2EFFFFFF");
                break;
        }
        body.setCornerRadius(radiusPx);
        if (rim) {
            body.setStroke(Math.max(1, Math.round(dp(1))), rimColor);
        }
        GradientDrawable sheen = gradient(new int[]{sheenTop, Color.TRANSPARENT, Color.TRANSPARENT});
        sheen.setCornerRadius(radiusPx);
        return new GlassLayer(new Drawable[]{body, sheen}, kind, tint);
    }

    /** Text colour that stays readable on the given glass. Keeps the requested colour when it already reads well. */
    public static int readableText(int requested, Kind onKind) {
        boolean light = PerazimTheme.isLight();
        float lum = Color.luminance(requested);
        if (onKind == Kind.ORANGE || onKind == Kind.TINTED) {
            return lum < 0.45f ? Color.WHITE : requested;
        }
        if (light) {
            return lum > 0.75f ? PerazimTheme.COLOR_TEXT_PRIMARY : requested;
        }
        if (lum >= 0.20f) {
            return requested;
        }
        float[] hsv = new float[3];
        Color.colorToHSV(requested, hsv);
        boolean purpleish = hsv[1] > 0.25f && hsv[0] >= 260f && hsv[0] <= 320f;
        return purpleish ? Color.parseColor("#E8CFF2") : PerazimTheme.COLOR_TEXT_PRIMARY;
    }

    private static GradientDrawable gradient(int[] colors) {
        return new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, colors);
    }

    private static int withAlpha(int color, int alpha) {
        return (color & 0x00FFFFFF) | (alpha << 24);
    }

    private static int lighten(int color, float amount) {
        int r = Color.red(color);
        int g = Color.green(color);
        int b = Color.blue(color);
        r = Math.round(r + (255 - r) * amount);
        g = Math.round(g + (255 - g) * amount);
        b = Math.round(b + (255 - b) * amount);
        return Color.rgb(r, g, b);
    }
}
