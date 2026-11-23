package org.perazimchurch.app.ai.guide;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The on-device memory of the guide: lessons, users.md and the user skill. Everything is encrypted with the vault
 * and never leaves the device. Sensitive questions are not learned from or recorded.
 */
public final class GuideMemory {
    static final String LESSONS_FILE = "lessons.bin";
    static final String PROFILE_FILE = "profile.bin";

    private final GuideDocs docs;
    private final GuideVault vault;
    private LessonStore lessons;
    private UserProfile profile;
    private List<String> lastInjected = new ArrayList<>();
    private List<String> lastTopics = new ArrayList<>();

    public enum Outcome { SAVED, REJECTED_NOT_STYLE, CLEARED }

    private GuideMemory(GuideDocs docs, GuideVault vault, LessonStore lessons, UserProfile profile) {
        this.docs = docs;
        this.vault = vault;
        this.lessons = lessons;
        this.profile = profile;
    }

    public static GuideMemory open(GuideDocs docs, GuideVault vault) {
        return new GuideMemory(docs, vault, LessonStore.decode(vault.read(LESSONS_FILE)),
                UserProfile.decode(vault.read(PROFILE_FILE)));
    }

    /** Builds the prompt block for this question: the locked rules, one playbook, style and lessons. */
    public synchronized GuideBlock contextFor(String question, long now) {
        List<GuideDocs.SkillEntry> routed = GuideRouter.route(question, docs.index(), 1);
        List<String> topics = new ArrayList<>();
        for (GuideDocs.SkillEntry s : routed) topics.add(s.id);
        lastTopics = topics;
        String playbookId = routed.isEmpty() ? null : routed.get(0).playbook;
        String playbookText = playbookId == null ? null : docs.playbook(playbookId);
        List<Lesson> chosen = lessons.select(topics, now, 2);
        List<String> ids = new ArrayList<>();
        List<String> texts = new ArrayList<>();
        for (Lesson l : chosen) { ids.add(l.id); texts.add(l.text); }
        lastInjected = ids;
        String style = profile.styleLine();
        return GuideBlock.build(docs.core(), playbookId, playbookText, style, ids, texts,
                GuideBlock.BUDGET_CHARS);
    }

    /**
     * Records what happened after an answer: topics for interest counts, explicit style requests, and self-check
     * issues turned into fixed style lessons. Sensitive questions are skipped entirely.
     */
    public synchronized void afterAnswer(String question, List<String> issueCodes, boolean sensitive, long now) {
        if (sensitive || GuideSafety.isSensitive(question)) {
            lastTopics = new ArrayList<>();
            return;
        }
        List<String> topics = lastTopics;
        lastTopics = new ArrayList<>();
        profile.countTopics(topics);
        profile.sentiment = UserProfile.clamp(0.8 * profile.sentiment + 0.2 * UserSkill.mood(question));
        for (String pref : UserSkill.preferences(question)) applyPreference(pref);
        for (String issue : issueCodes) {
            String note = issueLesson(issue);
            if (note != null) lessons.propose(noteKind(issue), note, topics, now);
        }
        persist();
    }

    /** Thumbs-up or thumbs-down on the last answer. A thumbs-down counts against lessons that were used. */
    public synchronized void feedback(boolean helpful, String question, String answer, long now) {
        if (GuideSafety.isSensitive(question)) return;
        if (helpful) {
            profile.feedbackUp++;
        } else {
            profile.feedbackDown++;
            lessons.recordFailure(lastInjected);
            String note = answer != null && answer.length() > 400
                    ? "Keep replies shorter and direct." : "Explain more clearly and simply.";
            lessons.propose(answer != null && answer.length() > 400 ? "LENGTH" : "STYLE", note, lastTopics, now);
        }
        profile.sentiment = UserProfile.clamp(0.8 * profile.sentiment + 0.2 * (helpful ? 1.0 : -1.0));
        persist();
    }

    /**
     * A user's correction. Only text that passes the style guard is kept. Anything about doctrine, safety, privacy,
     * scope or rules is refused and never reaches the prompt.
     */
    public synchronized Outcome correction(String question, String correctionText, long now) {
        if (GuideSafety.isSensitive(question)) return Outcome.REJECTED_NOT_STYLE;
        LessonStore.Outcome result = lessons.propose("STYLE", correctionText, lastTopics, now);
        if (result == LessonStore.Outcome.REJECTED) return Outcome.REJECTED_NOT_STYLE;
        persist();
        return Outcome.SAVED;
    }

    public synchronized void setDisplayName(String name) {
        String clean = name == null ? "" : GuideText.clean(name, 24).replaceAll("[^\\p{L} '-]", "").trim();
        profile.displayName = clean;
        persist();
    }

    public synchronized void setTone(String tone) {
        if (UserProfile.TONES.contains(tone)) { profile.tone = tone; persist(); }
    }

    public synchronized void setLength(String length) {
        if (UserProfile.LENGTHS.contains(length)) { profile.length = length; persist(); }
    }

    /** What the guide remembers, for the in-app view. Lesson texts and users.md only. */
    public synchronized String describe(long now) {
        lessons.expire(now);
        StringBuilder sb = new StringBuilder(profile.renderUsersMd());
        sb.append("\nLearned style notes (").append(lessons.all().size()).append("):\n");
        if (lessons.all().isEmpty()) sb.append("- none yet\n");
        for (Lesson l : lessons.all()) sb.append("- ").append(l.text).append('\n');
        return sb.toString();
    }

    /** Erases every personal file on this device. Bundled operating files are not affected. */
    public synchronized Outcome forgetAll() {
        lessons.clear();
        profile = new UserProfile();
        lastInjected = new ArrayList<>();
        lastTopics = new ArrayList<>();
        vault.deleteAll();
        return Outcome.CLEARED;
    }

    private void persist() {
        try {
            vault.write(LESSONS_FILE, lessons.encode());
            vault.write(PROFILE_FILE, profile.encode());
        } catch (IOException e) {
            // Memory is best effort: a failed write keeps the in-memory state for this session only.
        }
    }

    private void applyPreference(String pref) {
        int eq = pref.indexOf('=');
        if (eq <= 0) return;
        String key = pref.substring(0, eq), value = pref.substring(eq + 1);
        if ("length".equals(key)) profile.length = value;
        else if ("reading".equals(key)) profile.reading = value;
        else if ("tone".equals(key)) profile.tone = value;
    }

    /** Fixed, developer-written notes for self-check outcomes. No user text is used here. */
    static String issueLesson(String issue) {
        if (SelfCheck.ISSUE_QUOTE.equals(issue)) return "Keep quotes short and copy them exactly.";
        if (SelfCheck.ISSUE_REFERENCE.equals(issue)) return "Check the chapter number before writing a reference.";
        if (SelfCheck.ISSUE_LENGTH.equals(issue)) return "Keep replies to a few short sentences.";
        return null;
    }

    private static String noteKind(String issue) {
        return SelfCheck.ISSUE_REFERENCE.equals(issue) ? "VERSE_FORMAT" : "STYLE";
    }

    public List<String> topicsForTests() {
        return Collections.unmodifiableList(lastTopics);
    }
}
