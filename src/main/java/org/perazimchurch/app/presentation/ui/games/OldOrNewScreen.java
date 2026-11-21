package org.perazimchurch.app.presentation.ui.games;

import android.widget.Button;
import android.widget.TextView;

import org.perazimchurch.app.presentation.ui.games.common.GameFx;

import org.perazimchurch.app.domain.games.TestamentSort;
import org.perazimchurch.app.domain.games.catalog.TestamentDefinition;
import org.perazimchurch.app.domain.games.catalog.TestamentSession;
import org.perazimchurch.app.domain.games.engine.GameDefinition;
import org.perazimchurch.app.domain.games.engine.GameSession;
import org.perazimchurch.app.presentation.ui.games.common.GameChrome;
import org.perazimchurch.app.presentation.ui.games.common.GameContext;
import org.perazimchurch.app.presentation.ui.games.common.GameUi;

/**
 * Play screen for Old or New?. Drives the engine session and finishes through
 * {@link GameChrome#finishedRun(GameDefinition, String, GameSession, int, Runnable)}.
 * Registered in GameLaunchers with {@code GameRegistry.register("old_or_new", ctx -> ...)}.
 */
public final class OldOrNewScreen {

    private static final String TITLE = "Old or New?";

    private final GameContext ctx;
    private final GameDefinition definition = new TestamentDefinition(0);
    private long seed = System.nanoTime();

    public OldOrNewScreen(GameContext ctx) {
        this.ctx = ctx;
    }

    /** Starts a fresh run on the current screen. */
    public void start() {
        newRun();
    }

    private void newRun() {
        seed = System.nanoTime();
        render((TestamentSession) definition.createSession(1, seed), null, false);
    }

    private void render(TestamentSession game, String feedback, boolean popScore) {
        GameUi ui = ctx.ui();
        ctx.activity().setContentView(ui.page(TITLE, "Place each book in its testament. Eight books per run.", true));
        if (game.finished()) {
            String key = ctx.scorePrefix() + "TESTAMENT_SORT";
            if (game.score() > ctx.scores().getInt(key, 0)) ctx.scores().edit().putInt(key, game.score()).apply();
            new GameChrome(ui).finishedRun(definition, TITLE, game, 1, this::newRun);
            return;
        }
        TextView scoreLine = ui.text("Book " + game.position() + " of " + game.size() + " · Score " + game.score(), 12, false);
        if (popScore) GameFx.pulse(scoreLine, 0xFFFFAA3C);
        ui.text(game.prompt(), 20, true);
        if (feedback != null) ui.text(feedback, 13, false);
        new GameChrome(ui).pauseButton(() -> render(game, feedback, false));
        Button old = ui.button("Old Testament", false);
        staggerIn(old, 0);
        old.setOnClickListener(v -> {
            if (!ui.activeOwner()) return;
            ui.click(v);
            GameSession.Outcome outcome = game.answer("OLD");
            tapFeedback(v, outcome);
            String line = answerLine(game, outcome);
            v.postDelayed(() -> render(game, line, outcome == GameSession.Outcome.CORRECT), 280);
        });
        Button neu = ui.button("New Testament", false);
        staggerIn(neu, 1);
        neu.setOnClickListener(v -> {
            if (!ui.activeOwner()) return;
            ui.click(v);
            GameSession.Outcome outcome = game.answer("NEW");
            tapFeedback(v, outcome);
            String line = answerLine(game, outcome);
            v.postDelayed(() -> render(game, line, outcome == GameSession.Outcome.CORRECT), 280);
        });
    }

    /** Feedback for the book just placed, including its one-line fact. Null before the first answer. */
    private static String answerLine(TestamentSession game, GameSession.Outcome outcome) {
        TestamentSort.Book placed = game.lastBook();
        if (placed == null) return null;
        String home = placed.testament == TestamentSort.Testament.OLD ? "Old" : "New";
        String lead = outcome == GameSession.Outcome.CORRECT
                ? "Correct. "
                : "Not quite. " + placed.name + " belongs in the " + home + " Testament. ";
        return lead + placed.fact;
    }

    /** Correct or wrong feedback on the tapped button, then the next render once the effect has played. */
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
