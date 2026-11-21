package org.perazimchurch.app.presentation.ui.games.lampslide;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;

import org.perazimchurch.app.domain.games.lampslide.LampSlidePuzzle;

/**
 * Draws the Lamp Slide board from one picture cut into tiles. The picture is decoded once,
 * downsampled by the caller, and every draw reuses the same Rect and RectF objects so the frame
 * loop does not allocate. Slides animate over 140 ms and the picture fades to full colour on
 * completion.
 */
public final class LampSlideBoardView extends View {

    /** Called when the player taps a tile. The screen decides whether the move is legal. */
    public interface Listener {
        void onTileTapped(int index);
    }

    private static final long SLIDE_MS = 140L;
    private static final long FADE_MS = 300L;
    private static final float MUTED_SATURATION = 0.35f;

    private LampSlidePuzzle puzzle;
    private Bitmap picture;
    private Listener listener;

    private final Paint tilePaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final Paint gapPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint outlinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Rect src = new Rect();
    private final RectF dst = new RectF();
    private final ColorMatrix matrix = new ColorMatrix();
    private ColorMatrixColorFilter muted;
    private float mutedFor = -1f;

    private float saturation = MUTED_SATURATION;
    private int slideFrom = -1;
    private int slideTo = -1;
    private float slideProgress = 1f;
    private ValueAnimator slideAnimator;
    private ValueAnimator fadeAnimator;
    private boolean peeking;
    private int highlight = -1;
    private int downIndex = -1;
    private final float density;

    public LampSlideBoardView(Context context) { this(context, null); }

    public LampSlideBoardView(Context context, AttributeSet attrs) {
        super(context, attrs);
        density = context.getResources().getDisplayMetrics().density;
        gapPaint.setColor(0x33FFFFFF);
        outlinePaint.setStyle(Paint.Style.STROKE);
        outlinePaint.setStrokeWidth(2f * density);
        outlinePaint.setColor(0x66FFFFFF);
        setLayerType(LAYER_TYPE_HARDWARE, null);
    }

    public void setListener(Listener listener) { this.listener = listener; }

    /** Sets the board to show. The picture is shared, not copied, so the caller owns its life. */
    public void setBoard(LampSlidePuzzle board, Bitmap pictureBitmap) {
        this.puzzle = board;
        this.picture = pictureBitmap;
        this.saturation = board != null && board.solved() ? 1f : MUTED_SATURATION;
        this.slideFrom = -1;
        this.slideTo = -1;
        this.slideProgress = 1f;
        this.highlight = -1;
        invalidate();
    }

    /**
     * Animates a tile from one index to another. Call it after the puzzle has changed.
     * from is where the tile was, to is where it is now.
     */
    public void animateSlide(int from, int to) {
        if (slideAnimator != null) slideAnimator.cancel();
        slideFrom = from;
        slideTo = to;
        slideAnimator = ValueAnimator.ofFloat(0f, 1f);
        slideAnimator.setDuration(SLIDE_MS);
        slideAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        slideAnimator.addUpdateListener(a -> {
            slideProgress = (float) a.getAnimatedValue();
            invalidate();
        });
        slideAnimator.start();
    }

    /** Fades the picture to full colour. Call when the board is solved. */
    public void celebrate() {
        if (fadeAnimator != null) fadeAnimator.cancel();
        fadeAnimator = ValueAnimator.ofFloat(saturation, 1f);
        fadeAnimator.setDuration(FADE_MS);
        fadeAnimator.addUpdateListener(a -> {
            saturation = (float) a.getAnimatedValue();
            invalidate();
        });
        fadeAnimator.start();
    }

    /** Shows the whole picture in full colour while peeking. */
    public void setPeeking(boolean on) {
        peeking = on;
        invalidate();
    }

    /** Outlines one tile, for the "One move" hint. Pass -1 to clear. */
    public void setHighlight(int index) {
        highlight = index;
        invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int size = Math.min(getDefaultSize(0, widthMeasureSpec), getDefaultSize(0, heightMeasureSpec));
        int width = MeasureSpecHelper.resolve(widthMeasureSpec, size);
        setMeasuredDimension(width, width);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (puzzle == null || picture == null) return;
        int n = puzzle.size();
        float side = Math.min(getWidth(), getHeight());
        float cell = side / n;
        float inset = 2f * density;
        float corner = 8f * density;
        tilePaint.setColorFilter(saturation >= 1f ? null : mutedFilter());
        if (peeking) {
            tilePaint.setColorFilter(null);
            src.set(0, 0, picture.getWidth(), picture.getHeight());
            dst.set(0, 0, side, side);
            canvas.drawBitmap(picture, src, dst, tilePaint);
            return;
        }
        int tileW = picture.getWidth() / n;
        int tileH = picture.getHeight() / n;
        int gap = puzzle.gapIndex();
        for (int index = 0; index < n * n; index++) {
            if (index == gap || index == slideTo) continue;
            drawTile(canvas, index, puzzle.tileAt(index), cell, inset, corner, tileW, tileH);
        }
        if (slideTo >= 0 && slideFrom >= 0) {
            int value = puzzle.tileAt(slideTo);
            float fromCol = slideFrom % n;
            float fromRow = slideFrom / n;
            float toCol = slideTo % n;
            float toRow = slideTo / n;
            float col = fromCol + (toCol - fromCol) * slideProgress;
            float row = fromRow + (toRow - fromRow) * slideProgress;
            drawTileAt(canvas, value, col * cell, row * cell, cell, inset, corner, tileW, tileH);
        }
        setCell(dst, gap, cell, inset);
        canvas.drawRoundRect(dst, corner, corner, gapPaint);
        if (highlight >= 0 && highlight != gap) {
            setCell(dst, highlight, cell, inset);
            canvas.drawRoundRect(dst, corner, corner, outlinePaint);
        }
    }

    private void drawTile(Canvas canvas, int index, int value, float cell, float inset, float corner,
                          int tileW, int tileH) {
        int n = puzzle.size();
        drawTileAt(canvas, value, (index % n) * cell, (index / n) * cell, cell, inset, corner, tileW, tileH);
    }

    private void drawTileAt(Canvas canvas, int value, float x, float y, float cell, float inset,
                            float corner, int tileW, int tileH) {
        if (value <= 0) return;
        int n = puzzle.size();
        int source = value - 1;
        int sc = source % n;
        int sr = source / n;
        src.set(sc * tileW, sr * tileH, sc * tileW + tileW, sr * tileH + tileH);
        dst.set(x + inset, y + inset, x + cell - inset, y + cell - inset);
        canvas.drawBitmap(picture, src, dst, tilePaint);
    }

    private void setCell(RectF out, int index, float cell, float inset) {
        int n = puzzle.size();
        float x = (index % n) * cell;
        float y = (index / n) * cell;
        out.set(x + inset, y + inset, x + cell - inset, y + cell - inset);
    }

    /** Reuses one colour filter; it is rebuilt only when the saturation changes (during the fade). */
    private ColorMatrixColorFilter mutedFilter() {
        if (muted == null || mutedFor != saturation) {
            matrix.setSaturation(saturation);
            muted = new ColorMatrixColorFilter(matrix);
            mutedFor = saturation;
        }
        return muted;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (puzzle == null) return super.onTouchEvent(event);
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                downIndex = indexAt(event.getX(), event.getY());
                return true;
            case MotionEvent.ACTION_UP:
                int index = downIndex;
                downIndex = -1;
                if (index >= 0 && listener != null) {
                    listener.onTileTapped(index);
                    performClick();
                }
                return true;
            case MotionEvent.ACTION_CANCEL:
                downIndex = -1;
                return true;
            default:
                return true;
        }
    }

    private int indexAt(float x, float y) {
        float side = Math.min(getWidth(), getHeight());
        if (side <= 0 || x < 0 || y < 0 || x >= side || y >= side) return -1;
        int n = puzzle.size();
        int col = (int) (x / (side / n));
        int row = (int) (y / (side / n));
        return Math.min(row, n - 1) * n + Math.min(col, n - 1);
    }

    /** Square side that respects the parent's measure spec. */
    private static final class MeasureSpecHelper {
        static int resolve(int spec, int size) {
            int mode = View.MeasureSpec.getMode(spec);
            int available = View.MeasureSpec.getSize(spec);
            if (mode == View.MeasureSpec.EXACTLY) return available;
            if (mode == View.MeasureSpec.AT_MOST) return Math.min(available, size);
            return size;
        }
    }
}
