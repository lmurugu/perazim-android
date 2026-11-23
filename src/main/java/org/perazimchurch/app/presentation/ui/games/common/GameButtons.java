package org.perazimchurch.app.presentation.ui.games.common;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.RippleDrawable;
import android.content.res.ColorStateList;

import org.perazimchurch.app.ui.theme.PerazimTheme;

/**
 * Drawn game buttons: a rounded gradient pill with a soft glow ring. Drawn in code, not a bitmap, so no
 * dark square shows behind the rounded corners. Matches the hub cards: purple or orange, glow, rounded corners.
 */
public final class GameButtons {

    private GameButtons() { }

    /** A pill for a game button. {@code primary} true is orange, false is purple. */
    public static android.graphics.drawable.Drawable drawn(Context context, boolean primary) {
        float density = context.getResources().getDisplayMetrics().density;
        int top = primary ? 0xFFFFB347 : 0xFFB98CFF;
        int bottom = primary ? 0xFFFF6A00 : 0xFF5B2A86;
        GradientDrawable glow = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{top, bottom});
        glow.setCornerRadius(999f * density);
        glow.setStroke(Math.round(1.5f * density), primary ? 0x66FFD39A : 0x66E3CCFF);

        // Lower edge gives the chunky pressed look.
        GradientDrawable edge = new GradientDrawable();
        edge.setCornerRadius(999f * density);
        edge.setColor(primary ? 0xFF8A3600 : 0xFF2D1450);
        LayerDrawable pill = new LayerDrawable(new android.graphics.drawable.Drawable[]{edge, glow});
        pill.setLayerInset(1, 0, 0, 0, Math.round(5 * density));

        ColorStateList ripple = ColorStateList.valueOf(0x33FFFFFF);
        return new RippleDrawable(ripple, pill, null);
    }
}
