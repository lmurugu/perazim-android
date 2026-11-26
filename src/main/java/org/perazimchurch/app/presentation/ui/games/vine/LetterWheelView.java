package org.perazimchurch.app.presentation.ui.games.vine;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;

import org.perazimchurch.app.ui.theme.GlassKit;
import org.perazimchurch.app.ui.theme.PerazimTheme;

import java.util.ArrayList;
import java.util.List;

/**
 * The letter wheel. Drag across letters to spell a word; lifting the finger reports the word.
 * Each letter is picked once per swipe. The word being swiped shows in the centre.
 */
final class LetterWheelView extends View {

    /** Receives every finished swipe, including ones that match nothing. */
    interface Listener {
        void onSwipe(String word);
        /** Called when a letter joins the swipe, for haptics and sound. */
        void onLetter();
    }

    private final Paint ring = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint disc = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint trail = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint glowTrail = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final List<Character> letters = new ArrayList<>();
    private final List<Integer> path = new ArrayList<>();
    private float[] cx = new float[0];
    private float[] cy = new float[0];
    private float discPx;
    private Listener listener;
    private float pulse;
    private boolean tapMode;

    LetterWheelView(Context context) {
        super(context);
        ring.setStyle(Paint.Style.STROKE);
        ring.setStrokeWidth(dp(2));
        trail.setStyle(Paint.Style.STROKE);
        trail.setStrokeWidth(dp(6));
        trail.setStrokeCap(Paint.Cap.ROUND);
        trail.setColor(PerazimTheme.COLOR_ORANGE_SOFT);
        glowTrail.setStyle(Paint.Style.STROKE);
        glowTrail.setStrokeWidth(dp(16));
        glowTrail.setStrokeCap(Paint.Cap.ROUND);
        glowTrail.setColor(PerazimTheme.COLOR_ORANGE_SOFT);
        text.setTextAlign(Paint.Align.CENTER);
        text.setTypeface(Typeface.DEFAULT_BOLD);
        setContentDescription("Letter wheel. Drag across letters to spell a word.");
    }

    void setListener(Listener listener) { this.listener = listener; }

    /** Shows these letters in this order on the ring. */
    void setLetters(List<Character> order) {
        letters.clear();
        letters.addAll(order);
        path.clear();
        layoutLetters();
        invalidate();
    }

    boolean toggleTapMode() {
        tapMode = !tapMode;
        path.clear();
        invalidate();
        setContentDescription(tapMode ? "Tap letters to spell a word" : "Swipe letters to spell a word");
        return tapMode;
    }

    void submit() {
        StringBuilder word = new StringBuilder();
        for (int index : path) word.append(letters.get(index));
        path.clear();
        invalidate();
        if (listener != null && word.length() > 0) listener.onSwipe(word.toString());
    }

    /** Short glow on the ring after a correct word. */
    void pulseRing() {
        ValueAnimator animator = ValueAnimator.ofFloat(1f, 0f);
        animator.setDuration(350);
        animator.addUpdateListener(a -> {
            pulse = (float) a.getAnimatedValue();
            invalidate();
        });
        animator.start();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        layoutLetters();
    }

    private void layoutLetters() {
        int n = letters.size();
        cx = new float[n];
        cy = new float[n];
        if (n == 0 || getWidth() == 0) return;
        float pad = dp(10);
        float d = dp(46);
        float radius = Math.min(getWidth(), getHeight()) / 2f - pad - d / 2f;
        double circumference = 2 * Math.PI * radius;
        if (circumference < n * d * 1.15) d = (float) (circumference / (n * 1.15));
        discPx = d;
        text.setTextSize(Math.min(sp(17), d * 0.42f));
        for (int i = 0; i < n; i++) {
            double angle = -Math.PI / 2 + i * 2 * Math.PI / n;
            cx[i] = getWidth() / 2f + (float) (radius * Math.cos(angle));
            cy[i] = getHeight() / 2f + (float) (radius * Math.sin(angle));
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int n = letters.size();
        if (n == 0) return;
        float ringRadius = Math.min(getWidth(), getHeight()) / 2f - dp(10) - discPx / 2f;
        ring.setColor(PerazimTheme.COLOR_STROKE);
        ring.setAlpha(255);
        canvas.drawCircle(getWidth() / 2f, getHeight() / 2f, ringRadius, ring);
        if (pulse > 0) {
            ring.setColor(PerazimTheme.COLOR_ORANGE_SOFT);
            ring.setAlpha((int) (200 * pulse));
            canvas.drawCircle(getWidth() / 2f, getHeight() / 2f, ringRadius, ring);
        }
        // Glowing trail: a wide soft stroke under the solid line, with a bright tip at the finger.
        for (int i = 1; i < path.size(); i++) {
            int a = path.get(i - 1);
            int b = path.get(i);
            glowTrail.setAlpha(90);
            canvas.drawLine(cx[a], cy[a], cx[b], cy[b], glowTrail);
            canvas.drawLine(cx[a], cy[a], cx[b], cy[b], trail);
        }
        if (!path.isEmpty()) {
            int tip = path.get(path.size() - 1);
            glowTrail.setStyle(Paint.Style.FILL);
            glowTrail.setAlpha(140);
            canvas.drawCircle(cx[tip], cy[tip], discPx * 0.42f, glowTrail);
            glowTrail.setStyle(Paint.Style.STROKE);
        }
        for (int i = 0; i < n; i++) {
            boolean picked = path.contains(i);
            disc.setColor(picked ? PerazimTheme.COLOR_ORANGE_BRAND
                    : (GlassKit.light() ? 0xB3FFFFFF : PerazimTheme.COLOR_SURFACE_2));
            canvas.drawCircle(cx[i], cy[i], discPx / 2f, disc);
            text.setTextSize(Math.min(sp(17), discPx * 0.42f));
            text.setColor(picked ? PerazimTheme.COLOR_BG_DEEP : GlassKit.text());
            float baseline = cy[i] - (text.descent() + text.ascent()) / 2f;
            canvas.drawText(String.valueOf(letters.get(i)), cx[i], baseline, text);
        }
        StringBuilder swiped = new StringBuilder();
        for (int index : path) swiped.append(letters.get(index));
        if (swiped.length() > 0) {
            text.setColor(PerazimTheme.COLOR_ORANGE_SOFT);
            text.setTextSize(sp(18));
            canvas.drawText(swiped.toString(), getWidth() / 2f, getHeight() / 2f + dp(6), text);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                getParent().requestDisallowInterceptTouchEvent(true);
                if (!tapMode) path.clear();
                addAt(event.getX(), event.getY());
                return true;
            case MotionEvent.ACTION_MOVE:
                if (tapMode) return true;
                addAt(event.getX(), event.getY());
                return true;
            case MotionEvent.ACTION_UP:
                getParent().requestDisallowInterceptTouchEvent(false);
                if (!tapMode) submit();
                return true;
            case MotionEvent.ACTION_CANCEL:
                getParent().requestDisallowInterceptTouchEvent(false);
                path.clear();
                invalidate();
                return true;
            default:
                return super.onTouchEvent(event);
        }
    }

    private void addAt(float x, float y) {
        int hit = -1;
        float reach = discPx / 2f;
        for (int i = 0; i < letters.size(); i++) {
            float dx = x - cx[i];
            float dy = y - cy[i];
            if (dx * dx + dy * dy <= reach * reach) {
                hit = i;
                break;
            }
        }
        if (hit < 0) return;
        if (tapMode && !path.isEmpty() && path.get(path.size() - 1) == hit) {
            path.remove(path.size() - 1);
            invalidate();
            return;
        }
        if (path.contains(hit)) return;
        path.add(hit);
        performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
        if (listener != null) listener.onLetter();
        invalidate();
    }

    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
    private float sp(int value) { return value * getResources().getDisplayMetrics().scaledDensity; }
}
