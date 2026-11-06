package org.perazimchurch.app.ai.remote;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** In-memory, bounded online conversation. Never persists or shares offline-guide messages. */
public final class ConversationHistory {
    public static final int MAX_TURNS = 12;
    public static final int MAX_TEXT = 1200;
    private final List<Turn> turns = new ArrayList<>();

    public static final class Turn {
        public final String role;
        public final String text;
        public Turn(String role, String text) {
            if (!"user".equals(role) && !"assistant".equals(role)) throw new IllegalArgumentException("Unsupported conversation role");
            this.role = role;
            String value = text == null ? "" : text.trim();
            this.text = value.substring(0, Math.min(value.length(), MAX_TEXT));
        }
    }

    public synchronized List<Turn> snapshot() {
        return Collections.unmodifiableList(new ArrayList<>(turns));
    }

    public synchronized void addExchange(String question, String answer) {
        turns.add(new Turn("user", question));
        turns.add(new Turn("assistant", answer));
        while (turns.size() > MAX_TURNS) {
            turns.remove(0);
            turns.remove(0);
        }
    }

    public synchronized boolean isEmpty() { return turns.isEmpty(); }
    public synchronized void clear() { turns.clear(); }

    /** Compact context for the already-deployed one-turn relay (which ignores the history field). */
    public static String legacyQuestion(String question, List<Turn> history) {
        if (history.isEmpty() || question.length() > 350) return question;
        Turn previous = history.get(history.size() - 1);
        int room = Math.max(0, 500 - question.length() - 100);
        String context = previous.text.substring(0, Math.min(previous.text.length(), room));
        return "Continue our conversation without another greeting. Your previous answer: " + context
                + "\nMy follow-up: " + question;
    }

    /** Older deployed relays may still add a greeting to each answer. Remove only that opening. */
    public static String withoutRepeatedGreeting(String answer, boolean continuation) {
        String text = answer == null ? "" : answer.trim();
        if (!continuation) return text;
        return text.replaceFirst("(?i)^(?:shalom|hello|hi|greetings|welcome back)(?:,\\s*(?:dear\\s+)?(?:fellow\\s+pilgrim|friend|[\\p{L}]+))?[,!:.]*\\s+(?=\\S)", "").trim();
    }
}
