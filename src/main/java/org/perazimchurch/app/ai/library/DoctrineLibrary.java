package org.perazimchurch.app.ai.library;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Approved doctrine wording for the guide, read from the library's own files.
 *
 * <ul>
 *   <li>APPROVED_LEVI entries (entries.json) are shown as written: the Perazim view first, then the other views,
 *       then the KJV verses from the entry, then the no-verdict line.</li>
 *   <li>UNDER_REVIEW positions (positions.json) are shown as a developing view, labelled as under review.</li>
 *   <li>UNDECIDED positions are explained without a position.</li>
 *   <li>Anything else (APPROVED without wording, DRAFT_PENDING_LEVI) is never shown here; the question goes on
 *       to the model and its own evidence checks.</li>
 * </ul>
 */
public final class DoctrineLibrary {

    /** Question shapes that ask for a doctrinal position. Undecided topics are answered only for these. */
    private static final Pattern DOCTRINAL_CUE = Pattern.compile(
            "\\b(?:is it|is|are|should|can|may|position|view|views|teach|taught|biblical|biblically|permitted|allowed|" +
            "does the (?:church|bible|guide)|what does the (?:church|guide|bible|perazim)|what is the (?:church|guide|perazim)|church (?:position|teaching))\\b",
            Pattern.CASE_INSENSITIVE);
    private static final String ENTRIES = "ai/library/doctrine/entries.json";
    private static final String POSITIONS = "ai/library/doctrine/positions.json";
    private static DoctrineLibrary loaded;

    private final List<Topic> topics;

    private static final class Topic {
        final String id;
        final String name;
        final String status;
        final Pattern match;
        final String view;
        final List<String> otherViews;
        final List<String> verseLines;
        final String noVerdict;

        Topic(String id, String name, String status, Pattern match, String view, List<String> otherViews,
              List<String> verseLines, String noVerdict) {
            this.id = id;
            this.name = name;
            this.status = status;
            this.match = match;
            this.view = view;
            this.otherViews = otherViews;
            this.verseLines = verseLines;
            this.noVerdict = noVerdict;
        }
    }

    /** Topic matchers by id. A topic with no matcher is never routed here. */
    private static final java.util.Map<String, String> MATCH = new java.util.HashMap<>();
    static {
        MATCH.put("holy-spirit-baptism", "holy spirit|holy ghost|tongues|speaking in tongues|spirit baptism|baptism in the spirit|pentecostal");
        MATCH.put("prosperity-and-giving", "prosperity|prosperity gospel|seed offering|seed faith|sow a seed|giving seed|health and wealth");
        MATCH.put("polygamy", "polygamy|polygamous|polygamist|multiple wives|more than one wife|second wife|many wives");
        MATCH.put("dowry", "dowry|bride price|bride wealth|lobola|roora");
        MATCH.put("apostasy-assurance", "lose (?:their|his|her|my|your)? ?salvation|lose salvation|once saved always saved|eternal security|apostasy|apostate|fall away|backslid|assurance of salvation");
        MATCH.put("unconditional-election", "unconditional election|predestin|calvinis|elect of god|chosen before the foundation");
        MATCH.put("pre-trib-premillennialism", "rapture|pre-?trib|premillennial|pre-?millennial|tribulation|millennium|national israel");
        MATCH.put("women-in-ministry", "women (?:in|as) (?:ministry|leadership|preach|pastor|elder)|women preach|female pastor|women pastors|ordain women|women elders");
        MATCH.put("divorce-remarriage", "divorce|remarr");
        MATCH.put("tithing", "tithe|tithing");
        MATCH.put("alcohol", "alcohol|wine|beer|drunk|drunkenness");
        MATCH.put("healing-in-atonement", "healing in the atonement|atonement.{0,30}heal|divine healing");
        MATCH.put("evolution-age-of-earth", "evolution|evolve|age of the earth|young earth|old earth|creation science");
        MATCH.put("sexuality", "homosexu|same-sex|same sex|lgbt|transgender");
        MATCH.put("inerrancy-formulation", "inerran|infallib");
        MATCH.put("final-punishment", "eternal punishment|final punishment|annihilation|annihilationism|universalism|conditional immortality");
    }

    private DoctrineLibrary(List<Topic> topics) {
        this.topics = Collections.unmodifiableList(topics);
    }

    /** Parses the two library files. Only topics with a matcher and an allowed status are kept. */
    public static DoctrineLibrary fromJson(String entriesJson, String positionsJson) throws JSONException {
        List<Topic> topics = new ArrayList<>();
        JSONArray entries = new JSONObject(entriesJson).getJSONArray("entries");
        for (int i = 0; i < entries.length(); i++) {
            JSONObject e = entries.getJSONObject(i);
            String id = e.getString("id");
            if (!"APPROVED_LEVI".equals(e.optString("status")) || !MATCH.containsKey(id)) continue;
            List<String> others = strings(e.optJSONArray("otherViews"));
            List<String> verseLines = new ArrayList<>();
            JSONArray verses = e.optJSONArray("verses");
            if (verses != null) {
                for (int v = 0; v < Math.min(verses.length(), 3); v++) {
                    JSONObject verse = verses.getJSONObject(v);
                    verseLines.add(verse.getString("ref") + " (King James Version): “" + verse.getString("text") + "”");
                }
            }
            topics.add(new Topic(id, e.getString("topic"), "APPROVED_LEVI", pattern(id),
                    e.getString("perazimView"), others, verseLines, e.optString("noVerdict", "")));
        }
        JSONArray positions = new JSONObject(positionsJson).getJSONArray("topics");
        for (int i = 0; i < positions.length(); i++) {
            JSONObject p = positions.getJSONObject(i);
            String id = p.getString("id");
            String status = p.optString("status");
            if (!MATCH.containsKey(id) || topics.stream().anyMatch(t -> t.id.equals(id))) continue;
            if (!"UNDER_REVIEW".equals(status) && !"UNDECIDED".equals(status)) continue;
            String wording = "";
            JSONObject draft = p.optJSONObject("draft");
            if (draft != null && "UNDER_REVIEW".equals(status)) wording = draft.optString("wording", "");
            topics.add(new Topic(id, p.getString("topic"), status, pattern(id), wording,
                    Collections.emptyList(), Collections.emptyList(), ""));
        }
        return new DoctrineLibrary(topics);
    }

    /** Loads the bundled library once per process. Returns an empty library if the files cannot be read. */
    public static synchronized DoctrineLibrary load(android.content.Context context) {
        if (loaded != null) return loaded;
        try {
            loaded = fromJson(read(context, ENTRIES), read(context, POSITIONS));
        } catch (Exception e) {
            loaded = new DoctrineLibrary(new ArrayList<>());
        }
        return loaded;
    }

    /** The approved answer for a doctrine question, or null when the question is not one of these topics. */
    public String answerFor(String question) {
        if (question == null || question.trim().isEmpty()) return null;
        String normalized = question.toLowerCase(Locale.ROOT).replace('’', '\'');
        for (Topic t : topics) {
            if (!t.match.matcher(normalized).find()) continue;
            // An undecided topic in a personal or pastoral message is not a doctrine question: let the model answer.
            if ("UNDECIDED".equals(t.status) && !DOCTRINAL_CUE.matcher(normalized).find()) continue;
            return render(t);
        }
        return null;
    }

    private static String render(Topic t) {
        StringBuilder out = new StringBuilder();
        switch (t.status) {
            case "APPROVED_LEVI":
                out.append(t.view);
                if (!t.otherViews.isEmpty()) {
                    out.append("\n\nOther Christian views: ").append(String.join(" ", t.otherViews));
                }
                if (!t.verseLines.isEmpty()) {
                    out.append("\n\nScripture: ").append(String.join(" ", t.verseLines));
                }
                if (!t.noVerdict.isEmpty()) out.append("\n\n").append(t.noVerdict);
                break;
            case "UNDER_REVIEW":
                out.append("This is a developing view, under review, and not yet a settled church position. ");
                out.append(t.view.isEmpty()
                        ? t.name + " is under review by the church's leadership."
                        : withoutPlaceholders(t.view));
                break;
            default:
                out.append("The guide has not settled a position on ").append(t.name.toLowerCase(Locale.ROOT))
                        .append(", so I will not state one. Please speak with your pastor or a church leader about it.");
        }
        return out.toString();
    }

    /**
     * Drops internal placeholder sentences from approved wording. The alternative views for these topics are not yet
     * written, so the sentences that promise them are removed rather than shown to members.
     */
    static String withoutPlaceholders(String view) {
        return view.replace("Other positions are described fairly and labelled as alternatives.", "")
                .replace("No alternative text is written yet.", "")
                .replaceAll("\\s{2,}", " ")
                .trim();
    }

    private static java.util.regex.Pattern pattern(String id) {
        return Pattern.compile("\\b(?:" + MATCH.get(id) + ")\\b", Pattern.CASE_INSENSITIVE);
    }

    private static List<String> strings(JSONArray array) throws JSONException {
        List<String> out = new ArrayList<>();
        if (array != null) for (int i = 0; i < array.length(); i++) out.add(array.getString(i));
        return out;
    }

    private static String read(android.content.Context context, String path) throws IOException {
        try (InputStream in = context.getAssets().open(path)) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
