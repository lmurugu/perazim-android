package org.perazimchurch.app.presentation.ui.games;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.view.MotionEvent;
import android.view.View;

import androidx.core.content.ContextCompat;

import org.perazimchurch.app.R;
import org.perazimchurch.app.domain.games.LadderOfFaith;
import org.perazimchurch.app.ui.theme.PerazimTheme;

/**
 * The Ladder of Faith drawn as a winding path from rung 1 at the bottom to the crown at the top.
 * Passed rungs are green, the current rung glows orange, later rungs are locked. Milestones show a chest.
 * Zones along the way follow the Bible's story: Garden, Exodus, Kings, Prophets, Gospels, Crown.
 */
final class LadderMapView extends View {
    interface OnClimb { void climb(); }

    private static final String[] ZONES = {"Garden", "Exodus", "Kings", "Prophets", "Gospels"};
    private static final float SPACING_DP = 96f;
    private static final float TOP_DP = 120f;
    private static final float BOTTOM_DP = 70f;

    private final float density;
    private final Paint pathPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint nodePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint ringPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pillText = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Drawable chest;
    private final Drawable lock;
    private final Drawable crown;
    private int current = 1;
    private int best;
    private boolean finished;
    private OnClimb onClimb;

    LadderMapView(Context context) {
        super(context);
        density = context.getResources().getDisplayMetrics().density;
        pathPaint.setStyle(Paint.Style.STROKE);
        pathPaint.setStrokeCap(Paint.Cap.ROUND);
        pathPaint.setStrokeWidth(dp(7));
        pathPaint.setColor(0x66FFFFFF);
        pathPaint.setPathEffect(new DashPathEffect(new float[]{dp(2), dp(14)}, 0));
        ringPaint.setStyle(Paint.Style.STROKE);
        textPaint.setColor(0xFFFFFFFF);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTypeface(Typeface.DEFAULT_BOLD);
        textPaint.setTextSize(dp(17));
        pillPaint.setColor(0x33FFFFFF);
        pillText.setColor(0xFFFFFFFF);
        pillText.setTextSize(dp(13));
        pillText.setTypeface(Typeface.DEFAULT_BOLD);
        chest = ContextCompat.getDrawable(context, R.drawable.ic_game_chest);
        lock = ContextCompat.getDrawable(context, R.drawable.ic_game_lock);
        crown = ContextCompat.getDrawable(context, R.drawable.ic_game_crown);
        setContentDescription("Ladder of Faith map");
    }

    /** current is the 1-based rung being played; best is the highest rung ever reached on this phone. */
    void setProgress(int current, int best, boolean finished) {
        this.current = Math.max(1, Math.min(current, LadderOfFaith.RUNGS));
        this.best = best;
        this.finished = finished;
        setContentDescription("Ladder of Faith map. Rung " + this.current + " of " + LadderOfFaith.RUNGS
                + (finished ? ", run complete." : ", tap the glowing rung to climb."));
        invalidate();
    }

    void setOnClimb(OnClimb listener) { onClimb = listener; }

    /** Vertical centre of a rung, for scrolling it into view. */
    float nodeY(int rung) {
        return dp(TOP_DP) + (LadderOfFaith.RUNGS - rung) * dp(SPACING_DP);
    }

    private float nodeX(int rung) {
        float w = getWidth();
        return (float) (w / 2f + Math.sin(rung * 0.95) * w * 0.28f);
    }

    @Override protected void onMeasure(int widthSpec, int heightSpec) {
        int width = MeasureSpec.getSize(widthSpec);
        int height = (int) (dp(TOP_DP) + (LadderOfFaith.RUNGS - 1) * dp(SPACING_DP) + dp(BOTTOM_DP));
        setMeasuredDimension(width, height);
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        // Read theme colours on every draw so the map follows the light/dark switch.
        int ink = PerazimTheme.COLOR_TEXT_PRIMARY;
        pathPaint.setColor(withAlpha(ink, 0x77));
        pillPaint.setColor(withAlpha(ink, 0x26));
        pillText.setColor(ink);
        // Path from the bottom rung up to the crown.
        Path path = new Path();
        path.moveTo(nodeX(1), nodeY(1));
        for (int r = 2; r <= LadderOfFaith.RUNGS; r++) {
            float midY = (nodeY(r - 1) + nodeY(r)) / 2f;
            path.cubicTo(nodeX(r - 1), midY, nodeX(r), midY, nodeX(r), nodeY(r));
        }
        path.lineTo(getWidth() / 2f, dp(46));
        canvas.drawPath(path, pathPaint);

        // Zone labels every four rungs.
        for (int z = 0; z < ZONES.length; z++) {
            int rung = z * 4 + 2;
            float y = nodeY(rung);
            boolean left = nodeX(rung) > getWidth() / 2f;
            float textW = pillText.measureText(ZONES[z]);
            float x = left ? dp(12) : getWidth() - dp(12) - textW - dp(20);
            RectF pill = new RectF(x, y - dp(14), x + textW + dp(20), y + dp(14));
            canvas.drawRoundRect(pill, dp(14), dp(14), pillPaint);
            canvas.drawText(ZONES[z], x + dp(10), y + dp(5), pillText);
        }

        // Crown at the top.
        int cs = (int) dp(64);
        int cx = (int) (getWidth() / 2f);
        crown.setBounds(cx - cs / 2, (int) dp(14), cx + cs / 2, (int) dp(14) + cs);
        if (!finished || current < LadderOfFaith.RUNGS) crown.setAlpha(150); else crown.setAlpha(255);
        crown.draw(canvas);

        for (int r = 1; r <= LadderOfFaith.RUNGS; r++) drawNode(canvas, r);
    }

    private void drawNode(Canvas canvas, int r) {
        float x = nodeX(r), y = nodeY(r);
        float radius = dp(r == current && !finished ? 30 : 24);
        boolean passed = r < current || (finished && r <= current);
        boolean here = r == current && !finished;
        int top, bottom;
        if (passed) { top = 0xFF86EB94; bottom = 0xFF169A45; }
        else if (here) { top = 0xFFFFC46B; bottom = 0xFFE0600A; }
        else { top = withAlpha(PerazimTheme.COLOR_TEXT_PRIMARY, 0x55); bottom = withAlpha(PerazimTheme.COLOR_TEXT_PRIMARY, 0x30); }
        if (here) {
            ringPaint.setColor(0x55FFB25A);
            ringPaint.setStrokeWidth(dp(10));
            canvas.drawCircle(x, y, radius + dp(8), ringPaint);
        }
        nodePaint.setShader(new LinearGradient(x, y - radius, x, y + radius, top, bottom, Shader.TileMode.CLAMP));
        canvas.drawCircle(x, y, radius, nodePaint);
        nodePaint.setShader(null);
        ringPaint.setColor(withAlpha(PerazimTheme.COLOR_TEXT_PRIMARY, 0x99));
        ringPaint.setStrokeWidth(dp(2.5f));
        canvas.drawCircle(x, y, radius, ringPaint);

        if (!passed && !here) {
            int s = (int) dp(26);
            lock.setBounds((int) x - s / 2, (int) y - s / 2, (int) x + s / 2, (int) y + s / 2);
            lock.draw(canvas);
        } else {
            canvas.drawText(String.valueOf(r), x, y + dp(6), textPaint);
        }
        if (r == best && best > 0 && r != current) {
            pillText.setTextSize(dp(11));
            canvas.drawText("Best", x - dp(14), y - radius - dp(6), pillText);
            pillText.setTextSize(dp(13));
        }
        if (LadderOfFaith.isMilestone(r) && r < LadderOfFaith.RUNGS) {
            int s = (int) dp(38);
            float side = x > getWidth() / 2f ? -radius - dp(30) : radius + dp(30);
            chest.setBounds((int) (x + side) - s / 2, (int) y - s / 2, (int) (x + side) + s / 2, (int) y + s / 2);
            chest.setAlpha(passed ? 255 : 170);
            chest.draw(canvas);
        }
    }

    @Override public boolean onTouchEvent(MotionEvent event) {
        if (finished || onClimb == null) return false;
        if (event.getAction() == MotionEvent.ACTION_DOWN) return true;
        if (event.getAction() == MotionEvent.ACTION_UP) {
            float dx = event.getX() - nodeX(current), dy = event.getY() - nodeY(current);
            if (dx * dx + dy * dy < dp(44) * dp(44)) { performClick(); onClimb.climb(); return true; }
        }
        return false;
    }

    @Override public boolean performClick() { return super.performClick(); }

    private static int withAlpha(int color, int alpha) { return (color & 0x00FFFFFF) | (alpha << 24); }

    private float dp(float v) { return v * density; }
}
