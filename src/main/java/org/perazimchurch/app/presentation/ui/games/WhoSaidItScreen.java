package org.perazimchurch.app.presentation.ui.games;

import android.widget.Button;
import android.widget.TextView;

import org.perazimchurch.app.presentation.ui.games.common.GameFx;

import org.perazimchurch.app.domain.games.WhoSaidIt;
import org.perazimchurch.app.domain.games.catalog.WhoSaidItDefinition;
import org.perazimchurch.app.domain.games.catalog.WhoSaidItSession;
import org.perazimchurch.app.domain.games.engine.GameDefinition;
import org.perazimchurch.app.domain.games.engine.GameSession;
import org.perazimchurch.app.presentation.ui.games.common.GameChrome;
import org.perazimchurch.app.presentation.ui.games.common.GameContext;
import org.perazimchurch.app.presentation.ui.games.common.GameUi;

/**
 * Play screen for Who Said It?. Drives {@link WhoSaidItSession} and finishes through
 * {@link GameChrome#finishedRun(GameDefinition, String, GameSession, int, Runnable)}.
 * Registered in GameLaunchers with {@code GameRegistry.register("who_said_it", ctx -> ...)}.
 */
public final class WhoSaidItScreen {

    private static final String TITLE = "Who Said It?";

    private final GameContext ctx;
    private final GameDefinition definition = new WhoSaidItDefinition(0);
    private long seed = System.nanoTime();

    public WhoSaidItScreen(GameContext ctx) {
        this.ctx = ctx;
    }

    /** Starts a fresh run on the current screen. */
    public void start() {
        newRun();
    }

    private void newRun() {
        seed = System.nanoTime();
        render((WhoSaidItSession) definition.createSession(1, seed), null, false);
    }

    private void render(WhoSaidItSession game, String feedback, boolean popScore) {
        GameUi ui = ctx.ui();
        ctx.activity().setContentView(ui.page(TITLE, "Choose who spoke each quotation. Five quotations per run.", true));
        if (game.finished()) {
            String key = ctx.scorePrefix() + "WHO_SAID_IT";
            if (game.score() > ctx.scores().getInt(key, 0)) ctx.scores().edit().putInt(key, game.score()).apply();
            new GameChrome(ui).finishedRun(definition, TITLE, game, 1, this::newRun);
            return;
        }
        TextView scoreLine = ui.text("Quotation " + game.position() + " of " + game.size() + " · Score " + game.score(), 12, false);
        if (popScore) GameFx.pulse(scoreLine, 0xFFFFAA3C);
        ui.text(game.prompt(), 18, true);
        if (feedback != null) ui.text(feedback, 13, false);
        new GameChrome(ui).pauseButton(() -> render(game, feedback, false));
        int slot = 0;
        for (String choice : game.choices()) {
            Button option = ui.button(choice, false);
            staggerIn(option, slot++);
            option.setOnClickListener(v -> {
                if (!ui.activeOwner()) return;
                ui.click(v);
                GameSession.Outcome outcome = game.answer(choice);
                tapFeedback(v, outcome);
                String line = answerLine(game, outcome);
                v.postDelayed(() -> render(game, line, outcome == GameSession.Outcome.CORRECT), 280);
            });
        }
    }

    /** Feedback for the quotation just answered, with its reference. Null before the first answer. */
    private static String answerLine(WhoSaidItSession game, GameSession.Outcome outcome) {
        WhoSaidIt.Question answered = game.lastQuestion();
        if (answered == null) return null;
        String lead = outcome == GameSession.Outcome.CORRECT
                ? "Correct. "
                : "Not quite. It was " + answered.answer + ". ";
        return lead + answered.reference + ".";
    }

    /** Correct or wrong feedback on the tapped button. */
    private static void tapFeedback(android.view.View v, GameSession.Outcome outcome) {
        if (outcome == GameSession.Outcome.CORRECT) GameFx.correct(v); else GameFx.wrong(v);
    }

    /** Choices slide in one after another. Skipped when the system has animations turned off. */
    private static void staggerIn(android.view.View v, int slot) {
        if (!GameFx.motionEnabled(v.getContext())) return;
        v.setAlpha(0f);
        v.animate().alpha(1f).setStartDelay(60L * slot).setDuration(200).start();
    }
}
