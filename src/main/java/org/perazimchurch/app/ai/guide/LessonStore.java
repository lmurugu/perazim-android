package org.perazimchurch.app.ai.guide;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

/** Capped, deduplicated, expiring lessons. Lessons are only ever style notes (see LessonGuard). */
public final class LessonStore {
    public static final int CAP = 40;
    public static final long TTL_MILLIS = 120L * 24 * 60 * 60 * 1000;
    /** A lesson that keeps failing after use is removed rather than repeated. */
    public static final int MAX_FAILURES = 3;

    public enum Outcome { STORED, MERGED, REJECTED }

    private final List<Lesson> lessons = new ArrayList<>();

    /** Stores a new lesson, or merges a repeat into the existing one. */
    public synchronized Outcome propose(String kind, String text, List<String> topics, long now) {
        if (LessonGuard.reject(kind, text) != null) return Outcome.REJECTED;
        String clean = GuideText.clean(text, LessonGuard.MAX_CHARS);
        String id = idFor(kind, clean);
        expire(now);
        for (Lesson l : lessons) {
            if (l.id.equals(id)) {
                l.expiresAt = now + TTL_MILLIS;
                return Outcome.MERGED;
            }
        }
        lessons.add(new Lesson(id, kind, clean, topics, now, now + TTL_MILLIS));
        while (lessons.size() > CAP) evictWeakest();
        return Outcome.STORED;
    }

    /** Picks up to max lessons that share a topic with the question. General style lessons are a fallback. */
    public synchronized List<Lesson> select(List<String> topics, long now, int max) {
        expire(now);
        List<Lesson> scored = new ArrayList<>();
        for (Lesson l : lessons) {
            int overlap = 0;
            for (String t : topics) if (l.topics.contains(t)) overlap++;
            if (overlap > 0 || l.topics.isEmpty()) scored.add(l);
        }
        Collections.sort(scored, new Comparator<Lesson>() {
            @Override public int compare(Lesson a, Lesson b) {
                int ta = overlapOf(a, topics), tb = overlapOf(b, topics);
                if (ta != tb) return tb - ta;
                return Integer.compare(b.uses, a.uses);
            }
        });
        List<Lesson> out = new ArrayList<>();
        for (Lesson l : scored) {
            if (out.size() >= max) break;
            l.uses++;
            l.lastUsedAt = now;
            out.add(l);
        }
        return out;
    }

    /** The user disliked an answer that used these lessons. Repeated failures remove them. */
    public synchronized void recordFailure(List<String> lessonIds) {
        for (Iterator<Lesson> it = lessons.iterator(); it.hasNext(); ) {
            Lesson l = it.next();
            if (lessonIds.contains(l.id)) {
                l.failures++;
                if (l.failures >= MAX_FAILURES) it.remove();
            }
        }
    }

    public synchronized void expire(long now) {
        for (Iterator<Lesson> it = lessons.iterator(); it.hasNext(); ) {
            if (it.next().expiresAt <= now) it.remove();
        }
    }

    public synchronized List<Lesson> all() {
        return Collections.unmodifiableList(new ArrayList<>(lessons));
    }

    public synchronized void clear() {
        lessons.clear();
    }

    public synchronized String encode() {
        StringBuilder sb = new StringBuilder();
        for (Lesson l : lessons) sb.append(l.encode()).append('\n');
        return sb.toString();
    }

    public static LessonStore decode(String text) {
        LessonStore store = new LessonStore();
        if (text == null) return store;
        for (String line : text.split("\n")) {
            if (line.isEmpty()) continue;
            Lesson l = Lesson.decode(line);
            if (l != null && LessonGuard.reject(l.kind, l.text) == null && store.lessons.size() < CAP) {
                store.lessons.add(l);
            }
        }
        return store;
    }

    private static int overlapOf(Lesson l, List<String> topics) {
        int n = 0;
        for (String t : topics) if (l.topics.contains(t)) n++;
        return n;
    }

    private void evictWeakest() {
        Lesson worst = null;
        for (Lesson l : lessons) {
            if (worst == null || l.uses < worst.uses
                    || (l.uses == worst.uses && l.lastUsedAt < worst.lastUsedAt)) {
                worst = l;
            }
        }
        if (worst != null) lessons.remove(worst);
    }

    /** Stable id from kind and text, so the same note is merged rather than duplicated. */
    static String idFor(String kind, String text) {
        String key = kind + "|" + GuideText.normalise(text);
        long h = 1125899906842597L;
        for (int i = 0; i < key.length(); i++) h = 31 * h + key.charAt(i);
        return Long.toHexString(h);
    }
}
