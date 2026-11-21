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

import org.perazimchurch.app.domain.games.catalog.LampSlideDefinition;
import org.perazimchurch.app.domain.games.engine.GameDefinition;
import org.perazimchurch.app.domain.games.engine.KeyValueStore;
import org.perazimchurch.app.domain.games.engine.LevelProgression;
import org.perazimchurch.app.domain.games.lampslide.LampSlideLevels;
import org.perazimchurch.app.domain.games.lampslide.LampSlidePuzzle;
import org.perazimchurch.app.domain.games.lampslide.LampSlideScene;
import org.perazimchurch.app.domain.games.lampslide.LampSlideSession;
import org.perazimchurch.app.presentation.ui.games.common.GameCatalog;
import org.perazimchurch.app.presentation.ui.games.common.GameContext;
import org.perazimchurch.app.presentation.ui.games.common.GameFx;
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

    private LampSlideLauncher() {}

    /** Opens the level picker. Progress is saved under the signed-in account's prefix. */
    public static void open(GameContext ctx) {
        GameDefinition game = find();
        if (game == null) return;
        new Run(ctx, game).showPicker();
    }

    private static GameDefinition find() {
        for (GameDefinition game : GameCatalog.all()) {
            if (GAME_ID.equals(game.id())) return game;
        }
        return null;
    }

    /** One open dialog: the picker, the play screen and the results. Also the chrome's host. */
    private static final class Run implements GameChrome.Host {
        private final GameContext ctx;
        private final Activity activity;
        private final GameDefinition game;
        /** Stars and progress key: the account prefix plus the game id, as GameResults uses. */
        private final String storedId;
        private final Dialog dialog;
        private final ScrollView scroll;
        private final LevelProgression progression;
        private LinearLayout page;
        private int selected;
        private long seed;
        private boolean pickerShown;

        Run(GameContext ctx, GameDefinition game) {
            this.ctx = ctx;
            this.activity = ctx.activity();
            this.game = game;
            this.storedId = ctx.scorePrefix() + game.id();
            this.progression = new LevelProgression(new ProgressStore(ctx.scores()));
            this.dialog = new Dialog(activity, android.R.style.Theme_Translucent_NoTitleBar);
            this.scroll = new ScrollView(activity);
            scroll.setBackground(PerazimTheme.createBackgroundGradient());
            scroll.setFillViewport(true);
            dialog.setContentView(scroll);
            Window window = dialog.getWindow();
            if (window != null) window.setLayout(-1, -1);
            this.selected = Math.max(1, progression.unlockedLevel(storedId));
        }

        /** The level picker: bands of levels, stars per level, a preview and Start. */
        void showPicker() {
            if (!dialog.isShowing()) dialog.show();
            int unlocked = progression.unlockedLevel(storedId);
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
            boolean first = !pickerShown;
            pickerShown = true;
            band("3 × 3 board", 1, 10, unlocked, first);
            band("4 × 4 board", 11, 25, unlocked, first);
            band("5 × 5 board", 26, 40, unlocked, first);
            Button start = button("Start level " + selected, true);
            start.setOnClickListener(v -> start(selected, System.nanoTime()));
            GameFx.attachPress(start);
            Button close = button("Close", false);
            close.setOnClickListener(v -> dialog.dismiss());
            GameFx.attachPress(close);
            if (first) GameFx.enter(page);
        }

        private void band(String heading, int from, int to, int unlocked, boolean first) {
            text(heading.toUpperCase(java.util.Locale.ROOT), 12, true);
            GridLayout grid = new GridLayout(activity);
            grid.setColumnCount(5);
            for (int level = from; level <= to; level++) {
                final int value = level;
                boolean open = level <= unlocked;
                int stars = progression.bestStars(storedId, level);
                Button cell = button(level + "\n" + starText(stars), false);
                cell.setEnabled(open);
                cell.setAlpha(open ? 1f : 0.4f);
                cell.setOnClickListener(v -> {
                    selected = value;
                    showPicker();
                });
                GameFx.attachPress(cell);
                // The newest unlocked level sweeps a light shimmer once, when the picker opens.
                if (first && open && level == unlocked && unlocked > 1) {
                    cell.post(() -> GameFx.shimmer(cell));
                }
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

        /** Shared results. Next level is offered by GameChrome once the following level is unlocked. */
        private void finish(LampSlideSession session, int level) {
            new GameChrome(this).finishedRun(game, TITLE, session, level,
                    () -> start(level, System.nanoTime()),
                    () -> start(level + 1, System.nanoTime()));
        }

        private int totalStars() {
            int total = 0;
            for (int level = 1; level <= LampSlideLevels.LEVEL_COUNT; level++) {
                total += progression.bestStars(storedId, level);
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

        @Override public SharedPreferences scores() { return ctx.scores(); }

        @Override public String scorePrefix() { return ctx.scorePrefix(); }

        @Override public boolean activeOwner() { return ctx.ownerCheck().getAsBoolean(); }

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
            ctx.seedReset().run();
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
