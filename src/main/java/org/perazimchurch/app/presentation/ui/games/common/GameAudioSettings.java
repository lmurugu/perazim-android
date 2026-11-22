package org.perazimchurch.app.presentation.ui.games.common;

import android.app.Activity;
import android.content.SharedPreferences;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

/** Game sound settings for the current account: music and effects on or off, and their volumes. */
public final class GameAudioSettings {

    private GameAudioSettings() {}

    /** Shows the settings in a simple dialog. Changes apply at once and are saved per account. */
    public static void show(Activity activity, SharedPreferences scores, String scorePrefix) {
        GameAudio audio = GameAudio.get(activity);
        audio.bindAccount(scores, scorePrefix);
        LinearLayout box = new LinearLayout(activity);
        box.setOrientation(LinearLayout.VERTICAL);
        int pad = Math.round(20 * activity.getResources().getDisplayMetrics().density);
        box.setPadding(pad, pad, pad, pad);

        CheckBox music = new CheckBox(activity);
        music.setText("Background music");
        music.setChecked(audio.isMusicOn());
        music.setOnCheckedChangeListener((b, on) -> audio.setMusicOn(on));
        box.addView(music);

        CheckBox sfx = new CheckBox(activity);
        sfx.setText("Sound effects");
        sfx.setChecked(audio.isSfxOn());
        sfx.setOnCheckedChangeListener((b, on) -> audio.setSfxOn(on));
        box.addView(sfx);

        TextView musicLabel = new TextView(activity);
        musicLabel.setText("Music volume");
        box.addView(musicLabel);
        SeekBar musicBar = new SeekBar(activity);
        musicBar.setMax(100);
        musicBar.setProgress(audio.musicVolume());
        musicBar.setOnSeekBarChangeListener(new SimpleSeek(audio, true));
        box.addView(musicBar);

        TextView sfxLabel = new TextView(activity);
        sfxLabel.setText("Effects volume");
        box.addView(sfxLabel);
        SeekBar sfxBar = new SeekBar(activity);
        sfxBar.setMax(100);
        sfxBar.setProgress(audio.sfxVolume());
        sfxBar.setOnSeekBarChangeListener(new SimpleSeek(audio, false));
        box.addView(sfxBar);

        new android.app.AlertDialog.Builder(activity)
                .setTitle("Game sound")
                .setView(box)
                .setPositiveButton("Done", null)
                .show();
    }

    private static final class SimpleSeek implements SeekBar.OnSeekBarChangeListener {
        private final GameAudio audio;
        private final boolean musicLevel;
        SimpleSeek(GameAudio audio, boolean musicLevel) { this.audio = audio; this.musicLevel = musicLevel; }
        @Override public void onProgressChanged(SeekBar s, int p, boolean fromUser) {
            if (!fromUser) return;
            if (musicLevel) audio.setMusicVolume(p); else audio.setSfxVolume(p);
        }
        @Override public void onStartTrackingTouch(SeekBar s) { }
        @Override public void onStopTrackingTouch(SeekBar s) { }
    }
}
