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
import org.perazimchurch.app.domain.games.engine.GameSession;
import org.perazimchurch.app.domain.games.engine.LevelProgression;
import org.perazimchurch.app.domain.games.vine.VineWordsLevel;
import org.perazimchurch.app.domain.games.vine.VineWordsLevels;
import org.perazimchurch.app.domain.games.vine.VineWordsSession;
import org.perazimchurch.app.presentation.ui.games.GamesHubDialog;
import org.perazimchurch.app.presentation.ui.games.common.GameChrome;
import org.perazimchurch.app.ui.components.FeedbackManager;
import org.perazimchurch.app.ui.theme.PerazimTheme;

import java.util.List;

/**
 * The Vine Words play screen. Opens from the Games list through GameRegistry. It starts at the
 * highest unlocked level; the results and pause screens come from {@link GameChrome}.
 */
public final class VineWordsDialog extends Dialog implements GameChrome.Host {

    private static final String GAME_ID = "vine_words";

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
        startLevel(new LevelProgression(new Store()).unlockedLevel(scorePrefix + GAME_ID));
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
        hint.setOnClickListener(v -> {
            click(v);
            String text = session.hint();
            message.setText(text == null ? "No hints left on this level." : text);
            updateStatus();
            board.invalidate();
        });
        Button shuffle = button("Shuffle", false);
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
            feedback().gameCue(FeedbackManager.GameCue.MATCH);
            boolean bonus = word.equals(level.bonus());
            message.setText(bonus ? "Bonus word: " + word + ". One extra point." : "Found " + word + ".");
            if (!bonus) {
                board.glowWord(word);
                wheel.pulseRing();
            }
        } else {
            message.setText(word.length() < 2 ? " " : "Not a word in this puzzle. Try another swipe.");
        }
        updateStatus();
        board.invalidate();
        if (session.finished()) {
            feedback().gameCue(FeedbackManager.GameCue.FOUND_ALL);
            feedback().celebrateIn(getWindow());
            chrome().finishedRun(GAME_ID, "Vine Words", session, levelNumber, VineWordsLevels.LEVEL_COUNT,
                    () -> startLevel(levelNumber));
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
        scroll.setFillViewport(true);
        scroll.setBackground(PerazimTheme.createBackgroundGradient());
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
        button.setTextColor(primary ? PerazimTheme.COLOR_BG_DEEP : PerazimTheme.COLOR_TEXT_PRIMARY);
        button.setMinHeight(dp(48));
        button.setGravity(Gravity.CENTER);
        button.setPadding(dp(12), dp(10), dp(12), dp(10));
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(primary ? PerazimTheme.COLOR_ORANGE_BRAND : PerazimTheme.COLOR_SURFACE_2);
        shape.setCornerRadius(dp(14));
        button.setBackground(shape);
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
