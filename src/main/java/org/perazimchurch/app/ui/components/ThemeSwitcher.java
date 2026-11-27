package org.perazimchurch.app.ui.components;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;

import org.perazimchurch.app.R;
import org.perazimchurch.app.ui.theme.AppDialogs;
import org.perazimchurch.app.ui.theme.GlassKit;
import org.perazimchurch.app.ui.theme.PerazimTheme;

/**
 * Lets the person pick System (follows the phone), Light or Dark, and rebuilds the current screen in place.
 *
 * <p>The choice appears as three icons (computer, sun, moon) in a small popup under the button that was tapped.
 * The theme colours are read when each view is built, so applying the new mode and recreating the activity is
 * enough. The app never exits, so a switch cannot look like a crash.
 */
public final class ThemeSwitcher {

    private ThemeSwitcher() {
    }

    /** Three icons under the tapped button; System is the default. */
    public static void choose(final Activity activity, View anchor) {
        if (anchor == null) {
            choose(activity);
            return;
        }
        final int current = PerazimTheme.savedChoice(activity);
        final int[] icons = {R.drawable.ic_theme_system, R.drawable.gl_sun, R.drawable.gl_moon};
        final String[] names = {"System", "Light", "Dark"};
        final PopupWindow[] holder = new PopupWindow[1];

        LinearLayout row = new LinearLayout(activity);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER);
        int pad = GlassKit.px(activity, 4);
        row.setPadding(pad, pad, pad, pad);
        // One frosted card holds all three choices. Light mode: translucent white glass. Dark mode: frosted purple.
        // Solid panel: a translucent one let the text of the screen show through and looked like overlap.
        GradientDrawable panel = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                PerazimTheme.isLight()
                        ? new int[]{Color.parseColor("#FFFFFF"), Color.parseColor("#FFFFFF")}
                        : new int[]{Color.parseColor("#4F1E5C"), Color.parseColor("#4F1E5C")});
        panel.setCornerRadius(GlassKit.px(activity, 18));
        panel.setStroke(GlassKit.px(activity, 1), PerazimTheme.isLight() ? Color.parseColor("#E7D5EC") : Color.parseColor("#4A3554"));
        row.setBackground(panel);
        GlassKit.keep(row);

        for (int choice = 0; choice < icons.length; choice++) {
            final int picked = choice;
            ImageView icon = new ImageView(activity);
            icon.setImageResource(icons[choice]);
            int inner = GlassKit.px(activity, 6);
            icon.setPadding(inner, inner, inner, inner);
            // The chosen icon is orange; the others take the ink colour. No circle behind each one.
            icon.setColorFilter(choice == current ? Color.parseColor("#E17D2F")
                    : (PerazimTheme.isLight() ? Color.parseColor("#2A0F33") : Color.parseColor("#F5F0F7")));
            icon.setContentDescription(names[choice]);
            icon.setClickable(true);
            icon.setOnClickListener(v -> {
                if (holder[0] != null) {
                    holder[0].dismiss();
                }
                if (picked != current) {
                    PerazimTheme.saveChoice(activity, picked);
                    PerazimTheme.applyPreference(activity);
                    activity.recreate();
                }
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(GlassKit.px(activity, 30), GlassKit.px(activity, 30));
            lp.setMargins(GlassKit.px(activity, 2), 0, GlassKit.px(activity, 2), 0);
            row.addView(icon, lp);
        }

        PopupWindow popup = new PopupWindow(row, LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT, true);
        popup.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        popup.setElevation(GlassKit.px(activity, 8));
        popup.setOutsideTouchable(true);
        holder[0] = popup;
        row.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        int xOff = anchor.getWidth() - row.getMeasuredWidth();
        popup.showAsDropDown(anchor, xOff, GlassKit.px(activity, 6));
    }

    /** Fallback without an anchor: the three choices as a simple list. */
    public static void choose(Activity activity) {
        final int current = PerazimTheme.savedChoice(activity);
        String[] labels = {"System (follows your phone)", "Light", "Dark"};
        AppDialogs.builder(activity)
                .setTitle("Appearance")
                .setSingleChoiceItems(labels, current, (dialog, which) -> {
                    dialog.dismiss();
                    if (which != current) {
                        PerazimTheme.saveChoice(activity, which);
                        PerazimTheme.applyPreference(activity);
                        activity.recreate();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
