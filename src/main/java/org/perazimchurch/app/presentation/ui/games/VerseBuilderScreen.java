package org.perazimchurch.app.presentation.ui.games;

import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.perazimchurch.app.domain.games.BibleGames;
import org.perazimchurch.app.domain.games.catalog.HintRules;
import org.perazimchurch.app.domain.games.catalog.Levels;
import org.perazimchurch.app.domain.games.catalog.Streak;
import org.perazimchurch.app.domain.games.catalog.VerseBuilderSession;
import org.perazimchurch.app.domain.games.engine.GameDefinition;
import org.perazimchurch.app.domain.games.engine.GameSession;
import org.perazimchurch.app.presentation.ui.games.common.BibleBanks;
import org.perazimchurch.app.presentation.ui.games.common.GameChrome;
import org.perazimchurch.app.presentation.ui.games.common.GameContext;
import org.perazimchurch.app.presentation.ui.games.common.GameFx;
import org.perazimchurch.app.presentation.ui.games.common.GameUi;
import org.perazimchurch.app.presentation.ui.games.common.LevelPicker;

/**
 * Play screen for Verse Builder. Drives {@link VerseBuilderSession}: tap the shuffled word tiles
 * in order, then check the sentence. Each verse has one try, and the verse is shown afterwards.
 * Registered in GameLaunchers with {@code GameRegistry.register("verse_builder", ctx -> ...)}.
 */
public final class VerseBuilderScreen {

    private static final String TITLE = "Verse Builder";
    private static final int TILES_PER_ROW = 3;

    private final GameContext ctx;
    private final GameDefinition definition;
    private int level = 1;
    private int page;

    public VerseBuilderScreen(GameContext ctx) {
        this.ctx = ctx;
        this.definition = QuizScreen.find("verse_builder");
    }

    /** Opens the level picker on the current screen, on the page that holds the last level played. */
    public void start() {
        renderPicker(LevelPicker.pageOf(level));
    }

    private void renderPicker(int shown) {
        page = shown;
        GameUi ui = ctx.ui();
        ctx.activity().setContentView(ui.page(TITLE, "Pick a level. A level opens after two stars on the one before.", true));
        LevelPicker.addPage(ui, ctx.scores(), ctx.scorePrefix() + definition.id(), page, this::startRun, this::renderPicker);
    }

    /** Starts a run on one level, drawn from the verse bank for its band. */
    private void startRun(int chosen) {
        level = Levels.clamp(chosen);
        long seed = Levels.seed(level);
        render(new VerseBuilderSession(new BibleGames.Round(BibleBanks.verses(ctx.activity(), Levels.band(level)), seed), seed),
                null, false);
    }

    private void render(VerseBuilderSession game, String feedback, boolean popScore) {
        GameUi ui = ctx.ui();
        ctx.activity().setContentView(ui.page(TITLE, "Level " + level + " · Tap the words in order. One try per verse.", true));
        if (game.finished()) {
            new GameChrome(ui).finishedRun(definition, TITLE, game, level, () -> startRun(level), () -> startRun(level + 1));
            return;
        }
        TextView scoreLine = ui.text("Verse " + game.position() + " of " + game.size() + " · Score " + game.score(), 12, false);
        if (popScore) GameFx.pulse(scoreLine, 0xFFFFAA3C);
        if (game.streak().current() >= 2) {
            TextView streakBadge = ui.text("🔥 x" + game.streak().current(), 13, true);
            if (popScore) GameFx.pulse(streakBadge, 0xFFFFAA3C);
        }
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
        BibleGames.VersePuzzle puzzle = game.puzzle();
        String built = puzzle.sentence();
        TextView sentence = ui.text(built.isEmpty() ? "Tap the first word." : built, 17, true);
        if (built.isEmpty()) sentence.setAlpha(0.6f);

        LinearLayout row = null;
        int slot = 0;
        for (int id : puzzle.tiles()) {
            if (puzzle.selected(id)) continue;
            if (row == null || row.getChildCount() == TILES_PER_ROW) {
                row = newRow(ui);
            }
            Button tile = tileButton(ui, puzzle.word(id));
            row.addView(tile, tileParams(ui));
            staggerIn(tile, slot++);
            final int chosen = id;
            tile.setOnClickListener(v -> {
                if (!ui.activeOwner()) return;
                ui.click(v);
                puzzle.choose(chosen);
                render(game, null, false);
            });
        }

        LinearLayout controls = newRow(ui);
        if (!built.isEmpty()) {
            Button undo = tileButton(ui, "Undo");
            undo.setOnClickListener(v -> { if (ui.activeOwner()) { puzzle.undo(); render(game, null, false); } });
            controls.addView(undo, tileParams(ui));
            Button clear = tileButton(ui, "Clear");
            clear.setOnClickListener(v -> { if (ui.activeOwner()) { puzzle.clear(); render(game, null, false); } });
            controls.addView(clear, tileParams(ui));
        }
        if (puzzle.complete()) {
            Button check = ui.button("Check verse", true);
            check.setOnClickListener(v -> {
                if (!ui.activeOwner()) return;
                ui.click(v);
                GameSession.Outcome outcome = game.answer(puzzle.sentence());
                tapFeedback(v, outcome);
                String line = answerLine(game, outcome);
                v.postDelayed(() -> render(game, line, outcome == GameSession.Outcome.CORRECT), 280);
            });
        }
    }

    private LinearLayout newRow(GameUi ui) {
        LinearLayout row = new LinearLayout(ctx.activity());
        row.setOrientation(LinearLayout.HORIZONTAL);
        ui.addRow(row);
        return row;
    }

    private static Button tileButton(GameUi ui, String label) {
        Button tile = new Button(ui.activity());
        ui.styleButton(tile, label, false);
        GameFx.attachPress(tile);
        return tile;
    }

    private static LinearLayout.LayoutParams tileParams(GameUi ui) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, -2, 1f);
        params.setMargins(ui.dp(4), 0, ui.dp(4), ui.dp(8));
        return params;
    }

    /** The verse, with its reference, after the check. Null before the first answer. */
    private static String answerLine(VerseBuilderSession game, GameSession.Outcome outcome) {
        BibleGames.Card answered = game.lastCard();
        if (answered == null) return null;
        String lead = outcome == GameSession.Outcome.CORRECT
                ? "Correct. "
                : "Not quite. The verse reads: " + answered.answer + ". ";
        String bonus = game.streak().bonusAwarded() ? " Streak bonus +" + Streak.BONUS_POINTS + "." : "";
        return lead + answered.reference + "." + bonus;
    }

    private static void tapFeedback(View v, GameSession.Outcome outcome) {
        if (outcome == GameSession.Outcome.CORRECT) GameFx.correct(v); else GameFx.wrong(v);
    }

    private static void staggerIn(View v, int slot) {
        if (!GameFx.motionEnabled(v.getContext())) return;
        v.setAlpha(0f);
        v.animate().alpha(1f).setStartDelay(60L * slot).setDuration(200).start();
    }
}
