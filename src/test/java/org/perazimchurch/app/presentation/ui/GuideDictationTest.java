package org.perazimchurch.app.presentation.ui;

import android.Manifest;
import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import android.os.Looper;
import android.speech.SpeechRecognizer;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadow.api.Shadow;
import org.robolectric.shadows.ShadowSpeechRecognizer;

import java.util.ArrayList;
import java.util.Collections;

import static org.junit.Assert.*;
import static org.robolectric.Shadows.shadowOf;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34, application = Application.class)
public class GuideDictationTest {
    @Test public void listensAndReturnsTheFinalWordsToTheDraft() {
        Activity activity = Robolectric.buildActivity(Activity.class).setup().get();
        shadowOf(activity.getApplication()).grantPermissions(Manifest.permission.RECORD_AUDIO);
        ShadowSpeechRecognizer.setIsOnDeviceRecognitionAvailable(true);
        Transcript view = new Transcript();
        GuideDictation dictation = new GuideDictation(activity, view);
        dictation.request();
        ShadowSpeechRecognizer service = Shadow.extract(ShadowSpeechRecognizer.getLatestSpeechRecognizer());
        shadowOf(Looper.getMainLooper()).idle();
        service.triggerOnReadyForSpeech(new Bundle());
        service.triggerOnPartialResults(words("tell me"));
        shadowOf(Looper.getMainLooper()).idle();
        assertEquals("Listening…", view.status);
        assertEquals("tell me", view.partial);
        assertNull(view.draft);
        service.triggerOnResults(words("tell me about kindness"));
        shadowOf(Looper.getMainLooper()).idle();
        assertEquals("tell me about kindness", view.draft);
        assertTrue(service.isDestroyed());
    }

    private static Bundle words(String text) {
        Bundle result = new Bundle();
        result.putStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION,
                new ArrayList<>(Collections.singletonList(text)));
        return result;
    }

    private static final class Transcript implements GuideDictation.View {
        String status, partial, draft;
        @Override public void showListening(String status) { this.status = status; }
        @Override public void setLevel(float rms) { }
        @Override public void showPartial(String text) { partial = text; }
        @Override public void finishListening(String text, String notice) { draft = text; }
    }
}
