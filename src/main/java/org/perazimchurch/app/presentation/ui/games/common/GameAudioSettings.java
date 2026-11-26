package org.perazimchurch.app.presentation.ui.games.common;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.drawable.GradientDrawable;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;

import org.perazimchurch.app.ui.theme.PerazimTheme;

public final class GameAudioSettings {
    private GameAudioSettings() {}

    /** The same control in every game header. */
    public static Button button(Context context) {
        Button button = new Button(context);
        button.setText("Sound");
        button.setAllCaps(false);
        button.setSoundEffectsEnabled(false);
        button.setContentDescription("Game sound settings");
        button.setTextColor(PerazimTheme.COLOR_TEXT_PRIMARY);
        button.setTextSize(12);
        button.setMinWidth(0);
        button.setMinimumWidth(0);
        button.setMinHeight(dp(context, 48));
        button.setPadding(dp(context, 11), 0, dp(context, 11), 0);
        GradientDrawable fill = new GradientDrawable();
        fill.setColor(PerazimTheme.COLOR_SURFACE_SOLID);
        fill.setCornerRadius(dp(context, 13));
        fill.setStroke(dp(context, 1), PerazimTheme.COLOR_STROKE);
        button.setBackground(fill);
        button.setStateListAnimator(null);
        button.setOnClickListener(v -> show(context));
        return button;
    }

    public static void show(Activity activity, SharedPreferences scores, String scorePrefix) {
        GameAudio.get(activity).bindAccount(scores, scorePrefix);
        show(activity);
    }

    private static void show(Context context) {
        GameAudio audio = GameAudio.get(context);
        LinearLayout box = new LinearLayout(context);
        box.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(context, 20);
        box.setPadding(pad, dp(context, 6), pad, pad);
        Switch music = addSwitch(box, "Music", audio.isMusicOn());
        Switch voice = addSwitch(box, "Voice", audio.isAnnouncerOn());
        Switch effects = addSwitch(box, "Sound effects", audio.isSfxOn());
        music.setOnCheckedChangeListener((v, on) -> audio.setMusicOn(on));
        voice.setOnCheckedChangeListener((v, on) -> audio.setAnnouncerOn(on));
        effects.setOnCheckedChangeListener((v, on) -> audio.setSfxOn(on));

        TextView label = new TextView(context);
        label.setText("Quick mixes");
        label.setPadding(0, dp(context, 12), 0, dp(context, 3));
        box.addView(label);
        action(box, "Voice only", () -> mix(music, voice, effects, true, false));
        action(box, "Effects only", () -> mix(music, voice, effects, false, true));
        action(box, "Voice + effects", () -> mix(music, voice, effects, true, true));

        addVolume(box, "Music volume", audio.musicVolume(), audio::setMusicVolume);
        addVolume(box, "Voice volume", audio.voiceVolume(), audio::setVoiceVolume);
        addVolume(box, "Effects volume", audio.sfxVolume(), audio::setSfxVolume);
        action(box, "Preview round start", audio::previewStart);
        action(box, "Preview correct answer", () -> audio.previewAnswer(true));
        action(box, "Preview incorrect answer", () -> audio.previewAnswer(false));
        action(box, "Preview mix", audio::previewMix);

        ScrollView scroller = new ScrollView(context);
        scroller.addView(box);
        AlertDialog dialog = new AlertDialog.Builder(context)
                .setTitle("Game sound")
                .setView(scroller)
                .setPositiveButton("Done", null)
                .create();
        dialog.setOnDismissListener(d -> audio.stopAnnouncer());
        dialog.show();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setSoundEffectsEnabled(false);
    }

    private static void mix(Switch music, Switch voice, Switch effects, boolean hearVoice, boolean hearEffects) {
        music.setChecked(false);
        voice.setChecked(hearVoice);
        effects.setChecked(hearEffects);
    }

    private static Switch addSwitch(LinearLayout box, String label, boolean checked) {
        Switch control = new Switch(box.getContext());
        control.setText(label);
        control.setChecked(checked);
        control.setMinHeight(dp(box.getContext(), 50));
        control.setSoundEffectsEnabled(false);
        box.addView(control, new LinearLayout.LayoutParams(-1, -2));
        return control;
    }

    private static void action(LinearLayout box, String label, Runnable action) {
        Button button = new Button(box.getContext());
        button.setText(label);
        button.setAllCaps(false);
        button.setSoundEffectsEnabled(false);
        button.setOnClickListener(v -> action.run());
        box.addView(button, new LinearLayout.LayoutParams(-1, -2));
    }

    private static void addVolume(LinearLayout box, String name, int volume, java.util.function.IntConsumer apply) {
        TextView label = new TextView(box.getContext());
        label.setText(name);
        box.addView(label);
        android.widget.SeekBar slider = new android.widget.SeekBar(box.getContext());
        slider.setMax(100);
        slider.setProgress(volume);
        slider.setContentDescription(name);
        slider.setSoundEffectsEnabled(false);
        slider.setOnSeekBarChangeListener(new android.widget.SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(android.widget.SeekBar view, int progress, boolean user) {
                if (user) apply.accept(progress);
            }
            @Override public void onStartTrackingTouch(android.widget.SeekBar view) { }
            @Override public void onStopTrackingTouch(android.widget.SeekBar view) { }
        });
        box.addView(slider);
    }

    private static int dp(Context context, int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }
}
