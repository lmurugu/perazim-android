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
 * GATE: {@link #ENABLED} stays false until Levi approves the template (ai-eval/drafts/identify-refute-template-DRAFT.md).
 * While it is false, the other-faiths no-go zone keeps declining, and this class is only used by tests.
 */
public final class IdentifyRefute {

    public static final boolean ENABLED = false;

    static final String CLOSING = "We do not judge any person's eternal destiny. Test every teaching by Scripture, as the Bereans did, "
            + "and talk with a trusted church leader or your pastor.";

    /** Sentences that describe the guide or its method rather than the group's teaching. */
    private static final Pattern META = Pattern.compile(
            "\\b(guide|perazim|this guide|protestant reading|reformed readers?|checked here|both arguments|does not decide|"
                    + "leaves perazim|the guide's)\\b", Pattern.CASE_INSENSITIVE);

    private static final Pattern SENTENCE_END = Pattern.compile("(?<=[.!?])\\s+(?=[A-Z'‘“])");
    private static final Pattern ANCHOR = Pattern.compile("^(.+?)\\s+(\\d+):(\\d+)(?:-(\\d+))?$");

    /** Topic keywords mapped to an approved entry id and the name used in the answer. */
    private static final String[][] TOPICS = {
            {"jehovah|witness", "jesus_created", "Jehovah's Witnesses",
                    "Scripture says the Word was God and that all things were made by Him, so the Son is not a created being (John 1:1; Colossians 1:16)."},
            {"latter.day|mormon|lds", "eternal_progression", "the Latter-day Saint church",
                    "Scripture says there is one God, before and after whom none was formed, and that eternal life is God's gift, not a prize earned by exaltation (Isaiah 43:10; Romans 6:23)."},
            {"purgatory", "purgatory", "the Catholic Church",
                    "Scripture says the blood of Christ cleanses us from all sin, and that to be absent from the body is to be present with the Lord (1 John 1:7; 2 Corinthians 5:8)."},
            {"\\bmary\\b|mariology|mother of god", "mary_mediator", "the Catholic Church",
                    "Scripture names Christ as the one mediator between God and people, and says there is no other name by which we must be saved (1 Timothy 2:5; Acts 4:12)."},
    };
    private IdentifyRefute() {}

    /** The approved entry id for a question about one of the topics above, or null. */
    public static String entryIdFor(String question) {
        return topicFor(question) == null ? null : topicFor(question)[1];
    }

    private static String[] topicFor(String question) {
        if (question == null) return null;
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
        String teaching = teachingFrom(entry.optString("summary"));
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
        out.append("What ").append(topic[2]).append(" teaches, in its own words: ").append(teaching).append("\n\n");
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
