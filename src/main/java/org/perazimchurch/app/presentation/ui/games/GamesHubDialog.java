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
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.perazimchurch.app.data.local.session.SessionManager;
import org.perazimchurch.app.domain.games.BibleGames;
import org.perazimchurch.app.domain.games.BibleGames.Card;
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
        screen("Bible Games", "Learn, remember and enjoy. All three games work without internet.", false);
        gameCard(Mode.QUIZ, "Bible Quiz", "Five questions, four choices each. Discover the story behind every answer.");
        gameCard(Mode.WHO_AM_I, "Who am I?", "Read a Bible character's clues, ask for a hint, then type your answer.");
        gameCard(Mode.VERSE_ORDER, "Verse Builder", "Tap shuffled word tiles to rebuild a short KJV verse. Undo and try your order before checking.");
        text("Scores stay on this phone for the current account. Games do not change XP, streaks or Grace points.", 14, false);
    }

    private void gameCard(Mode selected, String title, String description) {
        LinearLayout card = new LinearLayout(activity);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(16), dp(12), dp(16), dp(16));
        card.setBackground(shape(PerazimTheme.COLOR_SURFACE));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.bottomMargin = dp(16);
        page.addView(card, params);
        addText(card, title, 21, true);
        addText(card, description, 16, false);
        String key = scorePrefix + selected.name();
        addText(card, scores.contains(key) ? "Your best: " + scores.getInt(key, 0) + "/5" : "Your best: not played yet", 14, false);
        Button start = new Button(activity);
        styleButton(start, "Play " + title, selected == Mode.QUIZ);
        card.addView(start, new LinearLayout.LayoutParams(-1, -2));
        start.setOnClickListener(v -> {
            if (!activeOwner()) return;
            mode = selected;
            seed = System.nanoTime();
            round = new Round(mode, seed);
            question();
        });
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
