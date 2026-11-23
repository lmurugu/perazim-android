package org.perazimchurch.app.ai.guide;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/** users.md for this device: preferred name, tone, length, reading level, topic counts and mood. No raw questions. */
public final class UserProfile {
    public static final Set<String> TONES = Collections.unmodifiableSet(new HashSet<>(Arrays.asList("warm", "plain", "formal")));
    public static final Set<String> LENGTHS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList("short", "medium")));
    public static final Set<String> READING = Collections.unmodifiableSet(new HashSet<>(Arrays.asList("simple", "standard")));
    public static final int MAX_TOPICS = 12;

    public String displayName = "";
    public String tone = "warm";
    public String length = "medium";
    public String reading = "standard";
    public double sentiment = 0.0;
    public int feedbackUp = 0;
    public int feedbackDown = 0;
    public final Map<String, Integer> topics = new LinkedHashMap<>();

    public void countTopics(Iterable<String> ids) {
        for (String id : ids) {
            Integer n = topics.get(id);
            if (n == null && topics.size() >= MAX_TOPICS) continue;
            topics.put(id, n == null ? 1 : n + 1);
        }
    }

    /** One short line for the prompt. Never includes topic counts or the name unless the user chose it. */
    public String styleLine() {
        StringBuilder sb = new StringBuilder();
        sb.append(tone).append(" tone; ").append("short".equals(length) ? "short replies" : "medium replies")
                .append("; ").append("simple".equals(reading) ? "plain words" : "standard wording");
        if (!displayName.isEmpty()) sb.append("; the user asked to be called ").append(displayName);
        return sb.toString();
    }

    public String renderUsersMd() {
        StringBuilder sb = new StringBuilder("# users.md (this device only)\n");
        sb.append("- Name to use: ").append(displayName.isEmpty() ? "not set" : displayName).append('\n');
        sb.append("- Tone: ").append(tone).append('\n');
        sb.append("- Length: ").append(length).append('\n');
        sb.append("- Reading level: ").append(reading).append('\n');
        sb.append("- Feedback: ").append(feedbackUp).append(" helpful, ").append(feedbackDown).append(" not helpful\n");
        sb.append("- Topics asked about (counts only): ");
        if (topics.isEmpty()) sb.append("none yet");
        for (Map.Entry<String, Integer> e : topics.entrySet()) sb.append(e.getKey()).append(' ').append(e.getValue()).append(' ');
        return sb.toString().trim() + "\n";
    }

    String encode() {
        StringBuilder sb = new StringBuilder();
        line(sb, "name", displayName);
        line(sb, "tone", tone);
        line(sb, "length", length);
        line(sb, "reading", reading);
        line(sb, "sentiment", String.valueOf(sentiment));
        line(sb, "up", String.valueOf(feedbackUp));
        line(sb, "down", String.valueOf(feedbackDown));
        for (Map.Entry<String, Integer> e : topics.entrySet()) line(sb, "topic." + e.getKey(), String.valueOf(e.getValue()));
        return sb.toString();
    }

    static UserProfile decode(String text) {
        UserProfile p = new UserProfile();
        if (text == null) return p;
        for (String line : text.split("\n")) {
            int eq = line.indexOf('=');
            if (eq <= 0) continue;
            String key = line.substring(0, eq);
            String value = Lesson.unescape(line.substring(eq + 1));
            try {
                if ("name".equals(key)) p.displayName = GuideText.clean(value, 24).replaceAll("[^\\p{L} '-]", "");
                else if ("tone".equals(key) && TONES.contains(value)) p.tone = value;
                else if ("length".equals(key) && LENGTHS.contains(value)) p.length = value;
                else if ("reading".equals(key) && READING.contains(value)) p.reading = value;
                else if ("sentiment".equals(key)) p.sentiment = clamp(Double.parseDouble(value));
                else if ("up".equals(key)) p.feedbackUp = Math.max(0, Integer.parseInt(value));
                else if ("down".equals(key)) p.feedbackDown = Math.max(0, Integer.parseInt(value));
                else if (key.startsWith("topic.") && p.topics.size() < MAX_TOPICS) {
                    p.topics.put(key.substring(6), Math.max(0, Integer.parseInt(value)));
                }
            } catch (NumberFormatException ignored) {
                // A damaged value is dropped; the rest of the profile still loads.
            }
        }
        return p;
    }

    private static void line(StringBuilder sb, String key, String value) {
        sb.append(key).append('=').append(Lesson.escape(value)).append('\n');
    }

    static double clamp(double v) {
        return Math.max(-1.0, Math.min(1.0, v));
    }
}
