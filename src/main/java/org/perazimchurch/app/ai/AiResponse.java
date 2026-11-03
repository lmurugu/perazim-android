package org.perazimchurch.app.ai;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Model representing an AI assistant response in the Perazim Breakthrough Guide.
 * Includes text, interactive action deep-link buttons, and follow-up chips.
 */
public class AiResponse {

    public static final String INTENT_NONE = "NONE";
    public static final String INTENT_NAVIGATE_HOME = "NAVIGATE_HOME";
    public static final String INTENT_NAVIGATE_GIVING = "NAVIGATE_GIVING";
    public static final String INTENT_NAVIGATE_SERMONS = "NAVIGATE_SERMONS";
    public static final String INTENT_NAVIGATE_LIVE = "NAVIGATE_LIVE";
    public static final String INTENT_NAVIGATE_WORSHIP = "NAVIGATE_WORSHIP";
    public static final String INTENT_NAVIGATE_FELLOWSHIP = "NAVIGATE_FELLOWSHIP";
    public static final String INTENT_NAVIGATE_PROFILE = "NAVIGATE_PROFILE";
    public static final String INTENT_OPEN_BIBLE = "OPEN_BIBLE";
    public static final String INTENT_LAUNCH_RIDDLES = "LAUNCH_RIDDLES";
    public static final String INTENT_LAUNCH_JOKES = "LAUNCH_JOKES";
    public static final String INTENT_REFLECTIONS = "REFLECTIONS";

    private final String messageText;
    private final String actionIntent;
    private final String actionButtonLabel;
    private final String actionPayload;
    private final List<String> suggestedChips;

    public AiResponse(@NonNull String messageText,
                      @NonNull String actionIntent,
                      @Nullable String actionButtonLabel,
                      @Nullable String actionPayload,
                      @Nullable List<String> suggestedChips) {
        this.messageText = messageText;
        this.actionIntent = actionIntent;
        this.actionButtonLabel = actionButtonLabel;
        this.actionPayload = actionPayload;
        this.suggestedChips = suggestedChips != null ? new ArrayList<>(suggestedChips) : Collections.emptyList();
    }

    public static AiResponse plainText(@NonNull String messageText, @Nullable List<String> suggestedChips) {
        return new AiResponse(messageText, INTENT_NONE, null, null, suggestedChips);
    }

    public static AiResponse withAction(@NonNull String messageText,
                                        @NonNull String actionIntent,
                                        @NonNull String actionButtonLabel,
                                        @Nullable String actionPayload,
                                        @Nullable List<String> suggestedChips) {
        return new AiResponse(messageText, actionIntent, actionButtonLabel, actionPayload, suggestedChips);
    }

    @NonNull
    public String getMessageText() {
        return messageText;
    }

    @NonNull
    public String getActionIntent() {
        return actionIntent;
    }

    @Nullable
    public String getActionButtonLabel() {
        return actionButtonLabel;
    }

    @Nullable
    public String getActionPayload() {
        return actionPayload;
    }

    @NonNull
    public List<String> getSuggestedChips() {
        return suggestedChips;
    }

    public boolean hasAction() {
        return !INTENT_NONE.equals(actionIntent) && actionButtonLabel != null && !actionButtonLabel.isEmpty();
    }
}
