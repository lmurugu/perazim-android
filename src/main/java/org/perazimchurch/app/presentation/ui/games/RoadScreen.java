package org.perazimchurch.app.presentation.ui.games;

import org.perazimchurch.app.presentation.ui.games.common.GameMissLog;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.List;
import org.perazimchurch.app.domain.games.catalog.RoadDefinition;
import org.perazimchurch.app.domain.games.catalog.RoadSession;
import org.perazimchurch.app.domain.games.engine.GameSession;
import org.perazimchurch.app.presentation.ui.games.common.GameChrome;
import org.perazimchurch.app.presentation.ui.games.common.GameContext;
import org.perazimchurch.app.presentation.ui.games.common.GameFx;
import org.perazimchurch.app.presentation.ui.games.common.GameResults;
import org.perazimchurch.app.presentation.ui.games.common.GameUi;

/**
 * Road to Jerusalem play screen. The dice-and-board race: each turn rolls a die, moves the traveller
 * along a track of squares, and the square landed on asks a Bible stage. Right answers keep the move.
 * The hint gives the first letter for 3 points.
 */
public final class RoadScreen {

    static final String ID = "road_to_jerusalem";
    private static final int COLUMNS = 5;
    private static final RoadDefinition GAME = new RoadDefinition(0);

    private RoadScreen() {}

    /** Opens the level picker. Only unlocked levels can be played. */
    public static void show(GameContext ctx) {
        GameUi ui = ctx.ui();
        ui.page(GAME.title(), "Choose a level");
        ui.levelPicker(ID, GAME.levelCount(), level -> startRun(ctx, level));
    }

    static void startRun(GameContext ctx, int level) {
        RoadSession run = (RoadSession) GAME.createSession(level, System.nanoTime());
        render(ctx, level, run, "Roll, move, and answer the stage you land on.");
    }

    private static void render(GameContext ctx, int level, RoadSession run, String feedbackText) {
        GameUi ui = ctx.ui();
        GameChrome chrome = new GameChrome(ui);
        ui.page(GAME.title(), GAME.levelName(level) + ", turn " + run.position() + " of " + run.size());
        drawBoard(ctx, run);
        ui.text("Die: " + run.roll() + "   Landing on square " + run.landingSquare(), 16, false);
        ui.text(run.prompt() == null ? "" : run.prompt(), 18, false);
        TextView feedback = ui.text(feedbackText, 14, false);
        TextView hintLine = ui.text("", 14, false);
        if (run.hintAvailable()) {
            Button hint = ui.button("Show a hint (costs 3 points)", false);
            hint.setOnClickListener(v -> hintLine.setText(run.hint()));
        }
        List<String> options = run.options();
        for (String option : options) {
            Button button = ui.button(option, false);
            button.setOnClickListener(v -> tap(ctx, level, run, chrome, option, button));
        }
        chrome.pauseButton(() -> render(ctx, level, run, feedback.getText().toString()));
    }

    /** Draws the track in rows of five squares, marking the traveller and Jerusalem. */
    private static void drawBoard(GameContext ctx, RoadSession run) {
        GameUi ui = ctx.ui();
        LinearLayout row = null;
        for (int square = 1; square <= RoadSession.FINISH; square++) {
            if ((square - 1) % COLUMNS == 0) {
                row = new LinearLayout(ctx.activity());
                row.setOrientation(LinearLayout.HORIZONTAL);
                ui.addRow(row);
            }
            TextView cell = new TextView(ctx.activity());
            String mark = square == RoadSession.FINISH ? "Jerusalem" : String.valueOf(square);
            cell.setText(square == run.square() ? "[" + mark + "]" : mark);
            cell.setTextSize(13);
            cell.setGravity(android.view.Gravity.CENTER);
            row.addView(cell, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        }
    }

    private static void tap(GameContext ctx, int level, RoadSession run, GameChrome chrome, String option, View v) {
        String asked = run.prompt();
        GameSession.Outcome outcome = run.answer(option);
        GameMissLog.answered(ctx.activity(), "road_to_jerusalem", asked, outcome);
        if (outcome == GameSession.Outcome.IGNORED) return;
        if (outcome == GameSession.Outcome.CORRECT) GameFx.correct(v);
        if (outcome == GameSession.Outcome.WRONG) GameFx.wrong(v);
        if (run.finished()) {
            Runnable replay = () -> startRun(ctx, level);
            Runnable next = level < GAME.levelCount() ? () -> startRun(ctx, level + 1) : null;
            chrome.finishedRun(GAME, GAME.title(), run, level, replay, next);
            return;
        }
        render(ctx, level, run, run.lastFeedback() == null ? "" : run.lastFeedback());
    }
}
