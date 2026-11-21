package org.perazimchurch.app.presentation.ui.games.common;

import android.content.SharedPreferences;
import android.graphics.drawable.BitmapDrawable;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import org.perazimchurch.app.domain.games.catalog.Levels;

import java.util.function.IntConsumer;

/**
 * Level picker shared by the five games, so they all feel the same. Twelve levels are drawn on
 * the level-map art, eight nodes per page: levels 1 to 8 on the first page and 9 to 12 on the
 * second, with Earlier and Later to turn the page. A level is locked until the one before it has
 * two or more stars, read through {@link GameResults}.
 */
public final class LevelPicker {

    /** Level nodes drawn on one page of the map. */
    public static final int NODES_PER_PAGE = 8;
    /** Pages needed for every level. */
    public static final int PAGES = (Levels.COUNT + NODES_PER_PAGE - 1) / NODES_PER_PAGE;

    private static final int NODE_DP = 84;
    private static final int MAP_DP = 360;

    private LevelPicker() { }

    /** The page that shows a level. */
    public static int pageOf(int level) {
        return (Levels.clamp(level) - 1) / NODES_PER_PAGE;
    }

    /**
     * Draws one page of the picker on the current page of {@code ui}. {@code storedId} is the
     * score-prefixed game id, {@code onPick} runs with the chosen level, and {@code onPage} runs
     * with the page to show when Earlier or Later is tapped (the screen redraws its header).
     */
    public static void addPage(GameUi ui, SharedPreferences prefs, String storedId, int page,
                               IntConsumer onPick, IntConsumer onPage) {
        int first = page * NODES_PER_PAGE + 1;
        int last = Math.min(Levels.COUNT, first + NODES_PER_PAGE - 1);
        int unlocked = GameResults.unlockedLevel(prefs, storedId);

        MapView map = new MapView(ui.activity());
        android.graphics.Bitmap art = GameArt.levelMap(ui.activity());
        if (art != null) map.setBackgroundDrawable(new BitmapDrawable(ui.activity().getResources(), art));
        for (int level = first; level <= last; level++) {
            map.addView(nodeButton(ui, prefs, storedId, level, level <= unlocked, onPick));
        }
        ui.addRow(map);

        if (page > 0) {
            Button earlier = ui.button("Earlier", false);
            earlier.setOnClickListener(v -> { if (ui.activeOwner()) { ui.click(v); onPage.accept(page - 1); } });
        }
        if (page < PAGES - 1) {
            Button later = ui.button("Later", false);
            later.setOnClickListener(v -> { if (ui.activeOwner()) { ui.click(v); onPage.accept(page + 1); } });
        }
    }

    private static Button nodeButton(GameUi ui, SharedPreferences prefs, String storedId, int level,
                                     boolean open, IntConsumer onPick) {
        Button node = new Button(ui.activity());
        String label = open
                ? level + "\n" + stars(GameResults.bestStars(prefs, storedId, level))
                : level + "\nLocked";
        ui.styleButton(node, label, level == GameResults.unlockedLevel(prefs, storedId));
        node.setMinHeight(ui.dp(NODE_DP));
        node.setMinimumWidth(ui.dp(NODE_DP));
        GameFx.attachPress(node);
        node.setEnabled(open);
        node.setOnClickListener(v -> {
            if (!ui.activeOwner()) return;
            ui.click(v);
            onPick.accept(level);
        });
        return node;
    }

    private static String stars(int count) {
        StringBuilder line = new StringBuilder();
        for (int i = 0; i < 3; i++) line.append(i < count ? "★" : "☆");
        return line.toString();
    }

    /** Places each child on a node of the level map, in the order of {@link GameArt#LEVEL_NODES}. */
    private static final class MapView extends FrameLayout {
        MapView(android.content.Context context) {
            super(context);
            setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(MAP_DP)));
        }

        private int dp(int value) {
            return Math.round(value * getResources().getDisplayMetrics().density);
        }

        @Override
        protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
            int width = right - left;
            int height = bottom - top;
            float[][] nodes = GameArt.levelNodesFor(width, height);
            int size = dp(NODE_DP);
            int count = Math.min(getChildCount(), nodes.length);
            for (int i = 0; i < count; i++) {
                int cx = Math.round(nodes[i][0]);
                int cy = Math.round(nodes[i][1]);
                getChildAt(i).layout(cx - size / 2, cy - size / 2, cx + size / 2, cy + size / 2);
            }
        }
    }
}
