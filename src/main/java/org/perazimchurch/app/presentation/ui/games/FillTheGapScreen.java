package org.perazimchurch.app.presentation.ui.games;

import android.widget.Button;

import org.perazimchurch.app.domain.games.FillTheGap;
import org.perazimchurch.app.domain.games.catalog.FillTheGapDefinition;
import org.perazimchurch.app.domain.games.catalog.FillTheGapSession;
import org.perazimchurch.app.domain.games.engine.GameDefinition;
import org.perazimchurch.app.domain.games.engine.GameSession;
import org.perazimchurch.app.presentation.ui.games.common.GameChrome;
import org.perazimchurch.app.presentation.ui.games.common.GameContext;
import org.perazimchurch.app.presentation.ui.games.common.GameUi;

/**
 * Play screen for Fill the Gap. Drives {@link FillTheGapSession} and finishes through
 * {@link GameChrome#finishedRun(GameDefinition, String, GameSession, int, Runnable)}.
 * Registered in GameLaunchers with {@code GameRegistry.register("fill_the_gap", ctx -> ...)}.
 */
public final class FillTheGapScreen {

    private static final String TITLE = "Fill the Gap";

    private final GameContext ctx;
    private final GameDefinition definition = new FillTheGapDefinition(0);
    private long seed = System.nanoTime();

    public FillTheGapScreen(GameContext ctx) {
        this.ctx = ctx;
    }

    /** Starts a fresh run on the current screen. */
    public void start() {
        newRun();
    }

    private void newRun() {
        seed = System.nanoTime();
        render((FillTheGapSession) definition.createSession(1, seed), null);
    }

    private void render(FillTheGapSession game, String feedback) {
        GameUi ui = ctx.ui();
        ctx.activity().setContentView(ui.page(TITLE, "Choose the missing word. Five verses per run.", true));
        if (game.finished()) {
            String key = ctx.scorePrefix() + "FILL_THE_GAP";
            if (game.score() > ctx.scores().getInt(key, 0)) ctx.scores().edit().putInt(key, game.score()).apply();
            new GameChrome(ui).finishedRun(definition, TITLE, game, 1, this::newRun);
            return;
        }
        ui.text("Verse " + game.position() + " of " + game.size() + " · Score " + game.score(), 12, false);
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

    /** Feedback for the verse just answered, with the word and reference. Null before the first answer. */
    private static String answerLine(FillTheGapSession game, GameSession.Outcome outcome) {
        FillTheGap.Question answered = game.lastQuestion();
        if (answered == null) return null;
        String lead = outcome == GameSession.Outcome.CORRECT
                ? "Correct. "
                : "Not quite. The word was " + answered.answer + ". ";
        return lead + answered.reference + ".";
    }
}
