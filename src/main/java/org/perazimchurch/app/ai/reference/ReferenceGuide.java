package org.perazimchurch.app.ai.reference;

import android.content.Context;
import android.util.Log;

import org.json.JSONArray;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Adds approved reference evidence to a question's evidence array. Called once per question, after the governed
 * package and scripture evidence has been selected. With no approved sources, or no bundled database, the input
 * array is returned unchanged.
 */
public final class ReferenceGuide {

    /** Largest excerpt of Henry's text returned in an answer. Excerpts are cut at a sentence end. */
    static final int MAX_HENRY_EXCERPT = 700;

    /**
     * Matthew Henry's commentary for a chapter the member names with his name (Tier 3, public domain). The verse's
     * Scripture comes first, from the bundled KJV, then a short excerpt labelled as Henry's own view. Returns null when
     * the question does not name Henry, names no passage, or Henry is not approved or not in the database.
     */
    public static String namedHenry(Context context, String question, org.perazimchurch.app.ai.library.KjvVerseText kjv) {
        try {
            if (context == null || question == null || kjv == null) return null;
            String q = question.toLowerCase(Locale.ROOT);
            if (!q.contains("matthew henry") && !q.contains("m. henry")) return null;
            int key = ReferenceKeys.parseReference(question);
            if (key <= 0) return null;
            ReferenceApprovals approvals = approvals(context);
            if (approvals.isEmpty() || !approvals.isApproved(ReferenceIndex.HENRY_SOURCE)) return null;
            int book = key / 1_000_000;
            int chapter = (key / 1000) % 1000;
            int verse = Math.max(1, key % 1000);
            String scripture = kjv.verse(book, chapter, verse);
            if (scripture == null || scripture.isEmpty()) return null;
            ReferenceIndex index = ReferenceIndex.open(context);
            if (index == null) return null;
            try {
                String headword = ReferenceKeys.chapterHeadword(key);
                ReferenceHit henry = null;
                for (ReferenceHit hit : index.commentaries(headword, approvals, 60)) {
                    if (ReferenceIndex.HENRY_SOURCE.equals(hit.sourceId)) {
                        henry = hit;
                        break;
                    }
                }
                if (henry == null || henry.text == null || henry.text.trim().isEmpty()) return null;
                return "Matthew Henry on " + headword + ", public domain (CCEL). It is his own view, not the Perazim "
                        + "position, and Scripture is the test.\n\n"
                        + "Scripture:\n" + headword + ":" + verse + ": “" + scripture + "”\n\n"
                        + "Henry on this chapter: " + excerpt(henry.text);
            } finally {
                index.close();
            }
        } catch (Exception e) {
            Log.w(TAG, "Henry excerpt skipped", e);
            return null;
        }
    }

    /** The start of a commentary body, cut at a sentence end where one is near the limit. */
    static String excerpt(String body) {
        String t = body.replaceAll("\\s+", " ").trim();
        if (t.length() <= MAX_HENRY_EXCERPT) return t;
        String cut = t.substring(0, MAX_HENRY_EXCERPT);
        int stop = cut.lastIndexOf(". ");
        if (stop > MAX_HENRY_EXCERPT / 2) cut = cut.substring(0, stop + 1);
        return cut.trim() + " …";
    }

    private static final String TAG = "ReferenceGuide";
    static final int MAX_REFERENCE_ITEMS = 2;
    /** One commentary excerpt at most. It is added last, after the governed position and the factual references. */
    static final int MAX_COMMENTARY_ITEMS = 1;
    private static final int MAX_WORDS = 4;
    /** Headword under which the approved Nicene-Constantinopolitan Creed is stored, in chunks. */
    static final String CREED_HEADWORD = "Nicene";
    /** All chunks of the creed are returned when it is asked for. The creed is three chunks of at most 560 characters. */
    static final int MAX_CREED_PARTS = 4;

    private static final Set<String> STOP = new HashSet<>(Arrays.asList(
            "about", "after", "again", "against", "also", "because", "before", "being", "between", "could",
            "does", "doing", "found", "from", "have", "into", "just", "like", "meaning", "mean", "more", "much",
            "must", "only", "other", "over", "please", "said", "same", "should", "show", "some", "such",
            "tell", "than", "that", "their", "them", "then", "there", "these", "they", "this", "those",
            "through", "under", "until", "very", "were", "what", "when", "where", "which", "while", "with",
            "would", "your", "bible", "verse", "verses", "scripture", "passage", "explain", "define", "word",
            "words", "chapter", "read", "give", "help", "know", "does", "want"));

    private ReferenceGuide() {}

    public static JSONArray augment(Context context, JSONArray evidence, String question) {
        try {
            ReferenceApprovals approvals = approvals(context);
            if (approvals.isEmpty() || question == null || question.trim().isEmpty()) return evidence;
            ReferenceIndex index = ReferenceIndex.open(context);
            if (index == null) return evidence;
            try {
                List<ReferenceHit> hits = new ArrayList<>();
                int verse = ReferenceKeys.parseReference(question);
                if (verse > 0) hits.addAll(index.crossReferences(verse, approvals, 2));
                for (String word : properNames(question)) {
                    if (hits.size() >= MAX_REFERENCE_ITEMS) break;
                    if (CREED_HEADWORD.equals(word)) continue;
                    hits.addAll(index.dictionary(word, approvals, 1));
                }
                JSONArray withReferences = ReferenceEvidence.append(evidence, hits, MAX_REFERENCE_ITEMS);
                if (asksForCreed(question)) {
                    withReferences = ReferenceEvidence.append(withReferences,
                            index.parts(CREED_HEADWORD, approvals, MAX_CREED_PARTS), MAX_CREED_PARTS);
                }
                if (verse <= 0) return withReferences;
                List<ReferenceHit> commentary = index.commentaries(ReferenceKeys.chapterHeadword(verse), approvals,
                        MAX_COMMENTARY_ITEMS);
                return ReferenceEvidence.append(withReferences, commentary, MAX_COMMENTARY_ITEMS);
            } finally {
                index.close();
            }
        } catch (Exception e) {
            Log.w(TAG, "Reference evidence skipped", e);
            return evidence;
        }
    }

    /** True when the question names the Nicene Creed. Its chunks are then returned in full, in order. */
    static boolean asksForCreed(String question) {
        return words(question).contains(CREED_HEADWORD);
    }

    static List<String> words(String question) {
        List<String> out = new ArrayList<>();
        for (String raw : question.split("[^\\p{L}]+")) {
            String word = raw.toLowerCase(Locale.ROOT);
            if (word.length() < 4 || STOP.contains(word)) continue;
            if (!out.contains(word)) out.add(capitalise(word));
            if (out.size() >= MAX_WORDS) break;
        }
        return out;
    }

    /**
     * Capitalised names in the question, for dictionary lookup. Only a word the user capitalised counts, so "so",
     * "on" and "no" in ordinary sentences never match a dictionary name. Question words and short words are skipped.
     */
    static List<String> properNames(String question) {
        List<String> out = new ArrayList<>();
        for (String raw : question.split("[^\\p{L}]+")) {
            if (raw.length() < 4 || !Character.isUpperCase(raw.charAt(0))) continue;
            String word = capitalise(raw.toLowerCase(Locale.ROOT));
            if (STOP.contains(word.toLowerCase(Locale.ROOT)) || out.contains(word)) continue;
            out.add(word);
            if (out.size() >= MAX_WORDS) break;
        }
        return out;
    }

    private static String capitalise(String word) {
        return word.substring(0, 1).toUpperCase(Locale.ROOT) + word.substring(1);
    }

    private static ReferenceApprovals approvals(Context context) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                context.getAssets().open(ReferenceApprovals.ASSET), StandardCharsets.UTF_8))) {
            StringBuilder json = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) json.append(line).append('\n');
            return ReferenceApprovals.parse(json.toString());
        } catch (Exception missing) {
            return ReferenceApprovals.none();
        }
    }
}
