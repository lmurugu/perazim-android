package org.perazimchurch.app.ai.local;

import android.content.Context;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.perazimchurch.app.ai.PerazimAiEngine;
import org.perazimchurch.app.ai.SoftSafety;
import org.perazimchurch.app.ai.library.ApprovedRoutes;
import org.perazimchurch.app.ai.library.DoctrineLibrary;
import org.perazimchurch.app.ai.library.KjvVerseText;
import org.perazimchurch.app.ai.library.NoGoZones;
import org.perazimchurch.app.ai.library.ScriptureRangeTable;
import org.perazimchurch.app.data.interlinear.StrongsStore;
import org.perazimchurch.app.domain.model.StrongsEntry;
import java.util.List;
import java.util.Locale;

/**
 * The one ordered answer path that runs before any model call. The app and the evaluation harness both use this, so
 * they cannot drift apart. Order: crisis, missing Bible reference, the pre-model route (overdose, harm to others,
 * safeguarding, sensitive, human decision, privacy and scope), approved doctrine, verbatim verse, no-go zone.
 * A null decision means the question may go on to the model, which still needs verified evidence.
 */
public final class PreModel {

    /** Where the answer came from. The status is what the evaluation reports. */
    public static final class Decision {
        public final String status;
        public final String answer;

        Decision(String status, String answer) {
            this.status = status;
            this.answer = answer;
        }
    }

    private static final Pattern BOOK_CHAPTER_VERSE = Pattern.compile("^(.+) (\\d+):(\\d+)$");
    private static final Pattern RANGE_END = Pattern.compile("\\d{1,3}\\s*[:.]\\s*\\d{1,3}\\s*[-–]\\s*(\\d{1,3})");
    static final int MAX_VERSES = 10;
    /** Translations the app does not quote (licensing decision: KJV and NASB only, NASB from the user's own file). */
    private static final Pattern OTHER_TRANSLATION = Pattern.compile(
            "\\b(?:ESV|NIV|NKJV|NLT|NRSV|CSB|NET|RSV|Message|Amplified|exact (?:wording|words) (?:of|in) the (?:ESV|NIV|NKJV|NLT))\\b",
            Pattern.CASE_INSENSITIVE);

    private PreModel() {}

    /** The decision for a question, or null when it may go to the model. */
    public static Decision decide(Context c, String question) {
        // Two-step check for vague exhaustion. Judged once per message; a repeat of the same message gives the same verdict.
        SoftSafety.Verdict safety = SoftSafety.judge(question);
        if (safety == SoftSafety.Verdict.CRISIS) {
            return new Decision("routed:crisis", PerazimAiEngine.crisisResponse().getMessageText());
        }
        if (safety == SoftSafety.Verdict.SOFT_CHECK) {
            return new Decision("routed:soft-safety-check", SoftSafety.checkText());
        }
        if (PerazimAiEngine.isCrisisQuery(question)) {
            return new Decision("routed:crisis", PerazimAiEngine.crisisResponse().getMessageText());
        }
        ScriptureRangeTable table = null;
        try {
            table = ScriptureRangeTable.fromJson(read(c, "ai/library/scripture/kjv-verse-counts.json"));
        } catch (Exception ignored) {
            table = null;
        }
        // A reference that is not in the King James Version is refused first, as production always has.
        if (table != null) {
            Decision missing = missingReference(table, question);
            if (missing != null) return missing;
        }
        // Strong's numbers and Easton's dictionary: answered from the bundled, approved texts, never from memory.
        Decision lexicon = lexiconDecision(c, question);
        if (lexicon != null) return lexicon;
        // Whether the member finished their routine: the guide cannot see their record from here.
        if (ROUTINE_STATUS.matcher(question).find()) {
            return new Decision("routed:app-status-unavailable", ROUTINE_STATUS_TEXT);
        }
        String route = PerazimAiEngine.preModelRoute(question);
        if (route != null) {
            return new Decision("routed:" + route, PerazimAiEngine.preModelResponse(route, question).getMessageText());
        }
        // Matthew Henry, only when a member asks for him by name: Scripture first, then his labelled excerpt (Tier 3).
        String henry = org.perazimchurch.app.ai.reference.ReferenceGuide.namedHenry(c, question, KjvVerseText.load(c));
        if (henry != null) return new Decision("routed:commentary-henry", henry);
        // John Wesley, only when a member asks for him by name and gives a chapter: Scripture first, then his labelled note.
        String wesley = org.perazimchurch.app.ai.reference.ReferenceGuide.namedWesley(c, question, KjvVerseText.load(c));
        if (wesley != null) return new Decision("routed:commentary-wesley", wesley);
        // Approved routes written from Levi's decisions of 2026-11-25 (Israel, cessationism, apostasy, election, grace, sovereignty).
        // Coercion, threats, abuse or female cutting in these topics go to the safeguarding response, never to a teaching answer.
        if (SAFEGUARD_TOPIC.matcher(question).find() && SAFEGUARD_COERCION.matcher(question).find()) {
            return new Decision("routed:safeguarding", PerazimAiEngine.preModelResponse("safeguarding", question).getMessageText());
        }
        ApprovedRoutes.Answer approved = ApprovedRoutes.answerFor(question, KjvVerseText.load(c));
        if (approved != null) return new Decision("routed:approved-" + approved.routeId, approved.text);
        String doctrine = DoctrineLibrary.load(c).answerFor(question);
        if (doctrine != null) return new Decision("routed:doctrine-approved", doctrine);
        if (org.perazimchurch.app.ai.library.IdentifyRefute.ENABLED) {
            String refute = identifyRefute(c, question);
            if (refute != null) return new Decision("routed:identify-refute", refute);
        }
        KjvVerseText verses = KjvVerseText.load(c);
        if (table != null) {
            Decision verse = verseDecision(table, verses, question);
            if (verse != null) return verse;
        }
        Decision search = verseSearchDecision(verses, question);
        if (search != null) return search;
        // A sermon written for a member, and a claim to act as a church officer: outside the guide's role, declined before the zones.
        if (SERMON_REQUEST.matcher(question).find()) return new Decision("routed:role-sermon", ROLE_SERMON_TEXT);
        if (ACT_AS_OFFICER.matcher(question).find()) return new Decision("routed:role-officer", ROLE_OFFICER_TEXT);
        // Two named Christian traditions compared as a whole: fair description and no ranking, labelled as views.
        if (comparesTraditions(question)) {
            return new Decision("routed:denominational-fair", DENOMINATIONAL_FAIR_TEXT);
        }
        // An ESV, NIV or NKJV request without a verse number gets the translation policy, not a no-go decline.
        if (OTHER_TRANSLATION.matcher(question).find()) {
            return new Decision("routed:translation-policy", PerazimAiEngine.translationPolicyResponse().getMessageText());
        }
        NoGoZones.Zone zone = NoGoZones.match(question);
        if (zone != null) return new Decision("routed:no-go:" + zone.id, zone.decline);
        // "That verse" with no book, chapter and verse in the question: ask which one is meant rather than guess.
        // This runs after the zones, so a zone decline always keeps its priority.
        if (VERSE_POINTER.matcher(question).find() && (table == null || reference(table, question) == null)) {
            return new Decision("routed:verse-clarify", "Which verse do you mean? Please give the book, chapter and verse, "
                    + "and I will quote it from the King James Version (KJV).");
        }
        return null;
    }

    /** A Strong's number named with its language ("Greek word G26"), or a question about Easton's dictionary. Null otherwise. */
    private static final Pattern STRONGS_NAMED = Pattern.compile(
            "\\b(?:greek|hebrew)\\s+(?:word\\s+)?([GH]\\d{1,4})\\b|\\b([GH]\\d{1,4})\\b\\s+(?:in\\s+)?(?:greek|hebrew|strong)",
            Pattern.CASE_INSENSITIVE);
    /** A member asking whether they finished their routine or streak. The guide cannot see that record from here. */
    private static final Pattern ROUTINE_STATUS = Pattern.compile(
            "\\b(?:did|have) i\\b.{0,40}\\b(?:finish|finished|complete|completed|done|do)\\b.{0,40}\\b(?:routine|routines|streak)\\b",
            Pattern.CASE_INSENSITIVE);
    private static final String ROUTINE_STATUS_TEXT = "I can't see your routine status from here, so I won't say whether you "
            + "finished it today. Open Home to check today's routine.";

    /** The bundled Strong's entry for a number named in the question, or Easton's entry for a named word, or null. */
    static Decision lexiconDecision(Context c, String question) {
        Matcher m = STRONGS_NAMED.matcher(question);
        if (m.find()) {
            String code = (m.group(1) != null ? m.group(1) : m.group(2)).toUpperCase(Locale.ROOT);
            StrongsEntry entry = StrongsStore.getEntry(c, code);
            if (entry == null) {
                return new Decision("routed:strongs-not-found", "I can't find " + code + " in the Strong's table bundled with the app, "
                        + "so I won't define it from memory. Please check the number in the Bible tab.");
            }
            String original = nz(entry.getOriginal()).isEmpty() ? "" : " (" + entry.getOriginal().trim() + ")";
            return new Decision("routed:strongs-lexicon", "Strong's " + code + original + ": " + nz(entry.getTransliteration())
                    + ".\n\nStrong's definition: " + nz(entry.getDefinition())
                    + "\n\nFrom Strong's Exhaustive Concordance (public domain), bundled with the app.");
        }
        String easton = org.perazimchurch.app.ai.reference.ReferenceGuide.namedEaston(c, question);
        return easton == null ? null : new Decision("routed:dictionary-easton", easton);
    }

    /** Topics whose approved answers must never be given to someone describing coercion or abuse. */
    private static final Pattern SAFEGUARD_TOPIC = Pattern.compile(
            "widow|inherit|levirat|circumci|ruracio|roora|mahari|lobola|dowry|bride price|funeral", Pattern.CASE_INSENSITIVE);
    /** Coercion, threats, abuse, or female cutting wording within those topics. */
    private static final Pattern SAFEGUARD_COERCION = Pattern.compile(
            "\\bforc\\w*|threat\\w*|beat\\w*|rape\\w*|abus\\w*|sexual\\w*|kill\\w*|harm\\w*|hurt\\w*|\\bfgm\\b|female|girls?|genital|mutilat\\w*",
            Pattern.CASE_INSENSITIVE);

    /** A full sermon requested (outlines for members are not covered here). */
    private static final Pattern SERMON_REQUEST = Pattern.compile(
            "\\b(?:write|draft|prepare|give me|make|compose|create)\\b(?!.{0,20}outline).{0,30}\\b(?:sermon|seromn|homily)\\b", Pattern.CASE_INSENSITIVE);
    /** A claim to act as a church officer or to confirm a payment. */
    private static final Pattern ACT_AS_OFFICER = Pattern.compile(
            "\\b(?:act|acting|pretend|be) (?:as|like) (?:the |our |a |an )?(?:church )?(?:treasurer|pastor|bishop|elder|secretary|chairman|church office)\\b|confirm (?:that )?my (?:tithe|offering|payment|giving|seed)", Pattern.CASE_INSENSITIVE);
    private static final String ROLE_SERMON_TEXT = "I am designed specifically to assist with Christian faith and the Bible, so I am not authorised to write a sermon for you. "
            + "I can help you study a passage in its context, and your pastor can help you prepare a sermon.";
    private static final String ROLE_OFFICER_TEXT = "I am designed specifically to assist with Christian faith and the Bible, so I am not authorised to act as a church officer or to confirm any payment. "
            + "Please ask the church office or the treasurer directly.";

    /** A pointer to a verse given earlier ("that verse"), which the single-question path cannot resolve. */
    private static final Pattern VERSE_POINTER = Pattern.compile(
            "\\b(?:that|this|the|previous|last) (?:verse|passage)\\b", Pattern.CASE_INSENSITIVE);
    /** Christian traditions a member may name together, for example in "Are Pentecostals less biblical than Presbyterians?". */
    private static final Pattern TRADITION_NAME = Pattern.compile(
            "\\b(pentecostal|presbyterian|baptist|methodist|anglican|lutheran|adventist|assemblies of god|reformed)s?\\b",
            Pattern.CASE_INSENSITIVE);
    /** Groups outside the Protestant, Pentecostal and Evangelical families. They are never put on equal footing with them here. */
    private static final Pattern NON_PROTESTANT = Pattern.compile(
            "\\b(catholic|catholics|roman|orthodox|mormon|mormons|latter-day|lds|jehovah|witness|witnesses|oneness|" +
            "christian science|unitarian|islam|muslim|hindu|buddhist|sikh|jew|jews)\\b", Pattern.CASE_INSENSITIVE);
    /** Comparative or ranking wording (including common misspellings such as "bibilcal"). */
    private static final Pattern COMPARATIVE_WORD = Pattern.compile(
            "\\b(?:less|more|better|worse|bib\\w*|true|real|right|wrong|correct|saved|than|versus|vs\\.?|or)\\b",
            Pattern.CASE_INSENSITIVE);
    private static final String DENOMINATIONAL_FAIR_TEXT = "The traditions you name both hold the Bible as God's word, and they "
            + "differ on some teaching and practice. I won't rank one tradition as less biblical than another. Each point of "
            + "difference can be tested by Scripture, and the app's approved answers give the Perazim position where the church "
            + "has one. Please talk through the points that matter to you with your pastor or a trusted church leader.";

    /** True when a question names two different Christian traditions and asks a comparative or ranking question about them. */
    static boolean comparesTraditions(String question) {
        Matcher m = TRADITION_NAME.matcher(question);
        java.util.Set<String> names = new java.util.HashSet<>();
        while (m.find()) names.add(m.group(1).toLowerCase(Locale.ROOT));
        if (NON_PROTESTANT.matcher(question).find()) return false;
        return names.size() >= 2 && COMPARATIVE_WORD.matcher(question).find();
    }

    private static String nz(String s) {
        return s == null ? "" : s.trim();
    }

    /** Identify-and-refute answer for an approved false-doctrine topic, or null. Used only while the gate is on. */
    static String identifyRefute(Context c, String question) {
        try {
            String id = org.perazimchurch.app.ai.library.IdentifyRefute.entryIdFor(question);
            if (id == null) return null;
            org.json.JSONObject root = new org.json.JSONObject(read(c, "ai/guide/comparative/comparative-kb.json"));
            org.json.JSONArray entries = root.getJSONArray("entries");
            for (int i = 0; i < entries.length(); i++) {
                if (id.equals(entries.getJSONObject(i).optString("id"))) {
                    return org.perazimchurch.app.ai.library.IdentifyRefute.answerFor(question, entries.getJSONObject(i),
                            KjvVerseText.load(c));
                }
            }
        } catch (Exception ignored) {
            return null;
        }
        return null;
    }

    /** The refusal for a confidently named reference that does not exist in the KJV, or null. */
    static Decision missingReference(ScriptureRangeTable table, String question) {
        Reference ref = reference(table, question);
        if (ref == null || ref.valid) return null;
        String book = ref.label.replaceFirst("\\s+\\d+:\\d+(?:-\\d+)?$", "");
        String text;
        if (ref.chapter > table.chapterCount(ref.bookNumber)) {
            text = book + " has " + table.chapterCount(ref.bookNumber) + " chapters, so there is no chapter " + ref.chapter
                    + ". Chapter " + ref.chapter + " does not exist in " + book + ", and it is not in the King James Version, so I cannot quote it.";
        } else if (ref.first > table.verseCount(ref.bookNumber, ref.chapter) || ref.last > table.verseCount(ref.bookNumber, ref.chapter)) {
            text = book + " " + ref.chapter + " has " + table.verseCount(ref.bookNumber, ref.chapter) + " verses, so there is no verse "
                    + (ref.first > table.verseCount(ref.bookNumber, ref.chapter) ? ref.first : ref.last)
                    + ". That verse does not exist in " + book + " " + ref.chapter + ", so I cannot quote it.";
        } else {
            text = "I can't find " + ref.label + " in the King James Version, so I won't quote it.";
        }
        return new Decision("routed:missing-reference", text + " Please check the reference, or ask a church leader.");
    }

    /**
     * The verbatim King James Version text for a valid reference that the bundled file holds, or a plain statement
     * when the file does not hold it. Returns null when the question names no confident, valid reference.
     */
    private static final Pattern MEANING_ASK = Pattern.compile(
            "\\bmean(?:s|ing)?\\b|\\bexplain|\\bcontext\\b|\\bpromis|\\bguarantee|\\bcontradict|\\bteach", Pattern.CASE_INSENSITIVE);

    static Decision verseDecision(ScriptureRangeTable table, KjvVerseText verses, String question) {
        Reference ref = reference(table, question);
        if (ref == null || !ref.valid) return null;
        if (ref.last - ref.first + 1 > MAX_VERSES) {
            return new Decision("routed:verse-range-too-long", "I can quote up to " + MAX_VERSES
                    + " verses at a time. Please ask for a shorter passage, and read the rest in the Bible tab.");
        }
        StringBuilder out = new StringBuilder();
        for (int v = ref.first; v <= ref.last; v++) {
            String text = verses == null ? null : verses.verse(ref.bookNumber, ref.chapter, v);
            if (text == null) {
                return new Decision("routed:verse-not-in-offline-text", "I can't quote " + ref.label
                        + " from memory, and this offline text does not hold that verse yet. Please read it in the Bible tab, "
                        + "which has the King James Version (KJV).");
            }
            if (out.length() > 0) out.append(' ');
            out.append('\u201c').append(text).append('\u201d');
        }
        String notice = OTHER_TRANSLATION.matcher(question).find()
                ? "I can't quote that translation here. The app holds the King James Version, so here it is in the KJV. "
                : "";
        // A request for meaning gets the verse and an honest note: the guide only explains what checked material supports.
        String meaning = MEANING_ASK.matcher(question).find()
                ? "\n\nI can quote the verse, but I can't give its meaning from checked material yet. Read the verses before and "
                + "after it in the Bible tab, and talk it through with your pastor or a trusted church leader."
                : "";
        return new Decision("routed:verse-kjv", notice + "From the King James Version (KJV), " + ref.label + ":\n\n" + out
                + "\n\nRead it in its context. The Bible tab shows the whole chapter." + meaning);
    }

    /** A quoted phrase of at least three words, the phrase after "the Bible says", or null. */
    // An opening quote is not inside a word (so "I've" and "can't" do not open one), and a closing quote is not followed by a letter.
    private static final Pattern QUOTED_PHRASE = Pattern.compile(
            "(?<!\\p{L})[\"'\u2018\u201c]([^\"'\u2019\u201d]{8,160}?)[\"'\u2019\u201d](?!\\p{L})");
    /**
     * Topics with verses the King James Version gives for them, read verbatim from the bundled text. Each topic is a
     * plain-language question shape from the Kenyan sets. Undecided and under-review topics never reach this list:
     * the approved doctrine route runs first.
     */
    static final Pattern[] TOPIC_PATTERNS = {
        Pattern.compile("love of money|money is the (?:root|source)|root of all evil|wealth|riches|\\brich\\b|silver", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\bdebt|\\bloans?\\b|\\bborrow|\\bowe\\b", Pattern.CASE_INSENSITIVE),
        Pattern.compile("witch|curse|cursed|\\bcharms?\\b|enchant|divination|sorcer|juju|magic|amulet|talisman", Pattern.CASE_INSENSITIVE),
        Pattern.compile("marri|husband|\\bwife\\b", Pattern.CASE_INSENSITIVE),
        Pattern.compile("healing|healed|\\bheal\\b|\\bsick|sickness", Pattern.CASE_INSENSITIVE),
        Pattern.compile("forgiv|forgave|revenge|bitterness|unforgiv", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\bafraid\\b|\\bscared\\b|\\bfear\\b|\\bdismayed\\b", Pattern.CASE_INSENSITIVE),
        Pattern.compile("cheerful giv|generous giv|offering to (?:the )?(?:church|god)|giving to (?:the )?church", Pattern.CASE_INSENSITIVE),
        Pattern.compile("pharaoh.*heart|heart of pharaoh", Pattern.CASE_INSENSITIVE),
        // Feeling that God is far off: the KJV promises of his presence (Psalm 34:18, Hebrews 13:5).
        Pattern.compile("\\bgod is (?:near|far|absent|silent|not here)\\b|\\bnear me\\b|feel (?:god|his presence|god's presence)|far from god|\\bforsak\\w*|\\babandon\\w*", Pattern.CASE_INSENSITIVE),
        // Afterlife and justification: KJV passages on the topic (Scripture is the source; no other teaching is added).
        Pattern.compile("afterlife|after death|life after death|hereafter", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\bjustif\\w*", Pattern.CASE_INSENSITIVE),
    };
    private static final Pattern TOPIC_EXCLUDE = Pattern.compile(
            "\\bhiv\\b|\\barvs?\\b|antiretro|condom|contracept|unforgivable|blasphem|atonement|divorc|remarr|medic|doctor|hospital|tablets?|\\bpills?\\b|\\bdrugs?\\b|clinic|\\bnurse|"
            + "stop taking|prayer cloth|\\btest(?:ed|s)?\\b|accus|bewitch|\\bmy (?:son|daughter|child|mother|father|aunt|uncle|"
            + "family|husband|wife|mother-in-law|grandmother|grandfather|cousin|brother|sister)\\b|\\bverse that proves|pay for|\\bpay\\b",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern TOPIC_REF = Pattern.compile("^(.+) (\\d+):(\\d+)$");
    static final String[][] TOPIC_REFS = {
        {"1 Timothy 6:10", "Ecclesiastes 5:10", "Hebrews 13:5"},
        {"Proverbs 22:7", "Psalms 37:21", "Romans 13:8"},
        {"Deuteronomy 18:10", "Proverbs 26:2", "1 John 4:4"},
        {"Genesis 2:24", "Hebrews 13:4", "Ephesians 5:25"},
        {"James 5:14", "James 5:15", "Exodus 15:26"},
        {"Matthew 6:14", "Matthew 6:15", "Ephesians 4:32"},
        {"Isaiah 41:10", "2 Timothy 1:7", "Philippians 4:6"},
        {"2 Corinthians 9:7", "Acts 20:35"},
        {"Exodus 9:12", "Romans 9:18"},
        {"Psalms 34:18", "Hebrews 13:5"},
        {"John 11:25", "2 Corinthians 5:8", "Hebrews 9:27"},
        {"Romans 3:24", "Romans 5:1", "Galatians 2:16"},
    };

    /** The phrase a question asks about: a quoted phrase. Null when there is none. */
    static String searchedPhrase(String question) {
        if (question == null) return null;
        Matcher quoted = QUOTED_PHRASE.matcher(question);
        // A single quoted word (for example a concept such as 'afterlife') is not a phrase to search for.
        if (quoted.find() && quoted.group(1).trim().contains(" ")) return quoted.group(1).trim();
        // Only a quoted wording is searched. "The Bible says ..." without quotation marks is too often a question
        // about a topic, not a phrase, so it is left to the topic route and the zones.
        return null;
    }

    /**
     * Verbatim KJV for a phrase the user quotes, and for the few topics with a fixed verse.
     * A phrase that is not in the KJV is said so plainly, never quoted from memory. Returns null when neither applies.
     */
    static Decision verseSearchDecision(KjvVerseText verses, String question) {
        if (verses == null || question == null) return null;
        String phrase = searchedPhrase(question);
        if (phrase != null) {
            List<KjvVerseText.Found> found = verses.findPhrase(phrase, 3);
            if (!found.isEmpty()) {
                StringBuilder out = new StringBuilder("From the King James Version (KJV):\n");
                for (KjvVerseText.Found f : found) out.append('\n').append(f.label).append(": \u201c").append(f.text).append('\u201d');
                return new Decision("routed:verse-phrase-kjv", out + "\n\nRead it in its context. The Bible tab shows the whole chapter.");
            }
        }
        String topic = topicVerses(verses, question);
        if (topic != null) {
            String prefix = phrase == null ? "" : "That exact wording is not in the Bible, so it is not Scripture as written. ";
            return new Decision(phrase == null ? "routed:verse-topic-kjv" : "routed:verse-topic-kjv-corrected",
                    prefix + "The King James Version speaks to this in:\n\n" + topic
                    + "\n\nRead it in its context. The Bible tab shows the whole chapter.");
        }
        if (phrase != null) {
            return new Decision("routed:verse-phrase-not-found", "That wording is not in the King James Version, and I cannot find it "
                    + "in the Bible as written, so I won't quote it. It does not appear as Scripture, and it is not found in the Bible. "
                    + "Please check the wording, or ask a church leader.");
        }
        return null;
    }

    private static String topicVerses(KjvVerseText verses, String question) {
        String normalized = question.toLowerCase(Locale.ROOT).replace('\u2019', '\'');
        // Medical, accusation and personal-affliction messages are not answered with a verse. They go to the zones, which
        // keep the pointer to a doctor, a church leader or a pastor.
        if (TOPIC_EXCLUDE.matcher(normalized).find()) return null;
        for (int t = 0; t < TOPIC_PATTERNS.length; t++) {
            if (!TOPIC_PATTERNS[t].matcher(normalized).find()) continue;
            StringBuilder out = new StringBuilder();
            for (String ref : TOPIC_REFS[t]) {
                Matcher m = TOPIC_REF.matcher(ref);
                if (!m.find()) continue;
                int book = verses.bookNumber(m.group(1));
                int chapter = Integer.parseInt(m.group(2));
                int verse = Integer.parseInt(m.group(3));
                String text = verses.verse(book, chapter, verse);
                if (text == null) continue;
                if (out.length() > 0) out.append("\n\n");
                out.append(ref).append(": \u201c").append(text).append('\u201d');
            }
            if (out.length() > 0) return out.toString();
        }
        return null;
    }

    /** A parsed reference: book, chapter, first and last verse, and whether the KJV has it. */
    static final class Reference {
        final int bookNumber;
        final int chapter;
        final int first;
        final int last;
        final boolean valid;
        final String label;

        Reference(int bookNumber, int chapter, int first, int last, boolean valid, String label) {
            this.bookNumber = bookNumber;
            this.chapter = chapter;
            this.first = first;
            this.last = last;
            this.valid = valid;
            this.label = label;
        }
    }

    /** Parses a confidently matched reference, or returns null when there is none. */
    static Reference reference(ScriptureRangeTable table, String question) {
        ScriptureReferenceNormalizer.Result ref = ScriptureReferenceNormalizer.normalize(question);
        if (ref == null || !ref.confident || ref.normalized == null) return null;
        Matcher m = BOOK_CHAPTER_VERSE.matcher(ref.normalized);
        if (!m.find()) return null;
        String book = m.group(1);
        int chapter = Integer.parseInt(m.group(2));
        int first = Integer.parseInt(m.group(3));
        int last = first;
        Matcher range = RANGE_END.matcher(question);
        if (range.find()) last = Integer.parseInt(range.group(1));
        int bookNumber = table.bookNumber(book);
        if (bookNumber <= 0) return null;
        String label = last == first ? ref.normalized : book + " " + chapter + ":" + first + "-" + last;
        boolean valid = last >= first && table.isValid(bookNumber, chapter, first) && table.isValid(bookNumber, chapter, last);
        return new Reference(bookNumber, chapter, first, last, valid, label);
    }

    private static String read(Context c, String path) throws java.io.IOException {
        try (java.io.InputStream in = c.getAssets().open(path)) {
            return new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        }
    }
}
