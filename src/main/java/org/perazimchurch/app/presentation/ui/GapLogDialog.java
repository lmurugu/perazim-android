package org.perazimchurch.app.presentation.ui;

import android.app.Activity;
import android.content.Intent;
import android.util.TypedValue;
import android.widget.ScrollView;
import android.widget.TextView;

import org.perazimchurch.app.ai.gaps.GapEntry;
import org.perazimchurch.app.ai.gaps.GapLog;
import org.perazimchurch.app.ai.gaps.GapReason;
import org.perazimchurch.app.core.concurrency.BoundedExecutors;
import org.perazimchurch.app.ui.theme.AppDialogs;

import java.util.List;

/**
 * The member's own list of questions the guide could not answer. The member can read it, clear it, or choose to send it.
 * Nothing is uploaded automatically: sending opens the Android share sheet with the exact text shown first.
 */
final class GapLogDialog {
    private static final int SHOW_LIMIT = 50;

    private GapLogDialog() {}

    static void show(final Activity activity) {
        BoundedExecutors.io().execute(() -> {
            final List<GapEntry> entries = GapLog.list(activity);
            activity.runOnUiThread(() -> showList(activity, entries));
        });
    }

    private static void showList(final Activity activity, final List<GapEntry> entries) {
        StringBuilder text = new StringBuilder();
        text.append("These questions are kept only on this phone, in encrypted storage. Names, numbers and emails were removed. "
                + "Nothing is sent unless you choose Send.\n\n");
        if (entries.isEmpty()) {
            text.append("None yet. When the guide cannot give a checked answer to a faith question, it appears here.");
        } else {
            for (int i = 0; i < entries.size() && i < SHOW_LIMIT; i++) {
                GapEntry e = entries.get(i);
                text.append("• ").append(e.question).append('\n')
                        .append("   ").append(GapReason.label(e.reason))
                        .append(" · asked ").append(e.count).append(e.count == 1 ? " time" : " times").append("\n\n");
            }
        }
        android.app.AlertDialog.Builder b = AppDialogs.builder(activity)
                .setTitle("Questions the guide could not answer")
                .setMessage(text.toString().trim());
        if (entries.isEmpty()) {
            b.setPositiveButton("OK", null);
        } else {
            b.setPositiveButton("Send to help improve", (d, w) -> showPreview(activity, entries))
                    .setNegativeButton("Clear log", (d, w) -> confirmClear(activity));
        }
        b.show();
    }

    private static void confirmClear(final Activity activity) {
        AppDialogs.builder(activity)
                .setTitle("Clear this list?")
                .setMessage("All questions in this list will be deleted from this phone.")
                .setPositiveButton("Clear", (d, w) -> BoundedExecutors.io().execute(() -> {
                    GapLog.clear(activity);
                    activity.runOnUiThread(() -> android.widget.Toast.makeText(activity,
                            "The list is cleared.", android.widget.Toast.LENGTH_SHORT).show());
                }))
                .setNegativeButton("Keep", null)
                .show();
    }

    /** Shows the exact text that would be shared. The share sheet receives this same string and nothing else. */
    private static void showPreview(final Activity activity, final List<GapEntry> entries) {
        final String exact = GapLog.exportText(entries);
        TextView body = new TextView(activity);
        body.setText(exact);
        body.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        body.setTextIsSelectable(true);
        int pad = Math.round(16 * activity.getResources().getDisplayMetrics().density);
        body.setPadding(pad, pad, pad, pad);
        ScrollView scroll = new ScrollView(activity);
        scroll.addView(body);
        AppDialogs.builder(activity)
                .setTitle("Check before you send")
                .setMessage("This is exactly what will be shared: " + entries.size()
                        + (entries.size() == 1 ? " question" : " questions") + ", with no name, account, phone number, "
                        + "device ID or the guide's answers.")
                .setView(scroll)
                .setPositiveButton("Share", (d, w) -> {
                    Intent send = new Intent(Intent.ACTION_SEND);
                    send.setType("text/plain");
                    send.putExtra(Intent.EXTRA_SUBJECT, "Perazim guide: questions it could not answer");
                    send.putExtra(Intent.EXTRA_TEXT, exact);
                    activity.startActivity(Intent.createChooser(send, "Send with"));
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
