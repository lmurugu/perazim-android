package org.perazimchurch.app.presentation.ui.games.mosaic;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Rect;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.OvershootInterpolator;

import org.perazimchurch.app.presentation.ui.games.common.GameFx;

import org.perazimchurch.app.domain.games.catalog.StoryMosaicSession;
import org.perazimchurch.app.domain.games.engine.GameSession;
import org.perazimchurch.app.domain.games.mosaic.MosaicBoard;

/**
 * Drag-and-drop board for Story Mosaic. The scene is one bitmap; each piece is drawn by clipping
 * that bitmap to the piece's rectangle, so no per-piece bitmaps are made (safe on 2 GB phones).
 * With no scene file yet, pieces are drawn as soft coloured tiles with their number.
 */
public final class StoryMosaicView extends View {

    /** Called after each drop that changes the board. */
    public interface Listener {
        void onDrop(GameSession.Outcome outcome);
    }

    private static final int PURPLE_DEEP = Color.parseColor("#2D0938");
    private static final int PURPLE_MID = Color.parseColor("#681A7D");
    private static final int ORANGE = Color.parseColor("#E17D2F");
    private static final int GREEN = Color.parseColor("#7BD389");

    private final StoryMosaicSession session;
    private final Bitmap scene;
    private final Listener listener;
    private final float density;

    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint image = new Paint(Paint.FILTER_BITMAP_FLAG);
    private final Rect src = new Rect();
    private final RectF dst = new RectF();
    private final RectF[] trayRects;

    private float gridLeft;
    private float gridTop;
    private float cell;
    private float trayCell;
    private int dragPiece = -1;
    private int selectedPiece = -1;
    private int tapSlot = -1;
    private boolean moved;
    private float downX;
    private float downY;
    private float dragX;
    private float dragY;

    // A piece that was dropped right flies into its slot with a small overshoot.
    private int snapPiece = -1;
    private float snapFromX;
    private float snapFromY;
    private float snapToX;
    private float snapToY;
    private float snapT;
    private ValueAnimator snapAnim;

    public StoryMosaicView(Context context, StoryMosaicSession session, Bitmap scene, Listener listener) {
        super(context);
        this.session = session;
        this.scene = scene;
        this.listener = listener;
        this.density = getResources().getDisplayMetrics().density;
        int pieces = session.board().pieceCount();
        this.trayRects = new RectF[pieces];
        for (int i = 0; i < pieces; i++) trayRects[i] = new RectF();

        fill.setStyle(Paint.Style.FILL);
        fill.setColor(Color.argb(36, 255, 255, 255));
        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeWidth(1.5f * density);
        stroke.setColor(Color.argb(72, 255, 255, 255));
        text.setColor(Color.WHITE);
        text.setTextAlign(Paint.Align.CENTER);
        text.setTextSize(14f * density);
        setContentDescription("Story Mosaic board");
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        int side = session.board().side();
        float pad = 16f * density;
        float boardSize = Math.min(w - 2 * pad, h * 0.56f);
        cell = boardSize / side;
        gridLeft = (w - boardSize) / 2f;
        gridTop = pad;

        // Tray: shrink the tray cell until every piece fits under the board.
        float trayTop = gridTop + boardSize + pad;
        float trayHeight = h - trayTop - pad;
        int n = trayRects.length;
        trayCell = cell;
        while (trayCell > 12f * density) {
            int perRow = Math.max(1, (int) ((w - 2 * pad) / (trayCell + 6f * density)));
            int rows = (n + perRow - 1) / perRow;
            if (rows * (trayCell + 6f * density) <= trayHeight) break;
            trayCell -= 2f * density;
        }
        int perRow = Math.max(1, (int) ((w - 2 * pad) / (trayCell + 6f * density)));
        float gap = 6f * density;
        float rowWidth = perRow * (trayCell + gap) - gap;
        float startX = (w - rowWidth) / 2f;
        for (int i = 0; i < n; i++) {
            int col = i % perRow;
            int row = i / perRow;
            float left = startX + col * (trayCell + gap);
            float top = trayTop + row * (trayCell + gap);
            trayRects[i].set(left, top, left + trayCell, top + trayCell);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        MosaicBoard board = session.board();
        int side = board.side();
        float radius = 8f * density;

        // Empty slots, with a faint orange edge in guided levels.
        for (int slot = 0; slot < board.pieceCount(); slot++) {
            float left = gridLeft + board.column(slot) * cell;
            float top = gridTop + board.row(slot) * cell;
            dst.set(left + 2, top + 2, left + cell - 2, top + cell - 2);
            canvas.drawRoundRect(dst, radius, radius, fill);
            if (session.guided()) {
                stroke.setColor(Color.argb(140, Color.red(ORANGE), Color.green(ORANGE), Color.blue(ORANGE)));
            } else {
                stroke.setColor(Color.argb(72, 255, 255, 255));
            }
            canvas.drawRoundRect(dst, radius, radius, stroke);
        }

        // Placed pieces sit in their slots.
        for (int piece = 0; piece < board.pieceCount(); piece++) {
            if (!board.isPlaced(piece) || piece == snapPiece) continue;
            float left = gridLeft + board.column(piece) * cell;
            float top = gridTop + board.row(piece) * cell;
            dst.set(left + 2, top + 2, left + cell - 2, top + cell - 2);
            drawPiece(canvas, piece, side, dst);
        }

        // Pieces still in the tray (the one being dragged is drawn last).
        for (int piece = 0; piece < board.pieceCount(); piece++) {
            if (board.isPlaced(piece) || (piece == dragPiece && moved)) continue;
            dst.set(trayRects[piece]);
            drawPiece(canvas, piece, side, dst);
            if (piece == selectedPiece) {
                stroke.setColor(0xFFFFC66C);
                canvas.drawRoundRect(dst, radius, radius, stroke);
            }
        }
        if (snapPiece >= 0) {
            float t = snapT;
            float x = snapFromX + (snapToX - snapFromX) * t;
            float y = snapFromY + (snapToY - snapFromY) * t;
            float size = trayCell + (cell - 4f - trayCell) * Math.min(1f, Math.max(0f, t));
            float half = size / 2f;
            dst.set(x - half, y - half, x + half, y + half);
            drawPiece(canvas, snapPiece, side, dst);
        }
        if (dragPiece >= 0 && moved) {
            float half = trayCell / 2f;
            dst.set(dragX - half, dragY - half, dragX + half, dragY + half);
            drawPiece(canvas, dragPiece, side, dst);
        }
    }

    private void drawPiece(Canvas canvas, int piece, int side, RectF target) {
        if (scene != null) {
            int col = piece % side;
            int row = piece / side;
            int sw = scene.getWidth();
            int sh = scene.getHeight();
            src.set(col * sw / side, row * sh / side, (col + 1) * sw / side, (row + 1) * sh / side);
            canvas.drawBitmap(scene, src, target, image);
        } else {
            // Placeholder tile: a soft colour from the purple and orange family, with its number.
            float hue = 280f + (piece * 37f) % 80f;
            fill.setColor(Color.HSVToColor(new float[] {hue, 0.55f, 0.55f}));
            canvas.drawRoundRect(target, 8f * density, 8f * density, fill);
            text.setColor(Color.WHITE);
            canvas.drawText(String.valueOf(piece + 1), target.centerX(),
                    target.centerY() + text.getTextSize() / 3f, text);
            fill.setColor(Color.argb(36, 255, 255, 255));
        }
        if (session.board().isPlaced(piece)) {
            stroke.setColor(GREEN);
            canvas.drawRoundRect(target, 8f * density, 8f * density, stroke);
            stroke.setColor(Color.argb(72, 255, 255, 255));
        }
    }

    private void startSnap(int piece, float fromX, float fromY, int slot) {
        MosaicBoard board = session.board();
        snapPiece = piece;
        snapFromX = fromX;
        snapFromY = fromY;
        snapToX = gridLeft + board.column(slot) * cell + cell / 2f;
        snapToY = gridTop + board.row(slot) * cell + cell / 2f;
        snapT = 0f;
        if (snapAnim != null) snapAnim.cancel();
        snapAnim = ValueAnimator.ofFloat(0f, 1f);
        snapAnim.setDuration(GameFx.motionEnabled(getContext()) ? 220 : 0);
        snapAnim.setInterpolator(new OvershootInterpolator(1.2f));
        snapAnim.addUpdateListener(a -> {
            snapT = (Float) a.getAnimatedValue();
            invalidate();
        });
        snapAnim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                snapPiece = -1;
                invalidate();
            }
        });
        snapAnim.start();
    }

    @Override
    protected void onDetachedFromWindow() {
        if (snapAnim != null) snapAnim.cancel();
        super.onDetachedFromWindow();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (session.finished()) return false;
        float x = event.getX();
        float y = event.getY();
        int slot = session.board().slotAt(x - gridLeft, y - gridTop, cell);
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                dragPiece = -1;
                tapSlot = selectedPiece >= 0 ? slot : -1;
                downX = x;
                downY = y;
                moved = false;
                for (int i = 0; i < trayRects.length; i++) {
                    if (!session.board().isPlaced(i) && trayRects[i].contains(x, y)) {
                        dragPiece = i;
                        break;
                    }
                }
                return dragPiece >= 0 || tapSlot >= 0;
            case MotionEvent.ACTION_MOVE:
                if (dragPiece >= 0) {
                    moved |= Math.hypot(x - downX, y - downY) >= 7 * density;
                    dragX = x;
                    dragY = y;
                    invalidate();
                }
                return dragPiece >= 0 || tapSlot >= 0;
            case MotionEvent.ACTION_UP:
                if (tapSlot >= 0 && slot == tapSlot) {
                    place(selectedPiece, slot, trayRects[selectedPiece].centerX(), trayRects[selectedPiece].centerY());
                } else if (dragPiece >= 0 && moved && slot >= 0) {
                    place(dragPiece, slot, x, y);
                } else if (dragPiece >= 0 && !moved) {
                    selectedPiece = selectedPiece == dragPiece ? -1 : dragPiece;
                    performClick();
                }
                clearGesture();
                return true;
            case MotionEvent.ACTION_CANCEL:
                clearGesture();
                return true;
            default:
                return super.onTouchEvent(event);
        }
    }

    @Override public boolean performClick() { return super.performClick(); }

    private void place(int piece, int slot, float x, float y) {
        GameSession.Outcome outcome = session.answer(piece + ":" + slot);
        if (outcome == GameSession.Outcome.CORRECT) {
            selectedPiece = -1;
            startSnap(piece, x, y, slot);
            GameFx.correct(this);
        } else if (outcome == GameSession.Outcome.WRONG) {
            GameFx.wrong(this);
        }
        if (listener != null) listener.onDrop(outcome);
    }

    private void clearGesture() {
        dragPiece = -1;
        tapSlot = -1;
        moved = false;
        invalidate();
    }
}
