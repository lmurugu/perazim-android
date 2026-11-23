package org.perazimchurch.app.presentation.ui.games;

import org.perazimchurch.app.presentation.ui.games.common.GameMissLog;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import org.perazimchurch.app.domain.games.catalog.WhoAmIDefinition;
import org.perazimchurch.app.domain.games.engine.GameSession;
import org.perazimchurch.app.presentation.ui.games.common.GameChrome;
import org.perazimchurch.app.presentation.ui.games.common.GameContext;
import org.perazimchurch.app.presentation.ui.games.common.GameFx;
import org.perazimchurch.app.presentation.ui.games.common.GameResults;
import org.perazimchurch.app.presentation.ui.games.common.GameUi;

/**
 * Who am I? play screen. Shows a level picker, then one person at a time: clues, an optional hint,
 * and a typed answer. Correct answers glow, wrong answers shake, and the run ends in the shared
 * results screen.
 */
public final class WhoAmIScreen {

    static final String ID = "who_am_i";
    private static final WhoAmIDefinition GAME = new WhoAmIDefinition(0);

    private WhoAmIScreen() {}

    /** Opens the level picker. Only unlocked levels can be played. */
    public static void show(GameContext ctx) {
        GameUi ui = ctx.ui();
        ui.page(GAME.title(), "Choose a level");
        ui.levelPicker(ID, GAME.levelCount(), level -> startRun(ctx, level));
    }

    static void startRun(GameContext ctx, int level) {
        GameSession session = GAME.createSession(level, System.nanoTime());
        renderQuestion(ctx, level, session);
    }

    private static void renderQuestion(GameContext ctx, int level, GameSession session) {
        GameUi ui = ctx.ui();
        GameChrome chrome = new GameChrome(ui);
        ui.page(GAME.title(), GAME.levelName(level) + ", question " + session.position() + " of " + session.size());
        ui.text(session.prompt(), 16, false);

        EditText input = new EditText(ctx.activity());
        input.setSingleLine(true);
        input.setHint("Type the name");
        ui.addRow(input);

        TextView hintLine = ui.text("", 14, false);
        if (session.hintAvailable()) {
            Button hint = ui.button("Show a hint", false);
            hint.setOnClickListener(v -> hintLine.setText(session.hint()));
        }
        TextView feedbackLine = ui.text("", 14, false);
        Button check = ui.button("Check answer", true);
        check.setOnClickListener(v -> {
            String asked = session.prompt();
            GameSession.Outcome outcome = session.answer(input.getText().toString());
            GameMissLog.answered(ctx.activity(), "who_am_i", asked, outcome);
            if (outcome == GameSession.Outcome.CORRECT) GameFx.correct(input);
            if (outcome == GameSession.Outcome.WRONG) GameFx.wrong(input);
            check.setEnabled(false);
            feedbackLine.setText(feedbackFor(session));
            if (session.finished()) {
                Runnable replay = () -> startRun(ctx, level);
                Runnable next = level < GAME.levelCount() ? () -> startRun(ctx, level + 1) : null;
                chrome.finishedRun(GAME, GAME.title(), session, level, replay, next);
            } else {
                Button next = ui.button("Next", true);
                next.setOnClickListener(n -> renderQuestion(ctx, level, session));
                ui.addRow(next);
            }
        });
        chrome.pauseButton(() -> renderQuestion(ctx, level, session));
    }

    private static String feedbackFor(GameSession session) {
        if (session instanceof org.perazimchurch.app.domain.games.catalog.WhoAmISession) {
            String last = ((org.perazimchurch.app.domain.games.catalog.WhoAmISession) session).lastFeedback();
            return last == null ? "" : last;
        }
        return "";
    }
}
