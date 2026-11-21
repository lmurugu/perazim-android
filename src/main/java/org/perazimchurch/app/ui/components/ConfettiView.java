package org.perazimchurch.app.ui.components;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.View;
import android.view.animation.LinearInterpolator;

import java.util.Random;

/** Lightweight confetti burst in the Perazim purple and orange. Removes itself when finished. */
final class ConfettiView extends View {

    private static final int[] COLORS = {
            0xFFE17D2F, 0xFFE8A86A, 0xFF8A2BA3, 0xFFA67BB3, 0xFFF5F0F7, 0xFF681A7D
    };
    private static final int COUNT = 90;
    private static final long DURATION_MS = 2400;

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final float[] x = new float[COUNT];
    private final float[] y = new float[COUNT];
    private final float[] vx = new float[COUNT];
    private final float[] vy = new float[COUNT];
    private final float[] size = new float[COUNT];
    private final int[] color = new int[COUNT];
    private boolean seeded = false;
    private float progress = 0f;

    ConfettiView(Context context) {
        super(context);
    }

    void start(final Runnable onDone) {
        ValueAnimator animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(DURATION_MS);
        animator.setInterpolator(new LinearInterpolator());
        animator.addUpdateListener(a -> {
            progress = (float) a.getAnimatedValue();
            invalidate();
            if (progress >= 1f) {
                onDone.run();
            }
        });
        animator.start();
    }

    private void seed(int w, int h) {
        Random r = new Random();
        float density = getResources().getDisplayMetrics().density;
        for (int i = 0; i < COUNT; i++) {
            x[i] = w / 2f + (r.nextFloat() - 0.5f) * w * 0.3f;
            y[i] = h * 0.35f;
            vx[i] = (r.nextFloat() - 0.5f) * w * 1.2f;
            vy[i] = -r.nextFloat() * h * 0.5f - h * 0.1f;
            size[i] = (4 + r.nextFloat() * 5) * density;
            color[i] = COLORS[r.nextInt(COLORS.length)];
        }
        seeded = true;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int w = getWidth();
        int h = getHeight();
        if (w == 0 || h == 0) {
            return;
        }
        if (!seeded) {
            seed(w, h);
        }
        float t = progress * DURATION_MS / 1000f;
        float gravity = h * 1.1f;
        int alpha = (int) (255 * Math.min(1f, (1f - progress) * 3f));
        for (int i = 0; i < COUNT; i++) {
            float px = x[i] + vx[i] * t;
            float py = y[i] + vy[i] * t + 0.5f * gravity * t * t;
            paint.setColor(color[i]);
            paint.setAlpha(alpha);
            canvas.save();
            canvas.rotate(t * 360f * (i % 2 == 0 ? 1 : -1), px, py);
            canvas.drawRect(px - size[i] / 2, py - size[i] / 4, px + size[i] / 2, py + size[i] / 4, paint);
            canvas.restore();
        }
    }
}
