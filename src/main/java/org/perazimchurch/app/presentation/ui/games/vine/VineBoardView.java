package org.perazimchurch.app.presentation.ui.games.vine;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.view.View;

import org.perazimchurch.app.domain.games.vine.CrosswordGrid;
import org.perazimchurch.app.domain.games.vine.VineWordsSession;
import org.perazimchurch.app.ui.theme.GlassKit;
import org.perazimchurch.app.ui.theme.PerazimTheme;

import java.util.HashSet;
import java.util.Set;

/** The crossword grid, drawn on a Canvas. Solved letters show; revealed hint letters are orange. */
final class VineBoardView extends View {

    private static final int MAX_CELL_DP = 44;

    private final VineWordsSession session;
    private final Paint cell = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint letter = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint glow = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Set<Long> glowCells = new HashSet<>();
    private final RectF box = new RectF();
    private float glowAlpha;
    private int cellPx;
    private int left;

    VineBoardView(Context context, VineWordsSession session) {
        super(context);
        this.session = session;
        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeWidth(dp(1));
        letter.setTypeface(Typeface.DEFAULT_BOLD);
        letter.setTextAlign(Paint.Align.CENTER);
        glow.setColor(PerazimTheme.COLOR_ORANGE_SOFT);
        setContentDescription("Crossword grid");
    }

    /** Lights up the cells of a word the player just solved. */
    void glowWord(String word) {
        glowCells.clear();
        for (CrosswordGrid.Slot slot : session.level().grid().slots()) {
            if (!slot.word.equals(word)) continue;
            for (int i = 0; i < slot.word.length(); i++) {
                glowCells.add((long) slot.rowAt(i) << 32 | slot.colAt(i));
            }
        }
        ValueAnimator animator = ValueAnimator.ofFloat(1f, 0f);
        animator.setDuration(450);
        animator.addUpdateListener(a -> {
            glowAlpha = (float) a.getAnimatedValue();
            invalidate();
        });
        animator.start();
    }

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        CrosswordGrid grid = session.level().grid();
        int width = MeasureSpec.getSize(widthSpec);
        cellPx = Math.min(width / grid.cols(), dp(MAX_CELL_DP));
        left = (width - cellPx * grid.cols()) / 2;
        setMeasuredDimension(width, cellPx * grid.rows());
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        CrosswordGrid grid = session.level().grid();
        float inset = dp(2);
        boolean light = GlassKit.light();
        letter.setTextSize(sp(17));
        for (int row = 0; row < grid.rows(); row++) {
            for (int col = 0; col < grid.cols(); col++) {
                if (!session.isLetterCell(row, col)) continue;
                float x = left + col * cellPx;
                float y = row * cellPx;
                box.set(x + inset, y + inset, x + cellPx - inset, y + cellPx - inset);
                float radius = dp(8);
                cell.setColor(light ? Color.argb(90, 255, 255, 255) : PerazimTheme.COLOR_SURFACE_2);
                canvas.drawRoundRect(box, radius, radius, cell);
                stroke.setColor(PerazimTheme.COLOR_STROKE);
                canvas.drawRoundRect(box, radius, radius, stroke);
                if (glowAlpha > 0 && glowCells.contains((long) row << 32 | col)) {
                    glow.setAlpha((int) (150 * glowAlpha));
                    canvas.drawRoundRect(box, radius, radius, glow);
                }
                char ch = session.knownLetter(row, col);
                if (ch != 0) {
                    boolean hinted = session.hintedCell(row, col);
                    letter.setColor(hinted ? PerazimTheme.COLOR_ORANGE_SOFT : GlassKit.text());
                    float baseline = y + cellPx / 2f - (letter.descent() + letter.ascent()) / 2f;
                    canvas.drawText(String.valueOf(ch), x + cellPx / 2f, baseline, letter);
                }
            }
        }
    }

    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
    private float sp(int value) { return value * getResources().getDisplayMetrics().scaledDensity; }
}
