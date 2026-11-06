package org.perazimchurch.app.ai.local;

import java.util.List;
import org.perazimchurch.app.ai.remote.ConversationHistory;

/** Qwen ChatML prompt, bounded to fit the compact model's 2048-token context. */
public final class LocalPrompt {
    private LocalPrompt() {}
    public static String build(String question, List<ConversationHistory.Turn> history) {
        StringBuilder p = new StringBuilder("<|im_start|>system\nYou are Perazim, a thoughtful Christian conversation companion. Answer the latest question directly and naturally. Continue the conversation without repeated greetings or introductions. Be specific, concise, and honest when unsure. Never invent Bible quotes or references. Do not claim to be a pastor or professional.\n<|im_end|>\n");
        int start = Math.max(0, history.size() - 4);
        for (int i = start; i < history.size(); i++) {
            ConversationHistory.Turn turn = history.get(i);
            p.append("<|im_start|>").append(turn.role).append('\n').append(clean(turn.text, 650)).append("<|im_end|>\n");
        }
        return p.append("<|im_start|>user\n").append(clean(question, 650)).append("<|im_end|>\n<|im_start|>assistant\n").toString();
    }
    private static String clean(String text, int length) {
        String t = (text == null ? "" : text).replace("<|im_start|>", "").replace("<|im_end|>", "").trim();
        return t.substring(0, Math.min(t.length(), length));
    }
}
