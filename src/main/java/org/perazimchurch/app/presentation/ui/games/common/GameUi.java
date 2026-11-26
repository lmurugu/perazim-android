package org.perazimchurch.app.presentation.ui.games.common;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.graphics.drawable.RippleDrawable;
import android.content.res.ColorStateList;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.perazimchurch.app.R;
import org.perazimchurch.app.ui.components.FeedbackManager;
import org.perazimchurch.app.ui.theme.PerazimTheme;

import java.util.function.BooleanSupplier;
import java.util.function.IntConsumer;

/**
 * Everything a game screen draws with: the scrolling page with its title and back button, text,
 * buttons, rounded shapes, spacing, the click sound, and the shared results and pause screens
 * (it implements {@link GameChrome.Host}).
 *
 * <pre>
 *   GameUi ui = new GameUi(activity, hub).configure(scores, prefix, ownerCheck, newSeed);
 *   setContentView(ui.page("Title", "Subtitle", true));
 *   ui.text("Question", 18, true);
 *   ui.button("Answer", true).setOnClickListener(...);
 * </pre>
 * See docs/games/ENGINE_USAGE.md.
 */
public final class GameUi implements GameChrome.Host {

    private final Activity activity;
    private final Runnable hubAction;
    private SharedPreferences scores;
    private String scorePrefix = "";
    private BooleanSupplier ownerCheck;
    private Runnable newSeed;
    private ScrollView scroll;
    private LinearLayout page;

    /** Drawing helpers only; no page until {@link #page} is called. The back button does nothing. */
    public GameUi(Activity activity) {
        this(activity, null);
    }

    /** {@code hub} runs when the back button or Back to Games is pressed. */
    public GameUi(Activity activity, Runnable hub) {
        this.activity = activity;
        this.hubAction = hub;
    }

    /**
     * Sets the score store, its key prefix, the owner check and the seed reset used by Replay.
     * Returns this, so it can be chained after the constructor.
     */
    public GameUi configure(SharedPreferences scores, String scorePrefix, BooleanSupplier ownerCheck, Runnable newSeed) {
        this.scores = scores;
        this.scorePrefix = scorePrefix == null ? "" : scorePrefix;
        this.ownerCheck = ownerCheck;
        this.newSeed = newSeed;
        return this;
    }

    /** Builds a new scrolling page with a title, subtitle and round back button. Attach the result as the content view. */
    public ScrollView page(String title) {
        return page(title, "", false);
    }

    /** Builds a new scrolling page with a title and subtitle. */
    public ScrollView page(String title, String subtitle) {
        return page(title, subtitle, false);
    }

    /**
     * Builds a new scrolling page. {@code playing} true means the back button returns to the games
     * list (Back to Games); false means it closes the hub. Both call the hub action.
     */
    public ScrollView page(String title, String subtitle, boolean playing) {
        scroll = new ScrollView(activity);
        scroll.setFillViewport(true);
        scroll.setBackground(PerazimTheme.createBackgroundGradient());
        // Edge to edge like the rest of the app: content is padded below the status bar and above the nav bar.
        scroll.setClipToPadding(false);
        scroll.setOnApplyWindowInsetsListener((v, insets) -> {
            scroll.setPadding(0, insets.getSystemWindowInsetTop(), 0, insets.getSystemWindowInsetBottom());
            return insets;
        });
        page = new LinearLayout(activity);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(14), dp(16), dp(14), dp(28));
        scroll.addView(page, new ScrollView.LayoutParams(-1, -2));
        // Small round glass back button, matching the mockups (not a full-width button).
        ImageView back = new ImageView(activity);
        back.setImageResource(R.drawable.ic_back_round);
        back.setPadding(dp(10), dp(10), dp(10), dp(10));
        back.setScaleType(ImageView.ScaleType.FIT_CENTER);
        back.setClickable(true);
        back.setContentDescription(playing ? "Back to games" : "Close games");
        GradientDrawable glass = new GradientDrawable();
        glass.setShape(GradientDrawable.OVAL);
        glass.setColor(PerazimTheme.COLOR_SURFACE_2);
        glass.setStroke(dp(1), PerazimTheme.COLOR_STROKE);
        back.setBackground(glass);
        back.setOnClickListener(v -> runHub());
        LinearLayout.LayoutParams backParams = new LinearLayout.LayoutParams(dp(44), dp(44));
        backParams.bottomMargin = dp(12);
        page.addView(back, backParams);
        text(title, 24, true);
        text(subtitle == null ? "" : subtitle, 13, false);
        gap(16);
        GameFx.enter(scroll);
        return scroll;
    }

    /** Sets the illustrated scene behind the current page (see {@link GameScene}). Call after {@link #page}. */
    public void scene(String name) {
        if (scroll != null) scroll.setBackground(GameScene.background(activity, name));
    }

    /**
     * Uses an existing page as the target of text, buttons and gaps. For hosts that build their own
     * page (the games hub) and still want these helpers.
     */
    public void setPage(LinearLayout existing) { this.page = existing; }

    /** The scroll view of the current page, or null before {@link #page} is called. */
    public ScrollView scroll() { return scroll; }

    private void runHub() {
        if (hubAction != null) hubAction.run();
    }

    // ---- GameChrome.Host ----

    @Override public Activity activity() { return activity; }

    @Override public SharedPreferences scores() { return scores; }

    @Override public String scorePrefix() { return scorePrefix; }

    @Override public boolean activeOwner() { return ownerCheck == null || ownerCheck.getAsBoolean(); }

    @Override public void screen(String title, String subtitle) { page(title, subtitle, false); }

    @Override public void newSeed() { if (newSeed != null) newSeed.run(); }

    @Override public void addRow(View row) { page.addView(row, wrapParams(12)); }

    @Override public void hub() { runHub(); }

    @Override public void click(View v) {
        // Game taps use the shared tap sound; the app's tone is the fallback when audio is unavailable.
        if (GameAudio.get(activity).play(GameAudio.Sfx.TAP)) {
            if (v != null && feedback().isHapticsEnabled()) v.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY);
        } else {
            feedback().click(v);
        }
    }

    // ---- Drawing helpers ----

    public FeedbackManager feedback() { return FeedbackManager.get(activity); }

    /** Converts design dp to pixels for this screen. */
    public int dp(int value) { return Math.round(value * activity.getResources().getDisplayMetrics().density); }

    /** Rounded fill for cards and buttons. */
    public GradientDrawable shape(int color) {
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(color);
        shape.setCornerRadius(dp(14));
        return shape;
    }

    /** Full-width layout params with a bottom margin in dp. */
    public LinearLayout.LayoutParams wrapParams(int bottomDp) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.bottomMargin = dp(bottomDp);
        return lp;
    }

    /** Adds a text line to the current page. */
    @Override public TextView text(String value, int size, boolean bold) { return addText(page, value, size, bold); }

    /** Adds a text line to any parent. */
    public TextView addText(LinearLayout parent, String value, int size, boolean bold) {
        TextView view = new TextView(activity);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(bold ? PerazimTheme.COLOR_TEXT_PRIMARY : PerazimTheme.COLOR_TEXT_SECONDARY);
        if (bold) view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        view.setLineSpacing(dp(3), 1f);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.bottomMargin = dp(12);
        parent.addView(view, params);
        return view;
    }

    /** Adds a full-width button to the current page. Primary buttons use the orange brand fill. */
    @Override public Button button(String label, boolean primary) {
        Button view = new Button(activity);
        styleButton(view, label, primary);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.bottomMargin = dp(10);
        page.addView(view, params);
        GameFx.attachPress(view);
        return view;
    }

    /**
     * Applies the game button look to an existing button. Drawn, not bitmap: a rounded gradient with a soft
     * glow, so no square background shows behind it. Primary is orange; secondary is purple.
     */
    public void styleButton(Button button, String label, boolean primary) {
        button.setText(label);
        button.setAllCaps(false);
        button.setSoundEffectsEnabled(false);
        button.setTextSize(15);
        button.setTextColor(primary ? 0xFF321C34 : PerazimTheme.COLOR_TEXT_PRIMARY);
        button.setShadowLayer(0, 0, 0, 0);
        button.setTypeface(button.getTypeface(), android.graphics.Typeface.BOLD);
        button.setMinHeight(dp(48));
        button.setMinimumHeight(dp(48));
        button.setMinimumWidth(0);
        button.setMinWidth(0);
        button.setGravity(Gravity.CENTER);
        button.setPadding(dp(12), dp(7), dp(12), dp(7));
        GradientDrawable fill = shape(primary ? 0xFFFFC773 : PerazimTheme.COLOR_SURFACE_SOLID);
        fill.setStroke(dp(1), primary ? 0xFFDF9747 : PerazimTheme.COLOR_STROKE);
        button.setBackground(new RippleDrawable(ColorStateList.valueOf(0x265F3281), fill, null));
        button.setStateListAnimator(null);
        button.setElevation(0f);
    }

    /**
     * A card-style panel on the current page, framed with the purple panel from the UI sheet. Add rows to it
     * with the returned layout's addView, or pass it to {@link #addText}.
     */
    public LinearLayout panel() {
        LinearLayout panel = new LinearLayout(activity);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(16), dp(16), dp(16), dp(16));
        GradientDrawable fill = shape(PerazimTheme.COLOR_SURFACE_SOLID);
        fill.setStroke(dp(1), PerazimTheme.COLOR_STROKE);
        panel.setBackground(fill);
        page.addView(panel, wrapParams(12));
        return panel;
    }

    /** Compact level grid. The current level stays visible when the picker first opens. */
    public void levelPicker(String gameId, int levelCount, IntConsumer onPick) {
        levelPicker(gameId, levelCount, onPick, level -> "");
    }

    public void levelPicker(String gameId, int levelCount, IntConsumer onPick,
                            java.util.function.IntFunction<String> describe) {
        String id = scorePrefix + gameId;
        if (levelCount < 1) return;
        int unlocked = Math.min(levelCount, GameResults.unlockedLevel(scores, id));
        LinearLayout holder = new LinearLayout(activity);
        holder.setOrientation(LinearLayout.VERTICAL);
        addRow(holder);
        drawLevels(holder, id, levelCount, unlocked, (unlocked - 1) / 8, onPick, describe);
    }

    private void drawLevels(LinearLayout holder, String id, int count, int unlocked, int index,
                            IntConsumer onPick, java.util.function.IntFunction<String> describe) {
        holder.removeAllViews();
        addText(holder, GameResults.clearedCount(scores, id, count, false) + " of " + count + " levels cleared", 13, true);
        for (int i = unlocked; i > 0; i--) {
            String previous = GameResults.lastLevelResult(scores, id, i);
            if (!previous.isEmpty()) {
                addText(holder, "Level " + i + " · " + previous, 12, false);
                break;
            }
        }
        String detail = describe.apply(unlocked);
        if (!detail.isEmpty()) addText(holder, detail, 13, false);
        Button resume = new Button(activity);
        styleButton(resume, "Play level " + unlocked, true);
        resume.setOnClickListener(v -> { if (activeOwner()) onPick.accept(unlocked); });
        holder.addView(resume, wrapParams(14));
        int first = index * 8 + 1;
        int last = Math.min(count, first + 7);
        addText(holder, "Levels " + first + "–" + last, 13, true);
        LinearLayout row = null;
        for (int level = first; level <= last; level++) {
            if ((level - first) % 4 == 0) {
                row = new LinearLayout(activity);
                holder.addView(row, wrapParams(5));
            }
            int stars = GameResults.bestStars(scores, id, level);
            String label = level + "\n" + (level > unlocked ? "Locked" : GameResults.starLine(stars));
            Button node = new Button(activity);
            styleButton(node, label, level == unlocked);
            node.setTextSize(12);
            node.setEnabled(level <= unlocked);
            node.setAlpha(level <= unlocked ? 1f : .58f);
            final int chosen = level;
            node.setOnClickListener(v -> { if (activeOwner()) onPick.accept(chosen); });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, -2, 1);
            lp.setMargins(dp(2), 0, dp(2), 0);
            row.addView(node, lp);
        }
        LinearLayout nav = new LinearLayout(activity);
        holder.addView(nav, wrapParams(0));
        for (int direction : new int[] {-1, 1}) {
            int target = index + direction;
            if (target < 0 || target * 8 >= count) continue;
            Button button = new Button(activity);
            styleButton(button, direction < 0 ? "Earlier levels" : "Later levels", false);
            button.setOnClickListener(v -> {
                if (activeOwner()) drawLevels(holder, id, count, unlocked, target, onPick, describe);
            });
            nav.addView(button, new LinearLayout.LayoutParams(0, -2, 1));
        }
    }

    public void progress(int done, int total, String label) {
        android.widget.ProgressBar bar = new android.widget.ProgressBar(activity, null,
                android.R.attr.progressBarStyleHorizontal);
        bar.setMax(Math.max(1, total));
        bar.setProgress(Math.min(done, total));
        bar.setProgressTintList(ColorStateList.valueOf(PerazimTheme.isLight() ? 0xFF74329F : 0xFFCDA1F0));
        bar.setContentDescription(label);
        addRow(bar);
    }

    /** Vertical space of the given height in dp on the current page. */
    public void gap(int height) {
        page.addView(new View(activity), new LinearLayout.LayoutParams(1, dp(height)));
    }
}
