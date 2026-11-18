package org.perazimchurch.app.ai.local;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/** Builds a llama.cpp grammar whose only possible citations are verified passages. */
final class CitationGrammar {
    private CitationGrammar() {}

    static String build(JSONArray evidence) {
        return build(evidence, 120);
    }

    static String build(JSONArray evidence, int maxAnswerChars) {
        if (evidence == null || evidence.length() == 0) {
            throw new IllegalArgumentException("A citation grammar requires verified evidence.");
        }
        List<Citation> citations = new ArrayList<>();
        for (int i = 0; i < evidence.length(); i++) {
            JSONObject source = evidence.optJSONObject(i);
            if (source != null && (!source.has("citationEligible") || source.optBoolean("citationEligible", false))) {
                citations.add(new Citation(source.optString("sourceId"),
                        source.optString("version"), source.optString("location")));
            }
        }
        return build(citations, maxAnswerChars);
    }

    static String build(List<Citation> citations) {
        return build(citations, 120);
    }

    static String build(List<Citation> citations, int maxAnswerChars) {
        if (citations == null || citations.isEmpty()) {
            throw new IllegalArgumentException("A citation grammar requires verified evidence.");
        }
        if (maxAnswerChars < 1 || maxAnswerChars > 1000) {
            throw new IllegalArgumentException("The answer limit is invalid.");
        }
        StringBuilder grammar = new StringBuilder("root ::= answer \"\\n\" citation\nanswer ::= [^\\n]{1,")
                .append(maxAnswerChars).append("}\ncitation ::= ");
        for (Citation source : citations) {
            if (grammar.charAt(grammar.length() - 1) != ' ') grammar.append(" | ");
            grammar.append(literal("[" + source.sourceId + "@" + source.version + ": "
                    + source.location + "]"));
        }
        if (grammar.charAt(grammar.length() - 1) == ' ') {
            throw new IllegalArgumentException("No valid citation source is available.");
        }
        return grammar.toString();
    }

    static final class Citation {
        final String sourceId;
        final String version;
        final String location;

        Citation(String sourceId, String version, String location) {
            this.sourceId = sourceId;
            this.version = version;
            this.location = location;
        }
    }

    private static String literal(String value) {
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\r", "\\r").replace("\n", "\\n").replace("\t", "\\t") + "\"";
    }
}
