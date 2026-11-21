package org.perazimchurch.app.presentation.ui.games.lampslide;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.perazimchurch.app.domain.games.engine.GameSession;
import org.perazimchurch.app.domain.games.lampslide.LampSlideScene;
import org.perazimchurch.app.domain.games.lampslide.LampSlideSession;

/**
 * Play screen for one Lamp Slide run: status line, board, hint buttons and the verse banner
 * shown on completion. Builds a plain View so the hub can add it without changes to this file.
 * Everything works offline.
 */
public final class LampSlideScreen {

    private static final int PEEK_MS = 2000;
    private static final int HINT_MS = 1600;

    private LampSlideScreen() {}

    /**
     * @param level  1-based level, shown in the status line
     * @param onBack called when the player leaves the run
     * @param onNext called when the player taps Continue after a solved board
     */
    public static View create(Context context, LampSlideSession session, int level,
                              Runnable onBack, Runnable onNext) {
        LinearLayout page = new LinearLayout(context);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setGravity(Gravity.CENTER_HORIZONTAL);
        int pad = dp(context, 16);
        page.setPadding(pad, pad, pad, pad);

        TextView status = text(context, "", 14, false);
        page.addView(status);

        LampSlideBoardView board = new LampSlideBoardView(context);
        Bitmap picture = decode(context, session.scene());
        board.setBoard(session.puzzle(), picture);
        LinearLayout.LayoutParams boardParams = new LinearLayout.LayoutParams(-1, -2);
        boardParams.topMargin = dp(context, 12);
        boardParams.bottomMargin = dp(context, 12);
        page.addView(board, boardParams);

        LinearLayout controls = new LinearLayout(context);
        controls.setOrientation(LinearLayout.HORIZONTAL);
        page.addView(controls, new LinearLayout.LayoutParams(-1, -2));

        TextView banner = text(context, "", 16, false);
        banner.setVisibility(View.GONE);
        page.addView(banner);

        Button next = button(context, "Continue");
        next.setVisibility(View.GONE);
        page.addView(next);

        Runnable refresh = () -> {
            int moves = session.puzzle().moves();
            int par = session.puzzle().par();
            int cap = Math.max(1, 3 - session.hintsUsed());
            status.setText("Level " + level + " · moves " + moves + " / par " + par
                    + " · up to " + cap + " star" + (cap == 1 ? "" : "s"));
        };
        refresh.run();

        final Runnable[] afterMove = new Runnable[1];
        afterMove[0] = () -> {
            refresh.run();
            if (session.finished()) {
                board.celebrate();
                LampSlideScene scene = session.scene();
                banner.setText("“" + scene.verse() + "”\n— " + scene.reference()
                        + (scene.prayer().isEmpty() ? "" : "\n\n" + scene.prayer()));
                banner.setVisibility(View.VISIBLE);
                next.setVisibility(View.VISIBLE);
            }
        };

        board.setListener(index -> {
            int gapBefore = session.puzzle().gapIndex();
            if (session.tapTile(index) == GameSession.Outcome.IGNORED) return;
            board.animateSlide(index, gapBefore);
            afterMove[0].run();
        });

        controls.addView(smallButton(context, "Peek", v -> {
            session.recordPeek();
            board.setPeeking(true);
            board.postDelayed(() -> board.setPeeking(false), PEEK_MS);
            refresh.run();
        }));
        controls.addView(smallButton(context, "Undo", v -> {
            if (session.finished()) return;
            int gapBefore = session.puzzle().gapIndex();
            if (session.undoMove()) {
                // The undone tile moves from the new gap position back to the old one.
                board.animateSlide(session.puzzle().gapIndex(), gapBefore);
                afterMove[0].run();
            }
        }));
        controls.addView(smallButton(context, "One move", v -> {
            int tile = session.useOneMoveHint();
            if (tile >= 0) {
                board.setHighlight(tile);
                board.postDelayed(() -> board.setHighlight(-1), HINT_MS);
            }
            refresh.run();
        }));

        next.setOnClickListener(v -> onNext.run());
        Button back = button(context, "Back to games");
        back.setOnClickListener(v -> onBack.run());
        page.addView(back);
        return page;
    }

    /** Decodes the scene picture once. The art is a drawable name, so a missing file gives a blank board. */
    private static Bitmap decode(Context context, LampSlideScene scene) {
        int resource = LampSlideScenes.imageResource(context, scene.image());
        if (resource == 0) return null;
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.RGB_565;
        return BitmapFactory.decodeResource(context.getResources(), resource, options);
    }

    private static Button smallButton(Context context, String label, View.OnClickListener action) {
        Button button = button(context, label);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, -2, 1f);
        params.setMargins(dp(context, 4), 0, dp(context, 4), 0);
        button.setLayoutParams(params);
        button.setOnClickListener(action);
        return button;
    }

    private static Button button(Context context, String label) {
        Button button = new Button(context);
        button.setText(label);
        button.setAllCaps(false);
        button.setTextColor(0xFFFFFFFF);
        return button;
    }

    private static TextView text(Context context, String value, int sp, boolean bold) {
        TextView view = new TextView(context);
        view.setText(value);
        view.setTextSize(sp);
        view.setTextColor(0xFFFFFFFF);
        view.setGravity(Gravity.CENTER_HORIZONTAL);
        view.setPadding(0, dp(context, 6), 0, dp(context, 6));
        if (bold) view.setTypeface(view.getTypeface(), android.graphics.Typeface.BOLD);
        return view;
    }

    private static int dp(Context context, int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }
}
