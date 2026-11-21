package org.perazimchurch.app.presentation.ui.games;

import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import org.perazimchurch.app.domain.games.BibleGames;
import org.perazimchurch.app.domain.games.catalog.QuizSession;
import org.perazimchurch.app.domain.games.catalog.Streak;
import org.perazimchurch.app.domain.games.engine.GameDefinition;
import org.perazimchurch.app.domain.games.engine.GameSession;
import org.perazimchurch.app.presentation.ui.games.common.GameChrome;
import org.perazimchurch.app.presentation.ui.games.common.GameCatalog;
import org.perazimchurch.app.presentation.ui.games.common.GameContext;
import org.perazimchurch.app.presentation.ui.games.common.GameFx;
import org.perazimchurch.app.presentation.ui.games.common.GameUi;

/**
 * Play screen for Bible Quiz. Drives {@link QuizSession} and finishes through
 * {@link GameChrome#finishedRun(GameDefinition, String, GameSession, int, Runnable)}.
 * Registered in GameLaunchers with {@code GameRegistry.register("bible_quiz", ctx -> ...)}.
 */
public final class QuizScreen {

    private static final String TITLE = "Bible Quiz";

    private final GameContext ctx;
    private final GameDefinition definition;
    private long seed = System.nanoTime();

    public QuizScreen(GameContext ctx) {
        this.ctx = ctx;
        this.definition = find("bible_quiz");
    }

    /** Starts a fresh run on the current screen. */
    public void start() {
        newRun();
    }

    private void newRun() {
        seed = System.nanoTime();
        render((QuizSession) definition.createSession(1, seed), null, false);
    }

    private void render(QuizSession game, String feedback, boolean popScore) {
        GameUi ui = ctx.ui();
        ctx.activity().setContentView(ui.page(TITLE, "Choose the answer to each question. Five questions per run.", true));
        if (game.finished()) {
            String key = ctx.scorePrefix() + "BIBLE_QUIZ";
            if (game.score() > ctx.scores().getInt(key, 0)) ctx.scores().edit().putInt(key, game.score()).apply();
            new GameChrome(ui).finishedRun(definition, TITLE, game, 1, this::newRun);
            return;
        }
        TextView scoreLine = ui.text("Question " + game.position() + " of " + game.size() + " · Score " + game.score(), 12, false);
        if (popScore) GameFx.pulse(scoreLine, 0xFFFFAA3C);
        if (game.streak().current() >= 2) {
            TextView streakBadge = ui.text("🔥 x" + game.streak().current(), 13, true);
            if (popScore) GameFx.pulse(streakBadge, 0xFFFFAA3C);
        }
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

    /** Explanation and reference for the question just answered. Null before the first answer. */
    private static String answerLine(QuizSession game, GameSession.Outcome outcome) {
        BibleGames.Card answered = game.lastCard();
        if (answered == null) return null;
        String lead = outcome == GameSession.Outcome.CORRECT
                ? "Correct. "
                : "Not quite. It was " + answered.answer + ". ";
        String bonus = game.streak().bonusAwarded() ? " Streak bonus +" + Streak.BONUS_POINTS + "." : "";
        return lead + answered.explanation + " " + answered.reference + "." + bonus;
    }

    /** Correct or wrong feedback on the tapped button. */
    private static void tapFeedback(View v, GameSession.Outcome outcome) {
        if (outcome == GameSession.Outcome.CORRECT) GameFx.correct(v); else GameFx.wrong(v);
    }

    /** Choices slide in one after another. Skipped when the system has animations turned off. */
    private static void staggerIn(View v, int slot) {
        if (!GameFx.motionEnabled(v.getContext())) return;
        v.setAlpha(0f);
        v.animate().alpha(1f).setStartDelay(60L * slot).setDuration(200).start();
    }

    /** The catalog entry for a game id, which the results screen and star rules read. */
    static GameDefinition find(String id) {
        for (GameDefinition game : GameCatalog.all()) {
            if (id.equals(game.id())) return game;
        }
        throw new IllegalStateException("Unknown game " + id);
    }
}
