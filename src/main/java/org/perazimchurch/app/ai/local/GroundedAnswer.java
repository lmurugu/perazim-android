package org.perazimchurch.app.ai.local;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Grounded answers. When evidence was retrieved, the answer body is the evidence text itself, verbatim. The model may
 * add one short framing sentence before it, but only if every claim in that sentence is supported: each content word
 * comes from the question or the evidence, and each quotation, reference, number and capitalised name is in the evidence
 * or the question. Anything else, and any prompt or template text, is rejected, and the evidence-only answer is used.
 */
public final class GroundedAnswer {

    static final int MAX_BODY_CHARS = 900;
    static final int MAX_FRAMING_CHARS = 220;

    private static final Set<String> FRAMING_STOP = new HashSet<>(Arrays.asList(
            "that", "this", "with", "from", "have", "has", "are", "was", "were", "will", "would", "should", "could",
            "into", "about", "there", "their", "they", "them", "your", "yours", "which", "what", "when", "where",
            "while", "than", "then", "also", "only", "some", "many", "more", "most", "such", "both", "each", "other",
            "does", "did", "not", "but", "and", "for", "the", "you", "can", "its", "our", "his", "her", "any", "all",
            "one", "two", "may", "might", "being", "been", "very", "just", "like", "know", "take", "make", "help",
            "please", "answer", "question", "guide", "church", "christian", "christians", "bible", "verse"));

    private static final Pattern TEMPLATE = Pattern.compile(
            "UNTRUSTED|EXCERPT|\\[title:|authority:|sourceId|contentHash|<\\||\\bInstruction\\b|\\bQuestion:|\\bAnswer:|" +
            "\\bEvidence\\b|\\bSYSTEM\\b|\\bEND\\b|##|\\bsystem prompt\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern REFERENCE = Pattern.compile("\\b\\d{1,3}:\\d{1,3}(?:[-–]\\d{1,3})?\\b");
    private static final Pattern NUMBER = Pattern.compile("\\d+");
    private static final Pattern QUOTE = Pattern.compile("[“\"]([^”\"]{2,})[”\"]");
    private static final Pattern CAPITALISED = Pattern.compile("\\b\\p{Lu}\\p{Ll}{2,}\\b");
    private static final Pattern WORD = Pattern.compile("[\\p{L}]{4,}");
    private static final Pattern NEGATION = Pattern.compile(
            "\\b(not|never|no|none|nor|cannot)\\b|n't", Pattern.CASE_INSENSITIVE);

    private GroundedAnswer() {}

    /** The evidence text, verbatim, with its source line. Empty when there is no evidence. */
    public static String body(JSONArray evidence) {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < evidence.length(); i++) {
            JSONObject e = evidence.optJSONObject(i);
            if (e == null) continue;
            String text = e.optString("text", "").trim();
            if (text.isEmpty()) continue;
            if (text.length() > MAX_BODY_CHARS) {
                int cut = text.lastIndexOf('.', MAX_BODY_CHARS);
                text = text.substring(0, cut > 0 ? cut + 1 : MAX_BODY_CHARS);
            }
            if (out.length() > 0) out.append("\n\n");
            out.append(text);
            String title = e.optString("title", "").trim();
            String location = e.optString("location", "").trim();
            String source = (title + (location.isEmpty() ? "" : ", " + location)).trim();
            if (!source.isEmpty()) out.append("\n(").append(source).append(')');
        }
        return out.toString();
    }

    /**
     * The final answer for a grounded question: the optional framing sentence (only when supported) and then the evidence
     * body. Returns null when there is no evidence, so the caller keeps its refusal.
     */
    public static String finalAnswer(String question, String modelRaw, JSONArray evidence) {
        String body = body(evidence);
        if (body.isEmpty()) return null;
        String framing = firstSentence(modelRaw);
        if (framing != null && accepts(framing, question, evidence)) {
            return framing + "\n\n" + body;
        }
        return body;
    }

    /** True when a model sentence is supported by the question and the evidence, and carries no prompt text. */
    public static boolean accepts(String sentence, String question, JSONArray evidence) {
        if (sentence == null || sentence.trim().isEmpty() || sentence.length() > MAX_FRAMING_CHARS) return false;
        if (TEMPLATE.matcher(sentence).find()) return false;
        String evidenceText = evidenceText(evidence);
        String q = question == null ? "" : question;
        String known = (evidenceText + " " + q).toLowerCase(Locale.ROOT);
        // A framing may not add a denial that the evidence or the question does not already state.
        Matcher neg = NEGATION.matcher(sentence);
        while (neg.find()) {
            if (!known.contains(neg.group().toLowerCase(Locale.ROOT))) return false;
        }
        String evidenceLower = evidenceText.toLowerCase(Locale.ROOT);
        Matcher quote = QUOTE.matcher(sentence);
        while (quote.find()) {
            if (!evidenceLower.contains(normal(quote.group(1)))) return false;
        }
        Matcher ref = REFERENCE.matcher(sentence);
        while (ref.find()) {
            if (!known.contains(ref.group())) return false;
        }
        Matcher number = NUMBER.matcher(sentence.replaceAll("\\d{1,3}:\\d{1,3}(?:[-–]\\d{1,3})?", " "));
        while (number.find()) {
            if (!known.contains(number.group())) return false;
        }
        String withoutQuotes = QUOTE.matcher(sentence).replaceAll(" ");
        Matcher name = CAPITALISED.matcher(withoutQuotes);
        while (name.find()) {
            String word = name.group().toLowerCase(Locale.ROOT);
            if (FRAMING_STOP.contains(word)) continue;
            if (!wordIn(known, word)) return false;
        }
        Set<String> questionWords = words(q);
        Matcher w = WORD.matcher(withoutQuotes.toLowerCase(Locale.ROOT));
        while (w.find()) {
            String word = w.group();
            if (FRAMING_STOP.contains(word) || questionWords.contains(word)) continue;
            if (!wordIn(evidenceLower, word)) return false;
        }
        return true;
    }

    /** The first sentence of a model reply, or null when there is none. */
    static String firstSentence(String raw) {
        if (raw == null) return null;
        String t = raw.replace("\r", " ").trim();
        int end = -1;
        for (int i = 0; i < t.length(); i++) {
            char c = t.charAt(i);
            if ((c == '.' || c == '!' || c == '?' || c == '\n') && (i + 1 == t.length() || Character.isWhitespace(t.charAt(i + 1)))) {
                end = i + 1;
                break;
            }
        }
        String first = (end < 0 ? t : t.substring(0, end)).trim();
        return first.isEmpty() ? null : first;
    }

    private static String evidenceText(JSONArray evidence) {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < evidence.length(); i++) {
            JSONObject e = evidence.optJSONObject(i);
            if (e == null) continue;
            out.append(e.optString("text", "")).append(' ').append(e.optString("location", "")).append(' ')
                    .append(e.optString("title", "")).append(' ');
        }
        return out.toString();
    }

    private static Set<String> words(String text) {
        Set<String> out = new HashSet<>();
        Matcher m = WORD.matcher(text.toLowerCase(Locale.ROOT));
        while (m.find()) out.add(m.group());
        return out;
    }

    private static boolean wordIn(String haystack, String word) {
        return Pattern.compile("(?<!\\p{L})" + Pattern.quote(word) + "(?!\\p{L})").matcher(haystack).find();
    }

    private static String normal(String s) {
        return s.toLowerCase(Locale.ROOT).replaceAll("\\s+", " ").trim();
    }
}
