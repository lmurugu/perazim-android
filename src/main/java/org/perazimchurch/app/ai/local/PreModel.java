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
import java.util.ArrayList;
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
        /** The full reply with its buttons, when the route has one. Null means a plain-text answer. */
        public final org.perazimchurch.app.ai.AiResponse response;

        Decision(String status, String answer) {
            this(status, answer, null);
        }

        Decision(String status, String answer, org.perazimchurch.app.ai.AiResponse response) {
            this.status = status;
            this.answer = answer;
            this.response = response;
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
        // Common misspellings of doctrine keywords are fixed here, on the path the phone and the evaluation both use (round 48).
        question = GuideTurnRouter.correctDoctrineSpelling(question);
        // Two-step check for vague exhaustion. Judged once per message; a repeat of the same message gives the same verdict.
        SoftSafety.Verdict safety = SoftSafety.judge(question);
        if (safety == SoftSafety.Verdict.CRISIS && PerazimAiEngine.safetyRoute(question) == null) {
            return new Decision("routed:crisis", PerazimAiEngine.crisisResponse().getMessageText());
        }
        if (safety == SoftSafety.Verdict.SOFT_CHECK) {
            return new Decision("routed:soft-safety-check", SoftSafety.checkText());
        }
        if (PerazimAiEngine.isCrisisQuery(question) && PerazimAiEngine.safetyRoute(question) == null) {
            return new Decision("routed:crisis", PerazimAiEngine.crisisResponse().getMessageText());
        }
        // Greetings, thanks, goodbyes, capability and complaint replies, and the first line of a marriage conversation.
        // These come after the safety checks above, so they never hide a crisis or abuse disclosure.
        ConversationalTurn.Reply chat = ConversationalTurn.reply(question);
        if (chat != null) return new Decision(chat.status, chat.text);
        // Pastoral replies (prayer, far from God, job loss, a child or a voice-hearing worry) run before the scope gate, but
        // never over a safety route. Legal, medical and money matters are referred on inside the reply itself.
        if (PerazimAiEngine.safetyRoute(question) == null && !PerazimAiEngine.isSafeguardingDisclosure(question)
                && !PerazimAiEngine.isHarmToOthersQuery(question) && !PerazimAiEngine.isOverdoseQuery(question)) {
            ApprovedRoutes.Answer pastoral = ApprovedRoutes.pastoralAnswerFor(question, KjvVerseText.load(c));
            if (pastoral != null) return new Decision("routed:approved-" + pastoral.routeId, pastoral.text);
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
        if ("scope-refusal".equals(route)) {
            // A verified KJV lookup is a Bible question even when the wording has no topic cue (Levi, 2026-11-25): a Scripture
            // reference with a misspelt book, a quoted phrase the KJV holds, or a Swahili lookup. Anything else is still declined.
            Decision lookup = scopeLookup(c, table, question);
            if (lookup != null) return lookup;
        }
        if (route != null) {
            org.perazimchurch.app.ai.AiResponse routed = PerazimAiEngine.preModelResponse(route, question);
            return new Decision("routed:" + route, routed.getMessageText(), routed);
        }
        // Everyday Kenyan wording such as "verse ya upendo" or "neno 'grace' liko wapi": read in English, then looked up in the KJV.
        Decision swahili = scopeLookup(c, table, question);
        if (swahili != null) return swahili;
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
        // A quoted phrase the member asks to find is searched in the KJV before any approved route (Levi, 2026-11-25).
        Decision phrase = phraseFirstDecision(KjvVerseText.load(c), question);
        if (phrase != null) return phrase;
        ApprovedRoutes.Answer approved = ApprovedRoutes.answerFor(question, KjvVerseText.load(c));
        if (approved != null) return new Decision("routed:approved-" + approved.routeId, approved.text);
        String doctrine = DoctrineLibrary.load(c).answerFor(question);
        if (doctrine != null) return new Decision("routed:doctrine-approved", doctrine);
        if (org.perazimchurch.app.ai.library.IdentifyRefute.ENABLED && !scriptureDoctrineQuestion(question)) {
            String refute = identifyRefute(c, question);
            if (refute != null) return new Decision("routed:identify-refute", refute);
        }
        KjvVerseText verses = KjvVerseText.load(c);
        if (table != null) {
            Decision verse = verseDecision(table, verses, question);
            if (verse != null) return verse;
            Decision chapter = chapterDecision(table, verses, question);
            if (chapter != null) return chapter;
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
        // Christian doctrine asked as what the Bible or our teaching says, with no other group named, is answered from Scripture
        // and the library, not declined as an other-faiths or research question (coordinator, 2026-11-25).
        boolean scriptureOnly = zone != null && ((scriptureDoctrineQuestion(question)
                && ("other-faiths-and-groups".equals(zone.id) || "history-authorship-research".equals(zone.id)))
                || (generalFaithQuestion(question) && GENERAL_FAITH_BYPASS.contains(zone.id)));
        if (zone != null && !scriptureOnly) return new Decision("routed:no-go:" + zone.id, zone.decline);
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

    /** Christian doctrine words (Mary, purgatory, saints, justification and so on). */
    private static final Pattern SCRIPTURE_DOCTRINE = Pattern.compile(
            "\\b(?:mary|purgatory|saints?|intercession|intercede|pope|justif\\w*|sola scriptura|lord'?s supper|communion|original sin|"
            + "hell|canon|indulgences?|confess\\w*|nicaea|nicene|grace|eucharist|immaculate conception|assumption of mary)\\b", Pattern.CASE_INSENSITIVE);
    /** Other groups and faiths named in the question: their description goes to the fair-description and refutation routes. */
    private static final Pattern NAMES_OTHER_GROUP = Pattern.compile(
            "\\b(?:catholics?|mormons?|latter-day saints?|jehovah|witness(?:es)?|muslims?|islam|hindus?|buddhists?|orthodox|"
            + "adventists?|sabbath|saturday|new age|karma|jains?)\\b", Pattern.CASE_INSENSITIVE);

    /** True for a Christian doctrine question that names no other group. */
    static boolean scriptureDoctrineQuestion(String question) {
        return question != null && SCRIPTURE_DOCTRINE.matcher(question).find() && !NAMES_OTHER_GROUP.matcher(question).find();
    }

    /**
     * Zones that fire on ordinary words ("angry", "money", "Hebrews") in a general faith question. A general question is
     * answered from Scripture instead of declined (2026-11-25). Crisis, mental-health, grief, betting, scope and
     * spirit-gifts zones are never bypassed here.
     */
    private static final java.util.Set<String> GENERAL_FAITH_BYPASS = new java.util.HashSet<>(java.util.Arrays.asList(
            "pastoral-personal", "giving-money-pressure", "salvation-works-apostasy", "history-authorship-research"));
    private static final Pattern FIRST_PERSON = Pattern.compile("\\b(?:i|i'm|i’m|me|my|mine|myself)\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern GENERAL_START = Pattern.compile(
            "^\\s*(?:what|why|how|is|are|does|do|can|should|must|who|will|did|was|were|when)\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern FAITH_WORD = Pattern.compile(
            "\\b(?:bible|scripture|god|jesus|christ|church|spirit|baptis\\w*|communion|lord|salvation|grace|faith|pray\\w*|hell|"
            + "heaven|sin|saints?|mary|pope|sabbath|tithe\\w*|sunday|angels?|marriage|hebrews|romans|corinthians|letter|epistle|"
            + "gospel|creed|canon|apocrypha|resurrection|rapture|elders?|women|worship|gifts?|tongues|miracle\\w*|healing|forgiv\\w*|"
            + "wealth\\w*|prosper\\w*|money|divorce|polygam\\w*|dowry|offering|witch\\w*|eternal|angry|anger|worry|worrying)\\b",
            Pattern.CASE_INSENSITIVE);
    /** Money-pressure, curse and personal-sensitive words: a general question with one of these is not bypassed. */
    private static final Pattern NOT_GENERAL = Pattern.compile(
            "curse|breakthrough|\\bseed\\b|demand|\\bsend\\b|\\bsow\\b|pay (?:the |a )?(?:pastor|prophet|bishop|man of god|witch|healer)|"
            + "depress|hopeless|suicid|kill|self-harm|\\bhiv\\b|cancer|abuse|rape|violence|\\bhit\\b|\\bbeat\\b|drunk|addict|"
            + "gambl|\\bbet\\b|condom|pregnan|abortion|starv|no food", Pattern.CASE_INSENSITIVE);

    /** A general question about the Christian faith: a faith word, no first-person words, no other group, no pressure words. */
    static boolean generalFaithQuestion(String question) {
        return question != null && GENERAL_START.matcher(question).find() && FAITH_WORD.matcher(question).find()
                && !FIRST_PERSON.matcher(question).find() && !NAMES_OTHER_GROUP.matcher(question).find()
                && !NOT_GENERAL.matcher(question).find();
    }

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
            "\\b(?:that|this|the|previous|last|next|other|another) (?:verse|passage)\\b|\\b(?:the )?next one\\b", Pattern.CASE_INSENSITIVE);
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
        String book = ref.label.replaceFirst(" \\d{1,3}:\\d{1,3}(?:-\\d{1,3})?$", "");
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
        if (ref.corrected) notice = "I read the book name as " + ref.label + ". " + notice;
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
        // Afterlife: KJV passages on the topic (Scripture is the source; no other teaching is added).
        Pattern.compile("afterlife|after death|life after death|hereafter", Pattern.CASE_INSENSITIVE),
        // The Holy Spirit as Comforter: KJV passages on the topic (the approved source; Nave's list is not bundled).
        Pattern.compile("comforter|holy spirit as comfort", Pattern.CASE_INSENSITIVE),
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
        {"John 14:16", "John 14:26", "John 15:26", "John 16:7"},
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

    /** A book and chapter with no verse ("Psalm 23", "Jude 3"): the first verses of the chapter, verbatim. */
    static Decision chapterDecision(ScriptureRangeTable table, KjvVerseText verses, String question) {
        if (table == null || verses == null || question == null) return null;
        String[] bc = ScriptureReferenceNormalizer.bookAndChapter(normalisedQuestion(question));
        if (bc == null) return null;
        int bookNumber = table.bookNumber(bc[0]);
        if (bookNumber <= 0) return null;
        int chapter = Integer.parseInt(bc[1]);
        if (!table.isValid(bookNumber, chapter, 1)) {
            // A one-chapter book: the number is the verse, as in "Jude 3".
            return table.isValid(bookNumber, 1, chapter) ? verseDecision(table, verses, bc[0] + " 1:" + chapter) : null;
        }
        int count = table.verseCount(bookNumber, chapter);
        int shown = Math.min(MAX_VERSES, count);
        Decision d = verseDecision(table, verses, bc[0] + " " + chapter + ":1-" + shown);
        if (d == null || !"routed:verse-kjv".equals(d.status) || count <= shown) return d;
        return new Decision(d.status, d.answer + "\n\n" + bc[0] + " " + chapter + " has " + count + " verses. These are verses 1 to "
                + shown + ". Read the rest in the Bible tab.");
    }

    /** A quoted phrase the member asks to find, when the KJV holds it. Null otherwise, so the other routes decide. */
    static Decision phraseFirstDecision(KjvVerseText verses, String question) {
        if (verses == null || question == null) return null;
        String q = normalisedQuestion(question);
        if (!PHRASE_FIND_INTENT.matcher(q).find()) return null;
        String phrase = searchedPhrase(q);
        if (phrase == null) {
            Matcher unquoted = UNQUOTED_PHRASE.matcher(q);
            if (unquoted.find()) phrase = unquoted.group(1).trim();
        }
        if (phrase == null) {
            // "Where is Jesus wept", "who said faith without works": the words after the lead-in, when the KJV holds them.
            Matcher lead = LEAD_IN_PHRASE.matcher(q);
            if (lead.find() && !LEAD_IN_OTHER.matcher(lead.group(1)).find()) phrase = lead.group(1).trim();
        }
        if (phrase == null || phrase.split("\\s+").length < 2) return null;
        List<KjvVerseText.Found> found = verses.findPhrase(phrase, 3);
        if (found.isEmpty()) return null;
        StringBuilder out = new StringBuilder("From the King James Version (KJV):\n");
        for (KjvVerseText.Found f : found) out.append('\n').append(f.label).append(": \u201c").append(f.text).append('\u201d');
        return new Decision("routed:verse-phrase-kjv", out + "\n\nRead it in its context. The Bible tab shows the whole chapter.");
    }

    private static final Pattern PHRASE_FIND_INTENT = Pattern.compile(
            "\\b(?:find|search|where|who said|which book|which verse|show|look up|locate|read)\\b|\\bphrase\\b");
    /** "find the phrase faith without works" (no quotation marks): the words after the word phrase. */
    private static final Pattern UNQUOTED_PHRASE = Pattern.compile("\\bphrase\\s+(?:of\\s+)?([a-z' ]{3,80}?)[?.!]*$");
    private static final Pattern LEAD_IN_PHRASE = Pattern.compile(
            "^(?:where is|where in the bible is|where does the bible say|who said|who says|find|search for|locate|look up)\\s+(.+?)[?.!]*$");
    /** Lead-in objects that are not wording: verses, words, passages, references and questions about a topic. */
    private static final Pattern LEAD_IN_OTHER = Pattern.compile(
            "^(?:the |a |an |any |this |that )?(?:verse|verses|word|words|passage|passages|book|chapter|scripture|scriptures|topic|thing|one)\\b"
                    + "|\\d|\\b(?:psalm|psalms|john|romans|genesis)\\b\\s*$");
    private static final Pattern VERSE_SAYS = Pattern.compile(
            "\\b(?:verses?|passages?|scriptures?)\\s+(?:says?|saying|that says|that reads|that states|that tells|that declares|that mentions)\\s+(.+?)[?.!]*$");
    /** "search for angel" or "count angel": the word after the search or count words, when no quotation marks are used. */
    private static final Pattern WORD_SEARCH_WORD = Pattern.compile(
            "\\b(?:word search|search for|search|count)\\s*:?\\s+(?:the\\s+)?(?:word\\s+)?([a-z]{2,})\\b");

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
        Decision word = wordSearchDecision(verses, question);
        if (word != null) return word;
        String topic = topicVerses(verses, question);
        if (topic != null) {
            String prefix = phrase == null ? "" : "That exact wording is not in the Bible, so it is not Scripture as written. ";
            return new Decision(phrase == null ? "routed:verse-topic-kjv" : "routed:verse-topic-kjv-corrected",
                    prefix + "The King James Version speaks to this in:\n\n" + topic
                    + "\n\nRead it in its context. The Bible tab shows the whole chapter.");
        }
        if (phrase == null) {
            Decision about = verseAboutDecision(verses, question);
            if (about != null) return about;
        }
        if (phrase != null) {
            return new Decision("routed:verse-phrase-not-found", "That wording is not in the King James Version, and I cannot find it "
                    + "in the Bible as written, so I won't quote it. It does not appear as Scripture, and it is not found in the Bible. "
                    + "Please check the wording, or ask a church leader.");
        }
        return null;
    }

    private static final Pattern WORD_QUOTED = Pattern.compile("['\u2018\u2019\u201c\u201d\"]([a-z]{2,})['\u2018\u2019\u201c\u201d\"]");
    private static final Pattern WORD_AFTER = Pattern.compile("\\bword\\s+([a-z]{2,})\\b");
    /** Resource, commentary and file requests: never answered by the scope lookup bypass. */
    private static final Pattern RESOURCE_ASK = Pattern.compile(
            "cross-?references?|commentar|\\bnotes?\\b|dictionary|easton|wesley|matthew henry|\\bstrong|interlinear|lexicon|greek|hebrew|"
            + "spreadsheet|upload|calculate", Pattern.CASE_INSENSITIVE);
    /** Web and current-information words: an unquoted "search for current information" is not a Bible word search. */
    private static final Pattern WEB_SCOPE = Pattern.compile(
            "\\b(?:internet|online|google|website|web|news|latest|information|info|current events|weather|prices?|today|facebook|youtube|whatsapp|tiktok)\\b");
    private static final Pattern WORD_SEARCH_INTENT = Pattern.compile(
            "\\b(?:where|find|search|look up|locate|how many times|how often|number of times|count|word search|appears?|occurs?|mentioned|which verses?)\\b");
    private static final Pattern WORD_MEANING = Pattern.compile("\\b(?:mean|means|meaning|define|definition|translate|greek|hebrew)\\b");
    private static final Pattern BOOK_SCOPE = Pattern.compile(
            "\\b(?:in|within|from)\\s+(?:the\\s+)?(?:book\\s+of\\s+)?((?:[1-3]\\s+)?[a-z]+(?:\\s+[a-z]+)?)\\s*[?.!]*$");
    private static final Pattern VERSE_ABOUT = Pattern.compile(
            "\\b(?:verses?|passages?|scriptures?)\\s+(?:about|on|for|concerning|regarding|that (?:talks?|speaks?|says?|teaches?|mentions?) about|on the topic of)\\s+(.+?)[?.!]*$");
    /** Words that carry no meaning for a search, so a search for "the Lord being my shepherd" looks for "lord" and "shepherd". */
    private static final java.util.Set<String> SEARCH_STOP = new java.util.HashSet<>(java.util.Arrays.asList(
            "the", "and", "for", "with", "that", "this", "from", "into", "about", "being", "are", "was", "were", "have",
            "has", "had", "does", "not", "your", "our", "his", "her", "its", "their", "them", "they", "you", "all", "any",
            "bible", "verse", "verses", "passage", "passages", "scripture", "scriptures", "word", "words", "book", "chapter",
            "says", "say", "said", "find", "where", "what", "which", "show", "give", "tell", "want", "need", "feel", "when",
            "how", "who", "whom", "why", "much", "many", "times", "appear", "appears", "occur", "occurs", "mentioned",
            "locate", "search", "look", "there", "here", "will", "shall", "would", "could", "should", "can", "may", "might",
            "must", "upon", "unto", "onto", "within", "without", "over", "under", "more", "most", "some", "such", "very"));

    private static String normalisedQuestion(String question) {
        return question.toLowerCase(Locale.ROOT).replace('\u2019', '\'').replaceAll("\\s+", " ").trim();
    }

    /** "Where is the word 'grace' found?", "How many times does faith appear in Romans?". Counts and lists the exact word. */
    static Decision wordSearchDecision(KjvVerseText verses, String question) {
        if (verses == null || question == null) return null;
        String q = normalisedQuestion(question);
        if (q.length() > 140 || WORD_MEANING.matcher(q).find() || !WORD_SEARCH_INTENT.matcher(q).find()) return null;
        if (TOPIC_EXCLUDE.matcher(q).find()) return null;
        String word = null;
        Matcher quoted = WORD_QUOTED.matcher(q);
        if (quoted.find()) word = quoted.group(1);
        else if (WEB_SCOPE.matcher(q).find()) return null;
        if (word == null) {
            Matcher after = WORD_AFTER.matcher(q);
            if (after.find() && !SEARCH_STOP.contains(after.group(1))) word = after.group(1);
        }
        if (word == null) {
            Matcher searched = WORD_SEARCH_WORD.matcher(q);
            if (searched.find()) word = searched.group(1);
        }
        if (word == null || SEARCH_STOP.contains(word)) return null;
        int book = -1;
        String scope = "the Bible";
        Matcher scoped = BOOK_SCOPE.matcher(q);
        if (scoped.find()) {
            int n = verses.bookNumber(scoped.group(1).trim());
            if (n >= 0) {
                book = n;
                scope = verses.bookName(n);
            }
        }
        KjvVerseText.WordHits hits = verses.wordHits(word, book, 25);
        if (hits.occurrences == 0) {
            return new Decision("routed:word-search-none", "The exact word \u201c" + word + "\u201d does not appear in "
                    + (book < 0 ? "the King James Version" : scope + " in the King James Version")
                    + ". Check the spelling, or try a related word.");
        }
        StringBuilder refs = new StringBuilder();
        for (KjvVerseText.Found f : hits.verses) {
            if (refs.length() > 0) refs.append(", ");
            refs.append(f.label);
        }
        String where = book < 0 ? "the Bible" : scope;
        StringBuilder out = new StringBuilder("The exact word \u201c" + word + "\u201d appears " + hits.occurrences
                + (hits.occurrences == 1 ? " time" : " times") + " in " + hits.verseCount
                + (hits.verseCount == 1 ? " verse" : " verses") + " of " + where + " (King James Version).\n\nReferences: " + refs);
        if (hits.verseCount > hits.verses.size()) out.append(", and ").append(hits.verseCount - hits.verses.size()).append(" more");
        out.append(".\n\nRead them in their context. The Bible tab shows the whole chapter.");
        return new Decision("routed:word-search-kjv", out.toString());
    }

    /** "The verse about the Lord being my shepherd": verbatim KJV verses that hold the subject's words. */
    static Decision verseAboutDecision(KjvVerseText verses, String question) {
        if (verses == null || question == null) return null;
        String q = normalisedQuestion(question);
        if (q.length() > 160 || TOPIC_EXCLUDE.matcher(q).find()) return null;
        Matcher m = VERSE_ABOUT.matcher(q);
        if (!m.find()) {
            m = VERSE_SAYS.matcher(q);
            if (!m.find()) return null;
            String said = m.group(1).trim();
            List<KjvVerseText.Found> exact = verses.findPhrase(said, 3);
            if (!exact.isEmpty()) {
                StringBuilder quoted = new StringBuilder("From the King James Version (KJV):\n");
                for (KjvVerseText.Found f : exact) quoted.append('\n').append(f.label).append(": \u201c").append(f.text).append('\u201d');
                return new Decision("routed:verse-phrase-kjv", quoted + "\n\nRead it in its context. The Bible tab shows the whole chapter.");
            }
        }
        List<String> words = new ArrayList<>();
        for (String token : m.group(1).split("[^a-z]+")) {
            if (token.length() >= 3 && !SEARCH_STOP.contains(token) && !words.contains(token) && verses.bookNumber(token) < 0) {
                words.add(token);
            }
        }
        if (words.isEmpty()) return null;
        String subject = String.join(" ", words);
        List<KjvVerseText.Found> found;
        if (words.size() == 1) {
            found = verses.findAllWordsRanked(words, 3);
        } else {
            found = verses.findAllWordsRanked(words, 3);
        }
        if (found.isEmpty()) {
            return new Decision("routed:verse-about-not-found", "I cannot find a verse in the King James Version that has "
                    + "all of these words together: " + subject + ". Try a shorter phrase, or read the Bible tab.");
        }
        String lead = q.contains("brief") || q.contains("short") || q.contains("simple")
                ? "Here is a brief answer from the King James Version (KJV), verses with " + subject + ":\n"
                : "From the King James Version (KJV), verses with " + subject + ":\n";
        StringBuilder out = new StringBuilder(lead);
        for (KjvVerseText.Found f : found) out.append('\n').append(f.label).append(": \u201c").append(f.text).append('\u201d');
        return new Decision("routed:verse-about-kjv", out + "\n\nRead it in its context. The Bible tab shows the whole chapter.");
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

    /**
     * KJV lookups that run before the scope gate and the topic routes: a reference, a quoted or unquoted phrase the KJV holds,
     * a verse-about or word search. Each one returns only when the KJV itself gives the answer.
     */
    static Decision scopeLookup(Context c, ScriptureRangeTable table, String question) {
        // Cross-references, commentaries, dictionaries and uploaded files have their own routes and refusals: they are not bypassed.
        if (RESOURCE_ASK.matcher(question).find()) return null;
        KjvVerseText kv = KjvVerseText.load(c);
        Decision found = table == null ? null : verseDecision(table, kv, question);
        if (found == null) found = phraseFirstDecision(kv, question);
        if (found != null) return found;
        // Unquoted English word searches ("Where is the word grace found?") must reach the word search, not stop here: the
        // early return that skipped English questions sent them to the no-source reply on the phone (2026-11-25 regression).
        String english = englishCues(normalisedQuestion(question));
        Decision about = verseAboutDecision(kv, english);
        return about != null ? about : wordSearchDecision(kv, english);
    }

    /** Everyday Swahili lookup words read as English, so the same KJV routes answer them. Only applied when a word changes. */
    static String englishCues(String q) {
        return q.replaceAll("\\bnisaidie kupata\\b", "find")
                .replaceAll("\\bverse ya\\b", "verse about")
                .replaceAll("\\bupendo\\b", "love")
                .replaceAll("\\bneno\\b", "word")
                .replaceAll("\\bliko wapi\\b", "where is")
                .replaceAll("\\bwapi\\b", "where")
                .replaceAll("\\bkwenye biblia\\b", "in the bible");
    }

    /** A parsed reference: book, chapter, first and last verse, and whether the KJV has it. */
    static final class Reference {
        final int bookNumber;
        final int chapter;
        final int first;
        final int last;
        final boolean valid;
        final String label;
        /** True when the book name was a close misspelling (Mathew for Matthew), read as the nearest book. */
        final boolean corrected;

        Reference(int bookNumber, int chapter, int first, int last, boolean valid, String label, boolean corrected) {
            this.bookNumber = bookNumber;
            this.chapter = chapter;
            this.first = first;
            this.last = last;
            this.valid = valid;
            this.label = label;
            this.corrected = corrected;
        }

    }

    /** Parses a matched reference, or returns null when there is none. A close misspelling of a book name is accepted and flagged. */
    static Reference reference(ScriptureRangeTable table, String question) {
        ScriptureReferenceNormalizer.Result ref = ScriptureReferenceNormalizer.normalize(question);
        if (ref == null || ref.normalized == null) return null;
        boolean corrected = !ref.confident && ref.clarify != null && ref.clarify.startsWith("Did you mean");
        if (!ref.confident && !corrected) return null;
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
        return new Reference(bookNumber, chapter, first, last, valid, label, corrected);
    }

    private static String read(Context c, String path) throws java.io.IOException {
        try (java.io.InputStream in = c.getAssets().open(path)) {
            return new String(org.perazimchurch.app.ai.IoCompat.readAll(in), java.nio.charset.StandardCharsets.UTF_8);
        }
    }
}
