package org.perazimchurch.app.presentation.ui.games.common;

import android.app.Activity;
import android.content.SharedPreferences;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

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

    /** Results screen for a finished run: stars, score, today's verse, Replay and Back to Games. */
    public void finishedRun(String gameId, String title, int score, int size, Runnable replay) {
        int stars = GameResults.recordRun(host.scores(), host.scorePrefix() + gameId, score, size);
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
        host.text("More levels are coming soon.", 12, false);
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
