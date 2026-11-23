package org.perazimchurch.app.presentation.ui.games.common;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.perazimchurch.app.ui.theme.PerazimTheme;

/**
 * Icon-first heads-up bar for a game: a pause chip, a stars chip and a lives chip. Shapes are drawn in
 * code, so there are no bitmaps behind the chips. Pictograms only; the stars and lives show as small drawn
 * glyphs with the count, so a player who cannot read can still follow them.
 */
public final class GameHud {

    private GameHud() { }

    /** A row with the pause chip, the stars chip (filled out of three) and the lives chip. */
    public static LinearLayout bar(Activity activity, int starsEarned, int livesLeft, Runnable onPause) {
        LinearLayout row = new LinearLayout(activity);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        int pad = dp(activity, 8);
        row.setPadding(0, pad, 0, pad);

        LinearLayout pause = chip(activity, "II", false);
        pause.setOnClickListener(v -> { if (onPause != null) onPause.run(); });
        row.addView(pause, new LinearLayout.LayoutParams(dp(activity, 52), dp(activity, 52)));

        LinearLayout stars = chip(activity, starString(starsEarned), true);
        LinearLayout.LayoutParams starParams = new LinearLayout.LayoutParams(0, dp(activity, 52), 1f);
        starParams.setMargins(dp(activity, 10), 0, dp(activity, 10), 0);
        row.addView(stars, starParams);

        LinearLayout lives = chip(activity, heartString(livesLeft), false);
        row.addView(lives, new LinearLayout.LayoutParams(dp(activity, 84), dp(activity, 52)));
        return row;
    }

    private static LinearLayout chip(Activity activity, String label, boolean stretch) {
        LinearLayout chip = new LinearLayout(activity);
        chip.setGravity(Gravity.CENTER);
        chip.setOrientation(LinearLayout.HORIZONTAL);
        GradientDrawable pill = new GradientDrawable();
        pill.setShape(GradientDrawable.RECTANGLE);
        pill.setCornerRadius(dp(activity, 26));
        pill.setColor(PerazimTheme.COLOR_SURFACE_2);
        pill.setStroke(dp(activity, 2), 0x668B5CF6);
        chip.setBackground(pill);
        chip.setClickable(true);
        TextView text = new TextView(activity);
        text.setText(label);
        text.setTextSize(stretch ? 18 : 15);
        text.setTextColor(PerazimTheme.COLOR_WHITE);
        text.setTypeface(Typeface.DEFAULT_BOLD);
        text.setShadowLayer(dp(activity, 3), 0, dp(activity, 1), 0x66000000);
        chip.addView(text);
        return chip;
    }

    /** Three star marks, filled ones first. Drawn as text marks, one per star, so no bitmap is needed. */
    static String starString(int earned) {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < 3; i++) out.append(i < earned ? "★" : "☆");
        return out.toString();
    }

    /** Heart marks for lives left, up to three. */
    static String heartString(int lives) {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < 3; i++) out.append(i < lives ? "♥" : "♡");
        return out.toString();
    }

    private static int dp(Activity activity, int value) {
        return Math.round(value * activity.getResources().getDisplayMetrics().density);
    }
}
