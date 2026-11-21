package org.perazimchurch.app.presentation.ui.games.common;

import android.content.SharedPreferences;
import android.widget.Button;
import android.widget.LinearLayout;

import org.perazimchurch.app.domain.games.catalog.Levels;

import java.util.function.IntConsumer;

/**
 * Level picker shared by the five games. Twelve levels in three bands of four, each showing its
 * best stars. A level is locked until the one before it has two or more stars, as recorded by
 * {@code LevelProgression} through {@link GameResults}. The level-map art sits behind the rows.
 */
public final class LevelPicker {

    private static final String[] BAND_NAMES = {"Easy", "Medium", "Hard"};

    private LevelPicker() { }

    /**
     * Adds the three bands of level buttons to the current page. {@code storedId} is the
     * score-prefixed game id used to save progress, and {@code onPick} runs with the chosen level.
     */
    public static void addLevels(GameUi ui, SharedPreferences prefs, String storedId, IntConsumer onPick) {
        int unlocked = GameResults.unlockedLevel(prefs, storedId);
        for (int band = 0; band < Levels.BANDS.length; band++) {
            ui.text(BAND_NAMES[band] + " · levels " + (band * Levels.PER_BAND + 1)
                    + " to " + ((band + 1) * Levels.PER_BAND), 13, true);
            LinearLayout row = null;
            for (int i = 0; i < Levels.PER_BAND; i++) {
                int level = band * Levels.PER_BAND + i + 1;
                if (i == 0 || row == null) row = newRow(ui);
                row.addView(levelButton(ui, prefs, storedId, level, level <= unlocked, onPick), tileParams(ui));
            }
        }
    }

    private static Button levelButton(GameUi ui, SharedPreferences prefs, String storedId, int level,
                                      boolean open, IntConsumer onPick) {
        Button button = new Button(ui.activity());
        String label = open ? stars(GameResults.bestStars(prefs, storedId, level)) : "Locked";
        ui.styleButton(button, "Level " + level + "\n" + label, level == GameResults.unlockedLevel(prefs, storedId));
        GameFx.attachPress(button);
        button.setEnabled(open);
        button.setOnClickListener(v -> {
            if (!ui.activeOwner()) return;
            ui.click(v);
            onPick.accept(level);
        });
        return button;
    }

    private static String stars(int count) {
        StringBuilder line = new StringBuilder();
        for (int i = 0; i < 3; i++) line.append(i < count ? "★" : "☆");
        return line.toString();
    }

    private static LinearLayout newRow(GameUi ui) {
        LinearLayout row = new LinearLayout(ui.activity());
        row.setOrientation(LinearLayout.HORIZONTAL);
        ui.addRow(row);
        return row;
    }

    private static LinearLayout.LayoutParams tileParams(GameUi ui) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, -2, 1f);
        params.setMargins(ui.dp(4), 0, ui.dp(4), ui.dp(8));
        return params;
    }
}
