package org.perazimchurch.app.ai.remote;

import android.content.Context;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public final class AiRelayPrivacyDeviceTest {
    @Test public void sensitivePastoralDisclosureIsStoppedBeforeOnlineEligibilityOrDispatch() throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        CountDownLatch completed = new CountDownLatch(1);
        AtomicReference<String> result = new AtomicReference<>();
        AiRelayClient.askQuestion(context,
                "I feel depressed after my family conflict; please keep this confidential.",
                new AiRelayClient.RelayCallback() {
                    @Override public void onSuccess(String answer, int remaining, boolean generated) {
                        result.set("Unexpected online answer");
                        completed.countDown();
                    }
                    @Override public void onError(String message) {
                        result.set(message);
                        completed.countDown();
                    }
                });
        assertTrue("Sensitive disclosure gate did not respond promptly", completed.await(5, TimeUnit.SECONDS));
        assertTrue("Sensitive content must be handled before the build/consent/network checks: " + result.get(),
                result.get() != null && result.get().contains("cannot promise confidentiality")
                        && result.get().contains("has not been sent for pastoral follow-up"));
    }
}
