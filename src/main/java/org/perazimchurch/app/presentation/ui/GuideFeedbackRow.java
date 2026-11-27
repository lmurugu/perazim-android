package org.perazimchurch.app.presentation.ui;

import android.app.Activity;
import android.graphics.Typeface;
import android.text.InputType;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.perazimchurch.app.ai.guide.GuideMemory;
import org.perazimchurch.app.ai.guide.GuideRuntime;
import org.perazimchurch.app.core.concurrency.BoundedExecutors;
import org.perazimchurch.app.ui.theme.AppDialogs;
import org.perazimchurch.app.ui.theme.GlassKit;

/**
 * "Helpful" and "Not helpful" under an on-phone answer. A not-helpful answer may carry a short correction,
 * which the guide keeps only when it is about style (see LessonGuard).
 */
final class GuideFeedbackRow {
    private GuideFeedbackRow() {}

    static void attach(final Activity activity, final LinearLayout container, final String question, final String answer) {
        final LinearLayout row = new LinearLayout(activity);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.END);
        row.setPadding(0, dp(activity, 2), dp(activity, 24), dp(activity, 8));
        final TextView helpful = pill(activity, "Helpful");
        final TextView notHelpful = pill(activity, "Not helpful");
        row.addView(helpful);
        row.addView(notHelpful);

        helpful.setOnClickListener(v -> {
            BoundedExecutors.io().execute(() -> GuideRuntime.feedback(activity, true, question, answer));
            replaceWithNote(activity, row, container, "Thank you. Noted.");
        });
        notHelpful.setOnClickListener(v -> {
            BoundedExecutors.io().execute(() -> GuideRuntime.feedback(activity, false, question, answer));
            askForCorrection(activity, row, container, question);
        });
        container.addView(row);
    }

    private static void askForCorrection(final Activity activity, final LinearLayout row,
                                         final LinearLayout container, final String question) {
        final EditText input = new EditText(activity);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        input.setHint("For example: shorter, or simpler words");
        input.setMaxLines(2);
        AppDialogs.builder(activity)
                .setTitle("Thank you. How should it be?")
                .setMessage("Only style and length can be taught. Doctrine, safety and privacy are never changed.")
                .setView(input)
                .setPositiveButton("Save note", (d, w) -> {
                    final String text = input.getText().toString();
                    BoundedExecutors.io().execute(() -> {
                        final GuideMemory.Outcome outcome = GuideRuntime.correction(activity, question, text);
                        activity.runOnUiThread(() -> Toast.makeText(activity,
                                outcome == GuideMemory.Outcome.SAVED ? "Saved on this device."
                                        : "That note was not saved. Only style notes can be kept.",
                                Toast.LENGTH_LONG).show());
                    });
                    replaceWithNote(activity, row, container, "Thank you. Noted.");
                })
                .setNegativeButton("Skip", (d, w) -> replaceWithNote(activity, row, container, "Thank you. Noted."))
                .show();
    }

    private static void replaceWithNote(Activity activity, LinearLayout row, LinearLayout container, String text) {
        int index = container.indexOfChild(row);
        container.removeView(row);
        TextView note = new TextView(activity);
        note.setText(text);
        note.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        note.setTextColor(GlassKit.muted());
        note.setPadding(dp(activity, 4), 0, 0, dp(activity, 8));
        if (index >= 0) container.addView(note, index); else container.addView(note);
    }

    private static TextView pill(Activity activity, String label) {
        TextView t = new TextView(activity);
        t.setText(label);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        t.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
        t.setTextColor(GlassKit.accent());
        t.setPadding(dp(activity, 12), dp(activity, 6), dp(activity, 12), dp(activity, 6));
        t.setBackground(GlassKit.glass(activity, 14, GlassKit.LEGACY_LIGHT_RIM));
        t.setClickable(true);
        t.setFocusable(true);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(dp(activity, 6), 0, 0, 0);
        t.setLayoutParams(lp);
        return t;
    }

    private static int dp(Activity activity, int value) {
        return Math.round(value * activity.getResources().getDisplayMetrics().density);
    }
}
