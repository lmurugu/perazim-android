package org.perazimchurch.app.presentation.ui.games.common;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Build;
import android.provider.Settings;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * The motion kit for game screens: button press bounce, screen enter and exit, correct and wrong
 * feedback, star pop with a particle burst, a score count-up, a shimmer for unlocked levels, a dim
 * behind the pause screen, haptics and short tones. Only ValueAnimator, ViewPropertyAnimator, ObjectAnimator
 * and Canvas are used, so it stays light on small phones. When the system asks to remove animations
 * (animator duration scale 0), every effect jumps straight to its end state. See docs/games/ENGINE_USAGE.md.
 */
public final class GameFx {

    /** Sound effects on or off. On by default; the app's settings screen can call {@link #setSoundEnabled}. */
    private static volatile boolean soundEnabled = true;
    /** Haptics on or off. On by default. */
    private static volatile boolean hapticsEnabled = true;
    private static ToneGenerator tones;
    private static final Random RANDOM = new Random();

    private GameFx() {}

    public static void setSoundEnabled(boolean on) { soundEnabled = on; }
    public static void setHapticsEnabled(boolean on) { hapticsEnabled = on; }

    /** False when the system has turned animations off ("Remove animations"). */
    public static boolean motionEnabled(Context context) {
        try {
            return Settings.Global.getFloat(context.getContentResolver(), Settings.Global.ANIMATOR_DURATION_SCALE, 1f) > 0f;
        } catch (RuntimeException e) {
            return true;
        }
    }

    private static float dp(View v, float value) {
        return value * v.getResources().getDisplayMetrics().density;
    }

    // ---- Buttons ----

    /** Press bounce: the button scales to 0.94 while pressed and springs back on release. Clicks still fire. */
    public static void attachPress(View button) {
        if (!motionEnabled(button.getContext())) return;
        button.setOnTouchListener((v, event) -> {
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    v.animate().cancel();
                    v.animate().scaleX(0.94f).scaleY(0.94f).setDuration(90).start();
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    v.animate().scaleX(1f).scaleY(1f).setDuration(220)
                            .setInterpolator(new android.view.animation.OvershootInterpolator(2f)).start();
                    break;
                default:
                    break;
            }
            return false;
        });
    }

    // ---- Screens ----

    /** Screen enter: slides up 24 dp and fades in. */
    public static void enter(View screen) {
        if (!motionEnabled(screen.getContext())) return;
        screen.setAlpha(0f);
        screen.setTranslationY(dp(screen, 24));
        screen.animate().alpha(1f).translationY(0f).setDuration(260)
                .setInterpolator(new android.view.animation.DecelerateInterpolator()).start();
    }

    /** Screen exit: slides down 24 dp and fades out, then runs {@code end}. */
    public static void exit(View screen, Runnable end) {
        if (!motionEnabled(screen.getContext())) {
            if (end != null) end.run();
            return;
        }
        screen.animate().alpha(0f).translationY(dp(screen, 24)).setDuration(180)
                .withEndAction(() -> { if (end != null) end.run(); }).start();
    }

    /** Dims the screen behind a pause overlay. Add the returned view to the root; remove it on resume. */
    public static View dim(ViewGroup root) {
        View shade = new View(root.getContext());
        shade.setBackgroundColor(Color.BLACK);
        shade.setAlpha(0f);
        root.addView(shade, new ViewGroup.LayoutParams(-1, -1));
        if (motionEnabled(root.getContext())) {
            shade.animate().alpha(0.55f).setDuration(200).start();
        } else {
            shade.setAlpha(0.55f);
        }
        return shade;
    }

    // ---- Answers ----

    /** Correct answer: a glow pulse and a light tick, plus a short haptic. */
    public static void correct(View view) {
        pulse(view, Color.argb(160, 255, 170, 60));
        haptic(view, true);
        if (!GameAudio.get(view.getContext()).play(GameAudio.Sfx.CORRECT)) tone(ToneGenerator.TONE_PROP_ACK);
    }

    /** Wrong answer: a shake, a soft buzz and a short haptic. */
    public static void wrong(View view) {
        shake(view);
        haptic(view, false);
        if (!GameAudio.get(view.getContext()).play(GameAudio.Sfx.WRONG)) tone(ToneGenerator.TONE_PROP_NACK);
    }

    /** Scale pulse with a glow in {@code glow}. */
    public static void pulse(View view, int glow) {
        if (!motionEnabled(view.getContext())) return;
        if (Build.VERSION.SDK_INT >= 28) view.setOutlineSpotShadowColor(glow);
        view.setElevation(dp(view, 12));
        view.animate().cancel();
        view.animate().scaleX(1.06f).scaleY(1.06f).setDuration(120)
                .withEndAction(() -> view.animate().scaleX(1f).scaleY(1f).setDuration(180).start()).start();
    }

    /** Horizontal shake for a wrong answer. */
    public static void shake(View view) {
        if (!motionEnabled(view.getContext())) return;
        float d = dp(view, 10);
        ObjectAnimator.ofFloat(view, View.TRANSLATION_X, 0f, -d, d, -d * 0.6f, d * 0.6f, 0f)
                .setDuration(380).start();
    }

    // ---- Results: stars, particles and count-up ----

    /**
     * A row of three stars with a particle layer on top. Put it on the page, then call
     * {@link #popStars} with the same frame.
     */
    public static FrameLayout starRow(Context context, int stars) {
        FrameLayout frame = new FrameLayout(context);
        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(android.view.Gravity.CENTER);
        for (int i = 0; i < 3; i++) {
            TextView star = new TextView(context);
            star.setText(i < stars ? "★" : "☆");
            star.setTextSize(38);
            star.setGravity(android.view.Gravity.CENTER);
            star.setPadding(dpInt(context, 6), 0, dpInt(context, 6), 0);
            row.addView(star);
        }
        frame.addView(row, new FrameLayout.LayoutParams(-1, -2));
        frame.addView(new ParticleView(context), new FrameLayout.LayoutParams(-1, -1));
        return frame;
    }

    private static int dpInt(Context context, float value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }

    /** Pops the lit stars in one after another, with a small burst on each. */
    public static void popStars(FrameLayout frame, int stars) {
        if (frame.getChildCount() == 0) return;
        LinearLayout row = (LinearLayout) frame.getChildAt(0);
        ParticleView particles = frame.getChildCount() > 1 ? (ParticleView) frame.getChildAt(1) : null;
        boolean motion = motionEnabled(frame.getContext());
        GameAudio audio = GameAudio.get(frame.getContext());
        for (int i = 0; i < row.getChildCount(); i++) {
            View star = row.getChildAt(i);
            if (!motion) continue;
            star.setScaleX(0f);
            star.setScaleY(0f);
            final boolean lit = i < stars;
            star.animate().scaleX(1f).scaleY(1f).setStartDelay(160L * i).setDuration(320)
                    .setInterpolator(new android.view.animation.OvershootInterpolator(2.2f))
                    .withStartAction(() -> {
                        if (lit && particles != null) particles.burst(star.getX() + star.getWidth() / 2f, star.getY() + star.getHeight() / 2f);
                        if (lit) haptic(star, true);
                        if (lit) audio.playPitched(GameAudio.Sfx.STAR, 1f + 0.12f * (i + 1));
                    }).start();
        }
        if (stars >= 3) audio.playPitched(GameAudio.Sfx.COMPLETE, 1f);
    }

    /** Counts a score up from 0 to {@code target}, showing "prefix N suffix". */
    public static void countUp(TextView view, int target, String prefix, String suffix) {
        if (!motionEnabled(view.getContext())) {
            view.setText(prefix + target + suffix);
            return;
        }
        ValueAnimator animator = ValueAnimator.ofInt(0, target);
        animator.setDuration(Math.min(900, 120L + 60L * target));
        animator.addUpdateListener(a -> view.setText(prefix + a.getAnimatedValue() + suffix));
        animator.start();
    }

    // ---- Unlocks ----

    /** One light sweep across a view, for a level that has just unlocked. Removed when it ends. */
    public static void shimmer(View view) {
        if (!motionEnabled(view.getContext())) return;
        ShimmerDrawable sweep = new ShimmerDrawable();
        sweep.setBounds(0, 0, view.getWidth(), view.getHeight());
        view.getOverlay().add(sweep);
        ValueAnimator animator = ValueAnimator.ofFloat(-view.getWidth(), view.getWidth());
        animator.setDuration(700);
        animator.addUpdateListener(a -> {
            sweep.setOffset((Float) a.getAnimatedValue());
            view.invalidate();
        });
        animator.addListener(new AnimatorListenerAdapter() {
            @Override public void onAnimationEnd(Animator animation) {
                view.getOverlay().remove(sweep);
            }
        });
        animator.start();
    }

    // ---- Haptics and tones ----

    /** A short haptic. {@code correct} gives a light tick, otherwise a firmer buzz. */
    public static void haptic(View view, boolean correct) {
        if (!hapticsEnabled) return;
        view.performHapticFeedback(correct ? HapticFeedbackConstants.VIRTUAL_KEY : HapticFeedbackConstants.LONG_PRESS);
    }

    private static void tone(int type) {
        if (!soundEnabled) return;
        try {
            if (tones == null) tones = new ToneGenerator(AudioManager.STREAM_MUSIC, 40);
            tones.startTone(type, 90);
        } catch (RuntimeException ignored) {
            // No audio device or tone generator: the game still works without sound.
        }
    }

    // ---- Drawables and overlays ----

    /** Particle burst layer. Draws circles that fly out and fade; no bitmaps. */
    static final class ParticleView extends android.view.View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
        private final Bitmap sheet;
        private final List<float[]> particles = new ArrayList<>();
        private float progress = 1f;
        private float cx;
        private float cy;

        ParticleView(Context context) {
            super(context);
            paint.setColor(Color.rgb(255, 180, 70));
            sheet = GameArt.particles(context);
        }

        void burst(float x, float y) {
            if (!motionEnabled(getContext())) return;
            cx = x;
            cy = y;
            particles.clear();
            float speed = dp(this, 56);
            for (int i = 0; i < 12; i++) {
                double angle = (Math.PI * 2 * i) / 12 + RANDOM.nextFloat() * 0.3;
                float v = speed * (0.6f + RANDOM.nextFloat() * 0.5f);
                particles.add(new float[] {(float) Math.cos(angle) * v, (float) Math.sin(angle) * v, RANDOM.nextInt(3)});
            }
            ValueAnimator animator = ValueAnimator.ofFloat(0f, 1f);
            animator.setDuration(600);
            animator.addUpdateListener(a -> {
                progress = (Float) a.getAnimatedValue();
                invalidate();
            });
            animator.start();
        }

        @Override protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            if (particles.isEmpty() || progress >= 1f) return;
            int alpha = Math.round(255 * (1f - progress));
            paint.setAlpha(alpha);
            float radius = dp(this, 4) * (1f - progress * 0.5f);
            float size = dp(this, 22) * (1f - progress * 0.5f);
            int[][] kinds = {GameArt.PARTICLE_STAR, GameArt.PARTICLE_SPARKLE, GameArt.PARTICLE_FLARE};
            for (float[] p : particles) {
                float x = cx + p[0] * progress;
                float y = cy + p[1] * progress;
                if (sheet == null) {
                    canvas.drawCircle(x, y, radius, paint);
                } else {
                    Rect from = GameArt.source(sheet, kinds[(int) p[2]]);
                    canvas.drawBitmap(sheet, from, new RectF(x - size / 2, y - size / 2, x + size / 2, y + size / 2), paint);
                }
            }
        }
    }

    /** A soft white band that sweeps across a view. */
    static final class ShimmerDrawable extends Drawable {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private float offset;

        void setOffset(float value) { offset = value; }

        @Override protected void onBoundsChange(Rect bounds) {
            int w = Math.max(1, bounds.width() / 2);
            paint.setShader(new LinearGradient(0, 0, w, 0,
                    new int[] {0x00FFFFFF, 0x59FFFFFF, 0x00FFFFFF}, null, Shader.TileMode.CLAMP));
        }

        @Override public void draw(Canvas canvas) {
            canvas.save();
            canvas.translate(offset, 0);
            canvas.drawRect(0, 0, getBounds().width(), getBounds().height(), paint);
            canvas.restore();
        }

        @Override public void setAlpha(int alpha) { paint.setAlpha(alpha); }
        @Override public void setColorFilter(android.graphics.ColorFilter colorFilter) { paint.setColorFilter(colorFilter); }
        @Override public int getOpacity() { return android.graphics.PixelFormat.TRANSLUCENT; }
    }
}
