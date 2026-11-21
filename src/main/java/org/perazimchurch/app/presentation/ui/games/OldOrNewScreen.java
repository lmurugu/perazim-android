package org.perazimchurch.app.presentation.ui.games;

import android.app.Activity;
import android.content.SharedPreferences;
import android.widget.Button;

import org.perazimchurch.app.domain.games.TestamentSort;
import org.perazimchurch.app.domain.games.catalog.TestamentDefinition;
import org.perazimchurch.app.domain.games.catalog.TestamentSession;
import org.perazimchurch.app.domain.games.engine.GameDefinition;
import org.perazimchurch.app.domain.games.engine.GameSession;
import org.perazimchurch.app.presentation.ui.games.common.GameChrome;
import org.perazimchurch.app.presentation.ui.games.common.GameUi;

import java.util.function.BooleanSupplier;

/**
 * Play screen for Old or New?. Drives the engine session and finishes through
 * {@link GameChrome#finishedRun(GameDefinition, String, GameSession, int, Runnable)}.
 * Stars are saved for level 1 through GameResults.finishRun.
 */
public final class OldOrNewScreen {

    private static final String TITLE = "Old or New?";

    private final Activity activity;
    private final SharedPreferences scores;
    private final String scorePrefix;
    private final BooleanSupplier ownerCheck;
    private final GameDefinition definition = new TestamentDefinition(0);
    private long seed = System.nanoTime();

    public OldOrNewScreen(Activity activity, SharedPreferences scores, String scorePrefix, BooleanSupplier ownerCheck) {
        this.activity = activity;
        this.scores = scores;
        this.scorePrefix = scorePrefix;
        this.ownerCheck = ownerCheck;
    }

    /** Starts a fresh run on the current screen. */
    public void start() {
        newRun();
    }

    private void newRun() {
        render((TestamentSession) definition.createSession(1, seed), null);
    }

    private GameUi ui(Runnable backAction) {
        return new GameUi(activity, backAction).configure(scores, scorePrefix, ownerCheck, () -> seed = System.nanoTime());
    }

    private void render(TestamentSession game, String feedback) {
        GameUi ui = ui(() -> activity.onBackPressed());
        activity.setContentView(ui.page(TITLE, "Place each book in its testament. Eight books per run.", true));
        if (game.finished()) {
            String key = scorePrefix + "TESTAMENT_SORT";
            if (game.score() > scores.getInt(key, 0)) scores.edit().putInt(key, game.score()).apply();
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
