package org.perazimchurch.app.ai.library;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Deterministic identify-and-refute answers for false-doctrine examples in the approved comparative KB.
 *
 * The teaching is identified from the approved entry's own summary, with sentences that describe the guide itself
 * removed. The biblical answer is given from at least two anchor passages, quoted word for word from the bundled KJV.
 * Nothing here judges an individual's eternal destiny.
 *
 * GATE: {@link #ENABLED} is on since Levi approved the template with revisions (2026-11-25). The heading is "What this
 * group teaches"; the group's own material is evidence of its position only; the teaching is refuted, not the people.
 */
public final class IdentifyRefute {

    /**
     * ON (Levi, 2026-11-25): the template is approved with revisions. Each topic below is answered only if its
     * approved entry, anchors and tests pass; the other-faiths zone declines every other group.
     */
    public static final boolean ENABLED = true;

    static final String CLOSING = "We do not judge any person's eternal destiny. Please talk with a trusted church leader or your pastor.";

    /** Sentences that describe the guide or its method rather than the group's teaching. */
    private static final Pattern META = Pattern.compile(
            "\\b(guide|perazim|this guide|protestant reading|reformed readers?|checked here|both arguments|does not decide|"
                    + "leaves perazim|the guide's|nicene|historic christianity)\\b", Pattern.CASE_INSENSITIVE);

    private static final Pattern SENTENCE_END = Pattern.compile("(?<=[.!?])\\s+(?=[A-Z'‘“])");
    private static final Pattern ANCHOR = Pattern.compile("^(.+?)\\s+(\\d+):(\\d+)(?:-(\\d+))?$");

    /** Topic keywords mapped to an approved entry id and the name used in the answer. */
    private static final String[][] TOPICS = {
            {"jehovah|witness", "jesus_created", "Jehovah's Witnesses",
                    "Scripture says the Word is God, and that all things were made through Him (John 1:1, 3). It also calls the Son God, says that His throne is for ever, and says that He laid the foundations of the earth (Hebrews 1:8, 10). So the Son is not a created being."},
            {"latter.day|mormon|lds", "eternal_progression", "the Latter-day Saint church",
                    "Latter-day Saint teaching distinguishes salvation from exaltation, and it grounds both in the Atonement of Jesus Christ. We do not say that they reject the Atonement. The specific disagreement is about God Himself: Scripture does not teach that a created person may become God. The LORD alone is God, and no god was formed before Him or after Him (Isaiah 43:10; Isaiah 44:6; 1 Timothy 6:15–16). Salvation is a gift of grace through faith, not of works, and good works are its fruit (Ephesians 2:8–10; Romans 6:23). We test their teaching about God and exaltation by Scripture."},
            {"purgatory", "purgatory", "the Catholic Church",
                    "Our biblical conclusion is that Christ offered one sacrifice for sins for ever (Hebrews 10:10–14), and that His blood cleanses us from all sin (1 John 1:7). The believer's hope after death is to be with Christ, as Jesus told the thief (Luke 23:43). Paul says that to depart and be with Christ is far better (Philippians 1:23; 2 Corinthians 5:6–8). We find no biblical teaching of a further purifying state after death."},
            {"mary.{0,60}(?:mediat|intercess|pray|invoc|saviour|savior|co-?redemp)|(?:mediat|intercess|pray|invoc)\\w*.{0,60}mary|mediatrix", "mary_mediator", "the Catholic Church",
                    "Scripture names Christ as the one mediator between God and people, and Paul commands prayer for all people in that light (1 Timothy 2:1-6). Christ alone is able to save completely those who come to God through Him (Hebrews 7:25), and there is no other name by which we must be saved (Acts 4:12). Scripture does not command the invocation of Mary, and it directs believers to come to the Father through Christ. We do not deny that Christians may pray for one another (1 Timothy 2:1)."},
    };
    /** Topic-specific teaching line, used where the approved summary is wider than the question (Levi, 2026-11-25). */
    private static final java.util.Map<String, String> TEACHING_OVERRIDE = new java.util.HashMap<>();
    static {
        TEACHING_OVERRIDE.put("mary_mediator", "Catholic teaching gives Mary the title Mediatrix and describes her role as dependent on Christ's unique mediation, not as a rival to it. Catholic teaching does not present Mary as equal to Christ or as an independent saviour.");
    }
    private IdentifyRefute() {}

    /** The approved entry id for a question about one of the topics above, or null. */
    public static String entryIdFor(String question) {
        return topicFor(question) == null ? null : topicFor(question)[1];
    }

    private static String[] topicFor(String question) {
        if (question == null) return null;
        // The Book of Mormon is a different question; the approved LDS answer covers exaltation (not answered here).
        if (Pattern.compile("book of mormon", Pattern.CASE_INSENSITIVE).matcher(question).find()) return null;
        for (String[] t : TOPICS) {
            if (Pattern.compile(t[0], Pattern.CASE_INSENSITIVE).matcher(question).find()) return t;
        }
        return null;
    }

    /**
     * The identify-and-refute answer for a question, or null when there is no topic, the entry is not an approved
     * false-doctrine example, or fewer than two anchor passages can be quoted verbatim (fail closed).
     */
    public static String answerFor(String question, JSONObject entry, KjvVerseText kjv) throws JSONException {
        String[] topic = topicFor(question);
        if (topic == null || entry == null || kjv == null) return null;
        if (!topic[1].equals(entry.optString("id"))) return null;
        if (!"FALSE_DOCTRINE_EXAMPLE".equals(entry.optString("classification"))) return null;
        if (!"APPROVED_LEVI".equals(entry.optString("status"))) return null;
        String teaching = TEACHING_OVERRIDE.containsKey(entry.optString("id"))
                ? TEACHING_OVERRIDE.get(entry.optString("id")) : teachingFrom(entry.optString("summary"));
        if (teaching.isEmpty()) return null;
        List<String> verses = new ArrayList<>();
        JSONArray anchors = entry.optJSONArray("anchors");
        if (anchors != null) {
            for (int i = 0; i < anchors.length(); i++) {
                String line = verseLine(anchors.optString(i), kjv);
                if (line != null) verses.add(line);
            }
        }
        if (verses.size() < 2) return null;
        StringBuilder out = new StringBuilder();
        out.append("What this group teaches: ").append(teaching).append("\n\n");
        out.append("What Scripture says:\n");
        for (String v : verses) out.append(v).append("\n");
        out.append("\nOur biblical answer: ").append(topic[3]).append("\n\n").append(CLOSING);
        return out.toString();
    }

    /** The first sentences of the approved summary that describe the group's teaching, without guide wording. */
    static String teachingFrom(String summary) {
        if (summary == null) return "";
        List<String> keep = new ArrayList<>();
        for (String s : SENTENCE_END.split(summary.trim())) {
            if (META.matcher(s).find()) continue;
            keep.add(s.trim());
            if (keep.size() == 3) break;
        }
        return String.join(" ", keep);
    }

    /** "Book c:v: “verbatim KJV text”", or null when the reference is not in the bundled KJV. */
    static String verseLine(String anchor, KjvVerseText kjv) {
        Matcher m = ANCHOR.matcher(anchor == null ? "" : anchor.trim());
        if (!m.find()) return null;
        int book = kjv.bookNumber(m.group(1));
        if (book <= 0) return null;
        int chapter = Integer.parseInt(m.group(2));
        int verse = Integer.parseInt(m.group(3));
        String text = kjv.verse(book, chapter, verse);
        if (text == null || text.isEmpty()) return null;
        return anchor.trim() + ": “" + text + "”";
    }
}
