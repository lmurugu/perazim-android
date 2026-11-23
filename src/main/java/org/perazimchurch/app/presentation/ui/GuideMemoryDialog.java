package org.perazimchurch.app.presentation.ui;

import android.app.Activity;
import android.text.InputType;
import android.widget.EditText;
import android.widget.Toast;

import org.perazimchurch.app.ai.guide.GuideRuntime;
import org.perazimchurch.app.core.concurrency.BoundedExecutors;
import org.perazimchurch.app.ui.theme.AppDialogs;

/** Shows what the on-phone guide remembers on this device, with options to set a name or forget everything. */
public final class GuideMemoryDialog {
    private GuideMemoryDialog() {}

    public static void show(final Activity activity) {
        AppDialogs.builder(activity)
                .setTitle("What the guide remembers")
                .setMessage(GuideRuntime.describe(activity)
                        + "\nEverything stays on this phone. Nothing is uploaded.")
                .setPositiveButton("Set name", (d, w) -> askName(activity))
                .setNeutralButton("Forget everything", (d, w) -> confirmForget(activity))
                .setNegativeButton("Close", null)
                .show();
    }

    private static void askName(final Activity activity) {
        final EditText input = new EditText(activity);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setHint("The name the guide should use");
        AppDialogs.builder(activity)
                .setTitle("Name to use")
                .setView(input)
                .setPositiveButton("Save", (d, w) -> {
                    final String name = input.getText().toString();
                    BoundedExecutors.io().execute(() -> GuideRuntime.setDisplayName(activity, name));
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private static void confirmForget(final Activity activity) {
        AppDialogs.builder(activity)
                .setTitle("Forget everything?")
                .setMessage("This removes the guide's learned notes, its style and its name for you on this phone. "
                        + "The app's own help and Bible content are not affected.")
                .setPositiveButton("Forget", (d, w) -> BoundedExecutors.io().execute(() -> {
                    GuideRuntime.forget(activity);
                    activity.runOnUiThread(() -> Toast.makeText(activity,
                            "The guide has forgotten your notes on this phone.", Toast.LENGTH_LONG).show());
                }))
                .setNegativeButton("Keep", null)
                .show();
    }
}
