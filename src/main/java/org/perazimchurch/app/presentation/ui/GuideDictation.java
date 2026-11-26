package org.perazimchurch.app.presentation.ui;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;

import java.util.ArrayList;
import java.util.Locale;

/** A single, user-initiated voice turn. Audio is handled by the selected Android speech service. */
public final class GuideDictation {
    public static final int PERMISSION_REQUEST = 7532;

    interface View {
        void showListening(String status);
        void setLevel(float rms);
        void showPartial(String text);
        void finishListening(String transcript, String notice);
    }

    private final Activity activity;
    private final View view;
    private final android.os.Handler handler = new android.os.Handler(android.os.Looper.getMainLooper());
    private SpeechRecognizer recognizer;
    private boolean waitingForPermission;
    private boolean useOnDevice;
    private String partial = "";
    private final Runnable stopTimeout = () -> complete(partial, "");

    GuideDictation(Activity activity, View view) {
        this.activity = activity;
        this.view = view;
    }

    void request() {
        if (recognizer != null || waitingForPermission) return;
        useOnDevice = Build.VERSION.SDK_INT >= 31 && SpeechRecognizer.isOnDeviceRecognitionAvailable(activity);
        if (useOnDevice) {
            askForMicrophone();
        } else if (SpeechRecognizer.isRecognitionAvailable(activity)) {
            new AlertDialog.Builder(activity).setTitle("Use Android dictation?")
                    .setMessage("Your phone’s speech service may use the internet to turn your voice into text. "
                            + "You can review the words before sending them to Perazim Assistant.")
                    .setNegativeButton("Keep typing", null)
                    .setPositiveButton("Use dictation", (dialog, which) -> askForMicrophone()).show();
        } else {
            view.finishListening("", "Dictation is unavailable on this phone. You can still type your message.");
        }
    }

    private void askForMicrophone() {
        if (activity.checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            start();
            return;
        }
        waitingForPermission = true;
        activity.requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, PERMISSION_REQUEST);
    }

    public void onPermissionResult(boolean granted) {
        if (!waitingForPermission) return;
        waitingForPermission = false;
        if (granted) start();
        else view.finishListening("", "Microphone access is off. You can still type your message.");
    }

    private void start() {
        partial = "";
        try {
            recognizer = useOnDevice ? SpeechRecognizer.createOnDeviceSpeechRecognizer(activity)
                    : SpeechRecognizer.createSpeechRecognizer(activity);
            SpeechRecognizer session = recognizer;
            session.setRecognitionListener(new RecognitionListener() {
                @Override public void onReadyForSpeech(Bundle extras) {
                    if (recognizer == session) view.showListening("Listening…");
                }
                @Override public void onBeginningOfSpeech() { }
                @Override public void onRmsChanged(float rms) {
                    if (recognizer == session) view.setLevel(rms);
                }
                @Override public void onBufferReceived(byte[] buffer) { }
                @Override public void onEndOfSpeech() {
                    if (recognizer == session) view.showListening("Preparing transcript…");
                }
                @Override public void onPartialResults(Bundle results) {
                    if (recognizer != session) return;
                    partial = bestMatch(results);
                    view.showPartial(partial);
                }
                @Override public void onResults(Bundle results) {
                    if (recognizer != session) return;
                    String text = bestMatch(results);
                    complete(text.isEmpty() ? partial : text, "");
                }
                @Override public void onError(int error) {
                    if (recognizer != session) return;
                    String missing = error == SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS
                            ? "Allow microphone access to dictate." : "Dictation couldn’t hear you. Try again or type your message.";
                    complete(partial, partial.isEmpty() ? missing : "");
                }
                @Override public void onEvent(int eventType, Bundle params) { }
            });
            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
                    .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    .putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toLanguageTag())
                    .putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
                    .putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    .putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);
            view.showListening("Starting microphone…");
            session.startListening(intent);
        } catch (RuntimeException e) {
            complete("", "Could not start Android dictation. You can still type your message.");
        }
    }

    void stop() {
        if (recognizer == null) return;
        try {
            view.showListening("Preparing transcript…");
            recognizer.stopListening();
            handler.removeCallbacks(stopTimeout);
            handler.postDelayed(stopTimeout, 5_000);
        } catch (RuntimeException e) {
            complete(partial, "");
        }
    }

    void cancel() {
        waitingForPermission = false;
        if (recognizer != null) complete("", "");
    }

    void pause() {
        if (recognizer != null) complete("", "");
    }

    private void complete(String text, String notice) {
        handler.removeCallbacks(stopTimeout);
        SpeechRecognizer active = recognizer;
        recognizer = null;
        if (active != null) active.destroy();
        view.finishListening(text, notice);
    }

    private static String bestMatch(Bundle results) {
        ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        return matches == null || matches.isEmpty() ? "" : matches.get(0).trim();
    }
}
