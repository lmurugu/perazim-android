package org.perazimchurch.app.presentation.ui.games.lampslide;

import android.app.Activity;
import android.app.Dialog;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.perazimchurch.app.data.local.session.SessionManager;
import org.perazimchurch.app.domain.games.engine.GameDefinition;
import org.perazimchurch.app.domain.games.lampslide.LampSlideSession;
import org.perazimchurch.app.presentation.ui.games.GamesHubDialog;
import org.perazimchurch.app.presentation.ui.games.common.GameCatalog;
import org.perazimchurch.app.presentation.ui.games.common.GameChrome;
import org.perazimchurch.app.ui.components.FeedbackManager;
import org.perazimchurch.app.ui.theme.PerazimTheme;

/**
 * Opens Lamp Slide full screen from the games registry. The shared results screen comes from
 * GameChrome, so the catalog star rule and the saved stars apply. Works offline.
 */
public final class LampSlideLauncher {

    static final String GAME_ID = "lamp_slide";
    private static final String TITLE = "Lamp Slide";
    private static final String SCORES_PREFS = "perazim_game_scores_v1";

    private LampSlideLauncher() {}

    /** Opens a fresh run at level 1, the same entry the registry uses. */
    public static void open(Activity activity) {
        GameDefinition game = find();
        if (game == null) return;
        new Run(activity, game).start(1, System.nanoTime());
    }

    private static GameDefinition find() {
        for (GameDefinition game : GameCatalog.all()) {
            if (GAME_ID.equals(game.id())) return game;
        }
        return null;
    }

    /** One open dialog: the play screen, then the shared results screen. Also the chrome's host. */
    private static final class Run implements GameChrome.Host {
        private final Activity activity;
        private final GameDefinition game;
        private final Dialog dialog;
        private final ScrollView scroll;
        private LinearLayout page;
        private long seed;

        Run(Activity activity, GameDefinition game) {
            this.activity = activity;
            this.game = game;
            this.dialog = new Dialog(activity, android.R.style.Theme_Translucent_NoTitleBar);
            this.scroll = new ScrollView(activity);
            scroll.setBackground(PerazimTheme.createBackgroundGradient());
            scroll.setFillViewport(true);
            dialog.setContentView(scroll);
            Window window = dialog.getWindow();
            if (window != null) window.setLayout(-1, -1);
        }

        void start(int level, long newSeed) {
            seed = newSeed;
            if (!dialog.isShowing()) dialog.show();
            LampSlideSession session = (LampSlideSession) game.createSession(level, seed);
            page = newPage();
            View screen = LampSlideScreen.create(activity, session, level, this::hub, () ->
                    new GameChrome(this).finishedRun(game, TITLE, session, level, () -> start(level, System.nanoTime())));
            page.addView(screen, new LinearLayout.LayoutParams(-1, -2));
        }

        private LinearLayout newPage() {
            LinearLayout page = new LinearLayout(activity);
            page.setOrientation(LinearLayout.VERTICAL);
            int pad = dp(16);
            page.setPadding(pad, pad, pad, pad);
            scroll.removeAllViews();
            scroll.addView(page, new ScrollView.LayoutParams(-1, -2));
            return page;
        }

        @Override public Activity activity() { return activity; }

        @Override public SharedPreferences scores() {
            return activity.getSharedPreferences(SCORES_PREFS, Activity.MODE_PRIVATE);
        }

        @Override public String scorePrefix() {
            return GamesHubDialog.ownerKey(SessionManager.getInstance(activity).getActiveUserId());
        }

        @Override public boolean activeOwner() { return dialog.isShowing(); }

        @Override public void screen(String title, String subtitle) {
            page = newPage();
            text(title, 24, true);
            text(subtitle, 13, false);
        }

        @Override public TextView text(String value, int sp, boolean bold) {
            TextView view = new TextView(activity);
            view.setText(value);
            view.setTextSize(sp);
            view.setTextColor(0xFFFFFFFF);
            view.setGravity(Gravity.CENTER_HORIZONTAL);
            view.setPadding(0, dp(6), 0, dp(6));
            if (bold) view.setTypeface(view.getTypeface(), Typeface.BOLD);
            page.addView(view, new LinearLayout.LayoutParams(-1, -2));
            return view;
        }

        @Override public Button button(String label, boolean primary) {
            Button button = new Button(activity);
            button.setText(label);
            button.setAllCaps(false);
            button.setTextColor(0xFFFFFFFF);
            page.addView(button, new LinearLayout.LayoutParams(-1, -2));
            return button;
        }

        @Override public void click(View v) {
            FeedbackManager.get(activity).click(v);
        }

        @Override public void newSeed() {
            seed = System.nanoTime();
        }

        @Override public void hub() {
            dialog.dismiss();
        }

        private int dp(int value) {
            return Math.round(value * activity.getResources().getDisplayMetrics().density);
        }
    }
}
