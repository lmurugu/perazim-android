package org.perazimchurch.app.ai.local;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Model-composed replies with sentence-level verification (Levi and the coordinator, 2026-11-25).
 *
 * <p>The model writes the reply. Each sentence is kept only if it passes the checks below; a rejected sentence is dropped.
 * If no sentence passes, the caller falls back to its approved text and the turn is counted as fixed. Crisis, safeguarding,
 * no-go zones and unreviewed doctrine never reach this class.
 */
public final class ConversationComposer {

    /** Runs the model on a prompt and returns its raw text, or null when no model is available. */
    public interface Runner {
        String generate(String prompt, int maxTokens);
    }

    /** The outcome for one turn: the composed text when a model sentence passed, otherwise null. */
    public static final class Result {
        public final String text;
        public final String rawModelText;

        Result(String text, String rawModelText) {
            this.text = text;
            this.rawModelText = rawModelText;
        }

        public boolean composed() { return text != null; }
    }

    static final int MAX_SENTENCES = 3;
    static final int MAX_TOKENS = 110;
    private static final Set<String> STOP = new HashSet<>(Arrays.asList(
            "that", "this", "with", "from", "your", "have", "will", "about", "what", "when", "where", "which", "there",
            "their", "they", "them", "than", "then", "into", "also", "more", "very", "just", "like", "help", "would",
            "could", "should", "being", "been", "were", "does", "make", "some", "such", "only", "over", "under"));

    /** Words that make a sentence a Scripture, doctrine, authority, medical or legal claim. Such sentences are dropped. */
    private static final Pattern FORBIDDEN = Pattern.compile(
            "\\b(?:bible|scripture|scriptures|verse|verses|psalm|psalms|proverbs|gospel|jesus|christ|apostle|paul|"
                    + "doctrine|church teaching|god says|god has said|god promises|god wants|the lord says|the lord said|"
                    + "pastor|bishop|levi|mutweri|counsell\\w*|therapist|psychiatr\\w*|doctor|clinic|hospital|medic\\w*|"
                    + "diagnos\\w*|dose|pills?|lawyer|legal|court|loan\\w*|invest\\w*|suicid\\w*|kill\\w*|die|dying|death|dead|"
                    + "destiny|heaven|hell|saved|salvation|sin|sins|demon\\w*|spirits?)\\b", Pattern.CASE_INSENSITIVE);
    /** Claims the guide must never make about itself. */
    private static final Pattern SELF_CLAIM = Pattern.compile(
            "\\b(?:i am (?:a |an |the )?(?:pastor|bishop|human|person|counsellor|doctor|priest|levi)|i'?m (?:a |an |the )?(?:pastor|bishop|human|person|counsellor|doctor|priest|levi)|"
                    + "i will pray for you|i'?ll pray for you|i have prayed|i prayed)\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern NUMBER = Pattern.compile("\\d");
    private static final Pattern QUOTE = Pattern.compile("[\"“”„«»]");

    private ConversationComposer() {}

    /**
     * Composes a reply. {@code approvedText} is the approved evidence for this turn (may be empty for small talk).
     * {@code previousReply} is the last reply shown, so a repeated reply is never returned.
     */
    public static Result compose(Runner runner, String question, String approvedText, String previousReply,
                                 List<String> recentTurns, boolean requireSupport) {
        if (runner == null || question == null || question.trim().isEmpty()) return new Result(null, null);
        String raw = runner.generate(prompt(question, approvedText, recentTurns), MAX_TOKENS);
        if (raw == null) return new Result(null, null);
        String text = verify(raw, question, approvedText, requireSupport);
        if (text != null && previousReply != null && text.trim().equalsIgnoreCase(previousReply.trim())) {
            text = null;
        }
        return new Result(text, raw);
    }

    static String prompt(String question, String approvedText, List<String> recentTurns) {
        StringBuilder p = new StringBuilder();
        p.append("<|im_start|>system\n");
        p.append("You are Perazim Guide, the digital assistant of Perazim Mission Church (PMCI). You are an AI, not a pastor, ");
        p.append("bishop or counsellor. Reply in English, warmly and in plain words, in at most three short sentences. ");
        p.append("Do not quote or cite Bible verses, do not name any book, chapter or verse, and do not say that God or the Bible ");
        p.append("says anything. Do not give medical, legal or financial advice, do not name people, and do not judge anyone's ");
        p.append("eternal destiny. You can help with Bible passages from the King James Version, prayer, questions of faith and ");
        p.append("life, and the app. If you are unsure, say so and invite the member to say more.\n");
        if (approvedText != null && !approvedText.trim().isEmpty()) {
            p.append("Approved material for this answer (use only this for any teaching point, and do not add other claims):\n");
            p.append(approvedText.trim()).append("\n");
        }
        p.append("<|im_end|>\n");
        if (recentTurns != null) {
            for (String turn : recentTurns) {
                p.append(turn).append("\n");
            }
        }
        p.append("<|im_start|>user\n").append(question.trim()).append("<|im_end|>\n<|im_start|>assistant\n");
        return p.toString();
    }

    /**
     * Keeps the sentences that pass every check, up to {@link #MAX_SENTENCES}. Returns null when none passes.
     * When {@code requireSupport} is set (approved pastoral text), a sentence must share enough content words with the
     * approved text or the question. Conversational turns rely on the forbidden-content checks instead.
     */
    public static String verify(String raw, String question, String approvedText, boolean requireSupport) {
        if (raw == null) return null;
        String cleaned = raw.replace("<|im_end|>", "").replace("<|endoftext|>", "").replaceAll("\\s+", " ").trim();
        if (cleaned.isEmpty()) return null;
        String support = ((approvedText == null ? "" : approvedText) + " " + (question == null ? "" : question))
                .toLowerCase(Locale.ROOT);
        boolean evidenceMode = requireSupport && approvedText != null && !approvedText.trim().isEmpty();
        List<String> kept = new ArrayList<>();
        for (String sentence : cleaned.split("(?<=[.!?])\\s+")) {
            if (kept.size() >= MAX_SENTENCES) break;
            String s = sentence.trim();
            if (!passes(s, support, evidenceMode)) continue;
            kept.add(s);
        }
        return kept.isEmpty() ? null : String.join(" ", kept);
    }

    static boolean passes(String s, String support, boolean evidenceMode) {
        if (s.isEmpty() || s.length() > 240) return false;
        int words = s.split("\\s+").length;
        if (words > 45) return false;
        if (words < 3) return false;
        if (NUMBER.matcher(s).find()) return false;
        if (QUOTE.matcher(s).find()) return false;
        if (FORBIDDEN.matcher(s).find()) return false;
        if (SELF_CLAIM.matcher(s).find()) return false;
        if (evidenceMode) {
            int content = 0;
            int supported = 0;
            for (String w : s.toLowerCase(Locale.ROOT).split("[^a-z']+")) {
                if (w.length() < 4 || STOP.contains(w)) continue;
                content++;
                if (support.contains(w)) supported++;
            }
            if (content == 0 || supported * 10 < content * 6) return false;
        }
        return true;
    }
}
