package org.perazimchurch.app.presentation.ui.games.vine;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.perazimchurch.app.R;
import org.perazimchurch.app.data.local.session.SessionManager;
import org.perazimchurch.app.domain.games.catalog.VineWordsDefinition;
import org.perazimchurch.app.domain.games.engine.GameSession;
import org.perazimchurch.app.domain.games.engine.LevelProgression;
import org.perazimchurch.app.domain.games.vine.VineWordsLevel;
import org.perazimchurch.app.domain.games.vine.VineWordsLevels;
import org.perazimchurch.app.domain.games.vine.VineWordsSession;
import org.perazimchurch.app.presentation.ui.games.GamesHubDialog;
import org.perazimchurch.app.presentation.ui.games.common.GameChrome;
import org.perazimchurch.app.presentation.ui.games.common.GameArt;
import org.perazimchurch.app.presentation.ui.games.common.GameFx;
import org.perazimchurch.app.presentation.ui.games.common.GameResults;
import org.perazimchurch.app.ui.components.FeedbackManager;
import org.perazimchurch.app.ui.theme.PerazimTheme;

import java.util.List;

/**
 * The Vine Words play screen. Opens from the Games list through GameRegistry. It starts at the
 * highest unlocked level; the results and pause screens come from {@link GameChrome}.
 */
public final class VineWordsDialog extends Dialog implements GameChrome.Host {

    private static final String GAME_ID = "vine_words";
    private static final VineWordsDefinition DEFINITION = new VineWordsDefinition(0, VineWordsContent::levels);

    private final Activity activity;
    private final String ownerId;
    private final String scorePrefix;
    private final SharedPreferences scores;
    private LinearLayout page;
    private VineWordsLevel level;
    private int levelNumber;
    private VineWordsSession session;
    private VineBoardView board;
    private LetterWheelView wheel;
    private TextView status;
    private TextView message;
    private long shuffleSeed = System.nanoTime();

    private VineWordsDialog(Activity activity) {
        super(activity, PerazimTheme.isLight()
                ? android.R.style.Theme_Material_Light_NoActionBar
                : android.R.style.Theme_Material_NoActionBar);
        this.activity = activity;
        this.ownerId = SessionManager.getInstance(activity).getActiveUserId();
        this.scorePrefix = GamesHubDialog.ownerKey(ownerId);
        this.scores = activity.getSharedPreferences("perazim_game_scores_v1", Context.MODE_PRIVATE);
    }

    /** Opens Vine Words on the given activity. */
    public static void show(Activity activity) {
        if (activity.isFinishing() || activity.isDestroyed()) return;
        VineWordsDialog dialog = new VineWordsDialog(activity);
        dialog.show();
        Window window = dialog.getWindow();
        if (window != null) {
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            window.getDecorView().setBackground(PerazimTheme.createBackgroundGradient());
        }
    }

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        Window window = getWindow();
        if (window != null) window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        renderPicker();
    }

    /** Level picker: the passage of the highest unlocked level, then one tile per level. */
    private void renderPicker() {
        int unlocked = new LevelProgression(new Store()).unlockedLevel(scorePrefix + GAME_ID);
        List<VineWordsLevel> levels = VineWordsContent.levels();
        int highlight = Math.max(1, Math.min(unlocked, levels.size()));
        screen("Vine Words", "Choose a level");
        text("PASSAGE", 12, true);
        VineWordsLevel current = levels.get(highlight - 1);
        LinearLayout card = new LinearLayout(activity);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(14), dp(14), dp(14), dp(14));
        GradientDrawable cardShape = new GradientDrawable();
        cardShape.setColor(PerazimTheme.COLOR_SURFACE);
        cardShape.setCornerRadius(dp(14));
        card.setBackground(cardShape);
        TextView reference = new TextView(activity);
        reference.setText(current.passage());
        reference.setTextSize(18);
        reference.setTextColor(PerazimTheme.COLOR_TEXT_PRIMARY);
        reference.setTypeface(null, android.graphics.Typeface.BOLD);
        card.addView(reference);
        StringBuilder verse = new StringBuilder();
        for (VineWordsLevel.Verse v : current.verses()) {
            if (verse.length() > 0) verse.append(' ');
            verse.append(v.text);
        }
        TextView quote = new TextView(activity);
        quote.setText("\u201C" + verse + "\u201D");
        quote.setTextSize(14);
        quote.setTextColor(PerazimTheme.COLOR_TEXT_SECONDARY);
        quote.setPadding(0, dp(6), 0, dp(6));
        card.addView(quote);
        TextView words = new TextView(activity);
        words.setText("Words to find: " + String.join(" \u00B7 ", current.targetWords()).toLowerCase(java.util.Locale.ROOT));
        words.setTextSize(13);
        words.setTextColor(PerazimTheme.COLOR_TEXT_SECONDARY);
        card.addView(words);
        page.addView(card, new LinearLayout.LayoutParams(-1, -2));
        text("LEVEL " + highlight + " OF " + levels.size(), 12, true);
        LevelProgression progression = new LevelProgression(new Store());
        String seenKey = scorePrefix + GAME_ID + ".seenUnlock";
        int seen = scores.getInt(seenKey, 1);
        if (unlocked > seen) {
            // Shimmer the level that was just unlocked, then remember it as seen.
            newlyUnlocked = unlocked;
            scores.edit().putInt(seenKey, unlocked).apply();
        }
        // Level-map path: scenes stack from the top band down to level 1 at the bottom, each
        // with up to NODES_PER_SCENE levels along the painted path. The page scrolls through them.
        android.graphics.Bitmap map = GameArt.levelMap(activity);
        int sceneCount = (levels.size() + NODES_PER_SCENE - 1) / NODES_PER_SCENE;
        LevelMapScene[] scenes = new LevelMapScene[sceneCount];
        for (int scene = sceneCount - 1; scene >= 0; scene--) {
            LevelMapScene view = new LevelMapScene(activity, map);
            scenes[scene] = view;
            int first = scene * NODES_PER_SCENE + 1;
            int last = Math.min(levels.size(), first + NODES_PER_SCENE - 1);
            for (int number = first; number <= last; number++) {
                TextView tile = levelTile(number, number <= unlocked,
                        progression.bestStars(scorePrefix + GAME_ID, number), number == highlight);
                view.addView(tile);
                if (number == newlyUnlocked) tile.post(() -> GameFx.shimmer(tile));
            }
            page.addView(view, new LinearLayout.LayoutParams(-1, -2));
        }
        // Open the scroll on the scene with the level to play, so it is in view.
        LevelMapScene playScene = scenes[(highlight - 1) / NODES_PER_SCENE];
        if (scrollView != null) {
            scrollView.post(() -> scrollView.scrollTo(0, Math.max(0, playScene.getTop() - dp(120))));
        }
    }

    private static final int NODES_PER_SCENE = 8;
    private ScrollView scrollView;

    private int newlyUnlocked = -1;

    private TextView levelTile(int number, boolean open, int stars, boolean current) {
        TextView tile = new TextView(activity);
        tile.setGravity(Gravity.CENTER);
        tile.setTextSize(13);
        tile.setTypeface(null, android.graphics.Typeface.BOLD);
        tile.setText(open && stars > 0 ? number + "\n" + GameResults.starLine(stars) : String.valueOf(number));
        tile.setTextColor(current ? PerazimTheme.COLOR_BG_DEEP : PerazimTheme.COLOR_TEXT_PRIMARY);
        GradientDrawable shape = new GradientDrawable();
        shape.setCornerRadius(dp(12));
        shape.setColor(current ? PerazimTheme.COLOR_ORANGE_BRAND : PerazimTheme.COLOR_SURFACE_2);
        tile.setBackground(shape);
        tile.setAlpha(open ? 1f : 0.4f);
        tile.setContentDescription((open ? "Level " : "Locked level ") + number);
        tile.setLayoutParams(new android.widget.FrameLayout.LayoutParams(dp(46), dp(46)));
        if (open) {
            tile.setOnClickListener(v -> { click(v); startLevel(number); });
            GameFx.attachPress(tile);
        }
        return tile;
    }

    // ---- levels and the play screen ----

    private void startLevel(int number) {
        List<VineWordsLevel> levels = VineWordsContent.levels();
        levelNumber = Math.max(1, Math.min(number, levels.size()));
        level = levels.get(levelNumber - 1);
        session = new VineWordsSession(level);
        shuffleSeed = System.nanoTime();
        renderPlay();
    }

    private void renderPlay() {
        screen("Vine Words", "Swipe letters on the wheel to fill the crossword.");
        status = text("", 14, false);
        board = new VineBoardView(activity, session);
        page.addView(board, new LinearLayout.LayoutParams(-1, -2));
        message = text(" ", 14, false);
        wheel = new LetterWheelView(activity);
        wheel.setListener(new LetterWheelView.Listener() {
            @Override public void onSwipe(String word) { answer(word); }
            @Override public void onLetter() { feedback().gameCue(FeedbackManager.GameCue.PICK); }
        });
        wheel.setLetters(level.wheelOrder(shuffleSeed));
        page.addView(wheel, new LinearLayout.LayoutParams(-1, dp(300)));
        LinearLayout row = new LinearLayout(activity);
        row.setOrientation(LinearLayout.HORIZONTAL);
        Button hint = button("Hint", false);
        GameFx.attachPress(hint);
        hint.setOnClickListener(v -> {
            click(v);
            String text = session.hint();
            message.setText(text == null ? "No hints left on this level." : text);
            updateStatus();
            board.invalidate();
        });
        Button shuffle = button("Shuffle", false);
        GameFx.attachPress(shuffle);
        shuffle.setOnClickListener(v -> {
            click(v);
            shuffleSeed = System.nanoTime();
            wheel.setLetters(level.wheelOrder(shuffleSeed));
        });
        row.addView(hint, weighted());
        row.addView(shuffle, weighted());
        page.addView(row, new LinearLayout.LayoutParams(-1, -2));
        chrome().pauseButton(this::renderPlay);
        updateStatus();
    }

    private void answer(String word) {
        if (session == null || session.finished()) return;
        GameSession.Outcome outcome = session.answer(word);
        if (outcome == GameSession.Outcome.CORRECT) {
            boolean bonus = word.equals(level.bonus());
            message.setText(bonus ? "Bonus word: " + word + ". One extra point." : "Found " + word + ".");
            if (bonus) {
                GameFx.correct(message);
            } else {
                GameFx.correct(board);
                board.flyWord(word);
                wheel.pulseRing();
            }
        } else if (word.length() >= 2) {
            message.setText("Not a word in this puzzle. Try another swipe.");
            GameFx.wrong(wheel);
        } else {
            message.setText(" ");
        }
        updateStatus();
        if (session.finished()) {
            feedback().gameCue(FeedbackManager.GameCue.FOUND_ALL);
            feedback().celebrateIn(getWindow());
            int finished = levelNumber;
            Runnable next = finished < VineWordsLevels.LEVEL_COUNT ? () -> startLevel(finished + 1) : null;
            board.burst(() -> chrome().finishedRun(DEFINITION, "Vine Words", session, finished,
                    () -> startLevel(finished), next));
        }
    }

    private void updateStatus() {
        status.setText("Level " + levelNumber + " of " + VineWordsLevels.LEVEL_COUNT
                + " · " + level.passage() + "\nWords " + session.solvedWords().size() + " of " + session.size()
                + " · Score " + session.score() + " · Hints used " + session.hintsUsed());
    }

    // ---- GameChrome.Host ----

    @Override public Activity activity() { return activity; }
    @Override public SharedPreferences scores() { return scores; }
    @Override public String scorePrefix() { return scorePrefix; }
    @Override public boolean activeOwner() {
        return ownerId != null && ownerId.equals(SessionManager.getInstance(activity).getActiveUserId());
    }
    @Override public void screen(String title, String subtitle) {
        ScrollView scroll = new ScrollView(activity);
        scrollView = scroll;
        scroll.setFillViewport(true);
        GameFx.enter(scroll);
        scroll.setBackground(org.perazimchurch.app.presentation.ui.games.common.GameScene.background(activity, "vine_scene"));
        page = new LinearLayout(activity);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(16), dp(16), dp(16), dp(28));
        scroll.addView(page, new ScrollView.LayoutParams(-1, -2));
        setContentView(scroll);
        ImageView back = new ImageView(activity);
        back.setImageResource(R.drawable.ic_back_round);
        back.setPadding(dp(10), dp(10), dp(10), dp(10));
        back.setContentDescription("Close Vine Words");
        GradientDrawable glass = new GradientDrawable();
        glass.setShape(GradientDrawable.OVAL);
        glass.setColor(PerazimTheme.COLOR_SURFACE_2);
        glass.setStroke(dp(1), PerazimTheme.COLOR_STROKE);
        back.setBackground(glass);
        back.setClickable(true);
        back.setOnClickListener(v -> dismiss());
        LinearLayout.LayoutParams backParams = new LinearLayout.LayoutParams(dp(44), dp(44));
        backParams.bottomMargin = dp(12);
        page.addView(back, backParams);
        text(title, 22, true);
        text(subtitle, 13, false);
    }
    @Override public TextView text(String value, int size, boolean bold) {
        TextView view = new TextView(activity);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(bold ? PerazimTheme.COLOR_TEXT_PRIMARY : PerazimTheme.COLOR_TEXT_SECONDARY);
        view.setTypeface(null, bold ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
        view.setPadding(0, dp(4), 0, dp(4));
        page.addView(view, new LinearLayout.LayoutParams(-1, -2));
        return view;
    }
    @Override public void addRow(View row) {
        page.addView(row, new LinearLayout.LayoutParams(-1, -2));
    }
    @Override public Button button(String label, boolean primary) {
        Button view = new Button(activity);
        styleButton(view, label, primary);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.bottomMargin = dp(10);
        page.addView(view, params);
        return view;
    }
    @Override public void click(View v) { feedback().click(v); }
    @Override public void newSeed() { shuffleSeed = System.nanoTime(); }
    @Override public void hub() { dismiss(); }

    private GameChrome chrome() { return new GameChrome(this); }
    private FeedbackManager feedback() { return FeedbackManager.get(activity); }

    private void styleButton(Button button, String label, boolean primary) {
        button.setText(label);
        button.setAllCaps(false);
        button.setTextSize(15);
        button.setTextColor(PerazimTheme.COLOR_WHITE);
        button.setMinHeight(dp(56));
        button.setGravity(Gravity.CENTER);
        button.setPadding(dp(12), dp(10), dp(12), dp(10));
        button.setBackground(org.perazimchurch.app.presentation.ui.games.common.GameButtons.drawn(activity, primary));
    }

    private LinearLayout.LayoutParams weighted() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, -2, 1f);
        params.setMargins(0, 0, dp(6), dp(10));
        return params;
    }

    private int dp(int value) { return Math.round(value * activity.getResources().getDisplayMetrics().density); }

    /** Per-owner progress store, backed by the same preferences as the other games. */
    private final class Store implements org.perazimchurch.app.domain.games.engine.KeyValueStore {
        @Override public int getInt(String key, int defaultValue) { return scores.getInt(key, defaultValue); }
        @Override public void putInt(String key, int value) { scores.edit().putInt(key, value).apply(); }
    }
}
