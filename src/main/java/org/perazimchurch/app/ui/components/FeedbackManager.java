package org.perazimchurch.app.ui.components;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

/**
 * Shared sound, haptic and celebration feedback for the whole app.
 *
 * <p>One place, three switches (sound, haptics, celebrations), all stored in one preference file.
 * Sounds are synthesised in code so no audio files are needed. Any feature (for example a trivia
 * game) can call {@link #click(View)}, {@link #reward(Activity)}, {@link #milestone(Activity)} or
 * {@link #celebrate(Activity, boolean)} and the user's settings are applied automatically.
 */
public final class FeedbackManager {

    private static final String PREFS = "perazim_feedback";
    private static final String KEY_SOUND = "sound_enabled";
    private static final String KEY_HAPTICS = "haptics_enabled";
    private static final String KEY_CELEBRATIONS = "celebrations_enabled";
    private static final int SAMPLE_RATE = 22050;

    private static FeedbackManager instance;

    private final Context appContext;

    private FeedbackManager(Context context) {
        this.appContext = context.getApplicationContext();
    }

    public static synchronized FeedbackManager get(Context context) {
        if (instance == null) {
            instance = new FeedbackManager(context);
        }
        return instance;
    }

    private SharedPreferences prefs() {
        return appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    // ---- Settings -------------------------------------------------------

    public boolean isSoundEnabled() { return prefs().getBoolean(KEY_SOUND, true); }
    public boolean isHapticsEnabled() { return prefs().getBoolean(KEY_HAPTICS, true); }
    public boolean isCelebrationsEnabled() { return prefs().getBoolean(KEY_CELEBRATIONS, true); }

    public void setSoundEnabled(boolean on) { prefs().edit().putBoolean(KEY_SOUND, on).apply(); }
    public void setHapticsEnabled(boolean on) { prefs().edit().putBoolean(KEY_HAPTICS, on).apply(); }
    public void setCelebrationsEnabled(boolean on) { prefs().edit().putBoolean(KEY_CELEBRATIONS, on).apply(); }

    // ---- Events ---------------------------------------------------------

    /** Soft tick when a button is pressed. */
    /** Short sound cues for the Bible games. They use the same sound switch as every other cue. */
    public enum GameCue { PICK, MATCH, MISS, FOUND_ALL }

    public void gameCue(GameCue cue) {
        switch (cue) {
            case PICK: playTones(new int[]{740}, 25, 0.15f); break;
            case MATCH: playTones(new int[]{784, 988}, 60, 0.25f); break;
            case MISS: playTones(new int[]{220, 180}, 90, 0.2f); break;
            case FOUND_ALL: playTones(new int[]{523, 659, 784, 1047}, 120, 0.35f); break;
            default: break;
        }
    }

    public void click(View view) {
        if (isHapticsEnabled() && view != null) {
            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
        }
        playTones(new int[]{880}, 35, 0.18f);
    }

    /** Small reward, for example earning XP or a correct answer. */
    public void reward(Activity activity) {
        vibrate(new long[]{0, 30, 40, 30});
        playTones(new int[]{660, 880}, 90, 0.3f);
    }

    /** Big moment, for example finishing the daily routine or reaching a streak. */
    public void milestone(Activity activity) {
        vibrate(new long[]{0, 40, 60, 40, 60, 90});
        playTones(new int[]{523, 659, 784, 1047}, 110, 0.35f);
        celebrate(activity, false);
    }

    /**
     * Confetti over the current screen. Respects the celebrations switch.
     * The sound is not played here; pass withSound=true to add the milestone jingle.
     */
    public void celebrate(Activity activity, boolean withSound) {
        if (withSound) {
            playTones(new int[]{523, 659, 784, 1047}, 110, 0.35f);
        }
        if (!isCelebrationsEnabled() || activity == null || activity.isFinishing()) {
            return;
        }
        celebrateIn(activity.getWindow());
    }

    /**
     * Confetti over any window, for example a dialog such as the Games screen, which sits above the
     * activity window. Respects the celebrations switch. Plays no sound.
     */
    public void celebrateIn(android.view.Window window) {
        if (!isCelebrationsEnabled() || window == null) {
            return;
        }
        final ViewGroup decor = (ViewGroup) window.getDecorView();
        final ConfettiView confetti = new ConfettiView(decor.getContext());
        confetti.setClickable(false);
        confetti.setFocusable(false);
        decor.addView(confetti, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        confetti.start(() -> decor.removeView(confetti));
    }

    /** Call from Activity.dispatchTouchEvent on ACTION_UP to give every tapped button a soft tick. */
    public void clickAt(Activity activity, float rawX, float rawY) {
        View hit = findClickable(activity.getWindow().getDecorView(), rawX, rawY);
        if (hit != null) {
            click(hit);
        }
    }

    private static View findClickable(View v, float rawX, float rawY) {
        if (v.getVisibility() != View.VISIBLE) {
            return null;
        }
        int[] loc = new int[2];
        v.getLocationOnScreen(loc);
        if (rawX < loc[0] || rawX > loc[0] + v.getWidth() || rawY < loc[1] || rawY > loc[1] + v.getHeight()) {
            return null;
        }
        if (v instanceof ViewGroup) {
            ViewGroup g = (ViewGroup) v;
            for (int i = g.getChildCount() - 1; i >= 0; i--) {
                View found = findClickable(g.getChildAt(i), rawX, rawY);
                if (found != null) {
                    return found;
                }
            }
        }
        return v.isClickable() && v.isEnabled() ? v : null;
    }

    // ---- Internals ------------------------------------------------------

    @SuppressWarnings("deprecation")
    private void vibrate(long[] pattern) {
        if (!isHapticsEnabled()) {
            return;
        }
        try {
            Vibrator v = (Vibrator) appContext.getSystemService(Context.VIBRATOR_SERVICE);
            if (v == null || !v.hasVibrator()) {
                return;
            }
            if (Build.VERSION.SDK_INT >= 26) {
                v.vibrate(VibrationEffect.createWaveform(pattern, -1));
            } else {
                v.vibrate(pattern, -1);
            }
        } catch (RuntimeException ignored) {
            // Haptics are optional; never crash the app for them.
        }
    }

    /** Plays a short sequence of sine tones on a background thread. */
    private void playTones(final int[] freqs, final int msEach, final float volume) {
        if (!isSoundEnabled()) {
            return;
        }
        new Thread(() -> {
            try {
                int perTone = SAMPLE_RATE * msEach / 1000;
                short[] pcm = new short[perTone * freqs.length];
                for (int t = 0; t < freqs.length; t++) {
                    for (int i = 0; i < perTone; i++) {
                        double env = Math.min(1.0, Math.min(i / 200.0, (perTone - i) / 400.0));
                        double s = Math.sin(2 * Math.PI * freqs[t] * i / SAMPLE_RATE) * env * volume;
                        pcm[t * perTone + i] = (short) (s * Short.MAX_VALUE);
                    }
                }
                AudioTrack track = new AudioTrack(
                        new AudioAttributes.Builder()
                                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                                .build(),
                        new AudioFormat.Builder()
                                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                                .setSampleRate(SAMPLE_RATE)
                                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                                .build(),
                        pcm.length * 2,
                        AudioTrack.MODE_STATIC,
                        AudioManager.AUDIO_SESSION_ID_GENERATE);
                track.write(pcm, 0, pcm.length);
                track.play();
                Thread.sleep((long) msEach * freqs.length + 150);
                track.release();
            } catch (Exception ignored) {
                // Sound is optional; never crash the app for it.
            }
        }, "perazim-feedback-sound").start();
    }
}
