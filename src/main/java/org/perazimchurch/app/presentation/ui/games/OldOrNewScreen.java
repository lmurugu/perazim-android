package org.perazimchurch.app.presentation.ui.games;

import org.perazimchurch.app.presentation.ui.games.common.GameMissLog;
import android.widget.Button;
import android.widget.TextView;

import org.perazimchurch.app.presentation.ui.games.common.GameFx;

import org.perazimchurch.app.domain.games.TestamentSort;
import org.perazimchurch.app.domain.games.catalog.TestamentDefinition;
import org.perazimchurch.app.domain.games.catalog.TestamentSession;
import org.perazimchurch.app.domain.games.engine.GameDefinition;
import org.perazimchurch.app.domain.games.engine.GameSession;
import org.perazimchurch.app.domain.games.catalog.HintRules;
import org.perazimchurch.app.domain.games.catalog.Levels;
import org.perazimchurch.app.presentation.ui.games.common.GameBanks;
import org.perazimchurch.app.presentation.ui.games.common.GameUi;

import org.perazimchurch.app.domain.games.catalog.Streak;
import org.perazimchurch.app.presentation.ui.games.common.GameChrome;
import org.perazimchurch.app.presentation.ui.games.common.GameContext;

/**
 * Play screen for Old or New?. Drives the engine session and finishes through
 * {@link GameChrome#finishedRun(GameDefinition, String, GameSession, int, Runnable)}.
 * Registered in GameLaunchers with {@code GameRegistry.register("old_or_new", ctx -> ...)}.
 */
public final class OldOrNewScreen {

    private static final String TITLE = "Old or New?";

    private final GameContext ctx;
    private final GameDefinition definition = new TestamentDefinition(0);
    private int level = 1;

    public OldOrNewScreen(GameContext ctx) {
        this.ctx = ctx;
    }

    /** Opens the level picker on the current screen. */
    public void start() {
        renderPicker();
    }

    private void renderPicker() {
        GameUi ui = ctx.ui();
        ctx.activity().setContentView(ui.page(TITLE, "Pick a level. A level opens after two stars on the one before.", true));
        ui.levelPicker(definition.id(), Levels.COUNT, this::startRun);
    }

    /** Starts a run on one level, drawn from the bundled bank for its band. */
    private void startRun(int chosen) {
        level = Levels.clamp(chosen);
        long seed = Levels.seed(level);
        render(new TestamentSession(new TestamentSort(GameBanks.testamentEntries(ctx.activity(), Levels.band(level)), seed)),
                null, false);
    }

    private void render(TestamentSession game, String feedback, boolean popScore) {
        GameUi ui = ctx.ui();
        ctx.activity().setContentView(ui.page(TITLE, "Level " + level + " · " + "Place each book in its testament. Eight books per run.", true));
        if (game.finished()) {
            String key = ctx.scorePrefix() + "TESTAMENT_SORT";
            if (game.score() > ctx.scores().getInt(key, 0)) ctx.scores().edit().putInt(key, game.score()).apply();
            new GameChrome(ui).finishedRun(definition, TITLE, game, level, () -> startRun(level), () -> startRun(level + 1));
            return;
        }
        TextView scoreLine = ui.text("Book " + game.position() + " of " + game.size() + " · Score " + game.score(), 12, false);
        if (popScore) GameFx.pulse(scoreLine, 0xFFFFAA3C);
        if (game.streak().current() >= 2) {
            TextView streakBadge = ui.text("🔥 x" + game.streak().current(), 13, true);
            if (popScore) GameFx.pulse(streakBadge, 0xFFFFAA3C);
        }
        ui.text(game.prompt(), 20, true);
        if (feedback != null) ui.text(feedback, 13, false);
        new GameChrome(ui).pauseButton(() -> render(game, feedback, false));
        if (game.hintAvailable()) {
            Button hint = ui.button("Hint (" + (HintRules.HINTS_PER_RUN - game.hintsUsed()) + " left)", false);
            hint.setOnClickListener(v -> {
                if (!ui.activeOwner()) return;
                ui.click(v);
                render(game, game.hint(), false);
            });
        }
        Button old = ui.button("Old Testament", false);
        staggerIn(old, 0);
        old.setOnClickListener(v -> {
            if (!ui.activeOwner()) return;
            ui.click(v);
            String asked = game.prompt();
            GameSession.Outcome outcome = game.answer("OLD");
            GameMissLog.answered(ctx.activity(), "old_or_new", asked, outcome);
            tapFeedback(v, outcome);
            String line = answerLine(game, outcome);
            v.postDelayed(() -> render(game, line, outcome == GameSession.Outcome.CORRECT), 280);
        });
        Button neu = ui.button("New Testament", false);
        staggerIn(neu, 1);
        neu.setOnClickListener(v -> {
            if (!ui.activeOwner()) return;
            ui.click(v);
            String asked = game.prompt();
            GameSession.Outcome outcome = game.answer("NEW");
            GameMissLog.answered(ctx.activity(), "old_or_new", asked, outcome);
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
        String bonus = game.streak().bonusAwarded() ? " Streak bonus +" + Streak.BONUS_POINTS + "." : "";
        return lead + placed.fact + bonus;
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
