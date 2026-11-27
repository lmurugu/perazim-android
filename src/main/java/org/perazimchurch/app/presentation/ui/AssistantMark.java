package org.perazimchurch.app.presentation.ui;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.os.SystemClock;
import android.view.View;
import android.widget.ImageView;

import org.perazimchurch.app.R;

/** Monochrome rendering of the church mark and a live, original listening animation. */
public final class AssistantMark {
    private AssistantMark() { }

    public static ImageView image(Context context, boolean whiteInk) {
        ImageView logo = new ImageView(context);
        logo.setImageResource(R.drawable.perazim_logo_black);
        logo.setScaleType(ImageView.ScaleType.FIT_CENTER);
        logo.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        float ink = whiteInk ? 255 : 0;
        logo.setColorFilter(new ColorMatrixColorFilter(new float[]{
                0, 0, 0, 0, ink,
                0, 0, 0, 0, ink,
                0, 0, 0, 0, ink,
                1.1f, 1.35f, 0.28f, 0, -22
        }));
        return logo;
    }

    static final class Orb extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF ring = new RectF();
        private float level;
        private boolean running;

        Orb(Context context) { super(context); }

        void setLevel(float rms) {
            level = 0.65f * level + 0.35f * Math.max(0f, Math.min(1f, (rms + 3f) / 13f));
            invalidate();
        }

        void animateListening(boolean animate) {
            running = animate && (android.os.Build.VERSION.SDK_INT < 26 || ValueAnimator.areAnimatorsEnabled());
            invalidate();
        }

        @Override protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float x = getWidth() / 2f;
            float y = getHeight() / 2f;
            float seconds = running ? (SystemClock.uptimeMillis() % 16_000) / 1000f : 0f;
            float radius = Math.min(x, y) * (0.43f + 0.07f * level);
            paint.setStyle(Paint.Style.FILL);
            paint.setShader(new RadialGradient(x, y, radius * 1.85f,
                    new int[]{0x88949494, 0x2CFFFFFF, Color.TRANSPARENT},
                    new float[]{0, .57f, 1}, Shader.TileMode.CLAMP));
            canvas.drawCircle(x, y, radius * 1.85f, paint);
            paint.setShader(null);
            paint.setColor(0xff111111);
            canvas.drawCircle(x, y, radius, paint);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(Math.max(1.5f, radius * .035f));
            for (int i = 0; i < 3; i++) {
                float spread = radius * (1f + .08f * i);
                ring.set(x - spread, y - spread, x + spread, y + spread);
                paint.setColor(i == 0 ? Color.WHITE : 0x778E8E8E);
                canvas.drawArc(ring, seconds * (i % 2 == 0 ? 45 : -33) + i * 76, 184, false, paint);
            }
            if (running) postInvalidateOnAnimation();
        }
    }
}
