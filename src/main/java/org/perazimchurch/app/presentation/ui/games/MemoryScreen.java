package org.perazimchurch.app.presentation.ui.games;

import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.List;
import org.perazimchurch.app.domain.games.catalog.MemoryDefinition;
import org.perazimchurch.app.domain.games.catalog.MemorySession;
import org.perazimchurch.app.domain.games.engine.GameSession;
import org.perazimchurch.app.presentation.ui.games.common.GameChrome;
import org.perazimchurch.app.presentation.ui.games.common.GameContext;
import org.perazimchurch.app.presentation.ui.games.common.GameFx;
import org.perazimchurch.app.presentation.ui.games.common.GameResults;
import org.perazimchurch.app.presentation.ui.games.common.GameUi;

/**
 * Memory Match play screen. Cards start face down. The first tap turns a card over; the second
 * either makes a matched pair (it glows and stays up) or misses, and both cards turn back.
 */
public final class MemoryScreen {

    static final String ID = "memory_match";
    private static final MemoryDefinition GAME = new MemoryDefinition(0);
    private static final int COLUMNS = 3;

    private MemoryScreen() {}

    /** Opens the level picker. Only unlocked levels can be played. */
    public static void show(GameContext ctx) {
        GameUi ui = ctx.ui();
        ui.page(GAME.title(), "Choose a level");
        ui.levelPicker(ID, GAME.levelCount(), level -> startRun(ctx, level));
    }

    static void startRun(GameContext ctx, int level) {
        MemorySession run = (MemorySession) GAME.createSession(level, System.nanoTime());
        render(ctx, level, run, -1, "Flip two cards at a time to find the matching pairs.");
    }

    /** Draws the board. {@code open} is the card turned over by the first tap, or -1. */
    private static void render(GameContext ctx, int level, MemorySession run, int open, String feedbackText) {
        GameUi ui = ctx.ui();
        GameChrome chrome = new GameChrome(ui);
        ui.page(GAME.title(), GAME.levelName(level) + ", " + run.position() + " of " + run.size() + " pairs");
        TextView feedback = ui.text(feedbackText, 14, false);
        TextView hintLine = ui.text("", 14, false);
        if (run.hintAvailable()) {
            Button hint = ui.button("Show a hint", false);
            hint.setOnClickListener(v -> hintLine.setText(run.hint()));
        }

        List<MemorySession.Card> cards = run.cards();
        LinearLayout board = null;
        for (int i = 0; i < cards.size(); i++) {
            if (i % COLUMNS == 0) {
                board = new LinearLayout(ctx.activity());
                board.setOrientation(LinearLayout.HORIZONTAL);
                ui.addRow(board);
            }
            final int index = i;
            Button card = new Button(ctx.activity());
            boolean showing = run.isMatched(i) || i == open;
            card.setText(showing ? cards.get(i).label : "?");
            card.setTextSize(14);
            card.setEnabled(!run.isMatched(i));
            board.addView(card, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
            card.setOnClickListener(v -> tap(ctx, level, run, chrome, feedback, index, open, card));
        }
        chrome.pauseButton(() -> render(ctx, level, run, open, feedback.getText().toString()));
    }

    private static void tap(GameContext ctx, int level, MemorySession run, GameChrome chrome, TextView feedback,
                            int index, int open, Button card) {
        GameSession.Outcome outcome = run.answer(String.valueOf(index));
        if (outcome == GameSession.Outcome.IGNORED) return;
        if (outcome == GameSession.Outcome.MOVED) {
            render(ctx, level, run, index, "Now find its partner.");
            return;
        }
        if (outcome == GameSession.Outcome.CORRECT) GameFx.correct(card);
        if (outcome == GameSession.Outcome.WRONG) GameFx.wrong(card);
        if (run.finished()) {
            Runnable replay = () -> startRun(ctx, level);
            Runnable next = level < GAME.levelCount() ? () -> startRun(ctx, level + 1) : null;
            chrome.finishedRun(GAME, GAME.title(), run, level, replay, next);
            return;
        }
        render(ctx, level, run, -1, run.lastFeedback() == null ? "" : run.lastFeedback());
    }
}
