package org.perazimchurch.app.presentation.ui.games;

import android.widget.Button;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.List;
import org.perazimchurch.app.domain.games.catalog.StoryChapterBank;
import org.perazimchurch.app.domain.games.catalog.StoryTimelineSession;
import org.perazimchurch.app.domain.games.catalog.TimelineDefinition;
import org.perazimchurch.app.domain.games.engine.GameSession;
import org.perazimchurch.app.presentation.ui.games.common.GameChrome;
import org.perazimchurch.app.presentation.ui.games.common.GameContext;
import org.perazimchurch.app.presentation.ui.games.common.GameFx;
import org.perazimchurch.app.presentation.ui.games.common.GameResults;
import org.perazimchurch.app.presentation.ui.games.common.GameUi;

/**
 * Story Timeline play screen. Events are shown shuffled; tapping the next one in story order snaps
 * it into place, a wrong tap shakes and gives a gentle nudge.
 */
public final class StoryTimelineScreen {

    static final String ID = "story_timeline";
    private static final TimelineDefinition GAME = new TimelineDefinition(0);

    private StoryTimelineScreen() {}

    /** Opens the level picker. Only unlocked levels can be played. */
    public static void show(GameContext ctx) {
        GameUi ui = ctx.ui();
        int unlocked = GameResults.unlockedLevel(ctx.scores(), ctx.scorePrefix() + ID);
        ui.page(GAME.title(), "Choose a level");
        for (int level = 1; level <= GAME.levelCount(); level++) {
            final int chosen = level;
            Button button = ui.button(GAME.levelName(level), level == unlocked);
            button.setEnabled(level <= unlocked);
            button.setOnClickListener(v -> startRun(ctx, chosen));
        }
    }

    static void startRun(GameContext ctx, int level) {
        GameSession session = GAME.createSession(level, System.nanoTime());
        renderRun(ctx, level, (StoryTimelineSession) session);
    }

    private static void renderRun(GameContext ctx, int level, StoryTimelineSession run) {
        GameUi ui = ctx.ui();
        GameChrome chrome = new GameChrome(ui);
        ui.page(GAME.title(), GAME.levelName(level) + ", tap in story order");
        ui.text(run.prompt(), 16, false);
        TextView progress = ui.text("Placed 0 of " + run.size(), 14, true);
        TextView feedbackLine = ui.text("", 14, false);

        List<Button> buttons = new ArrayList<>();
        List<StoryChapterBank.Event> events = run.shuffled();
        for (int i = 0; i < events.size(); i++) {
            final int index = i;
            Button button = ui.button(events.get(i).title, false);
            buttons.add(button);
            button.setOnClickListener(v -> {
                GameSession.Outcome outcome = run.answer(String.valueOf(index));
                if (outcome == GameSession.Outcome.CORRECT) {
                    GameFx.correct(button);
                    button.setEnabled(false);
                }
                if (outcome == GameSession.Outcome.WRONG) GameFx.wrong(button);
                progress.setText("Placed " + run.placed() + " of " + run.size());
                feedbackLine.setText(run.lastFeedback() == null ? "" : run.lastFeedback());
                if (run.finished()) {
                    Runnable replay = () -> startRun(ctx, level);
                    Runnable next = level < GAME.levelCount() ? () -> startRun(ctx, level + 1) : null;
                    chrome.finishedRun(GAME, GAME.title(), run, level, replay, next);
                }
            });
        }
        chrome.pauseButton(() -> renderRun(ctx, level, run));
    }
}
