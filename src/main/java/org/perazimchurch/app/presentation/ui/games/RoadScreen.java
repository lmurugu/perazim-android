package org.perazimchurch.app.presentation.ui.games;

import android.view.View;
import android.widget.Button;
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
 * Road to Jerusalem play screen. Six Bible stages make one journey; each stage is a multiple-choice
 * question about a place or person on the road. The hint gives the first letter for 3 points.
 */
public final class RoadScreen {

    static final String ID = "road_to_jerusalem";
    private static final RoadDefinition GAME = new RoadDefinition(0);

    private RoadScreen() {}

    /** Opens the level picker. Only unlocked levels can be played. */
    public static void show(GameContext ctx) {
        GameUi ui = ctx.ui();
        int unlocked = GameResults.unlockedLevel(ctx.scores(), ctx.scorePrefix() + ID);
        ui.page(GAME.title(), "Choose a level");
        for (int level = 1; level <= GAME.levelCount(); level++) {
            final int chosen = level;
            Button button = ui.button(GAME.levelName(level), level == unlocked);
            button.setEnabled(level <= unlocked);
            button.setOnClickListener(v -> startRun(ctx, chosen));
        }
    }

    static void startRun(GameContext ctx, int level) {
        RoadSession run = (RoadSession) GAME.createSession(level, System.nanoTime());
        render(ctx, level, run, "Walk the road one stage at a time.");
    }

    private static void render(GameContext ctx, int level, RoadSession run, String feedbackText) {
        GameUi ui = ctx.ui();
        GameChrome chrome = new GameChrome(ui);
        ui.page(GAME.title(), GAME.levelName(level) + ", stage " + run.position() + " of " + run.size());
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

    private static void tap(GameContext ctx, int level, RoadSession run, GameChrome chrome, String option, View v) {
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
