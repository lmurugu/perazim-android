package org.perazimchurch.app.presentation.ui.games;

import android.widget.Button;
import android.widget.TextView;

import org.perazimchurch.app.presentation.ui.games.common.GameFx;

import org.perazimchurch.app.domain.games.FillTheGap;
import org.perazimchurch.app.domain.games.catalog.FillTheGapDefinition;
import org.perazimchurch.app.domain.games.catalog.FillTheGapSession;
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
 * Play screen for Fill the Gap. Drives {@link FillTheGapSession} and finishes through
 * {@link GameChrome#finishedRun(GameDefinition, String, GameSession, int, Runnable)}.
 * Registered in GameLaunchers with {@code GameRegistry.register("fill_the_gap", ctx -> ...)}.
 */
public final class FillTheGapScreen {

    private static final String TITLE = "Fill the Gap";

    private final GameContext ctx;
    private final GameDefinition definition = new FillTheGapDefinition(0);
    private int level = 1;

    public FillTheGapScreen(GameContext ctx) {
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
        render(new FillTheGapSession(new FillTheGap(GameBanks.verses(ctx.activity(), Levels.band(level)), seed)), null, false);
    }

    private void render(FillTheGapSession game, String feedback, boolean popScore) {
        GameUi ui = ctx.ui();
        ctx.activity().setContentView(ui.page(TITLE, "Level " + level + " · " + "Choose the missing word. Five verses per run.", true));
        if (game.finished()) {
            String key = ctx.scorePrefix() + "FILL_THE_GAP";
            if (game.score() > ctx.scores().getInt(key, 0)) ctx.scores().edit().putInt(key, game.score()).apply();
            new GameChrome(ui).finishedRun(definition, TITLE, game, level, () -> startRun(level), () -> startRun(level + 1));
            return;
        }
        TextView scoreLine = ui.text("Verse " + game.position() + " of " + game.size() + " · Score " + game.score(), 12, false);
        if (popScore) GameFx.pulse(scoreLine, 0xFFFFAA3C);
        if (game.streak().current() >= 2) {
            TextView streakBadge = ui.text("🔥 x" + game.streak().current(), 13, true);
            if (popScore) GameFx.pulse(streakBadge, 0xFFFFAA3C);
        }
        ui.text(game.prompt(), 18, true);
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

    /** Feedback for the verse just answered, with the word and reference. Null before the first answer. */
    private static String answerLine(FillTheGapSession game, GameSession.Outcome outcome) {
        FillTheGap.Question answered = game.lastQuestion();
        if (answered == null) return null;
        String lead = outcome == GameSession.Outcome.CORRECT
                ? "Correct. "
                : "Not quite. The word was " + answered.answer + ". ";
        String bonus = game.streak().bonusAwarded() ? " Streak bonus +" + Streak.BONUS_POINTS + "." : "";
        return lead + answered.reference + "." + bonus;
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
