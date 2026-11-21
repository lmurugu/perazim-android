package org.perazimchurch.app.presentation.ui.games.common;

import android.app.Activity;
import android.content.SharedPreferences;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

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
        showResults(host.scores(), host.scorePrefix() + gameId, title, stars, score, size, replay);
    }

    /**
     * Results for an engine run. Saves the stars once through GameResults.finishRun, which also
     * takes lives into account, then shows them.
     */
    public void finishedRun(String gameId, String title, GameSession session, Runnable replay) {
        finishedRun(gameId, title, session, 1, 1, replay);
    }

    /** Results for an engine run on a given level. Stars are saved for that level only. */
    public void finishedRun(String gameId, String title, GameSession session, int level, int levelCount, Runnable replay) {
        int stars = GameResults.finishRun(host.scores(), host.scorePrefix() + gameId, level, levelCount, session);
        showResults(host.scores(), host.scorePrefix() + gameId, title, stars, session.score(), session.size(), replay);
    }

    /** Results screen: stars, score, today's verse, Replay and Back to Games. */
    private void showResults(SharedPreferences prefs, String storedId, String title, int stars, int score, int size, Runnable replay) {
        host.screen(title, "Run complete");
        host.text(GameResults.starLine(stars), 28, true);
        host.text("Score: " + score + " of " + size, 16, true);
        TextView verse = host.text("Loading today's verse...", 13, false);
        GameResults.loadVerseOfDay(host.activity(), loaded -> verse.setText(loaded));
        Button again = host.button("Replay", true);
        again.setOnClickListener(v -> {
            if (!host.activeOwner()) return;
            host.newSeed();
            replay.run();
        });
        Button next = host.button("Next level", false);
        next.setEnabled(false);
        host.text("Best on this phone: " + GameResults.starLine(GameResults.bestStars(prefs, storedId)), 13, false);
        host.text("Get 2 stars or more to unlock the next level.", 12, false);
        host.button("Back to Games", false).setOnClickListener(v -> host.hub());
    }

    /** Pause button for an active run. Resume re-renders the same question. */
    public void pauseButton(Runnable resume) {
        Button pause = host.button("Pause", false);
        pause.setOnClickListener(v -> {
            if (!host.activeOwner()) return;
            host.click(v);
            host.screen("Paused", "Your run is saved on this screen. Take a breath.");
            host.button("Resume", true).setOnClickListener(r -> {
                if (!host.activeOwner()) return;
                resume.run();
            });
            host.button("Back to Games", false).setOnClickListener(r -> host.hub());
        });
    }
}
