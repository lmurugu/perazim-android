package org.perazimchurch.app.presentation.ui.games;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.perazimchurch.app.data.local.session.SessionManager;
import org.perazimchurch.app.domain.games.BibleGames;
import org.perazimchurch.app.domain.games.BibleGames.Card;
import org.perazimchurch.app.domain.games.LadderOfFaith;
import org.perazimchurch.app.domain.games.BibleGames.Mode;
import org.perazimchurch.app.domain.games.BibleGames.Round;
import org.perazimchurch.app.domain.games.BibleGames.VersePuzzle;
import org.perazimchurch.app.ui.theme.PerazimTheme;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/** Three complete offline Bible games; best scores are local and account-scoped. */
public final class GamesHubDialog extends Dialog {
    private final Activity activity;
    private final String ownerId;
    private final SharedPreferences scores;
    private final String scorePrefix;
    private LinearLayout page;
    private ScrollView scroll;
    private Round round;
    private Mode mode;
    private VersePuzzle puzzle;
    private LadderOfFaith ladder;
    private long seed;

    public static void show(Activity activity) {
        if (activity.isFinishing() || activity.isDestroyed()) return;
        GamesHubDialog dialog = new GamesHubDialog(activity);
        dialog.show();
        Window window = dialog.getWindow();
        if (window != null) window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
    }

    private GamesHubDialog(Activity activity) {
        super(activity);
        this.activity = activity;
        ownerId = SessionManager.getInstance(activity).getActiveUserId();
        scorePrefix = ownerKey(ownerId);
        scores = activity.getSharedPreferences("perazim_game_scores_v1", Context.MODE_PRIVATE);
    }

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        Window window = getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(PerazimTheme.COLOR_BG_DEEP));
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        }
        hub();
    }

    private boolean activeOwner() {
        boolean same = ownerId.equals(SessionManager.getInstance(activity).getActiveUserId());
        if (!same) {
            Toast.makeText(activity, "Your account changed. Open Games again to continue.", Toast.LENGTH_SHORT).show();
            dismiss();
        }
        return same;
    }

    private void screen(String title, String subtitle, boolean playing) {
        scroll = new ScrollView(activity);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(PerazimTheme.COLOR_BG_DEEP);
        page = new LinearLayout(activity);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(20), dp(16), dp(20), dp(28));
        scroll.addView(page, new ScrollView.LayoutParams(-1, -2));
        setContentView(scroll);
        Button back = button(playing ? "← Games" : "Close games", false);
        back.setOnClickListener(v -> {
            hideKeyboard();
            if (playing) hub(); else dismiss();
        });
        text(title, 28, true);
        text(subtitle, 15, false);
        gap(16);
    }

    private void hub() {
        if (!activeOwner()) return;
        round = null;
        screen("Bible Games", "Learn, remember and enjoy. All four games work without internet.", false);
        statsBar();
        gameCard(Mode.QUIZ, "Bible Quiz", "Five questions, four choices each. Discover the story behind every answer.");
        gameCard(Mode.WHO_AM_I, "Who am I?", "Read a Bible character's clues, ask for a hint, then type your answer.");
        gameCard(Mode.VERSE_ORDER, "Verse Builder", "Tap shuffled word tiles to rebuild a short KJV verse. Undo and try your order before checking.");
        gameCard(Mode.LADDER, "Ladder of Faith", "Climb 20 rungs from easy to hard with three lives. Every run draws fresh questions from a bank of nearly 300.");
        text("Scores stay on this phone for the current account. Games do not change XP, streaks or Grace points.", 14, false);
    }

    private void statsBar() {
        LinearLayout bar = new LinearLayout(activity);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams barParams = new LinearLayout.LayoutParams(-1, -2);
        barParams.bottomMargin = dp(16);
        page.addView(bar, barParams);
        int streak = currentStreak();
        stat(bar, org.perazimchurch.app.R.drawable.ic_game_star, stars() + " stars");
        stat(bar, org.perazimchurch.app.R.drawable.ic_game_flame, streak + (streak == 1 ? " day" : " days"));
        stat(bar, org.perazimchurch.app.R.drawable.ic_game_trophy, "Best " + scores.getInt(scorePrefix + Mode.LADDER.name(), 0) + "/" + LadderOfFaith.RUNGS);
    }

    private void stat(LinearLayout bar, int icon, String label) {
        LinearLayout pill = new LinearLayout(activity);
        pill.setOrientation(LinearLayout.HORIZONTAL);
        pill.setGravity(Gravity.CENTER_VERTICAL);
        pill.setPadding(dp(8), dp(6), dp(10), dp(6));
        pill.setBackground(shape(PerazimTheme.COLOR_SURFACE));
        pill.addView(icon(icon, 26), new LinearLayout.LayoutParams(dp(26), dp(26)));
        TextView text = new TextView(activity);
        text.setText(label);
        text.setTextSize(14);
        text.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        text.setTextColor(PerazimTheme.COLOR_TEXT_PRIMARY);
        text.setPadding(dp(6), 0, 0, 0);
        text.setSingleLine(true);
        pill.addView(text);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, -2, 1f);
        params.setMargins(dp(2), 0, dp(2), 0);
        bar.addView(pill, params);
    }

    private ImageView icon(int drawable, int sizeDp) {
        ImageView view = new ImageView(activity);
        view.setImageResource(drawable);
        view.setScaleType(ImageView.ScaleType.FIT_CENTER);
        view.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        return view;
    }

    private static int iconFor(Mode mode) {
        switch (mode) {
            case QUIZ: return org.perazimchurch.app.R.drawable.ic_game_scroll;
            case WHO_AM_I: return org.perazimchurch.app.R.drawable.ic_game_who;
            case VERSE_ORDER: return org.perazimchurch.app.R.drawable.ic_game_puzzle;
            default: return org.perazimchurch.app.R.drawable.ic_game_ladder;
        }
    }

    // ---- Stars and daily streak (local to this phone and account) ----

    private int stars() { return scores.getInt(scorePrefix + "stars", 0); }
    private void addStar() { scores.edit().putInt(scorePrefix + "stars", stars() + 1).apply(); }
    private static long today() {
        long now = System.currentTimeMillis();
        return (now + java.util.TimeZone.getDefault().getOffset(now)) / 86_400_000L;
    }
    private int currentStreak() {
        long last = scores.getLong(scorePrefix + "streakDay", -1);
        int streak = scores.getInt(scorePrefix + "streak", 0);
        return last >= today() - 1 ? streak : 0;
    }
    private void recordPlay() {
        long last = scores.getLong(scorePrefix + "streakDay", -1);
        int next = org.perazimchurch.app.domain.games.GameStreak.next(last, today(), scores.getInt(scorePrefix + "streak", 0));
        scores.edit().putLong(scorePrefix + "streakDay", today()).putInt(scorePrefix + "streak", next).apply();
    }

    private void gameCard(Mode selected, String title, String description) {
        LinearLayout card = new LinearLayout(activity);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(16), dp(12), dp(16), dp(16));
        card.setBackground(shape(PerazimTheme.COLOR_SURFACE));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.bottomMargin = dp(16);
        page.addView(card, params);
        LinearLayout head = new LinearLayout(activity);
        head.setOrientation(LinearLayout.HORIZONTAL);
        head.setGravity(Gravity.CENTER_VERTICAL);
        head.addView(icon(iconFor(selected), 52), new LinearLayout.LayoutParams(dp(52), dp(52)));
        TextView titleView = new TextView(activity);
        titleView.setText(title);
        titleView.setTextSize(21);
        titleView.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        titleView.setTextColor(PerazimTheme.COLOR_TEXT_PRIMARY);
        titleView.setPadding(dp(12), 0, 0, 0);
        head.addView(titleView);
        LinearLayout.LayoutParams headParams = new LinearLayout.LayoutParams(-1, -2);
        headParams.bottomMargin = dp(8);
        card.addView(head, headParams);
        addText(card, description, 16, false);
        String key = scorePrefix + selected.name();
        int total = selected == Mode.LADDER ? LadderOfFaith.RUNGS : 5;
        addText(card, scores.contains(key) ? "Your best: " + scores.getInt(key, 0) + "/" + total : "Your best: not played yet", 14, false);
        Button start = new Button(activity);
        styleButton(start, "Play " + title, selected == Mode.QUIZ);
        card.addView(start, new LinearLayout.LayoutParams(-1, -2));
        start.setOnClickListener(v -> {
            if (!activeOwner()) return;
            mode = selected;
            seed = System.nanoTime();
            recordPlay();
            if (mode == Mode.LADDER) { startLadder(); return; }
            round = new Round(mode, seed);
            question();
        });
    }

    // ---- Ladder of Faith ----

    /**
     * Game events hook. No-op on this branch: FeedbackManager lives in PR #23, which is not in this
     * branch. Wire it here once PR #23 lands so the shared sound, haptics and confetti settings apply.
     */
    private void announce(List<LadderOfFaith.Event> events) {
        // Intentionally empty until FeedbackManager (PR #23) is available on this branch.
    }

    private void startLadder() {
        seed = System.nanoTime();
        ladder = new LadderOfFaith(seed);
        showMap();
    }

    private void showMap() {
        if (!activeOwner()) return;
        boolean done = ladder.finished();
        screen("Ladder of Faith", done ? "Run complete · Score " + ladder.score()
                : "Rung " + ladder.position() + " of " + LadderOfFaith.RUNGS + " · " + tierName(LadderOfFaith.tierOf(ladder.position())), true);
        livesRow();
        LadderMapView map = new LadderMapView(activity);
        int best = scores.getInt(scorePrefix + Mode.LADDER.name(), 0);
        map.setProgress(done ? ladder.score() : ladder.position(), best, done);
        map.setOnClimb(this::ladderQuestion);
        page.addView(map, new LinearLayout.LayoutParams(-1, -2));
        if (done) {
            button("See results", true).setOnClickListener(v -> ladderFinished());
        } else {
            button("Climb rung " + ladder.position(), true).setOnClickListener(v -> ladderQuestion());
            int rung = ladder.position();
            scroll.post(() -> scroll.scrollTo(0, Math.max(0, (int) (map.getTop() + map.nodeY(rung)) - scroll.getHeight() / 2)));
        }
    }

    private void livesRow() {
        LinearLayout row = new LinearLayout(activity);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setContentDescription(ladder.lives() + " lives left");
        for (int i = 0; i < LadderOfFaith.LIVES; i++) {
            ImageView heart = icon(org.perazimchurch.app.R.drawable.ic_game_heart, 28);
            heart.setAlpha(i < ladder.lives() ? 1f : 0.25f);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp(28), dp(28));
            params.rightMargin = dp(4);
            row.addView(heart, params);
        }
        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(-1, -2);
        rowParams.bottomMargin = dp(10);
        page.addView(row, rowParams);
    }

    private void ladderQuestion() {
        if (!activeOwner()) return;
        if (ladder.finished()) { ladderFinished(); return; }
        Card card = ladder.current();
        screen("Ladder of Faith", "Rung " + ladder.position() + " of " + LadderOfFaith.RUNGS + " · " + tierName(LadderOfFaith.tierOf(ladder.position())) + " · Score " + ladder.score() + " · Lives " + ladder.lives(), true);
        livesRow();
        text(card.prompt, 23, true);
        if (ladder.peeked()) text("Verse Peek: " + card.reference, 16, true);
        Button hint = button("Show a hint", false);
        TextView hintText = text("", 16, false);
        hintText.setVisibility(View.GONE);
        hint.setOnClickListener(v -> { hintText.setText(card.hint); hintText.setVisibility(View.VISIBLE); hint.setEnabled(false); });

        LinearLayout lifelines = new LinearLayout(activity);
        lifelines.setOrientation(LinearLayout.HORIZONTAL);
        page.addView(lifelines, new LinearLayout.LayoutParams(-1, -2));
        Button fifty = new Button(activity);
        styleButton(fifty, "50/50", false);
        lifelineIcon(fifty, org.perazimchurch.app.R.drawable.ic_game_fifty);
        fifty.setEnabled(ladder.fiftyAvailable());
        fifty.setOnClickListener(v -> { ladder.useFiftyFifty(); announce(ladder.drainEvents()); ladderQuestion(); });
        Button peek = new Button(activity);
        styleButton(peek, "Verse Peek", false);
        lifelineIcon(peek, org.perazimchurch.app.R.drawable.ic_game_eye);
        peek.setEnabled(ladder.peekAvailable());
        peek.setOnClickListener(v -> { ladder.usePeek(); announce(ladder.drainEvents()); ladderQuestion(); });
        Button pass = new Button(activity);
        styleButton(pass, "Pass", false);
        lifelineIcon(pass, org.perazimchurch.app.R.drawable.ic_game_skip);
        pass.setEnabled(ladder.passAvailable());
        pass.setOnClickListener(v -> { ladder.usePass(); announce(ladder.drainEvents()); ladderQuestion(); });
        for (Button b : new Button[] { fifty, peek, pass }) {
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, -2, 1f);
            params.setMargins(dp(2), dp(3), dp(2), dp(3));
            lifelines.addView(b, params);
        }
        gap(8);

        List<String> hidden = ladder.hiddenOptions();
        List<String> options = new ArrayList<>(card.options);
        Collections.shuffle(options, new java.util.Random(seed + ladder.position()));
        for (String option : options) {
            if (hidden.contains(option)) continue;
            button(option, false).setOnClickListener(v -> ladderAnswer(option));
        }
    }

    private void lifelineIcon(Button button, int drawable) {
        android.graphics.drawable.Drawable d = androidx.core.content.ContextCompat.getDrawable(activity, drawable);
        if (d == null) return;
        d.setBounds(0, 0, dp(30), dp(30));
        button.setCompoundDrawables(null, d, null, null);
        button.setCompoundDrawablePadding(dp(2));
        if (!button.isEnabled()) button.setAlpha(0.45f);
    }

    private static String tierName(int tier) {
        return tier == 1 ? "Easy" : tier == 2 ? "Medium" : "Hard";
    }

    private void ladderAnswer(String value) {
        if (!activeOwner() || ladder.answered()) return;
        hideKeyboard();
        Card card = ladder.current();
        if (ladder.answer(value)) addStar();
        List<LadderOfFaith.Event> events = ladder.drainEvents();
        announce(events);
        boolean milestone = ladder.milestone();
        String headline = ladder.correct()
                ? (milestone ? "Milestone reached!" : "Well done!")
                : "Let's learn this one";
        screen(headline, "Score " + ladder.score() + " · Lives " + ladder.lives(), true);
        text(card.answer, 24, true);
        text(card.reference, 17, false);
        text(card.explanation, 18, false);
        boolean last = ladder.finished();
        button(last ? "See results" : "Next rung", true).setOnClickListener(v -> {
            if (last) { showMap(); return; }
            if (ladder.next()) { if (milestone) showMap(); else ladderQuestion(); }
        });
        if (!last) button("View ladder map", false).setOnClickListener(v -> { if (ladder.next()) showMap(); });
    }

    private void ladderFinished() {
        if (!activeOwner()) return;
        String key = scorePrefix + Mode.LADDER.name();
        int best = Math.max(scores.getInt(key, 0), ladder.score());
        scores.edit().putInt(key, best).apply();
        screen("Ladder complete", "Ladder of Faith", true);
        text(ladder.score() + " / " + LadderOfFaith.RUNGS, 40, true);
        text(ladder.score() == LadderOfFaith.RUNGS ? "You climbed the whole ladder. Keep putting what you learn into practice." : "Every rung is a chance to learn. Read the references and climb again.", 19, false);
        text("Your best on this phone: " + best + "/" + LadderOfFaith.RUNGS, 17, false);
        button("Climb again", true).setOnClickListener(v -> startLadder());
        button("View ladder map", false).setOnClickListener(v -> showMap());
        button("Choose another game", false).setOnClickListener(v -> hub());
    }

    private String modeTitle() {
        return mode == Mode.QUIZ ? "Bible Quiz" : mode == Mode.WHO_AM_I ? "Who am I?" : "Verse Builder";
    }

    private void question() {
        if (!activeOwner()) return;
        if (round.finished()) { finishRound(); return; }
        Card card = round.current();
        screen(modeTitle(), "Question " + round.position() + " of " + round.size() + " · Score " + round.score(), true);
        text(card.prompt, 23, true);
        if (mode == Mode.VERSE_ORDER) text(card.reference, 16, false);
        Button hint = button("Show a hint", false);
        TextView hintText = text("", 16, false);
        hintText.setVisibility(View.GONE);
        hint.setOnClickListener(v -> { hintText.setText(card.hint); hintText.setVisibility(View.VISIBLE); hint.setEnabled(false); });
        gap(12);

        if (mode == Mode.QUIZ) {
            List<String> options = new ArrayList<>(card.options);
            Collections.shuffle(options, new java.util.Random(seed + round.position()));
            for (String option : options) button(option, false).setOnClickListener(v -> submit(option));
        } else if (mode == Mode.WHO_AM_I) {
            EditText answer = new EditText(activity);
            answer.setTextColor(PerazimTheme.COLOR_TEXT_PRIMARY);
            answer.setHintTextColor(PerazimTheme.COLOR_TEXT_SECONDARY);
            answer.setHint("Character's name");
            answer.setTextSize(19);
            answer.setSingleLine(true);
            answer.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_FLAG_CAP_WORDS);
            answer.setBackground(shape(PerazimTheme.COLOR_SURFACE_2));
            answer.setPadding(dp(14), dp(14), dp(14), dp(14));
            page.addView(answer, new LinearLayout.LayoutParams(-1, -2));
            button("Check answer", true).setOnClickListener(v -> {
                if (answer.getText().toString().trim().isEmpty()) { answer.setError("Enter a name first"); return; }
                submit(answer.getText().toString());
            });
        } else {
            puzzle = new VersePuzzle(card, seed + round.position());
            LinearLayout puzzleArea = new LinearLayout(activity);
            puzzleArea.setOrientation(LinearLayout.VERTICAL);
            page.addView(puzzleArea, new LinearLayout.LayoutParams(-1, -2));
            renderPuzzle(puzzleArea);
        }
        button("Reveal answer", false).setOnClickListener(v -> submit(""));
    }

    private void renderPuzzle(LinearLayout area) {
        area.removeAllViews();
        TextView sentence = addText(area, puzzle.sentence().isEmpty() ? "Your verse will appear here…" : puzzle.sentence(), 22, true);
        sentence.setPadding(dp(12), dp(12), dp(12), dp(12));
        sentence.setBackground(shape(PerazimTheme.COLOR_SURFACE));
        addText(area, "Tap each word in order. Every tile is used once.", 15, false);
        // Rows adapt to font scaling; at most two word tiles compete for width.
        LinearLayout row = null;
        int count = 0;
        for (int id : puzzle.tiles()) {
            if (count++ % 2 == 0) {
                row = new LinearLayout(activity);
                row.setOrientation(LinearLayout.HORIZONTAL);
                area.addView(row, new LinearLayout.LayoutParams(-1, -2));
            }
            Button tile = new Button(activity);
            styleButton(tile, puzzle.word(id), false);
            tile.setEnabled(!puzzle.selected(id));
            tile.setAlpha(puzzle.selected(id) ? 0.4f : 1f);
            tile.setContentDescription(puzzle.word(id) + ", word tile " + (id + 1));
            LinearLayout.LayoutParams tileParams = new LinearLayout.LayoutParams(0, -2, 1f);
            tileParams.setMargins(dp(2), dp(3), dp(2), dp(3));
            row.addView(tile, tileParams);
            tile.setOnClickListener(v -> { if (puzzle.choose(id)) renderPuzzle(area); });
        }
        Button undo = new Button(activity);
        styleButton(undo, "Undo last word", false);
        area.addView(undo, new LinearLayout.LayoutParams(-1, -2));
        undo.setOnClickListener(v -> { puzzle.undo(); renderPuzzle(area); });
        Button clear = new Button(activity);
        styleButton(clear, "Start verse again", false);
        area.addView(clear, new LinearLayout.LayoutParams(-1, -2));
        clear.setOnClickListener(v -> { puzzle.clear(); renderPuzzle(area); });
        Button check = new Button(activity);
        styleButton(check, "Check verse", true);
        check.setEnabled(puzzle.complete());
        area.addView(check, new LinearLayout.LayoutParams(-1, -2));
        check.setOnClickListener(v -> submit(puzzle.sentence()));
    }

    private void submit(String value) {
        if (!activeOwner() || round.answered()) return;
        hideKeyboard();
        Card card = round.current();
        boolean correct = round.answer(value);
        if (correct) addStar();
        screen(correct ? "Well done!" : "Let's learn this one", "Score " + round.score() + " · Question " + round.position() + " of " + round.size(), true);
        text(card.answer, 24, true);
        text(card.reference, 17, false);
        text(card.explanation, 18, false);
        button(round.position() == round.size() ? "See results" : "Next question", true).setOnClickListener(v -> {
            if (round.next()) question();
        });
    }

    private void finishRound() {
        if (!activeOwner()) return;
        String key = scorePrefix + mode.name();
        int best = Math.max(scores.getInt(key, 0), round.score());
        scores.edit().putInt(key, best).apply();
        screen("Round complete", modeTitle(), true);
        text(round.score() + " / " + round.size(), 40, true);
        text(round.score() == round.size() ? "Wonderful remembering! Keep putting what you learn into practice." : "Every round is a chance to learn. Read the references and try again.", 19, false);
        text("Your best on this phone: " + best + "/" + round.size(), 17, false);
        button("Play a new round", true).setOnClickListener(v -> {
            seed = System.nanoTime();
            round = new Round(mode, seed);
            question();
        });
        button("Choose another game", false).setOnClickListener(v -> hub());
    }

    private TextView text(String value, int size, boolean bold) { return addText(page, value, size, bold); }
    private TextView addText(LinearLayout parent, String value, int size, boolean bold) {
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
    private Button button(String label, boolean primary) {
        Button view = new Button(activity);
        styleButton(view, label, primary);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.bottomMargin = dp(10);
        page.addView(view, params);
        return view;
    }
    private void styleButton(Button button, String label, boolean primary) {
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
    private GradientDrawable shape(int color) {
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(color);
        shape.setCornerRadius(dp(14));
        return shape;
    }
    private void gap(int height) {
        page.addView(new View(activity), new LinearLayout.LayoutParams(1, dp(height)));
    }
    private int dp(int value) { return Math.round(value * activity.getResources().getDisplayMetrics().density); }
    private void hideKeyboard() {
        InputMethodManager input = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
        Window window = getWindow();
        if (input != null && window != null) input.hideSoftInputFromWindow(window.getDecorView().getWindowToken(), 0);
    }
    static String ownerKey(String owner) {
        try {
            byte[] bytes = MessageDigest.getInstance("SHA-256").digest(owner.getBytes(StandardCharsets.UTF_8));
            StringBuilder key = new StringBuilder();
            for (byte value : bytes) key.append(String.format(Locale.ROOT, "%02x", value & 0xff));
            return key + ".";
        } catch (NoSuchAlgorithmException impossible) { throw new IllegalStateException(impossible); }
    }
}
