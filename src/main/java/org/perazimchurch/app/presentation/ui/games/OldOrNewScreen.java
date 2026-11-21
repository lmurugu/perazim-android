package org.perazimchurch.app.presentation.ui.games;

import android.widget.Button;

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
        render((TestamentSession) definition.createSession(1, seed), null);
    }

    private void render(TestamentSession game, String feedback) {
        GameUi ui = ctx.ui();
        ctx.activity().setContentView(ui.page(TITLE, "Place each book in its testament. Eight books per run.", true));
        if (game.finished()) {
            String key = ctx.scorePrefix() + "TESTAMENT_SORT";
            if (game.score() > ctx.scores().getInt(key, 0)) ctx.scores().edit().putInt(key, game.score()).apply();
            new GameChrome(ui).finishedRun(definition, TITLE, game, 1, this::newRun);
            return;
        }
        ui.text("Book " + game.position() + " of " + game.size() + " · Score " + game.score(), 12, false);
        ui.text(game.prompt(), 20, true);
        if (feedback != null) ui.text(feedback, 13, false);
        new GameChrome(ui).pauseButton(() -> render(game, feedback));
        Button old = ui.button("Old Testament", false);
        old.setOnClickListener(v -> {
            if (!ui.activeOwner()) return;
            ui.click(v);
            GameSession.Outcome outcome = game.answer("OLD");
            render(game, answerLine(game, outcome));
        });
        Button neu = ui.button("New Testament", false);
        neu.setOnClickListener(v -> {
            if (!ui.activeOwner()) return;
            ui.click(v);
            GameSession.Outcome outcome = game.answer("NEW");
            render(game, answerLine(game, outcome));
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
}
