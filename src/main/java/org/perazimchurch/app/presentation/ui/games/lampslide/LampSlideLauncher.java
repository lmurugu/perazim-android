package org.perazimchurch.app.presentation.ui.games.lampslide;

import android.app.Activity;
import android.app.Dialog;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.perazimchurch.app.presentation.ui.games.common.GameAudioSettings;
import org.perazimchurch.app.domain.games.catalog.LampSlideDefinition;
import org.perazimchurch.app.domain.games.engine.GameDefinition;
import org.perazimchurch.app.domain.games.engine.KeyValueStore;
import org.perazimchurch.app.domain.games.engine.LevelProgression;
import org.perazimchurch.app.domain.games.lampslide.LampSlideLevels;
import org.perazimchurch.app.domain.games.lampslide.LampSlidePuzzle;
import org.perazimchurch.app.domain.games.lampslide.LampSlideScene;
import org.perazimchurch.app.domain.games.lampslide.LampSlideSession;
import org.perazimchurch.app.presentation.ui.games.common.GameCatalog;
import org.perazimchurch.app.presentation.ui.games.common.GameAudio;
import org.perazimchurch.app.presentation.ui.games.common.GameContext;
import org.perazimchurch.app.presentation.ui.games.common.GameResults;
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
    /** Levels per page of the level map: one per node on the painted path. */
    static final int MAP_PAGE_SIZE = 8;

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
        /** Which page of the level map is showing: eight levels per page. */
        private int mapPage;
        private long seed;
        private boolean pickerShown;

        Run(GameContext ctx, GameDefinition game) {
            this.ctx = ctx;
            this.activity = ctx.activity();
            this.game = game;
            this.storedId = ctx.scorePrefix() + game.id();
            this.progression = new LevelProgression(new ProgressStore(ctx.scores()));
            this.dialog = new Dialog(activity, PerazimTheme.isLight()
                    ? android.R.style.Theme_Material_Light_NoActionBar
                    : android.R.style.Theme_Material_NoActionBar);
            this.dialog.setOnDismissListener(d -> ctx.hub().run());
            this.scroll = new ScrollView(activity);
            scroll.setBackground(PerazimTheme.createBackgroundGradient());
            scroll.setFillViewport(true);
            dialog.setContentView(scroll);
            Window window = dialog.getWindow();
            if (window != null) window.setLayout(-1, -1);
            this.selected = Math.max(1, progression.unlockedLevel(storedId));
            this.mapPage = (selected - 1) / MAP_PAGE_SIZE;
        }

        /** The level picker: bands of levels, stars per level, a preview and Start. */
        void showPicker() {
            GameAudio.get(activity).stopAnnouncer();
            if (!activeOwner()) { dialog.dismiss(); return; }
            if (!dialog.isShowing()) {
                dialog.show();
                if (dialog.getWindow() != null) dialog.getWindow().setLayout(-1, -1);
            }
            int unlocked = progression.unlockedLevel(storedId);
            page = newPage();
            text("Lamp Slide", 24, true);
            page.addView(GameAudioSettings.button(activity), new LinearLayout.LayoutParams(-2, -2));
            text("Choose a painting", 13, false);
            text(GameResults.clearedCount(scores(), storedId, game.levelCount(), false) + " / "
                    + game.levelCount() + " cleared · " + totalStars() + " stars", 13, false);
            LampSlideDefinition definition = (LampSlideDefinition) game;
            LampSlideScene scene = definition.sceneFor(selected);
            int size = LampSlideLevels.boardSize(selected);
            ImageView image = new ImageView(activity);
            android.graphics.Bitmap painting = LampSlideScenes.loadImage(activity, scene.image(), null);
            if (painting != null) image.setImageBitmap(painting);
            image.setScaleType(ImageView.ScaleType.CENTER_CROP);
            page.addView(image, new LinearLayout.LayoutParams(-1, dp(171)));
            text("Level " + selected + " · " + scene.reference(), 16, true);
            text(size + " × " + size + " board · par " + LampSlidePuzzle.walkLength(size) + " moves", 13, false);
            text("“" + scene.verse() + "”", 14, false);
            boolean first = !pickerShown;
            pickerShown = true;
            levelMap(unlocked, first);
            Button start = button("Start level " + selected, true);
            start.setOnClickListener(v -> start(selected, System.nanoTime()));
            GameFx.attachPress(start);
            Button close = button("Close", false);
            close.setOnClickListener(v -> dialog.dismiss());
            GameFx.attachPress(close);
            if (first) GameFx.enter(page);
        }

        /**
         * The level map: eight levels at a time on the painted path, five pages for forty levels.
         * Locked nodes are dimmed. Earlier and Later move between pages.
         */
        private void levelMap(int unlocked, boolean animate) {
            final int pages = (LampSlideLevels.LEVEL_COUNT + MAP_PAGE_SIZE - 1) / MAP_PAGE_SIZE;
            mapPage = Math.max(0, Math.min(mapPage, pages - 1));
            final int pageFirst = mapPage * MAP_PAGE_SIZE + 1;
            final int pageLast = Math.min(LampSlideLevels.LEVEL_COUNT, pageFirst + MAP_PAGE_SIZE - 1);

            text("Levels " + pageFirst + " to " + pageLast, 12, true);
            LinearLayout row = null;
            for (int level = pageFirst; level <= pageLast; level++) {
                if ((level - pageFirst) % 4 == 0) {
                    row = new LinearLayout(activity);
                    page.addView(row, new LinearLayout.LayoutParams(-1, -2));
                }
                final int value = level;
                boolean open = level <= unlocked;
                int stars = progression.bestStars(storedId, level);
                Button node = detached(level + "\n" + (open ? starText(stars) : "Locked"), level == selected);
                node.setTextSize(12);
                node.setEnabled(open);
                node.setAlpha(open ? 1f : .55f);
                node.setOnClickListener(v -> { selected = value; showPicker(); });
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, -2, 1);
                params.setMargins(dp(2), dp(2), dp(2), dp(2));
                row.addView(node, params);
                if (animate && level == unlocked) node.post(() -> GameFx.shimmer(node));
            }

            LinearLayout nav = new LinearLayout(activity);
            nav.setOrientation(LinearLayout.HORIZONTAL);
            Button earlier = detached("Earlier levels", false);
            earlier.setEnabled(mapPage > 0);
            earlier.setOnClickListener(v -> {
                mapPage--;
                showPicker();
            });
            GameFx.attachPress(earlier);
            Button later = detached("Later levels", false);
            later.setEnabled(mapPage < pages - 1);
            later.setOnClickListener(v -> {
                mapPage++;
                showPicker();
            });
            GameFx.attachPress(later);
            nav.addView(earlier, new LinearLayout.LayoutParams(0, -2, 1f));
            nav.addView(later, new LinearLayout.LayoutParams(0, -2, 1f));
            page.addView(nav, new LinearLayout.LayoutParams(-1, -2));
        }

        private void start(int level, long newSeed) {
            if (!activeOwner() || level > progression.unlockedLevel(storedId)) return;
            GameAudio.get(activity).startRound(GAME_ID);
            seed = newSeed;
            selected = level;
            page = newPage();
            LampSlideSession session = (LampSlideSession) game.createSession(level, seed);
            View screen = LampSlideScreen.create(activity, session, level, () -> {
                        selected = progression.unlockedLevel(storedId);
                        mapPage = (selected - 1) / MAP_PAGE_SIZE;
                        showPicker();
                    },
                    () -> {
                        if (activeOwner()) GameResults.saveLevel(scores(), storedId, level, game.levelCount(),
                                game.starRule().stars(session), true, result(session));
                    },
                    () -> finish(session, level));
            page.addView(screen, new LinearLayout.LayoutParams(-1, -2));
        }

        private String result(LampSlideSession session) {
            return session.puzzle().moves() + " moves · Par " + session.puzzle().par()
                    + " · " + session.hintsUsed() + " hints";
        }

        private void finish(LampSlideSession session, int level) {
            int nextLevel = level + 1;
            boolean hasNext = nextLevel <= game.levelCount() && progression.unlockedLevel(storedId) >= nextLevel;
            int side = LampSlideLevels.boardSize(nextLevel);
            new GameChrome(this).showLevelResult(TITLE, level, game.starRule().stars(session), true,
                    result(session), hasNext ? "Next painting · " + side + " × " + side + " board" : "All paintings cleared.",
                    GameAudio.get(activity).announcerLine(GAME_ID, GameAudio.Reaction.WIN),
                    hasNext ? () -> start(nextLevel, System.nanoTime()) : null,
                    () -> start(level, System.nanoTime()), () -> {
                        selected = progression.unlockedLevel(storedId);
                        mapPage = (selected - 1) / MAP_PAGE_SIZE;
                        showPicker();
                    });
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
            GameAudio.get(activity).setReactionWindow(dialog.getWindow());
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
            page.addView(GameAudioSettings.button(activity), new LinearLayout.LayoutParams(-2, -2));
            text(subtitle, 13, false);
        }

        @Override public TextView text(String value, int sp, boolean bold) {
            TextView view = new TextView(activity);
            view.setText(value);
            view.setTextSize(sp);
            view.setTextColor(PerazimTheme.COLOR_TEXT_PRIMARY);
            view.setGravity(Gravity.CENTER_HORIZONTAL);
            view.setPadding(0, dp(6), 0, dp(6));
            if (bold) view.setTypeface(view.getTypeface(), Typeface.BOLD);
            page.addView(view, new LinearLayout.LayoutParams(-1, -2));
            return view;
        }

        @Override public void addRow(View row) {
            page.addView(row, new LinearLayout.LayoutParams(-1, -2));
        }

        @Override public Button button(String label, boolean primary) {
            Button button = new Button(activity);
            ctx.ui().styleButton(button, label, primary);
            GameFx.attachPress(button);
            page.addView(button, new LinearLayout.LayoutParams(-1, -2));
            return button;
        }

        /** A button that is not on the page yet. button() adds to the page, so it is taken back out here. */
        private Button detached(String label, boolean primary) {
            Button b = button(label, primary);
            page.removeView(b);
            return b;
        }

        @Override public void click(View v) {
            FeedbackManager.get(activity).click(v);
        }

        @Override public void newSeed() {
            seed = System.nanoTime();
            ctx.seedReset().run();
        }

        @Override public void hub() { dialog.dismiss(); }

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
