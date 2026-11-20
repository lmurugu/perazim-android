package org.perazimchurch.app.ui.components;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
import android.os.SystemClock;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.perazimchurch.app.R;
import org.perazimchurch.app.ui.theme.GlassKit;

/**
 * Cold-start screen. It is the first thing the activity shows: solid black with the logo exactly where the launch
 * window draws it, so there is no jump. The church name and slogan fade in under the logo. The app is built right
 * after the first frame; the moment it is ready, the words fade away and the black lifts, so nothing lingers.
 *
 * <p>The fades use view property animators, which run on the render thread, so they keep moving while the app is
 * being built on the main thread.
 */
public final class StartupSplash {

    private static final long WORDS_DELAY_MS = 300;
    private static final long WORDS_IN_MS = 700;
    /** The words stay up at least this long, so a fast start still lets them be read. */
    private static final long MIN_SHOWN_MS = 1800;
    private static final long WORDS_OUT_MS = 200;
    private static final long BLACK_OUT_MS = 350;

    private static FrameLayout overlay;
    private static View name;
    private static View tagline;
    private static long beganAt;

    private StartupSplash() {
    }

    /** Shows the splash as the activity's content, then runs {@code buildApp} after the first frame is drawn. */
    public static void begin(final Activity activity, final Runnable buildApp) {
        if (Build.VERSION.SDK_INT >= 30) {
            // Same full-screen space as the launch window, so the logo does not move.
            activity.getWindow().setDecorFitsSystemWindows(false);
        }
        final FrameLayout root = new FrameLayout(activity);
        root.setBackgroundColor(Color.BLACK);
        root.setClickable(true);
        // The automatic glass styling must leave the splash exactly as built (it would paint a grey gradient).
        GlassKit.keep(root);

        ImageView logo = new ImageView(activity);
        logo.setImageResource(R.drawable.perazim_logo_black);
        GlassKit.keep(logo);
        root.addView(logo, new FrameLayout.LayoutParams(GlassKit.px(activity, 202), GlassKit.px(activity, 202), Gravity.CENTER));

        LinearLayout column = new LinearLayout(activity);
        column.setOrientation(LinearLayout.VERTICAL);
        column.setGravity(Gravity.CENTER_HORIZONTAL);
        GlassKit.keep(column);

        TextView title = new TextView(activity);
        title.setText("Perazim Mission Church");
        title.setTextColor(Color.WHITE);
        title.setTextSize(20);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        GlassKit.keep(title);
        title.setLetterSpacing(0.06f);
        title.setGravity(Gravity.CENTER);
        column.addView(title, new LinearLayout.LayoutParams(-2, -2));

        TextView slogan = new TextView(activity);
        slogan.setText("The Place of Great Breakthrough");
        slogan.setTextColor(Color.parseColor("#E17D2F"));
        slogan.setTextSize(13);
        GlassKit.keep(slogan);
        slogan.setLetterSpacing(0.08f);
        slogan.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams lpSlogan = new LinearLayout.LayoutParams(-2, -2);
        lpSlogan.topMargin = GlassKit.px(activity, 6);
        column.addView(slogan, lpSlogan);

        FrameLayout.LayoutParams lpColumn = new FrameLayout.LayoutParams(-1, -2, Gravity.TOP | Gravity.CENTER_HORIZONTAL);
        lpColumn.topMargin = activity.getResources().getDisplayMetrics().heightPixels / 2 + GlassKit.px(activity, 120);
        root.addView(column, lpColumn);

        title.setAlpha(0f);
        slogan.setAlpha(0f);
        title.animate().alpha(1f).setStartDelay(WORDS_DELAY_MS).setDuration(WORDS_IN_MS).start();
        slogan.animate().alpha(1f).setStartDelay(WORDS_DELAY_MS + 150).setDuration(WORDS_IN_MS).start();

        overlay = root;
        name = title;
        tagline = slogan;
        beganAt = SystemClock.elapsedRealtime();
        activity.setContentView(root);

        // Build the app after the splash has been drawn once, so the logo and fades are on screen first.
        root.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                root.getViewTreeObserver().removeOnPreDrawListener(this);
                // Give the words time to fade in while the main thread is still free; building the app blocks it.
                root.postDelayed(buildApp, WORDS_DELAY_MS + WORDS_IN_MS + 150);
                return true;
            }
        });
    }

    /** True while the cold-start splash is the current content. */
    public static boolean isActive() {
        return overlay != null;
    }

    /** Puts the finished app under the splash, then fades the words and the black away at once. */
    public static void showApp(final Activity activity, View app) {
        final FrameLayout splash = overlay;
        if (splash == null) {
            activity.setContentView(app);
            return;
        }
        ViewGroup parent = (ViewGroup) splash.getParent();
        if (parent != null) {
            parent.removeView(splash);
        }
        FrameLayout stack = new FrameLayout(activity);
        GlassKit.keep(stack);
        stack.addView(app, new FrameLayout.LayoutParams(-1, -1));
        stack.addView(splash, new FrameLayout.LayoutParams(-1, -1));
        activity.setContentView(stack);

        long wait = Math.max(0, MIN_SHOWN_MS - (SystemClock.elapsedRealtime() - beganAt));
        final View words1 = name;
        final View words2 = tagline;
        overlay = null;
        name = null;
        tagline = null;
        splash.postDelayed(() -> {
            words1.animate().cancel();
            words2.animate().cancel();
            words1.animate().alpha(0f).setStartDelay(0).setDuration(WORDS_OUT_MS).start();
            words2.animate().alpha(0f).setStartDelay(0).setDuration(WORDS_OUT_MS).start();
            splash.animate().alpha(0f).setStartDelay(WORDS_OUT_MS).setDuration(BLACK_OUT_MS)
                    .withEndAction(() -> stack.removeView(splash)).start();
        }, wait);
    }
}
