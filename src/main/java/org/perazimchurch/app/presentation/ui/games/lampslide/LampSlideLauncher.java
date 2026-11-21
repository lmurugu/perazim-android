package org.perazimchurch.app.presentation.ui.games.lampslide;

import android.app.Activity;
import android.app.Dialog;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.perazimchurch.app.data.local.session.SessionManager;
import org.perazimchurch.app.domain.games.catalog.LampSlideDefinition;
import org.perazimchurch.app.domain.games.engine.GameDefinition;
import org.perazimchurch.app.domain.games.engine.KeyValueStore;
import org.perazimchurch.app.domain.games.engine.LevelProgression;
import org.perazimchurch.app.domain.games.lampslide.LampSlideLevels;
import org.perazimchurch.app.domain.games.lampslide.LampSlidePuzzle;
import org.perazimchurch.app.domain.games.lampslide.LampSlideScene;
import org.perazimchurch.app.domain.games.lampslide.LampSlideSession;
import org.perazimchurch.app.presentation.ui.games.GamesHubDialog;
import org.perazimchurch.app.presentation.ui.games.common.GameCatalog;
import org.perazimchurch.app.presentation.ui.games.common.GameChrome;
import org.perazimchurch.app.ui.components.FeedbackManager;
import org.perazimchurch.app.ui.theme.PerazimTheme;

/**
 * Opens Lamp Slide full screen from the games registry. Shows the level picker first, then plays
 * a level. Results go through GameChrome so saved stars and the catalog star rule apply. Levels
 * unlock through LevelProgression. Works offline.
 */
public final class LampSlideLauncher {

    static final String GAME_ID = "lamp_slide";
    private static final String TITLE = "Lamp Slide";
    private static final String SCORES_PREFS = "perazim_game_scores_v1";

    private LampSlideLauncher() {}

    /** Opens the level picker. */
    public static void open(Activity activity) {
        GameDefinition game = find();
        if (game == null) return;
        new Run(activity, game).showPicker();
    }

    private static GameDefinition find() {
        for (GameDefinition game : GameCatalog.all()) {
            if (GAME_ID.equals(game.id())) return game;
        }
        return null;
    }

    /** One open dialog: the picker, the play screen and the results. Also the chrome's host. */
    private static final class Run implements GameChrome.Host {
        private final Activity activity;
        private final GameDefinition game;
        private final Dialog dialog;
        private final ScrollView scroll;
        private final LevelProgression progression;
        private LinearLayout page;
        private Button lastNext;
        private int selected;
        private long seed;

        Run(Activity activity, GameDefinition game) {
            this.activity = activity;
            this.game = game;
            this.progression = new LevelProgression(new ProgressStore(prefsFile()));
            this.dialog = new Dialog(activity, android.R.style.Theme_Translucent_NoTitleBar);
            this.scroll = new ScrollView(activity);
            scroll.setBackground(PerazimTheme.createBackgroundGradient());
            scroll.setFillViewport(true);
            dialog.setContentView(scroll);
            Window window = dialog.getWindow();
            if (window != null) window.setLayout(-1, -1);
            this.selected = Math.max(1, progression.unlockedLevel(GAME_ID));
        }

        private SharedPreferences prefsFile() {
            return activity.getSharedPreferences(SCORES_PREFS, Activity.MODE_PRIVATE);
        }

        /** The level picker: bands of levels, stars per level, a preview and Start. */
        void showPicker() {
            if (!dialog.isShowing()) dialog.show();
            int unlocked = progression.unlockedLevel(GAME_ID);
            page = newPage();
            text("Lamp Slide", 24, true);
            text("Choose a painting", 13, false);
            text("Stars so far: " + totalStars(), 13, false);
            LampSlideDefinition definition = (LampSlideDefinition) game;
            LampSlideScene scene = definition.sceneFor(selected);
            int size = LampSlideLevels.boardSize(selected);
            ImageView image = new ImageView(activity);
            int resource = LampSlideScenes.imageResource(activity, scene.image());
            if (resource != 0) image.setImageResource(resource);
            image.setScaleType(ImageView.ScaleType.CENTER_CROP);
            page.addView(image, new LinearLayout.LayoutParams(-1, dp(220)));
            text("Level " + selected + " · " + scene.reference(), 16, true);
            text(size + " × " + size + " board · par " + LampSlidePuzzle.walkLength(size) + " moves", 13, false);
            text("“" + scene.verse() + "”", 14, false);
            band("3 × 3 board", 1, 10, unlocked);
            band("4 × 4 board", 11, 25, unlocked);
            band("5 × 5 board", 26, 40, unlocked);
            Button start = button("Start level " + selected, true);
            start.setOnClickListener(v -> start(selected, System.nanoTime()));
            Button close = button("Close", false);
            close.setOnClickListener(v -> dialog.dismiss());
        }

        private void band(String heading, int from, int to, int unlocked) {
            text(heading.toUpperCase(java.util.Locale.ROOT), 12, true);
            GridLayout grid = new GridLayout(activity);
            grid.setColumnCount(5);
            for (int level = from; level <= to; level++) {
                final int value = level;
                boolean open = level <= unlocked;
                int stars = progression.bestStars(GAME_ID, level);
                Button cell = button(level + "\n" + starText(stars), false);
                cell.setEnabled(open);
                cell.setAlpha(open ? 1f : 0.4f);
                cell.setOnClickListener(v -> {
                    selected = value;
                    showPicker();
                });
                GridLayout.LayoutParams params = new GridLayout.LayoutParams();
                params.width = 0;
                params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
                params.setMargins(dp(3), dp(3), dp(3), dp(3));
                grid.addView(cell, params);
            }
            page.addView(grid, new LinearLayout.LayoutParams(-1, -2));
        }

        private void start(int level, long newSeed) {
            seed = newSeed;
            selected = level;
            page = newPage();
            LampSlideSession session = (LampSlideSession) game.createSession(level, seed);
            View screen = LampSlideScreen.create(activity, session, level, this::showPicker, () -> finish(session, level));
            page.addView(screen, new LinearLayout.LayoutParams(-1, -2));
        }

        /** Shows the shared results, then enables Next level when the next level is open. */
        private void finish(LampSlideSession session, int level) {
            lastNext = null;
            new GameChrome(this).finishedRun(game, TITLE, session, level, () -> start(level, System.nanoTime()));
            if (lastNext != null && level < LampSlideLevels.LEVEL_COUNT
                    && progression.unlockedLevel(GAME_ID) > level) {
                Button next = lastNext;
                next.setEnabled(true);
                next.setAlpha(1f);
                next.setOnClickListener(v -> start(level + 1, System.nanoTime()));
            }
        }

        private int totalStars() {
            int total = 0;
            for (int level = 1; level <= LampSlideLevels.LEVEL_COUNT; level++) {
                total += progression.bestStars(GAME_ID, level);
            }
            return total;
        }

        private static String starText(int stars) {
            StringBuilder out = new StringBuilder();
            for (int i = 1; i <= 3; i++) out.append(i <= stars ? "★" : "☆");
            return out.toString();
        }

        private LinearLayout newPage() {
            LinearLayout page = new LinearLayout(activity);
            page.setOrientation(LinearLayout.VERTICAL);
            page.setGravity(Gravity.CENTER_HORIZONTAL);
            int pad = dp(16);
            page.setPadding(pad, pad, pad, pad);
            scroll.removeAllViews();
            scroll.addView(page, new ScrollView.LayoutParams(-1, -2));
            return page;
        }

        @Override public Activity activity() { return activity; }

        @Override public SharedPreferences scores() {
            return prefsFile();
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
            if ("Next level".equals(label)) lastNext = button;
            page.addView(button, new LinearLayout.LayoutParams(-1, -2));
            return button;
        }

        @Override public void click(View v) {
            FeedbackManager.get(activity).click(v);
        }

        @Override public void newSeed() {
            seed = System.nanoTime();
        }

        /** "Back to Games" in the results returns to the picker. */
        @Override public void hub() {
            showPicker();
        }

        private int dp(int value) {
            return Math.round(value * activity.getResources().getDisplayMetrics().density);
        }
    }

    /** Progress stored in the same preferences file as the other games. */
    private static final class ProgressStore implements KeyValueStore {
        private final SharedPreferences prefs;

        ProgressStore(SharedPreferences prefs) { this.prefs = prefs; }

        @Override public int getInt(String key, int defaultValue) { return prefs.getInt(key, defaultValue); }

        @Override public void putInt(String key, int value) { prefs.edit().putInt(key, value).apply(); }
    }
}
