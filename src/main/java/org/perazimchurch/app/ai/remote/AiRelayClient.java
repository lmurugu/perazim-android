package org.perazimchurch.app.ai.remote;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

import java.util.List;

/**
 * Fail-closed placeholder until an authenticated server-side AI relay contract is
 * implemented and verified. The Android client never carries a shared relay key.
 */
public final class AiRelayClient {
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static final String UNAVAILABLE =
            "Online AI is unavailable in this build while the authenticated church service is being verified. " +
            "Use built-in app help or try again after a verified service is available.";

    public interface RelayCallback {
        void onSuccess(@NonNull String answer, int remainingQuestionsToday);
        void onQuotaExceeded(int remainingQuestionsToday);
        void onError(@NonNull String errorMessage, boolean shouldFallbackOffline);
    }

    private AiRelayClient() { }

    public static void askQuestion(@NonNull Context context, @NonNull String question,
                                   @NonNull RelayCallback callback) {
        askQuestion(context, question, java.util.Collections.emptyList(), callback);
    }

    public static void askQuestion(@NonNull Context context, @NonNull String question,
                                   @NonNull List<ConversationHistory.Turn> history,
                                   @NonNull RelayCallback callback) {
        MAIN.post(() -> callback.onError(UNAVAILABLE, false));
    }
}
