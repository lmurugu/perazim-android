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

import org.perazimchurch.app.presentation.ui.games.common.GameAudioSettings;
import org.perazimchurch.app.domain.games.catalog.StoryMosaicSession;
import org.perazimchurch.app.domain.games.catalog.StoryMosaicDefinition;
import org.perazimchurch.app.domain.games.engine.GameSession;
import org.perazimchurch.app.presentation.ui.games.common.GameContext;
import org.perazimchurch.app.presentation.ui.games.common.GameAudio;
import org.perazimchurch.app.presentation.ui.games.common.GameResults;
import org.perazimchurch.app.presentation.ui.games.common.GameFx;
import org.perazimchurch.app.domain.games.engine.KeyValueStore;
import org.perazimchurch.app.domain.games.engine.LevelProgression;
import org.perazimchurch.app.domain.games.mosaic.MosaicBoard;
import org.perazimchurch.app.domain.games.mosaic.MosaicPuzzles;
import org.perazimchurch.app.domain.games.engine.GameSession;
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
    private java.util.function.BooleanSupplier ownerCheck = () -> true;

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
        StoryMosaicDialog dialog = new StoryMosaicDialog(ctx.activity(), ctx.scores(), ctx.scorePrefix() + GAME_ID);
        dialog.ownerCheck = ctx.ownerCheck();
        dialog.setOnDismissListener(d -> ctx.hub().run());
        dialog.show();
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
        GameAudio.get(getContext()).setReactionWindow(getWindow());
        GameAudio.get(getContext()).stopAnnouncer();
        releaseScene();
        root.removeAllViews();
        LinearLayout column = column(dp(16));

        TextView title = text("Story Mosaic", 22, true);
        LinearLayout header = new LinearLayout(getContext());
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.addView(title, new LinearLayout.LayoutParams(0, -2, 1f));
        header.addView(GameAudioSettings.button(getContext()));
        column.addView(header);
        column.addView(text(GameResults.clearedCount(prefs, progressId, levelCount, true) + " / "
                + levelCount + " scenes cleared · Every chapter is open.", 14, false));
        android.widget.EditText search = new android.widget.EditText(getContext());
        search.setSingleLine(true);
        search.setTextColor(Color.WHITE);
        search.setHintTextColor(0xFFD6BDD8);
        search.setHint("Find a chapter");
        column.addView(search);
        List<Button> chapters = new java.util.ArrayList<>();

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
            android.graphics.drawable.GradientDrawable background = new android.graphics.drawable.GradientDrawable();
            background.setCornerRadius(dp(13));
            background.setStroke(dp(1), complete ? ORANGE : 0xFF684276);
            background.setColor(0xFF492957);
            row.setBackground(background);
            row.setPadding(dp(12), dp(8), dp(12), dp(8));
            row.setMinHeight(dp(58));
            row.setOnClickListener(v -> showPuzzle(target));
            GameFx.attachPress(row);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
            lp.topMargin = dp(8);
            column.addView(row, lp);
            chapters.add(row);
            if (c == nextChapter) shimmerRow = row;
        }

        search.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                String query = s.toString().trim().toLowerCase(java.util.Locale.ROOT);
                for (Button row : chapters) {
                    row.setVisibility(row.getText().toString().toLowerCase(java.util.Locale.ROOT).contains(query)
                            ? View.VISIBLE : View.GONE);
                }
            }
            @Override public void afterTextChanged(android.text.Editable text) {}
        });
        column.setFocusableInTouchMode(true);
        column.requestFocus();

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
        if (!ownerCheck.getAsBoolean()) { dismiss(); return; }
        GameAudio audio = GameAudio.get(getContext());
        audio.startRound(GAME_ID);
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
        header.addView(GameAudioSettings.button(getContext()));
        column.addView(header);
        GameFx.enter(column);

        int pieces = session.board().pieceCount();
        String reference = puzzle.reference + " · Scene " + puzzle.number;
        TextView progress = text(reference + " · 0 / " + pieces + " placed", 14, false);
        column.addView(progress);

        TextView hint = text("Drag a piece, or tap it and then its slot.", 14, false);
        hint.setTextColor(Color.parseColor("#F2C94C"));
        hint.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
        column.addView(hint);

        TextView caption = text("", 16, false);
        caption.setVisibility(View.GONE);
        caption.setGravity(Gravity.CENTER);

        Button hintButton = smallButton("Show a hint");
        hintButton.setVisibility(session.hintAvailable() ? View.VISIBLE : View.GONE);
        hintButton.setOnClickListener(v -> {
            String clue = session.hint();
            if (clue == null) return;
            String reaction = audio.announce(GAME_ID, GameAudio.Reaction.HELP);
            hint.setText(reaction.isEmpty() ? clue : clue + "\n" + reaction);
        });

        FrameLayout boardHolder = new FrameLayout(getContext());
        StoryMosaicView board = new StoryMosaicView(getContext(), session, scene, outcome -> {
            if (!ownerCheck.getAsBoolean()) { dismiss(); return; }
            progress.setText(reference + " · " + session.board().placedCount() + " / " + pieces + " placed");
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
        GameResults.saveLevel(prefs, progressId, level, levelCount, stars, true,
                session.board().pieceCount() + " pieces · " + session.board().wrongDrops() + " wrong placements");
        hintButton.setVisibility(View.GONE);
        String win = GameAudio.get(getContext()).announce(GAME_ID, GameAudio.Reaction.WIN, true);
        hint.setText(win.isEmpty() ? "Scene cleared · Result saved" : "Scene cleared · Result saved\n" + win);
        // Show the title at once; swap in the KJV text when the background read finishes.
        caption.setText(puzzle.title + "\n— " + puzzle.reference);
        caption.setVisibility(View.VISIBLE);
        GameFx.enter(caption);
        android.widget.FrameLayout burst = GameFx.starRow(getContext(), stars);
        column.addView(burst, column.indexOfChild(caption));
        GameFx.popStars(burst, stars, false);
        final Context ctx = appContext;
        final String reference = puzzle.reference;
        new Thread(() -> {
            String verse = KjvLookup.text(ctx, reference);
            if (verse == null) return;
            mainHandler.post(() -> caption.setText("\u201c" + verse + "\u201d\n— " + reference));
        }, "kjv-caption").start();
        if (level < levelCount) {
            int side = MosaicBoard.sideForLevel(level + 1);
            column.addView(text("Next scene · " + side + " × " + side + " grid", 13, false));
        }
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
        button.setSoundEffectsEnabled(false);
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
