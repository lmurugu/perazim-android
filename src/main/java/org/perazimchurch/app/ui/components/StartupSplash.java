package org.perazimchurch.app.ui.components;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.perazimchurch.app.R;
import org.perazimchurch.app.ui.theme.GlassKit;

/**
 * Start-up screen: solid black, the Perazim logo glides in and settles, the church name rises under it,
 * then everything fades into the app. Shown on a cold start only, never when a theme switch rebuilds the screen.
 */
public final class StartupSplash {

    private static final long LOGO_IN_MS = 900;
    private static final long NAME_IN_MS = 700;
    private static final long HOLD_MS = 700;
    private static final long FADE_OUT_MS = 500;

    private StartupSplash() {
    }

    public static void show(Activity activity) {
        final ViewGroup decor = (ViewGroup) activity.getWindow().getDecorView();
        final FrameLayout overlay = new FrameLayout(activity);
        overlay.setBackgroundColor(Color.BLACK);
        overlay.setClickable(true);

        // The logo sits exactly where the launch screen draws it, so the hand-over is seamless.
        final ImageView logo = new ImageView(activity);
        logo.setImageResource(R.drawable.perazim_logo_black);
        overlay.addView(logo, new FrameLayout.LayoutParams(GlassKit.px(activity, 240), GlassKit.px(activity, 240), Gravity.CENTER));

        LinearLayout column = new LinearLayout(activity);
        column.setOrientation(LinearLayout.VERTICAL);
        column.setGravity(Gravity.CENTER_HORIZONTAL);

        final TextView name = new TextView(activity);
        name.setText("Perazim Mission Church");
        name.setTextColor(Color.WHITE);
        name.setTextSize(20);
        name.setTypeface(Typeface.DEFAULT_BOLD);
        name.setLetterSpacing(0.06f);
        name.setGravity(Gravity.CENTER);
        column.addView(name, new LinearLayout.LayoutParams(-2, -2));

        final TextView tagline = new TextView(activity);
        tagline.setText("The Place of Great Breakthrough");
        tagline.setTextColor(Color.parseColor("#E17D2F"));
        tagline.setTextSize(13);
        tagline.setLetterSpacing(0.08f);
        tagline.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams lpTag = new LinearLayout.LayoutParams(-2, -2);
        lpTag.topMargin = GlassKit.px(activity, 6);
        column.addView(tagline, lpTag);

        FrameLayout.LayoutParams lpColumn = new FrameLayout.LayoutParams(-1, -2, Gravity.TOP | Gravity.CENTER_HORIZONTAL);
        lpColumn.topMargin = activity.getResources().getDisplayMetrics().heightPixels / 2 + GlassKit.px(activity, 132);
        overlay.addView(column, lpColumn);
        decor.addView(overlay, new ViewGroup.LayoutParams(-1, -1));
        overlay.setElevation(GlassKit.px(activity, 100));

        name.setAlpha(0f);
        name.setTranslationY(GlassKit.px(activity, 14));
        tagline.setAlpha(0f);
        tagline.setTranslationY(GlassKit.px(activity, 14));

        DecelerateInterpolator ease = new DecelerateInterpolator(1.6f);
        AnimatorSet logoIn = new AnimatorSet();
        logoIn.playTogether(
                ObjectAnimator.ofFloat(logo, View.SCALE_X, 1f, 1.06f, 1f),
                ObjectAnimator.ofFloat(logo, View.SCALE_Y, 1f, 1.06f, 1f));
        logoIn.setDuration(LOGO_IN_MS);
        logoIn.setInterpolator(ease);

        AnimatorSet nameIn = new AnimatorSet();
        nameIn.playTogether(
                ObjectAnimator.ofFloat(name, View.ALPHA, 0f, 1f),
                ObjectAnimator.ofFloat(name, View.TRANSLATION_Y, GlassKit.px(activity, 14), 0f),
                ObjectAnimator.ofFloat(tagline, View.ALPHA, 0f, 1f),
                ObjectAnimator.ofFloat(tagline, View.TRANSLATION_Y, GlassKit.px(activity, 14), 0f));
        nameIn.setDuration(NAME_IN_MS);
        nameIn.setInterpolator(ease);
        nameIn.setStartDelay(150);

        ObjectAnimator fadeOut = ObjectAnimator.ofFloat(overlay, View.ALPHA, 1f, 0f);
        fadeOut.setDuration(FADE_OUT_MS);
        fadeOut.setStartDelay(HOLD_MS);

        AnimatorSet all = new AnimatorSet();
        all.play(logoIn);
        all.play(nameIn).after(0);
        all.play(fadeOut).after(logoIn);
        all.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                decor.removeView(overlay);
            }
        });
        all.start();
    }
}
