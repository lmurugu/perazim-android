package org.perazimchurch.app.presentation.ui.games.common;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Typeface;
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
        return scroll;
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

    @Override public void hub() { runHub(); }

    @Override public void click(View v) { feedback().click(v); }

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
        return view;
    }

    /** Applies the game button look to an existing button. */
    public void styleButton(Button button, String label, boolean primary) {
        button.setText(label);
        button.setAllCaps(false);
        button.setTextSize(17);
        button.setTextColor(primary ? PerazimTheme.COLOR_BG_DEEP : PerazimTheme.COLOR_TEXT_PRIMARY);
        button.setMinHeight(dp(52));
        button.setMinimumWidth(0);
        button.setMinWidth(0);
        button.setGravity(Gravity.CENTER);
        button.setPadding(dp(12), dp(12), dp(12), dp(12));
        button.setBackground(shape(primary ? PerazimTheme.COLOR_ORANGE_BRAND : PerazimTheme.COLOR_SURFACE_2));
    }

    /** Vertical space of the given height in dp on the current page. */
    public void gap(int height) {
        page.addView(new View(activity), new LinearLayout.LayoutParams(1, dp(height)));
    }
}
