package org.perazimchurch.app.presentation.ui.games;

import android.widget.Button;

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
        render((WhoSaidItSession) definition.createSession(1, seed), null);
    }

    private void render(WhoSaidItSession game, String feedback) {
        GameUi ui = ctx.ui();
        ctx.activity().setContentView(ui.page(TITLE, "Choose who spoke each quotation. Five quotations per run.", true));
        if (game.finished()) {
            String key = ctx.scorePrefix() + "WHO_SAID_IT";
            if (game.score() > ctx.scores().getInt(key, 0)) ctx.scores().edit().putInt(key, game.score()).apply();
            new GameChrome(ui).finishedRun(definition, TITLE, game, 1, this::newRun);
            return;
        }
        ui.text("Quotation " + game.position() + " of " + game.size() + " · Score " + game.score(), 12, false);
        ui.text(game.prompt(), 18, true);
        if (feedback != null) ui.text(feedback, 13, false);
        new GameChrome(ui).pauseButton(() -> render(game, feedback));
        for (String choice : game.choices()) {
            Button option = ui.button(choice, false);
            option.setOnClickListener(v -> {
                if (!ui.activeOwner()) return;
                ui.click(v);
                GameSession.Outcome outcome = game.answer(choice);
                render(game, answerLine(game, outcome));
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
}
