package org.perazimchurch.app.presentation.ui.games.common;

import android.app.Activity;
import android.content.SharedPreferences;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;

import org.perazimchurch.app.domain.games.engine.GameDefinition;
import org.perazimchurch.app.domain.games.engine.GameSession;

/**
 * The shared results screen and pause overlay for every game. A game screen supplies its drawing
 * and state through {@link Host}; this class decides what the results and pause screens show.
 * See docs/games/ENGINE_USAGE.md.
 */
public final class GameChrome {

    /** What the host screen provides. GamesHubDialog implements it for the games tab. */
    public interface Host {
        Activity activity();
        SharedPreferences scores();
        String scorePrefix();
        /** False when the owner changed while a game was open. Callers must stop. */
        boolean activeOwner();
        /** Draws a new page header: title and subtitle, no playing layout. */
        void screen(String title, String subtitle);
        TextView text(String value, int size, boolean bold);
        /** Adds a full-width row (for example the star row) to the page. */
        void addRow(View row);
        Button button(String label, boolean primary);
        void click(View v);
        /** Gives the next run a fresh seed before a replay. */
        void newSeed();
        /** Returns to the Games list. */
        void hub();
    }

    private final Host host;

    public GameChrome(Host host) {
        this.host = host;
    }

    /** Results for a run that is scored by score and size only (the older games). */
    public void finishedRun(String gameId, String title, int score, int size, Runnable replay) {
        int stars = GameResults.recordRun(host.scores(), host.scorePrefix() + gameId, score, size);
        showResults(host.scores(), host.scorePrefix() + gameId, title, stars, score, size, replay, null);
    }

    /**
     * Results for an engine run. Saves the stars once through GameResults.finishRun, which also
     * takes lives into account, then shows them.
     */
    public void finishedRun(String gameId, String title, GameSession session, Runnable replay) {
        finishedRun(gameId, title, session, 1, 1, replay);
    }

    /** Results for a catalog game, using that game's own star rule. */
    public void finishedRun(GameDefinition game, String title, GameSession session, int level, Runnable replay) {
        finishedRun(game, title, session, level, replay, null);
    }

    /**
     * Results for a catalog game with a Next level action. Next level is enabled when the level
     * after this one is unlocked; pass null to leave it off.
     */
    public void finishedRun(GameDefinition game, String title, GameSession session, int level, Runnable replay, Runnable next) {
        String storedId = host.scorePrefix() + game.id();
        int stars = GameResults.finishRun(host.scores(), storedId, game, level, session);
        boolean canNext = next != null && level < game.levelCount()
                && GameResults.unlockedLevel(host.scores(), storedId) > level;
        showResults(host.scores(), storedId, title, stars, session.score(), session.size(), replay, canNext ? next : null);
    }

    /** Results for an engine run on a given level. Stars are saved for that level only. */
    public void finishedRun(String gameId, String title, GameSession session, int level, int levelCount, Runnable replay) {
        int stars = GameResults.finishRun(host.scores(), host.scorePrefix() + gameId, level, levelCount, session);
        showResults(host.scores(), host.scorePrefix() + gameId, title, stars, session.score(), session.size(), replay, null);
    }

    /** Results screen: stars, score, today's verse, Replay and Back to Games. */
    private void showResults(SharedPreferences prefs, String storedId, String title, int stars, int score, int size,
                             Runnable replay, Runnable next) {
        host.screen(title, "Run complete");
        FrameLayout starFrame = GameFx.starRow(host.activity(), stars);
        host.addRow(starFrame);
        GameFx.popStars(starFrame, stars);
        TextView scoreLine = host.text("Score: 0 of " + size, 16, true);
        GameFx.countUp(scoreLine, score, "Score: ", " of " + size);
        TextView verse = host.text("Loading today's verse...", 13, false);
        GameResults.loadVerseOfDay(host.activity(), loaded -> verse.setText(loaded));
        Button again = host.button("Replay", true);
        again.setOnClickListener(v -> {
            if (!host.activeOwner()) return;
            host.newSeed();
            replay.run();
        });
        if (next != null) host.button("Next level", true).setOnClickListener(v -> {
            if (host.activeOwner()) next.run();
        });
        host.text("Best on this phone: " + GameResults.starLine(GameResults.bestStars(prefs, storedId)), 13, false);
        host.button("Back to Games", false).setOnClickListener(v -> host.hub());
    }

    /** Presents a result that has already been stored at the point of completion. */
    public void showLevelResult(String title, int level, int stars, boolean cleared, String result,
                                String nextDetail, String announcerLine, Runnable next, Runnable replay, Runnable levels) {
        if (!host.activeOwner()) return;
        host.screen(title, (cleared ? "Level cleared" : "Run ended") + " · Result saved on this phone");
        if (stars > 0) {
            FrameLayout frame = GameFx.starRow(host.activity(), stars);
            host.addRow(frame);
            GameFx.popStars(frame, stars, false);
        }
        host.text("Level " + level + " · " + result, 17, true);
        if (announcerLine != null && !announcerLine.isEmpty()) host.text(announcerLine, 15, false);
        if (nextDetail != null && !nextDetail.isEmpty()) host.text(nextDetail, 14, false);
        if (next != null) host.button("Next level · " + (level + 1), true)
                .setOnClickListener(v -> { if (host.activeOwner()) next.run(); });
        host.button("Replay level", next == null).setOnClickListener(v -> {
            if (host.activeOwner()) replay.run();
        });
        host.button("All levels", false).setOnClickListener(v -> {
            if (host.activeOwner()) levels.run();
        });
        host.button("Back to Games", false).setOnClickListener(v -> host.hub());
    }

    /** Pause button for an active run. Resume re-renders the same question. */
    public void pauseButton(Runnable resume) {
        Button pause = host.button("Pause", false);
        pause.setOnClickListener(v -> {
            if (!host.activeOwner()) return;
            host.click(v);
            GameAudio.get(host.activity()).stopAnnouncer();
            host.screen("Paused", "Your run is saved on this screen. Take a breath.");
            host.button("Resume", true).setOnClickListener(r -> {
                if (!host.activeOwner()) return;
                resume.run();
            });
            host.button("Back to Games", false).setOnClickListener(r -> host.hub());
        });
    }
}
