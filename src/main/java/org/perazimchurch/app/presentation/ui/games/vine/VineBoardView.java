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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * The crossword grid, drawn on a Canvas. Solved letters show; revealed hint letters are orange.
 * A found word's letters fly up from the wheel into their cells, and a finished level bursts.
 */
final class VineBoardView extends View {

    private static final int MAX_CELL_DP = 44;
    private static final int BURST_PARTICLES = 28;

    /** One letter in flight: where it lands and when its flight starts (0 to 1 over the word). */
    private static final class Flight {
        final char letter;
        final int row;
        final int col;
        final float start;

        Flight(char letter, int row, int col, float start) {
            this.letter = letter;
            this.row = row;
            this.col = col;
            this.start = start;
        }
    }

    private final VineWordsSession session;
    private final Paint cell = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint letter = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint glow = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint particle = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Set<Long> glowCells = new HashSet<>();
    private final Set<Long> flyingCells = new HashSet<>();
    private final List<Flight> flights = new ArrayList<>();
    private final RectF box = new RectF();
    private final Random random = new Random();
    private float glowAlpha;
    private float flyProgress = -1f;
    private float burstProgress = -1f;
    private float[] burstX = new float[0];
    private float[] burstY = new float[0];
    private float[] burstDx = new float[0];
    private float[] burstDy = new float[0];
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
        particle.setStyle(Paint.Style.FILL);
        setContentDescription("Crossword grid");
    }

    /** Lights up the cells of a word and flies its letters in from the bottom of the grid. */
    void flyWord(String word) {
        glowCells.clear();
        flights.clear();
        flyingCells.clear();
        for (CrosswordGrid.Slot slot : session.level().grid().slots()) {
            if (!slot.word.equals(word)) continue;
            int count = slot.word.length();
            for (int i = 0; i < count; i++) {
                long key = cellKey(slot.rowAt(i), slot.colAt(i));
                glowCells.add(key);
                flyingCells.add(key);
                flights.add(new Flight(slot.word.charAt(i), slot.rowAt(i), slot.colAt(i), i * 0.08f));
            }
            break;
        }
        ValueAnimator animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(520);
        animator.addUpdateListener(a -> {
            flyProgress = (float) a.getAnimatedValue();
            glowAlpha = 1f - flyProgress;
            invalidate();
        });
        animator.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override public void onAnimationEnd(android.animation.Animator animation) {
                flyProgress = -1f;
                flights.clear();
                flyingCells.clear();
                invalidate();
            }
        });
        animator.start();
    }

    /** Celebration when the level is cleared: particles burst out of the grid, then onDone runs. */
    void burst(Runnable onDone) {
        int count = BURST_PARTICLES;
        burstX = new float[count];
        burstY = new float[count];
        burstDx = new float[count];
        burstDy = new float[count];
        float cx = getWidth() / 2f;
        float cy = getHeight() / 2f;
        for (int i = 0; i < count; i++) {
            burstX[i] = cx;
            burstY[i] = cy;
            double angle = random.nextDouble() * Math.PI * 2;
            float speed = dp(120) + random.nextFloat() * dp(160);
            burstDx[i] = (float) Math.cos(angle) * speed;
            burstDy[i] = (float) Math.sin(angle) * speed;
        }
        ValueAnimator animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(900);
        animator.addUpdateListener(a -> {
            burstProgress = (float) a.getAnimatedValue();
            invalidate();
        });
        animator.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override public void onAnimationEnd(android.animation.Animator animation) {
                burstProgress = -1f;
                invalidate();
                if (onDone != null) onDone.run();
            }
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
                if (glowAlpha > 0 && glowCells.contains(cellKey(row, col))) {
                    glow.setAlpha((int) (150 * glowAlpha));
                    canvas.drawRoundRect(box, radius, radius, glow);
                }
                char ch = session.knownLetter(row, col);
                if (ch != 0 && !flyingCells.contains(cellKey(row, col))) {
                    letter.setColor(session.hintedCell(row, col) ? PerazimTheme.COLOR_ORANGE_SOFT : GlassKit.text());
                    float baseline = y + cellPx / 2f - (letter.descent() + letter.ascent()) / 2f;
                    canvas.drawText(String.valueOf(ch), x + cellPx / 2f, baseline, letter);
                }
            }
        }
        drawFlights(canvas);
        drawBurst(canvas);
    }

    private void drawFlights(Canvas canvas) {
        if (flyProgress < 0 || flights.isEmpty()) return;
        float startX = getWidth() / 2f;
        float startY = getHeight() - cellPx / 2f;
        letter.setColor(GlassKit.text());
        for (Flight f : flights) {
            float t = clamp((flyProgress - f.start) / 0.6f);
            float eased = 1f - (1f - t) * (1f - t);
            float tx = left + f.col * cellPx + cellPx / 2f;
            float ty = f.row * cellPx + cellPx / 2f;
            float x = startX + (tx - startX) * eased;
            float y = startY + (ty - startY) * eased;
            float baseline = y - (letter.descent() + letter.ascent()) / 2f;
            // A soft glow behind the flying letter makes it read as the word being sent.
            glow.setAlpha((int) (120 * (1f - t)));
            canvas.drawCircle(x, y, cellPx * 0.45f, glow);
            canvas.drawText(String.valueOf(f.letter), x, baseline, letter);
        }
    }

    private void drawBurst(Canvas canvas) {
        if (burstProgress < 0) return;
        float fade = 1f - burstProgress;
        for (int i = 0; i < burstX.length; i++) {
            float x = burstX[i] + burstDx[i] * burstProgress;
            float y = burstY[i] + burstDy[i] * burstProgress + dp(120) * burstProgress * burstProgress;
            particle.setColor(i % 2 == 0 ? PerazimTheme.COLOR_ORANGE_SOFT : PerazimTheme.COLOR_ORANGE_BRAND);
            particle.setAlpha((int) (255 * fade));
            canvas.drawCircle(x, y, dp(4) * (0.6f + 0.4f * fade), particle);
        }
    }

    private static float clamp(float v) { return v < 0 ? 0 : Math.min(v, 1f); }

    private static long cellKey(int row, int col) { return ((long) row << 32) | (col & 0xffffffffL); }

    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
    private float sp(int value) { return value * getResources().getDisplayMetrics().scaledDensity; }
}
