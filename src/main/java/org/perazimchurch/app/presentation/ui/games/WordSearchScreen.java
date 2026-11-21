package org.perazimchurch.app.presentation.ui.games;

import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import org.perazimchurch.app.domain.games.catalog.WordSearchDefinition;
import org.perazimchurch.app.domain.games.catalog.WordSearchGrid;
import org.perazimchurch.app.domain.games.catalog.WordSearchSession;
import org.perazimchurch.app.domain.games.engine.GameSession;
import org.perazimchurch.app.presentation.ui.games.common.GameChrome;
import org.perazimchurch.app.presentation.ui.games.common.GameContext;
import org.perazimchurch.app.presentation.ui.games.common.GameFx;
import org.perazimchurch.app.presentation.ui.games.common.GameResults;
import org.perazimchurch.app.presentation.ui.games.common.GameUi;

/**
 * Word Search play screen. The player taps the first letter of a word, then the last letter. A
 * straight line that spells a new word glows; anything else shakes and gets a gentle nudge.
 */
public final class WordSearchScreen {

    static final String ID = "word_search";
    private static final WordSearchDefinition GAME = new WordSearchDefinition(0);

    private WordSearchScreen() {}

    /** Opens the level picker. Only unlocked levels can be played. */
    public static void show(GameContext ctx) {
        GameUi ui = ctx.ui();
        ui.page(GAME.title(), "Choose a level");
        ui.levelPicker(ID, GAME.levelCount(), level -> startRun(ctx, level));
    }

    static void startRun(GameContext ctx, int level) {
        WordSearchSession run = (WordSearchSession) GAME.createSession(level, System.nanoTime());
        render(ctx, level, run);
    }

    private static void render(GameContext ctx, int level, WordSearchSession run) {
        GameUi ui = ctx.ui();
        GameChrome chrome = new GameChrome(ui);
        ui.page(GAME.title(), GAME.levelName(level) + ", " + run.themeTitle());
        ui.text(run.prompt(), 15, false);
        TextView feedback = ui.text("Tap the first letter, then the last letter.", 14, false);
        TextView hintLine = ui.text("", 14, false);
        if (run.hintAvailable()) {
            Button hint = ui.button("Show a hint", false);
            hint.setOnClickListener(v -> hintLine.setText(run.hint()));
        }

        final int[] start = {-1, -1};
        WordSearchGrid grid = run.grid();
        LinearLayout board = new LinearLayout(ctx.activity());
        board.setOrientation(LinearLayout.VERTICAL);
        for (int r = 0; r < WordSearchGrid.SIZE; r++) {
            LinearLayout row = new LinearLayout(ctx.activity());
            row.setOrientation(LinearLayout.HORIZONTAL);
            for (int c = 0; c < WordSearchGrid.SIZE; c++) {
                final int row0 = r;
                final int col0 = c;
                Button cell = new Button(ctx.activity());
                cell.setText(String.valueOf(grid.letter(r, c)));
                cell.setTextSize(15);
                cell.setAllCaps(true);
                row.addView(cell, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
                cell.setOnClickListener(v -> pick(ctx, level, run, chrome, feedback, start, row0, col0, v));
            }
            board.addView(row);
        }
        ui.addRow(board);
        chrome.pauseButton(() -> render(ctx, level, run));
    }

    private static void pick(GameContext ctx, int level, WordSearchSession run, GameChrome chrome, TextView feedback,
                             int[] start, int r, int c, View cell) {
        if (start[0] < 0) {
            start[0] = r;
            start[1] = c;
            feedback.setText("Now tap the last letter.");
            return;
        }
        GameSession.Outcome outcome = run.answer(start[0] + "," + start[1] + "," + r + "," + c);
        start[0] = -1;
        start[1] = -1;
        if (outcome == GameSession.Outcome.CORRECT) GameFx.correct(cell);
        if (outcome == GameSession.Outcome.WRONG) GameFx.wrong(cell);
        feedback.setText(run.lastFeedback() == null ? "" : run.lastFeedback());
        if (run.finished()) {
            Runnable replay = () -> startRun(ctx, level);
            Runnable next = level < GAME.levelCount() ? () -> startRun(ctx, level + 1) : null;
            chrome.finishedRun(GAME, GAME.title(), run, level, replay, next);
        }
    }
}
