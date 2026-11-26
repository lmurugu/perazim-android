package org.perazimchurch.app.ai.local;

import android.content.Context;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.perazimchurch.app.ai.IoCompat;
import org.perazimchurch.app.ai.PerazimAiEngine;
import org.perazimchurch.app.ai.guide.GuideSafety;
import org.perazimchurch.app.ai.library.KjvVerseText;
import org.perazimchurch.app.ai.library.ScriptureRangeTable;

/**
 * Answers an in-scope Christian question from the model's own knowledge when no approved excerpt matches (Levi, 2026-11-25).
 * Only faith, Bible, church, ministry and faith-decision questions that are not sensitive qualify. The answer is accepted only
 * when every Scripture reference exists in the King James Version, every quoted passage is KJV wording, nothing is claimed as an
 * official Perazim position, and no other translation is named. Anything else keeps the honest no-source reply.
 */
final class OwnKnowledge {
    private OwnKnowledge() {}

    /** Prefix on every accepted answer: the reader always knows it is not an approved or official position. */
    static final String LABEL = "General Bible-based guidance from the guide's own knowledge, not an official Perazim position. ";

    private static final Pattern FAITH_CONTEXT = Pattern.compile(
            "\\b(?:bible|biblia|scripture|scriptures|verse|verses|god|lord|jesus|christ|christian|christians|church|kanisa|pastor|"
            + "prayer|pray|faith|gospel|holy spirit|salvation|saved|sin|sins|baptism|baptise|baptize|heaven|hell|doctrine|theology|"
            + "sabbath|worship|ministry|bishop|tithe|tithing|mungu|sala|neno|yesu|mchungaji|purgatory|mary|saint|saints|pope|"
            + "justification|justified|sola|scripture|scriptures|communion|supper|canon|indulgence|indulgences|nicaea|nicene|"
            + "creed|mediator|intercession|grace|sin|confession|confess|eucharist)\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern OTHER_TRANSLATION = Pattern.compile("\\b(?:ESV|NIV|NKJV|NLT)\\b");
    /** Any wording that presents an answer as the church's official, approved or decided position. */
    private static final Pattern OFFICIAL_CLAIM = Pattern.compile(
            "\\bofficial\\b|\\bperazim (?:position|teaching|doctrine|policy|view)\\b|\\b(?:church|perazim) (?:has decided|requires|decrees)\\b"
            + "|\\bapproved (?:position|teaching|doctrine)\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern REFERENCE = Pattern.compile(
            "\\b((?:[1-3] )?[A-Z][a-z]+(?: (?:of )?[A-Z][a-z]+)*)\\s(\\d{1,3}):(\\d{1,3})(?:[-–](\\d{1,3}))?\\b");
    private static final Pattern QUOTED = Pattern.compile("[“\"]([^”\"]{3,400})[”\"]");

    /** True for a faith question that is not sensitive and is not a safety, privacy or out-of-scope request. */
    static boolean allowed(String question) {
        if (question == null || question.trim().isEmpty()) return false;
        if (GuideSafety.isSensitive(question)) return false;
        if (GENERAL_TOPIC.matcher(question).find()) return false;
        // App navigation and pastoral verdicts (judgement, punishment, personal worth) stay with the approved routes and the fixed replies.
        if (APP_OR_VERDICT.matcher(question).find()) return false;
        return FAITH_CONTEXT.matcher(question).find();
    }

    private static final Pattern APP_OR_VERDICT = Pattern.compile(
            "\\b(?:in the app|bible tab|the app|punish\\w*|judg\\w*|angry|my fault|bad christian|good enough|not a good|"
            + "i think god|am i)\\b", Pattern.CASE_INSENSITIVE);
    /** A reply that passes a verdict on a person's circumstances or standing before God. Never accepted. */
    private static final Pattern PERSONAL_VERDICT = Pattern.compile(
            "\\bgod is (?:punishing|judging|angry with|against) (?:you|him|her|them)\\b|\\byou are (?:being )?punished\\b|"
            + "\\byou are (?:a )?(?:bad|not a good|unsaved|not saved)\\b", Pattern.CASE_INSENSITIVE);

    /** General-knowledge subjects that mention the Bible only in passing: declined, never answered from the model's knowledge. */
    private static final Pattern GENERAL_TOPIC = Pattern.compile(
            "\\b(?:scientific|science|theor(?:y|ies)|evolution|causes of|assignment|essay|business plan|mathematics|artificial intelligence|"
            + "football|election|economy|economics|unemployment|latest developments|news|spreadsheet|cv|resume)\\b", Pattern.CASE_INSENSITIVE);

    /** Passages offered to the model for one question (retrieval-first, Levi 2026-11-25). */
    static final int PASSAGE_LIMIT = 6;

    /** Words that describe the question rather than its topic, so they never choose a passage. */
    private static final java.util.Set<String> TOPIC_STOP = new java.util.HashSet<>(java.util.Arrays.asList(
            "what", "does", "bible", "teach", "teaches", "about", "explain", "tell", "please", "should", "would", "could",
            "believe", "christian", "christians", "church", "question", "means", "mean", "with", "from", "this", "that",
            "have", "your", "their", "there", "which", "when", "where", "whether", "they", "them", "were", "been"));

    /**
     * King James Version passages whose words best match the question, best first, each as "label: text". Empty when the
     * question has no topic word or nothing matches. Only verbatim bundled KJV text is returned.
     */
    static java.util.List<String> passagesFor(KjvVerseText kv, String question, java.util.List<Draft> drafts) {
        java.util.List<String> out = new ArrayList<>();
        if (kv == null || question == null) return out;
        // Library drafts for this topic come first: their wording is retrieval material for the model, never shown as approved.
        if (drafts != null) {
            for (Draft d : drafts) {
                if (!d.match.matcher(question).find()) continue;
                out.add("Library draft for review (not an approved position): " + d.view);
                out.addAll(d.lines);
            }
        }
        java.util.List<String> words = new ArrayList<>();
        for (String w : question.toLowerCase(Locale.ROOT).split("[^\\p{L}]+")) {
            if (w.length() >= 4 && !TOPIC_STOP.contains(w) && !words.contains(w)) words.add(w);
        }
        if (words.isEmpty()) return out;
        for (KjvVerseText.Found f : kv.findAllWordsRanked(words, PASSAGE_LIMIT)) {
            out.add(f.label + ": " + f.text);
        }
        return out;
    }

    /** The user turn for a question with no approved excerpt. The system rules are the same ones the grounded answers use. */
    static String prompt(String question) {
        return prompt(question, Collections.<String>emptyList());
    }

    /** As {@link #prompt(String)}, with the retrieved King James Version passages the answer may quote from. */
    static String prompt(String question, java.util.List<String> passages) {
        LocalPrompt.Built base = LocalPrompt.buildBounded(question, Collections.emptyList(), new org.json.JSONArray(), "");
        String system = base.prompt.substring(0, base.prompt.indexOf("<|im_end|>"));
        StringBuilder passageBlock = new StringBuilder();
        if (passages != null && !passages.isEmpty()) {
            passageBlock.append("King James Version passages that may help (quote only these, word for word, and cite only these):\n");
            for (String passage : passages) passageBlock.append(passage).append("\n");
        } else {
            passageBlock.append("No approved excerpt matches this question.\n");
        }
        return system + "<|im_end|>\n<|im_start|>user\n" + passageBlock
                + "Answer from Scripture in two to five plain English sentences. Quote only King James Version wording you are certain of, "
                + "and cite only references you are certain exist. If you are not sure, say so briefly and point to the Bible tab and the pastor.\n"
                + "USER QUESTION:\n" + question.trim() + "<|im_end|>\n<|im_start|>assistant\n";
    }

    /**
     * Checks a model reply. Returns the labelled answer, or null when any rule fails.
     */
    static String accept(String raw, KjvVerseText kv, ScriptureRangeTable table) {
        return accept(raw, kv, table, null);
    }

    /** Words that name no topic: question words and words that appear in nearly every verse. */
    private static final java.util.Set<String> TOPIC_NEUTRAL = new java.util.HashSet<>(java.util.Arrays.asList(
            "does", "did", "what", "why", "how", "that", "this", "with", "from", "for", "are", "was", "were", "will",
            "can", "should", "about", "bible", "scripture", "says", "christian", "christians", "believer", "believers",
            "people", "there", "their", "they", "them", "your", "you", "have", "into", "when", "which", "who", "whom",
            "the", "and", "not", "any", "all", "but", "one", "God", "Lord", "Jesus", "Christ", "holy", "spirit", "something",
            "teach", "teaches", "taught", "answer", "question", "mean", "means", "give", "make", "need", "true"));

    /** True when the reply repeats any run of eight words, the loop the on-device model falls into (2026-11-25). */
    static boolean repeatsItself(String text) {
        String[] w = text.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9' ]", " ").trim().split("\\s+");
        java.util.Set<String> seen = new java.util.HashSet<>();
        for (int i = 0; i + 8 <= w.length; i++) {
            if (!seen.add(String.join(" ", java.util.Arrays.copyOfRange(w, i, i + 8)))) return true;
        }
        return false;
    }

    /**
     * True when at least one cited verse shares a topic word with the question. A verse cited for a question it does not
     * touch is the wrong passage, so the reply is refused (2026-11-25).
     */
    static boolean citesTopicWord(String question, java.util.Collection<String> verseTexts) {
        java.util.Set<String> topic = new java.util.HashSet<>();
        for (String word : question.toLowerCase(Locale.ROOT).replaceAll("[^a-z' ]", " ").split("\\s+")) {
            if (word.length() >= 4 && !TOPIC_NEUTRAL.contains(word)) topic.add(word);
        }
        if (topic.isEmpty()) return true;
        for (String verse : verseTexts) {
            if (verse == null) continue;
            String lower = verse.toLowerCase(Locale.ROOT);
            for (String word : topic) {
                if (lower.matches("(?s).*\\b" + Pattern.quote(word) + "\\w*\\b.*")) return true;
            }
        }
        return false;
    }

    static String accept(String raw, KjvVerseText kv, ScriptureRangeTable table, String question) {
        if (raw == null || kv == null || table == null) return null;
        String text = ModelOutput.stripEndMarkers(raw);
        if (text.isEmpty()) return null;
        if (repeatsItself(text)) return null;
        int words = text.split("\\s+").length;
        if (words < 10 || words > 110) return null;
        if (OTHER_TRANSLATION.matcher(text).find()) return null;
        if (PerazimAiEngine.claimsOfficialStatus(text) || PerazimAiEngine.claimsAppActionOrHumanAuthority(text)) return null;
        if (OFFICIAL_CLAIM.matcher(text).find() || PERSONAL_VERDICT.matcher(text).find()) return null;
        // Any teaching rests on at least two Scripture passages (source rule): fewer than two valid references is not accepted.
        java.util.Set<String> validRefs = new java.util.HashSet<>();
        java.util.List<String> citedVerses = new ArrayList<>();
        Matcher refs = REFERENCE.matcher(text);
        while (refs.find()) {
            int book = table.bookNumber(refs.group(1).trim());
            if (book <= 0) return null;
            int chapter = Integer.parseInt(refs.group(2));
            int first = Integer.parseInt(refs.group(3));
            if (!table.isValid(book, chapter, first)) return null;
            if (refs.group(4) != null) {
                int last = Integer.parseInt(refs.group(4));
                if (!table.isValid(book, chapter, last) || last < first) return null;
            }
            validRefs.add(book + ":" + chapter + ":" + first);
            citedVerses.add(kv.verse(book, chapter, first));
        }
        if (validRefs.size() < 2) return null;
        if (question != null && !citesTopicWord(question, citedVerses)) return null;
        Matcher quoted = QUOTED.matcher(text);
        while (quoted.find()) {
            String passage = quoted.group(1).trim();
            if (passage.split("\\s+").length < 3) continue;
            if (kv.findPhrase(passage, 1).isEmpty()) return null;
        }
        return LABEL + text;
    }

    /** One draft library entry: the view, its KJV passage lines, and the words that choose it. */
    static final class Draft {
        final Pattern match;
        final String view;
        final java.util.List<String> lines;
        Draft(Pattern match, String view, java.util.List<String> lines) {
            this.match = match;
            this.view = view;
            this.lines = lines;
        }
    }

    private static java.util.List<Draft> drafts;

    /** The draft entries in assets/ai/library/doctrine/drafts.json, read once. Empty when the file cannot be read. */
    static synchronized java.util.List<Draft> loadDrafts(Context c) {
        if (drafts != null) return drafts;
        java.util.List<Draft> out = new ArrayList<>();
        try (InputStream in = c.getAssets().open("ai/library/doctrine/drafts.json")) {
            org.json.JSONArray entries = new org.json.JSONObject(new String(IoCompat.readAll(in), StandardCharsets.UTF_8))
                    .getJSONArray("entries");
            for (int i = 0; i < entries.length(); i++) {
                org.json.JSONObject e = entries.getJSONObject(i);
                java.util.List<String> lines = new ArrayList<>();
                org.json.JSONArray verses = e.getJSONArray("verses");
                for (int v = 0; v < verses.length(); v++) {
                    org.json.JSONObject verse = verses.getJSONObject(v);
                    lines.add(verse.getString("ref") + " (King James Version): " + verse.getString("text"));
                }
                out.add(new Draft(Pattern.compile(e.getString("match"), Pattern.CASE_INSENSITIVE), e.getString("view"), lines));
            }
        } catch (Exception e) {
            out = new ArrayList<>();
        }
        drafts = out;
        return drafts;
    }

    /** The accepted own-knowledge answer for this question, or null. Never throws. */
    static String tryAnswer(Context c, String question) {
        if (!allowed(question)) return null;
        try {
            ScriptureRangeTable table = loadTable(c);
            KjvVerseText kv = KjvVerseText.load(c);
            String built = prompt(question, passagesFor(kv, question, loadDrafts(c)));
            String raw;
            if (OnDeviceAi.testRunner != null) {
                raw = OnDeviceAi.testRunner.generate(built, null, 160);
            } else {
                if (!OnDeviceAi.runtimeAvailable(c) || !OnDeviceAi.isInstalled(c) || !OnDeviceAi.isModelVerified(c)) return null;
                raw = OnDeviceAi.runModelProcess(c, built, null, 160);
            }
            return accept(raw, kv, table, question);
        } catch (Exception e) {
            return null;
        }
    }

    /** Prefix on the Scripture-only reply: it quotes verses and says the fuller explanation is still being prepared (2026-11-25). */
    static final String SCRIPTURE_ONLY_LEAD = "I don't have a fully checked explanation of this yet, so I won't guess. "
            + "These are the Scripture passages most relevant to your question (King James Version):\n";
    static final String SCRIPTURE_ONLY_TAIL = "\nA fuller explanation is being prepared. Please talk it through with your pastor in the meantime.";

    /**
     * A Scripture-only reply for a Christian doctrine question the model could not answer: the most relevant KJV passages, at
     * least two, verbatim, and no teaching claim. Null when fewer than two passages are found.
     */
    static String scriptureOnlyReply(Context c, String question) {
        try {
            KjvVerseText kv = KjvVerseText.load(c);
            java.util.List<Draft> draftList = loadDrafts(c);
            java.util.List<String> verses = new ArrayList<>();
            for (String passage : passagesFor(kv, question, draftList)) {
                if (passage.startsWith("Library draft")) continue;
                if (!verses.contains(passage)) verses.add(passage);
                if (verses.size() == 3) break;
            }
            if (verses.size() < 2) return null;
            StringBuilder out = new StringBuilder(SCRIPTURE_ONLY_LEAD);
            for (String verse : verses) out.append(verse).append("\n");
            return out.toString().trim() + SCRIPTURE_ONLY_TAIL;
        } catch (Exception e) {
            return null;
        }
    }

    static ScriptureRangeTable loadTable(Context c) throws Exception {
        try (InputStream in = c.getAssets().open("ai/library/scripture/kjv-verse-counts.json")) {
            return ScriptureRangeTable.fromJson(new String(IoCompat.readAll(in), StandardCharsets.UTF_8));
        }
    }
}
