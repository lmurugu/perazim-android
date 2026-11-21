package org.perazimchurch.app.presentation.ui.games.common;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.widget.FrameLayout;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
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
        GameFx.attachPress(view);
        return view;
    }

    /** Applies the game button look to an existing button: the orange or purple pill frame, or a plain fill. */
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
        // The pill frame is drawn with a height-based corner, so it reads as a rounded button at any width.
        Drawable pill = GameArt.frame(activity, primary ? GameArt.FRAME_PILL_ORANGE : GameArt.FRAME_PILL_PURPLE,
                GameArt.FRAME_BORDER, dp(26));
        button.setBackground(pill != null ? pill : shape(primary ? PerazimTheme.COLOR_ORANGE_BRAND : PerazimTheme.COLOR_SURFACE_2));
    }

    /**
     * A card-style panel on the current page, framed with the purple panel from the UI sheet. Add rows to it
     * with the returned layout's addView, or pass it to {@link #addText}.
     */
    public LinearLayout panel() {
        LinearLayout panel = new LinearLayout(activity);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(16), dp(16), dp(16), dp(16));
        Drawable frame = GameArt.frame(activity, GameArt.FRAME_PANEL_WIDE, GameArt.FRAME_BORDER, dp(18));
        panel.setBackground(frame != null ? frame : shape(PerazimTheme.COLOR_SURFACE));
        page.addView(panel, wrapParams(12));
        return panel;
    }

    /** Level nodes on one page of the level map. */
    public static final int LEVEL_NODES_PER_PAGE = GameArt.LEVEL_NODES.length;
    private static final int LEVEL_NODE_DP = 84;
    private static final int LEVEL_MAP_DP = 360;

    /**
     * The shared level picker. Levels are drawn on the level-map art, eight nodes per page, with Earlier
     * and Later to turn the page. A level is open once the one before it has two or more stars. Locked
     * levels are dimmed, each open node shows its best stars, and the newest unlocked level shimmers.
     * The picker opens on the page that holds the newest unlocked level. {@code onPick} runs with the
     * level the player taps. Uses the games preferences and the owner prefix from {@link #configure}.
     */
    public void levelPicker(String gameId, int levelCount, IntConsumer onPick) {
        String storedId = (scorePrefix == null ? "" : scorePrefix) + gameId;
        int count = Math.max(1, levelCount);
        int unlocked = Math.min(count, GameResults.unlockedLevel(scores, storedId));
        int pages = (count + LEVEL_NODES_PER_PAGE - 1) / LEVEL_NODES_PER_PAGE;
        int startPage = Math.max(0, Math.min(pages - 1, (unlocked - 1) / LEVEL_NODES_PER_PAGE));
        LinearLayout holder = new LinearLayout(activity);
        holder.setOrientation(LinearLayout.VERTICAL);
        page.addView(holder, wrapParams(0));
        drawLevelPage(holder, storedId, count, pages, startPage, onPick);
    }

    private void drawLevelPage(LinearLayout holder, String storedId, int count, int pages, int pageIndex, IntConsumer onPick) {
        holder.removeAllViews();
        int first = pageIndex * LEVEL_NODES_PER_PAGE + 1;
        int last = Math.min(count, first + LEVEL_NODES_PER_PAGE - 1);
        int unlocked = Math.min(count, GameResults.unlockedLevel(scores, storedId));

        LevelMapView map = new LevelMapView(activity, dp(LEVEL_NODE_DP));
        Bitmap art = GameArt.levelMap(activity);
        if (art != null) map.setBackground(new BitmapDrawable(activity.getResources(), art));
        Button newest = null;
        for (int level = first; level <= last; level++) {
            boolean open = level <= unlocked;
            boolean isNewest = level == unlocked;
            String label = open
                    ? level + "\n" + GameResults.starLine(GameResults.bestStars(scores, storedId, level))
                    : level + "\nLocked";
            Button node = new Button(activity);
            styleButton(node, label, isNewest);
            node.setMinHeight(dp(LEVEL_NODE_DP));
            node.setMinimumWidth(dp(LEVEL_NODE_DP));
            GameFx.attachPress(node);
            node.setEnabled(open);
            node.setAlpha(open ? 1f : 0.4f);
            final int chosen = level;
            node.setOnClickListener(v -> {
                if (!activeOwner()) return;
                click(v);
                onPick.accept(chosen);
            });
            map.addView(node);
            if (isNewest) newest = node;
        }
        holder.addView(map, new LinearLayout.LayoutParams(-1, dp(LEVEL_MAP_DP)));
        if (newest != null) {
            Button shine = newest;
            shine.post(() -> GameFx.shimmer(shine));
        }

        if (pages > 1) {
            LinearLayout nav = new LinearLayout(activity);
            nav.setOrientation(LinearLayout.HORIZONTAL);
            if (pageIndex > 0) {
                nav.addView(pageButton("Earlier", () -> drawLevelPage(holder, storedId, count, pages, pageIndex - 1, onPick)));
            }
            if (pageIndex < pages - 1) {
                nav.addView(pageButton("Later", () -> drawLevelPage(holder, storedId, count, pages, pageIndex + 1, onPick)));
            }
            holder.addView(nav, wrapParams(0));
        }
    }

    private Button pageButton(String label, Runnable turn) {
        Button button = new Button(activity);
        styleButton(button, label, false);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, -2, 1f);
        lp.setMargins(dp(4), 0, dp(4), dp(10));
        button.setLayoutParams(lp);
        GameFx.attachPress(button);
        button.setOnClickListener(v -> {
            if (!activeOwner()) return;
            click(v);
            turn.run();
        });
        return button;
    }

    /** Places each node on a point of the level map, in the order of {@link GameArt#LEVEL_NODES}. */
    private static final class LevelMapView extends FrameLayout {
        private final int nodePx;

        LevelMapView(Context context, int nodePx) {
            super(context);
            this.nodePx = nodePx;
        }

        @Override protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
            float[][] nodes = GameArt.levelNodesFor(right - left, bottom - top);
            int count = Math.min(getChildCount(), nodes.length);
            for (int i = 0; i < count; i++) {
                int cx = Math.round(nodes[i][0]);
                int cy = Math.round(nodes[i][1]);
                getChildAt(i).layout(cx - nodePx / 2, cy - nodePx / 2, cx + nodePx / 2, cy + nodePx / 2);
            }
        }
    }

    /** Vertical space of the given height in dp on the current page. */
    public void gap(int height) {
        page.addView(new View(activity), new LinearLayout.LayoutParams(1, dp(height)));
    }
}
