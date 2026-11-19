package org.perazimchurch.app.ui.theme;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.view.View;
import android.widget.FrameLayout;

/**
 * Real frosted glass over a photo: a pane that shows a blurred copy of the photo lying behind it,
 * then a tint and a light rim, like CSS {@code backdrop-filter: blur()}.
 *
 * <p>The pane must be a direct child of the frame that holds the photo, and the photo must fill that frame
 * with centre-crop scaling (vertical focus {@code focusY}, 0 = top, 0.5 = centre). The blur is computed once
 * in software, so it works on every Android version.
 */
public class FrostPane extends FrameLayout {

    private final Bitmap blurred;
    private final float focusY;
    private final float radius;
    private final Paint bitmapPaint = new Paint(Paint.FILTER_BITMAP_FLAG | Paint.ANTI_ALIAS_FLAG);
    private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint rimPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path clip = new Path();
    private final RectF rect = new RectF();
    private final Matrix matrix = new Matrix();

    /**
     * @param photo   the photo shown behind the pane (not modified)
     * @param focusY  the photo's vertical crop focus, as used for the photo itself
     * @param radiusPx corner radius
     * @param tint    colour laid over the blur, e.g. rgba(30,10,40,0.45)
     * @param rim     1dp border colour
     */
    public FrostPane(Context context, Bitmap photo, float focusY, float radiusPx, int tint, int rim) {
        super(context);
        this.blurred = photo == null ? null : blur(photo);
        this.focusY = focusY;
        this.radius = radiusPx;
        setWillNotDraw(false);
        fillPaint.setColor(tint);
        rimPaint.setStyle(Paint.Style.STROKE);
        rimPaint.setStrokeWidth(Math.max(1f, context.getResources().getDisplayMetrics().density));
        rimPaint.setColor(rim);
        GlassKit.keep(this);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int w = getWidth();
        int h = getHeight();
        rect.set(0, 0, w, h);
        clip.reset();
        clip.addRoundRect(rect, radius, radius, Path.Direction.CW);
        canvas.save();
        canvas.clipPath(clip);
        View parent = getParent() instanceof View ? (View) getParent() : null;
        if (blurred != null && parent != null && parent.getWidth() > 0 && parent.getHeight() > 0) {
            int pw = parent.getWidth();
            int ph = parent.getHeight();
            float scale = Math.max(pw / (float) blurred.getWidth(), ph / (float) blurred.getHeight());
            float dx = (pw - blurred.getWidth() * scale) / 2f;
            float dy = (ph - blurred.getHeight() * scale) * focusY;
            matrix.reset();
            matrix.setScale(scale, scale);
            matrix.postTranslate(dx - getLeft(), dy - getTop());
            canvas.drawBitmap(blurred, matrix, bitmapPaint);
        }
        canvas.drawRect(rect, fillPaint);
        canvas.restore();
        float inset = rimPaint.getStrokeWidth() / 2f;
        rect.inset(inset, inset);
        canvas.drawRoundRect(rect, radius, radius, rimPaint);
        super.onDraw(canvas);
    }

    /** Downscale, box-blur three times, keep small: drawn back with bilinear filtering it reads as a soft blur. */
    static Bitmap blur(Bitmap src) {
        int w = Math.max(8, src.getWidth() / 12);
        int h = Math.max(8, src.getHeight() / 12);
        Bitmap small = Bitmap.createScaledBitmap(src, w, h, true);
        int[] px = new int[w * h];
        small.getPixels(px, 0, w, 0, 0, w, h);
        for (int pass = 0; pass < 3; pass++) {
            px = boxBlur(px, w, h, 2);
        }
        Bitmap out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        out.setPixels(px, 0, w, 0, 0, w, h);
        if (small != src) {
            small.recycle();
        }
        return out;
    }

    private static int[] boxBlur(int[] in, int w, int h, int r) {
        int[] tmp = new int[in.length];
        int[] out = new int[in.length];
        // horizontal
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int rs = 0, gs = 0, bs = 0, n = 0;
                for (int k = -r; k <= r; k++) {
                    int xx = Math.min(w - 1, Math.max(0, x + k));
                    int c = in[y * w + xx];
                    rs += Color.red(c);
                    gs += Color.green(c);
                    bs += Color.blue(c);
                    n++;
                }
                tmp[y * w + x] = Color.rgb(rs / n, gs / n, bs / n);
            }
        }
        // vertical
        for (int x = 0; x < w; x++) {
            for (int y = 0; y < h; y++) {
                int rs = 0, gs = 0, bs = 0, n = 0;
                for (int k = -r; k <= r; k++) {
                    int yy = Math.min(h - 1, Math.max(0, y + k));
                    int c = tmp[yy * w + x];
                    rs += Color.red(c);
                    gs += Color.green(c);
                    bs += Color.blue(c);
                    n++;
                }
                out[y * w + x] = Color.rgb(rs / n, gs / n, bs / n);
            }
        }
        return out;
    }
}
