package org.perazimchurch.app.ai.guide;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** A short style note learned on this device. Stored as one encoded line inside the encrypted vault. */
public final class Lesson {
    public final String id;
    public final String kind;
    public final String text;
    public final List<String> topics;
    public final long createdAt;
    public long lastUsedAt;
    public long expiresAt;
    public int uses;
    public int failures;

    Lesson(String id, String kind, String text, List<String> topics, long createdAt, long expiresAt) {
        this.id = id;
        this.kind = kind;
        this.text = text;
        this.topics = Collections.unmodifiableList(new ArrayList<>(topics));
        this.createdAt = createdAt;
        this.lastUsedAt = createdAt;
        this.expiresAt = expiresAt;
    }

    /** One line: id, kind, uses, failures, created, last used, expires, topics (comma), text. */
    String encode() {
        StringBuilder csv = new StringBuilder();
        for (String t : topics) {
            if (csv.length() > 0) csv.append(',');
            csv.append(t);
        }
        return join(id, kind, String.valueOf(uses), String.valueOf(failures), String.valueOf(createdAt),
                String.valueOf(lastUsedAt), String.valueOf(expiresAt), csv.toString(), text);
    }

    static Lesson decode(String line) {
        String[] f = line.split("\t", -1);
        if (f.length != 9) return null;
        try {
            List<String> topics = new ArrayList<>();
            for (String t : f[7].split(",")) if (!t.isEmpty()) topics.add(t);
            Lesson lesson = new Lesson(f[0], f[1], unescape(f[8]), topics, Long.parseLong(f[4]), Long.parseLong(f[6]));
            lesson.uses = Integer.parseInt(f[2]);
            lesson.failures = Integer.parseInt(f[3]);
            lesson.lastUsedAt = Long.parseLong(f[5]);
            return lesson;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String join(String... parts) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            if (i > 0) sb.append('\t');
            sb.append(escape(parts[i]));
        }
        return sb.toString();
    }

    static String escape(String s) {
        return s.replace("\\", "\\\\").replace("\t", "\\t").replace("\n", "\\n").replace("\r", " ");
    }

    static String unescape(String s) {
        StringBuilder out = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\\' && i + 1 < s.length()) {
                char n = s.charAt(++i);
                out.append(n == 't' ? '\t' : n == 'n' ? '\n' : n);
            } else {
                out.append(c);
            }
        }
        return out.toString();
    }
}
