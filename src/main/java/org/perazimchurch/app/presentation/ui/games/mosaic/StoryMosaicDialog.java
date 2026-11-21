package org.perazimchurch.app.presentation.ui.games.mosaic;

import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.perazimchurch.app.domain.games.catalog.StoryMosaicSession;
import org.perazimchurch.app.domain.games.catalog.StoryMosaicDefinition;
import org.perazimchurch.app.domain.games.engine.GameSession;
import org.perazimchurch.app.presentation.ui.games.common.GameContext;
import org.perazimchurch.app.presentation.ui.games.common.GameArt;
import org.perazimchurch.app.presentation.ui.games.common.GameFx;
import org.perazimchurch.app.domain.games.engine.KeyValueStore;
import org.perazimchurch.app.domain.games.engine.LevelProgression;
import org.perazimchurch.app.domain.games.mosaic.MosaicBoard;
import org.perazimchurch.app.domain.games.mosaic.MosaicPuzzles;
import org.perazimchurch.app.domain.games.mosaic.MosaicStars;

import java.io.InputStream;
import java.util.List;

/**
 * Story Mosaic screens: the chapter map, then one puzzle at a time. Progress is kept on this
 * phone only, in the same preferences file as the other games. Nothing is uploaded.
 */
public final class StoryMosaicDialog extends Dialog {

    private static final String GAME_ID = "story_mosaic";
    private static final int BG = Color.parseColor("#2D0938");
    private static final int ORANGE = Color.parseColor("#E17D2F");

    private final Context appContext;
    private final SharedPreferences prefs;
    private final String progressId;
    private final LevelProgression progression;
    private final int levelCount = MosaicPuzzles.all().size();
    private final android.os.Handler mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());
    private FrameLayout root;
    private Bitmap scene;

    public StoryMosaicDialog(Context context, SharedPreferences prefs, String progressId) {
        super(context, android.R.style.Theme_DeviceDefault_NoActionBar_Fullscreen);
        this.appContext = context.getApplicationContext();
        this.prefs = prefs;
        this.progressId = progressId;
        this.progression = new LevelProgression(new PrefsStore(prefs));
        this.root = new FrameLayout(context);
        this.root.setBackgroundColor(BG);
        setContentView(root);
    }

    /** Opens Story Mosaic from the Games hub through GameRegistry. Progress is per account. */
    public static void show(GameContext ctx) {
        new StoryMosaicDialog(ctx.activity(), ctx.scores(), ctx.scorePrefix() + GAME_ID).show();
    }

    @Override
    public void show() {
        super.show();
        showMap();
    }

    @Override
    public void dismiss() {
        releaseScene();
        super.dismiss();
    }

    // ---- chapter map -------------------------------------------------------------------------

    private void showMap() {
        releaseScene();
        root.removeAllViews();
        LinearLayout column = column(dp(16));

        TextView title = text("Story Mosaic", 22, true);
        column.addView(title);
        column.addView(text("Put each Bible scene together. Every chapter is open.", 14, false));

        List<Integer> starts = MosaicPuzzles.chapterStarts();
        // The first chapter with a scene still to finish gets the shimmer.
        int nextChapter = -1;
        for (int c = 0; c < starts.size() && nextChapter < 0; c++) {
            int first0 = starts.get(c);
            int last0 = (c + 1 < starts.size() ? starts.get(c + 1) : levelCount) - 1;
            for (int i = first0; i <= last0; i++) {
                if (progression.bestStars(progressId, i + 1) == 0) {
                    nextChapter = c;
                    break;
                }
            }
        }
        View shimmerRow = null;
        for (int c = 0; c < starts.size(); c++) {
            int first = starts.get(c);
            int last = (c + 1 < starts.size() ? starts.get(c + 1) : levelCount) - 1;
            int done = 0;
            for (int i = first; i <= last; i++) {
                if (progression.bestStars(progressId, i + 1) > 0) done++;
            }
            final int target = nextUnfinished(first, last);
            Button row = new Button(getContext());
            row.setAllCaps(false);
            row.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
            row.setText(MosaicPuzzles.at(first).chapterTitle + "\n" + done + " of " + (last - first + 1) + " scenes");
            row.setTextColor(Color.WHITE);
            boolean complete = done == last - first + 1;
            android.graphics.drawable.Drawable pill = GameArt.frame(getContext(),
                    complete ? GameArt.FRAME_PILL_ORANGE : GameArt.FRAME_PILL_PURPLE,
                    GameArt.FRAME_BORDER, dp(18));
            if (pill != null) {
                row.setBackground(pill);
            } else {
                row.setBackgroundColor(complete ? ORANGE : Color.argb(36, 255, 255, 255));
            }
            row.setOnClickListener(v -> showPuzzle(target));
            GameFx.attachPress(row);
            // A rail on the left: the dot and the line below it light up when the chapter is finished.
            LinearLayout rail = new LinearLayout(getContext());
            rail.setOrientation(LinearLayout.VERTICAL);
            rail.setGravity(Gravity.CENTER_HORIZONTAL);
            View dot = new View(getContext());
            dot.setBackgroundColor(complete ? ORANGE : Color.argb(60, 255, 255, 255));
            rail.addView(dot, new LinearLayout.LayoutParams(dp(14), dp(14)));
            View link = new View(getContext());
            link.setBackgroundColor(complete ? ORANGE : Color.argb(60, 255, 255, 255));
            rail.addView(link, new LinearLayout.LayoutParams(dp(3), dp(50)));
            LinearLayout line = new LinearLayout(getContext());
            line.setOrientation(LinearLayout.HORIZONTAL);
            line.addView(rail, new LinearLayout.LayoutParams(dp(20), LinearLayout.LayoutParams.WRAP_CONTENT));
            line.addView(row, new LinearLayout.LayoutParams(0, dp(64), 1f));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.topMargin = dp(10);
            column.addView(line, lp);
            if (c == nextChapter) shimmerRow = row;
        }

        ScrollView scroll = new ScrollView(getContext());
        scroll.addView(column);
        root.addView(scroll, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        GameFx.enter(column);
        if (shimmerRow != null) {
            final View target = shimmerRow;
            target.post(() -> GameFx.shimmer(target));
        }
    }

    private int nextUnfinished(int first, int last) {
        for (int i = first; i <= last; i++) {
            if (progression.bestStars(progressId, i + 1) == 0) return i;
        }
        return first;
    }

    // ---- one puzzle --------------------------------------------------------------------------

    private void showPuzzle(int index) {
        releaseScene();
        int clamped = Math.max(0, Math.min(index, levelCount - 1));
        int level = clamped + 1;
        MosaicPuzzles.Puzzle puzzle = MosaicPuzzles.at(clamped);
        scene = loadScene(puzzle.assetName());
        StoryMosaicSession session = new StoryMosaicSession(puzzle, MosaicBoard.sideForLevel(level),
                level <= StoryMosaicDefinition.GUIDED_LEVELS, level * 7919L);

        root.removeAllViews();
        LinearLayout column = column(dp(12));

        LinearLayout header = new LinearLayout(getContext());
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        Button back = smallButton("Chapters");
        back.setOnClickListener(v -> showMap());
        header.addView(back);
        TextView heading = text(puzzle.chapterTitle, 18, true);
        heading.setPadding(dp(12), 0, 0, 0);
        header.addView(heading, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        column.addView(header);
        GameFx.enter(column);

        TextView progress = text(puzzle.reference + "  ·  scene " + puzzle.number, 14, false);
        column.addView(progress);

        TextView hint = text("", 14, false);
        hint.setTextColor(Color.parseColor("#F2C94C"));
        column.addView(hint);

        TextView caption = text("", 16, false);
        caption.setVisibility(View.GONE);
        caption.setGravity(Gravity.CENTER);

        Button hintButton = smallButton("Show a hint");
        hintButton.setVisibility(session.hintAvailable() ? View.VISIBLE : View.GONE);
        hintButton.setOnClickListener(v -> hint.setText(session.hint() == null ? "" : session.hint()));

        FrameLayout boardHolder = new FrameLayout(getContext());
        StoryMosaicView board = new StoryMosaicView(getContext(), session, scene, outcome -> {
            if (session.finished()) {
                onFinished(session, level, puzzle, caption, hintButton, hint, column);
            }
        });
        column.addView(boardHolder, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));
        boardHolder.addView(board, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));

        column.addView(hintButton);
        column.addView(caption);
        root.addView(column, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
    }

    private void onFinished(StoryMosaicSession session, int level, MosaicPuzzles.Puzzle puzzle,
                            TextView caption, Button hintButton, TextView hint, LinearLayout column) {
        int stars = MosaicStars.RULE.stars(session);
        progression.record(progressId, level, stars, levelCount);
        hintButton.setVisibility(View.GONE);
        hint.setText("");
        // Show the title at once; swap in the KJV text when the background read finishes.
        caption.setText(puzzle.title + "\n— " + puzzle.reference);
        caption.setVisibility(View.VISIBLE);
        GameFx.enter(caption);
        android.widget.FrameLayout burst = GameFx.starRow(getContext(), stars);
        column.addView(burst, column.indexOfChild(caption));
        GameFx.popStars(burst, stars);
        final Context ctx = appContext;
        final String reference = puzzle.reference;
        new Thread(() -> {
            String verse = KjvLookup.text(ctx, reference);
            if (verse == null) return;
            mainHandler.post(() -> caption.setText("\u201c" + verse + "\u201d\n— " + reference));
        }, "kjv-caption").start();
        Button next = smallButton(level < levelCount ? "Next scene" : "Back to chapters");
        next.setOnClickListener(v -> {
            if (level < levelCount) showPuzzle(level);
            else showMap();
        });
        column.addView(next);
    }

    // ---- helpers -----------------------------------------------------------------------------

    private Bitmap loadScene(String assetName) {
        try (InputStream in = appContext.getAssets().open("games/story/" + assetName)) {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = 2;
            options.inPreferredConfig = Bitmap.Config.RGB_565;
            return BitmapFactory.decodeStream(in, null, options);
        } catch (Exception e) {
            // No art yet: the board draws placeholder tiles.
            return null;
        }
    }

    private void releaseScene() {
        if (scene != null) {
            scene.recycle();
            scene = null;
        }
    }

    private LinearLayout column(int padding) {
        LinearLayout column = new LinearLayout(getContext());
        column.setOrientation(LinearLayout.VERTICAL);
        column.setPadding(padding, padding, padding, padding);
        return column;
    }

    private TextView text(String value, int sp, boolean bold) {
        TextView view = new TextView(getContext());
        view.setText(value);
        view.setTextSize(sp);
        view.setTextColor(Color.WHITE);
        if (bold) view.setTypeface(Typeface.DEFAULT_BOLD);
        view.setPadding(0, dp(6), 0, dp(6));
        return view;
    }

    private Button smallButton(String label) {
        Button button = new Button(getContext());
        button.setAllCaps(false);
        button.setText(label);
        button.setTextColor(Color.WHITE);
        button.setBackgroundColor(ORANGE);
        GameFx.attachPress(button);
        return button;
    }

    private int dp(int value) {
        return Math.round(value * getContext().getResources().getDisplayMetrics().density);
    }

    /** Local store for the progression rules. Writes go to the games preferences file only. */
    private static final class PrefsStore implements KeyValueStore {
        private final SharedPreferences prefs;

        PrefsStore(SharedPreferences prefs) { this.prefs = prefs; }

        @Override public int getInt(String key, int defaultValue) { return prefs.getInt(key, defaultValue); }

        @Override public void putInt(String key, int value) { prefs.edit().putInt(key, value).apply(); }
    }
}
