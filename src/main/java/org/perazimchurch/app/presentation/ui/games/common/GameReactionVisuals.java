package org.perazimchurch.app.presentation.ui.games.common;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowInsets;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.perazimchurch.app.R;
import org.perazimchurch.app.ui.components.FeedbackManager;

import java.util.Random;

/** Lightweight reaction stickers and a win burst. The overlay never handles game touches. */
final class GameReactionVisuals {
    private static final Object OVERLAY = new Object();
    private static final int GOLD = 0xFFFFCD79;
    private static final int LAVENDER = 0xFFE2C5FF;
    private static final int SKY = 0xFF9ADBEA;

    private GameReactionVisuals() {}

    static void clear(Window window) {
        if (window == null || !(window.peekDecorView() instanceof FrameLayout)) return;
        FrameLayout root = (FrameLayout) window.peekDecorView();
        View overlay = root.findViewWithTag(OVERLAY);
        if (overlay != null) root.removeView(overlay);
    }

    static void show(Window window, String gameId, GameAudio.Reaction reaction, String line) {
        if (window == null || !(window.peekDecorView() instanceof FrameLayout)) return;
        FrameLayout root = (FrameLayout) window.peekDecorView();
        if (!root.isAttachedToWindow()) return;
        clear(window);

        Context context = root.getContext();
        Look look = lookFor(gameId);
        FrameLayout overlay = new FrameLayout(context);
        overlay.setTag(OVERLAY);
        overlay.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
        overlay.setClickable(false);
        int colour = reaction == GameAudio.Reaction.WIN ? GOLD
                : reaction == GameAudio.Reaction.HELP ? SKY : LAVENDER;
        boolean win = reaction == GameAudio.Reaction.WIN;
        boolean motion = GameFx.motionEnabled(context);
        Burst burst = win && motion && FeedbackManager.get(context).isCelebrationsEnabled()
                ? new Burst(context, look.pattern) : null;
        if (burst != null) overlay.addView(burst, new FrameLayout.LayoutParams(-1, -1));

        LinearLayout card = new LinearLayout(context);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(dp(context, 10), dp(context, 8), dp(context, 12), dp(context, 8));
        card.setBackground(shape(context, 0xFF351C4B, colour));
        card.setElevation(dp(context, 8));

        ImageView sticker = new ImageView(context);
        sticker.setImageResource(look.art);
        sticker.setScaleType(ImageView.ScaleType.CENTER_CROP);
        sticker.setBackground(shape(context, 0xFF523168, colour));
        sticker.setClipToOutline(true);
        card.addView(sticker, new LinearLayout.LayoutParams(dp(context, 45), dp(context, 45)));

        LinearLayout words = new LinearLayout(context);
        words.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams wordSpace = new LinearLayout.LayoutParams(0, -2, 1f);
        wordSpace.leftMargin = dp(context, 10);
        card.addView(words, wordSpace);
        TextView title = new TextView(context);
        title.setText(win ? look.win : reaction == GameAudio.Reaction.HELP ? look.help : look.retry);
        title.setTextSize(15);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setTextColor(colour);
        words.addView(title);
        TextView caption = new TextView(context);
        caption.setText(line);
        caption.setTextColor(0xFFFFF7FF);
        caption.setTextSize(12);
        words.addView(caption);

        FrameLayout.LayoutParams pos = new FrameLayout.LayoutParams(-1, -2, Gravity.TOP);
        WindowInsets insets = root.getRootWindowInsets();
        int safeTop = insets == null ? 0 : insets.getSystemWindowInsetTop();
        pos.setMargins(dp(context, 15), safeTop + dp(context, 59), dp(context, 15), 0);
        overlay.addView(card, pos);
        root.addView(overlay, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        if (burst != null) burst.start();
        if (motion) {
            card.setAlpha(0f);
            card.setTranslationY(-dp(context, 11));
            card.animate().alpha(1f).translationY(0f).setDuration(180).start();
        }
        overlay.postDelayed(() -> {
            if (overlay.getParent() == root) root.removeView(overlay);
        }, win ? 3200L : 2500L);
    }

    private static GradientDrawable shape(Context context, int fill, int border) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(fill);
        drawable.setCornerRadius(dp(context, 14));
        drawable.setStroke(dp(context, 1), border);
        return drawable;
    }

    private static int dp(Context context, int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }

    private static Look lookFor(String id) {
        switch (id) {
            case "who_am_i": return new Look(R.drawable.game_who_am_i, "Case closed", "Mystery lives on", "Detective mode", 0);
            case "verse_builder": return new Look(R.drawable.game_verse_builder, "Word by word", "Words on the loose", "First word first", 1);
            case "ladder_of_faith": return new Look(R.drawable.game_ladder, "Top rung!", "Wobbly rung", "Handrail unlocked", 0);
            case "word_search": return new Look(R.drawable.game_word_search, "Word hunter", "Still hiding", "Search party", 1);
            case "memory_match": return new Look(R.drawable.game_memory_match, "Match magic", "Not twins", "Memory jog", 1);
            case "story_timeline": return new Look(R.drawable.game_timeline, "Story in order", "A little later", "Back to the start", 2);
            case "road_to_jerusalem": return new Look(R.drawable.game_road, "Jerusalem!", "Good race", "Roadside tip", 2);
            case "old_or_new": return new Look(R.drawable.game_old_or_new, "Books sorted", "Wrong shelf", "Shelf check", 0);
            case "who_said_it": return new Look(R.drawable.game_who_said_it, "You knew the voice", "Guess again", "Who was there?", 0);
            case "fill_the_gap": return new Look(R.drawable.game_fill_gap, "Gap closed", "That word escaped", "Read it out", 1);
            case "vine_words": return new Look(R.drawable.game_vine_words, "Look what grew", "Wrong branch", "Vine support", 1);
            case "lamp_slide": return new Look(R.drawable.game_lamp_slide, "Picture restored", "Tile said no", "Light the way", 2);
            case "story_mosaic": return new Look(R.drawable.game_story_mosaic, "It all fits", "Wrong home", "Clue piece", 2);
            default: return new Look(R.drawable.game_trivia, "Quiz cleared", "Plot twist", "Clue crew", 0);
        }
    }

    private static final class Look {
        final int art;
        final String win;
        final String retry;
        final String help;
        final int pattern;

        Look(int art, String win, String retry, String help, int pattern) {
            this.art = art;
            this.win = win;
            this.retry = retry;
            this.help = help;
            this.pattern = pattern;
        }
    }

    private static final class Burst extends View {
        private static final int[] COLOURS = {0xFFFFCC70, 0xFFE8BAFF, 0xFFAE8BEB, 0xFFF7A8C9, 0xFF8FDCE4};
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final float[] dx = new float[42];
        private final float[] dy = new float[42];
        private final int pattern;
        private ValueAnimator clock;
        private float time;

        Burst(Context context, int pattern) {
            super(context);
            this.pattern = pattern;
            Random random = new Random();
            for (int i = 0; i < dx.length; i++) {
                dx[i] = (random.nextFloat() - .5f) * 1.35f;
                dy[i] = .26f + random.nextFloat() * .75f;
            }
        }

        void start() {
            clock = ValueAnimator.ofFloat(0f, 1f);
            clock.setDuration(1450L);
            clock.addUpdateListener(a -> {
                time = (float) a.getAnimatedValue();
                invalidate();
            });
            clock.start();
        }

        @Override protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float t = time;
            if (t >= 1f) return;
            int alpha = Math.min(255, (int) ((1f - t) * 360));
            for (int i = 0; i < dx.length; i++) {
                paint.setColor(COLOURS[(i + pattern) % COLOURS.length]);
                paint.setAlpha(alpha);
                float x = getWidth() * (.5f + dx[i] * t);
                float y = getHeight() * (.31f - dy[i] * t * .54f + t * t * .66f);
                float size = dp(getContext(), 3 + i % 5);
                canvas.save();
                canvas.rotate(i * 19f + t * 190f, x, y);
                if (pattern == 0 && i % 3 == 0) {
                    canvas.drawRect(x - size, y - size / 4, x + size, y + size / 4, paint);
                    canvas.drawRect(x - size / 4, y - size, x + size / 4, y + size, paint);
                } else if (pattern == 1 && i % 4 == 0) {
                    paint.setTypeface(Typeface.DEFAULT_BOLD);
                    paint.setTextSize(size * 2.15f);
                    canvas.drawText(i % 2 == 0 ? "A" : "✦", x, y, paint);
                } else if (pattern == 2 && i % 3 == 0) {
                    canvas.drawRoundRect(x-size*.7f, y-size*.7f, x+size*.7f, y+size*.7f, size/4, size/4, paint);
                } else {
                    canvas.drawRect(x - size / 2, y - size, x + size / 2, y + size, paint);
                }
                canvas.restore();
            }
        }

        @Override protected void onDetachedFromWindow() {
            if (clock != null) clock.cancel();
            super.onDetachedFromWindow();
        }
    }
}
