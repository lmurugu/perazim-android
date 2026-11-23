package org.perazimchurch.app.ui.components;

import org.perazimchurch.app.ui.theme.AppDialogs;

import android.app.Activity;
import android.app.AlertDialog;

import org.perazimchurch.app.ui.theme.DisplaySettings;

/**
 * Lets the person choose the app size, the text size and the font, in that order. The screen is rebuilt in place
 * once they are done, so the change shows straight away and is kept for next time.
 */
public final class DisplaySettingsDialog {

    private DisplaySettingsDialog() {
    }

    public static void show(Activity activity) {
        final int appBefore = DisplaySettings.appSize(activity);
        final boolean[] changed = {false};
        AppDialogs.builder(activity)
                .setTitle("App size")
                .setSingleChoiceItems(DisplaySettings.APP_LABELS, appBefore, (dialog, which) -> {
                    dialog.dismiss();
                    if (which != appBefore) {
                        DisplaySettings.saveAppSize(activity, which);
                        changed[0] = true;
                    }
                    askText(activity, changed);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private static void askText(Activity activity, boolean[] changed) {
        final int textBefore = DisplaySettings.textSize(activity);
        AppDialogs.builder(activity)
                .setTitle("Text size")
                .setSingleChoiceItems(DisplaySettings.TEXT_LABELS, textBefore, (dialog, which) -> {
                    dialog.dismiss();
                    if (which != textBefore) {
                        DisplaySettings.saveTextSize(activity, which);
                        changed[0] = true;
                    }
                    askFont(activity, changed);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private static void askFont(Activity activity, boolean[] changed) {
        final int fontBefore = DisplaySettings.fontChoice(activity);
        AppDialogs.builder(activity)
                .setTitle("Font")
                .setSingleChoiceItems(DisplaySettings.FONT_LABELS, fontBefore, (dialog, which) -> {
                    dialog.dismiss();
                    if (which != fontBefore) {
                        DisplaySettings.saveFontChoice(activity, which);
                        changed[0] = true;
                    }
                })
                .setNegativeButton("Cancel", null)
                .setOnDismissListener(dialog -> {
                    if (changed[0]) {
                        activity.recreate();
                    }
                })
                .show();
    }
}
