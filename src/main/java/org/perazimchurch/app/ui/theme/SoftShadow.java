package org.perazimchurch.app.ui.theme;

import android.graphics.Bitmap;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Outline;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Region;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.HorizontalScrollView;
import android.widget.ScrollView;

/**
 * A CSS-style {@code box-shadow: 0 dy blur color} drawn behind a view's own background.
 *
 * <p>Android elevation shadows are grey, weak and show through translucent glass, so the mockups' soft
 * drop shadows and orange glows are drawn here instead: a blurred rounded shape rendered once in software
 * (small, then scaled up, so it is cheap and works on every Android version), drawn outside the view's
 * bounds and never underneath the view itself. The view's parents are told not to clip it.
 */
public final class SoftShadow extends Drawable implements Drawable.Callback {

    private static final float SCALE = 1f / 3f;

    private final Drawable face;
    private final int color;
    private final float dy;
    private final float blur;
    private final float radius;
    private final Paint bitmapPaint = new Paint(Paint.FILTER_BITMAP_FLAG | Paint.ANTI_ALIAS_FLAG);
    private final Path cutout = new Path();
    private final RectF rect = new RectF();
    private Bitmap cache;
    private int cacheW = -1;
    private int cacheH = -1;
    private int pad;
    private boolean unclipped;

    /**
     * @param face     the view's real background (may be null)
     * @param color    shadow colour including alpha, e.g. rgba(225,125,47,0.4)
     * @param dyPx     downward offset
     * @param blurPx   CSS blur radius
     * @param radiusPx corner radius of the shape; use a huge value for pills and circles
     */
    public SoftShadow(Drawable face, int color, float dyPx, float blurPx, float radiusPx) {
        this.face = face;
        this.color = color;
        this.dy = dyPx;
        this.blur = Math.max(1f, blurPx);
        this.radius = radiusPx;
        bitmapPaint.setColor(color);
        if (face != null) {
            face.setCallback(this);
        }
    }

    public Drawable getFace() {
        return face;
    }

    @Override
    protected void onBoundsChange(Rect bounds) {
        if (face != null) {
            face.setBounds(bounds);
        }
    }

    @Override
    public void draw(Canvas canvas) {
        Rect b = getBounds();
        int w = b.width();
        int h = b.height();
        if (w > 0 && h > 0 && Color.alpha(color) > 0) {
            ensureCache(w, h);
            if (cache != null) {
                float r = Math.min(radius, Math.min(w, h) / 2f);
                rect.set(b);
                cutout.reset();
                cutout.addRoundRect(rect, r, r, Path.Direction.CW);
                int save = canvas.save();
                // Like CSS, the shadow never shows underneath the element itself (glass stays clean).
                if (Build.VERSION.SDK_INT >= 26) {
                    canvas.clipOutPath(cutout);
                } else {
                    canvas.clipPath(cutout, Region.Op.DIFFERENCE);
                }
                canvas.translate(b.left - pad, b.top - pad + dy);
                canvas.scale(1f / SCALE, 1f / SCALE);
                canvas.drawBitmap(cache, 0, 0, bitmapPaint);
                canvas.restoreToCount(save);
            }
            unclipParentsOnce();
        }
        if (face != null) {
            face.draw(canvas);
        }
    }

    private void ensureCache(int w, int h) {
        if (cache != null && w == cacheW && h == cacheH) {
            return;
        }
        cacheW = w;
        cacheH = h;
        pad = (int) Math.ceil(blur * 1.2f);
        int bw = Math.max(1, Math.round((w + pad * 2) * SCALE));
        int bh = Math.max(1, Math.round((h + pad * 2) * SCALE));
        try {
            Bitmap bmp = Bitmap.createBitmap(bw, bh, Bitmap.Config.ALPHA_8);
            Canvas c = new Canvas(bmp);
            Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
            p.setColor(Color.BLACK);
            // CSS blur radius B is a Gaussian with sigma B/2; BlurMaskFilter's radius is about sigma * 1.7.
            float blurRadius = Math.max(0.5f, blur * 0.5f * 1.7f * SCALE);
            p.setMaskFilter(new BlurMaskFilter(blurRadius, BlurMaskFilter.Blur.NORMAL));
            float r = Math.min(radius, Math.min(w, h) / 2f) * SCALE;
            RectF shape = new RectF(pad * SCALE, pad * SCALE, (pad + w) * SCALE, (pad + h) * SCALE);
            c.drawRoundRect(shape, r, r, p);
            if (cache != null) {
                cache.recycle();
            }
            cache = bmp;
        } catch (OutOfMemoryError e) {
            cache = null;
        }
    }

    /** Let the shadow spill outside the view, as CSS does, without unclipping any scrolling container. */
    private void unclipParentsOnce() {
        if (unclipped) {
            return;
        }
        unclipped = true;
        Callback cb = getCallback();
        if (!(cb instanceof View)) {
            return;
        }
        final View view = (View) cb;
        view.post(() -> unclip(view));
    }

    static void unclip(View view) {
        ViewParent parent = view.getParent();
        for (int level = 0; level < 3 && parent instanceof ViewGroup; level++) {
            if (parent instanceof ScrollView || parent instanceof HorizontalScrollView) {
                return;
            }
            ViewGroup group = (ViewGroup) parent;
            if (group.getClipChildren()) {
                group.setClipChildren(false);
            }
            if (group.getClipToPadding()) {
                group.setClipToPadding(false);
            }
            parent = group.getParent();
        }
    }

    // ------------------------------------------------------------------ delegate to the face

    @Override
    public boolean getPadding(Rect padding) {
        return face != null ? face.getPadding(padding) : super.getPadding(padding);
    }

    @Override
    public void getOutline(Outline outline) {
        if (face != null) {
            face.getOutline(outline);
        } else {
            super.getOutline(outline);
        }
    }

    @Override
    public boolean isStateful() {
        return face != null && face.isStateful();
    }

    @Override
    protected boolean onStateChange(int[] state) {
        return face != null && face.setState(state);
    }

    @Override
    public void setAlpha(int alpha) {
        bitmapPaint.setAlpha(Color.alpha(color) * alpha / 255);
        if (face != null) {
            face.setAlpha(alpha);
        }
    }

    @Override
    public void setColorFilter(ColorFilter colorFilter) {
        if (face != null) {
            face.setColorFilter(colorFilter);
        }
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }

    @Override
    public void invalidateDrawable(Drawable who) {
        invalidateSelf();
    }

    @Override
    public void scheduleDrawable(Drawable who, Runnable what, long when) {
        scheduleSelf(what, when);
    }

    @Override
    public void unscheduleDrawable(Drawable who, Runnable what) {
        unscheduleSelf(what);
    }
}
