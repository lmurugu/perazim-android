package org.perazimchurch.app.presentation.ui.games;

import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import java.util.List;
import org.perazimchurch.app.domain.games.catalog.LadderDefinition;
import org.perazimchurch.app.domain.games.catalog.LadderSession;
import org.perazimchurch.app.domain.games.engine.GameSession;
import org.perazimchurch.app.presentation.ui.games.common.GameChrome;
import org.perazimchurch.app.presentation.ui.games.common.GameContext;
import org.perazimchurch.app.presentation.ui.games.common.GameFx;
import org.perazimchurch.app.presentation.ui.games.common.GameResults;
import org.perazimchurch.app.presentation.ui.games.common.GameUi;

/**
 * Ladder of Faith play screen. Each rung is a multiple-choice question; a wrong answer costs a life
 * and three wrong answers end the climb. Verse Peek shows the reference once per run.
 */
public final class LadderScreen {

    static final String ID = "ladder_of_faith";
    private static final LadderDefinition GAME = new LadderDefinition(0);

    private LadderScreen() {}

    /** Opens the level picker. Only unlocked levels can be played. */
    public static void show(GameContext ctx) {
        GameUi ui = ctx.ui();
        ui.page(GAME.title(), "Choose a level");
        ui.levelPicker(ID, GAME.levelCount(), level -> startRun(ctx, level));
    }

    static void startRun(GameContext ctx, int level) {
        LadderSession run = (LadderSession) GAME.createSession(level, System.nanoTime());
        render(ctx, level, run, "Climb one rung at a time. Three wrong answers end the climb.");
    }

    private static void render(GameContext ctx, int level, LadderSession run, String feedbackText) {
        GameUi ui = ctx.ui();
        GameChrome chrome = new GameChrome(ui);
        ui.page(GAME.title(), GAME.levelName(level) + ", rung " + run.position() + " of " + run.size());
        ui.text(run.prompt() == null ? "" : run.prompt(), 18, false);
        TextView feedback = ui.text(feedbackText, 14, false);
        TextView peekLine = ui.text("", 14, false);
        if (run.hintAvailable()) {
            Button peek = ui.button("Verse Peek (costs 3 points)", false);
            peek.setOnClickListener(v -> peekLine.setText(run.hint()));
        }
        List<String> options = run.options();
        for (String option : options) {
            Button button = ui.button(option, false);
            button.setOnClickListener(v -> tap(ctx, level, run, chrome, option, button));
        }
        chrome.pauseButton(() -> render(ctx, level, run, feedback.getText().toString()));
    }

    private static void tap(GameContext ctx, int level, LadderSession run, GameChrome chrome, String option, View v) {
        GameSession.Outcome outcome = run.answer(option);
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
