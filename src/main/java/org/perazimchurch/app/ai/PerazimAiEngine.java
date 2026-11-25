package org.perazimchurch.app.ai;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.perazimchurch.app.BuildConfig;
import org.perazimchurch.app.data.local.preference.GamificationStore;
import org.perazimchurch.app.data.local.session.SessionManager;
import org.perazimchurch.app.domain.model.SpiritualLevel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Built-in offline app-navigation guide. This is a rules engine, not a language model.
 * Features:
 * - Ultra-lightweight footprint (<1.2MB overhead).
 * - Instant (<15ms) on-device query classification on budget hardware (MediaTek Helio G70).
 * - Live gamification state inspection (XP, Streaks, Grace points, Level progression).
 * - Natural language deep-linking to app sections (Tithe/Giving, Bible, Sermons, Hymns, Prayer, Riddles, Jokes).
 * - Strict domain boundary guardrails locking the assistant exclusively to church topics.
 */
public class PerazimAiEngine {

    private static final Pattern CRISIS_PATTERN = CrisisPhrases.PATTERN;
    private static final Pattern CRISIS_DISCLOSURE_PATTERN = Pattern.compile(
            "\\b(?:ananipiga|ananinyanyasa|ananitishia|ananiua|ananibaka|kunipiga|ninapigwa|ninanyanyaswa)\\b|" +
            "\\b(?:(?:my|our) (?:husband|wife|spouse|partner|boyfriend|girlfriend|parent|father|mother|caregiver) " +
                    "(?:hits?|beats?|beating|struck|choked|attacked|threatened) (?:me|us|my child|our child)|" +
                    "(?:hit|hits|hitting|beat|beats|beating|struck|choked|choking|attacked|threatened) (?:me|us|my child|our child))\\b|" +
                    "\\b(?:afraid|scared|fear) (?:that )?(?:my )?(?:partner|husband|wife|spouse|parent|father|mother|caregiver) " +
                    "(?:will|might|may|is going to) (?:hurt|hit|beat|attack|kill) (?:me|my child|our child)\\b",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern SENSITIVE_PASTORAL_REQUEST = Pattern.compile(
            "\\b(?:keep (?:this|it) confidential|keep (?:this|it) secret|don't tell anyone|do not tell anyone|never tell (?:anyone|anybody)|" +
                    "please don't share this|please do not share this|pastoral intervention)\\b",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern PERSONAL_CONTEXT = Pattern.compile(
            "\\b(?:i am|i'm|i feel|i felt|i have been|i was|my|our|we|my friend|someone close to me)\\b",
            Pattern.CASE_INSENSITIVE);
    /** General doctrine questions about falling away or the unforgivable sin: answered by the approved route, not the destiny decline. */
    private static final Pattern PERSEVERANCE_QUESTION = Pattern.compile(
            "keep sinning|never saved|lose (?:our |my |their )?(?:salvation|place in heaven)|salvaiton|salvtion|unforgivable|on purpose|fall(?:s|ing)? away|apostat|aposttae|judas|peter denied", Pattern.CASE_INSENSITIVE);
    private static final Pattern SAYS_CLAIM_QUESTION = Pattern.compile(
            "\\b(?:pastor|preacher|church|bishop|elder|leader|someone) says\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern FIRST_PERSON_CONTEXT = Pattern.compile(
            "\\b(?:i|i'm|i am|i feel|i felt|i have|i was|my|me)\\b", Pattern.CASE_INSENSITIVE);
    /** A factual question such as "Is that adultery?" about a spouse's conduct (Added 2026-11-25). */
    private static final Pattern FACTUAL_INFIDELITY_QUESTION = Pattern.compile(
            "\\bis (?:that|this|it|he|she|what (?:he|she|they) (?:is )?doing) (?:adultery|cheating|infidelity|unfaithful|a sin|wrong)\\b",
            Pattern.CASE_INSENSITIVE);
    /** Personal distress, fear or danger in the speaker's own words (Added 2026-11-25). */
    private static final Pattern PERSONAL_DISTRESS = Pattern.compile(
            "\\b(?:i|i'm|i am|i feel|i felt|me|myself|suicid\\w*|abuse\\w*|hit|beat\\w*|threat\\w*|afraid|scared|harm\\w*|kill\\w*|" +
                    "depress\\w*|anx\\w*|hurt\\w*|forc\\w*)\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern SENSITIVE_PASTORAL_TOPIC = Pattern.compile(
            "\\b(?:abuse|assault|rape|violence|hit me|beating|marital conflict|family conflict|divorce|" +
                    "separation|trauma|grief|bereavement|mourning|depress(?:ion|ed)?|anx(?:iety|ious)|" +
                    "panic attack|self[ -]?harm|suicid(?:e|al)|addict(?:ion|ed)|overdose|confession|confess\\w*|" +
                    "confidential|secret)\\b",
            Pattern.CASE_INSENSITIVE);
    /** Requests the guide cannot meet from inside the app: browsing, live news, recent research, or a link to open. */
    private static final Pattern LIVE_RESEARCH_PATTERN = Pattern.compile(
            "https?://|\\bbrowse\\b|\\bsearch the (?:internet|web)\\b|\\blive news\\b|\\blatest news\\b|\\bacademic research\\b|" +
            "\\brecent (?:research|studies|papers|news)\\b|\\bfind (?:me )?(?:recent )?(?:research|studies|papers)\\b|\\bgoogle\\b",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern[] CLEARLY_OUT_OF_SCOPE_PATTERNS = {
            // Job applications and CVs are not something the guide writes.
            Pattern.compile("\\b(?:write|draft|help me (?:write|prepare)|prepare)\\b.{0,30}\\b(?:cv|curriculum vitae|resume|cover letter)\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\b(?:kenya'?s?|employment|labou?r|parliament|government)\\b.{0,40}\\b(?:laws?|legislation|acts?)\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\b(?:court|lawyer|attorney|landlord|tenant|evict|eviction)\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\b(?:best|cheapest|good)\\s+(?:car|cars|phone|phones|laptop|laptops|house|houses|tv|television)\\b|\\bto buy\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\b(?:love letter|romantic (?:letter|message|poem)|write (?:me )?a (?:love )?(?:poem|song|letter) (?:for|to) (?:my|a) (?:girlfriend|boyfriend|crush))\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\b(?:which|what)\\s+(?:stock|stocks|share|shares|cryptocurrency|crypto|bitcoin|forex|bond|bonds)\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\b(?:buy|sell|trade|invest in)\\b.{0,70}\\b(?:stock|stocks|share|shares|cryptocurrency|crypto|bitcoin|forex|bond|bonds)\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\b(?:should i|recommend|help me choose|help me pick)\\b.{0,70}\\b(?:investment portfolio|retirement portfolio|personal budget|payday loan|mortgage|credit card|tax return|insurance policy)\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\b(?:what|which|recommend|prescribe)\\b.{0,50}\\b(?:medicine|medication|drug|dosage|dose|treatment)\\b|\\b(?:diagnose me|diagnose my|medical advice|prescribe medication)\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\b(?:give me legal advice|should i sue|how (?:do|can) i sue|file (?:a )?lawsuit|draft (?:a )?(?:legal )?contract|represent me in court)\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\b(?:who won|latest score|current score|score of)\\b.{0,60}\\b(?:football|match|game|election|sports?)\\b|\\b(?:write|draft) (?:me )?a campaign speech\\b", Pattern.CASE_INSENSITIVE),
            // General-purpose tasks the app does not offer: code, arithmetic, recipes, weather, travel, entertainment.
            Pattern.compile("\\b(?:write|debug|fix|explain|generate)\\b.{0,40}\\b(?:python|javascript|typescript|sql|html|css|c\\+\\+|java code|code snippet|source code|regex)\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\b(?:solve|calculate|compute|integrate|differentiate)\\b.{0,40}\\b(?:equation|integral|derivative|algebra|calculus|percentage|square root)\\b|\\bwhat is \\d+\\s*(?:[-+*/x×÷]|times|plus|minus|divided by)\\s*\\d+", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\b(?:recipe|how (?:do|to|can) i (?:cook|bake)|ingredients for)\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\b(?:dose|dosage|ibuprofen|paracetamol|acetaminophen|antibiotics?|aspirin)\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\b(?:weather (?:today|tomorrow|forecast|this week)|forecast for (?:today|tomorrow|this week))\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\b(?:book|find|cheap)\\b.{0,30}\\b(?:flights?|hotels?|airbnb|train tickets?)\\b|\\bvisa application\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\b(?:movie|movies|netflix|tv show|celebrity|box office)\\b", Pattern.CASE_INSENSITIVE),
            // Denylist applied to every query, including ones that contain allowlisted church or Bible words
            // (for example "John", "Paul", "Grace", "offering", "profile"). Finance and investment first.
            Pattern.compile("\\b(?:share prices?|stock prices?|stock market|stock exchange|dividends?|index funds?|mutual funds?|"
                    + "etfs?|forex|crypto\\w*|bitcoin|ethereum|dogecoin|blockchain|nfts?|day trad(?:e|er|ers|ing)|beats? the market|"
                    + "high[- ]yield|savings accounts?|fixed deposits?|mortgages?|lenders?|payday loans?|credit scores?|interest rates?|"
                    + "pension funds?|retirement funds?|invest(?:ing|ment|ments)? (?:my |your |our |the )?(?:money|savings|cash|funds?)|"
                    + "investment (?:advice|portfolio|returns?|strategy|options?))\\b", Pattern.CASE_INSENSITIVE),
            // Entertainment, music charts and celebrity facts.
            Pattern.compile("\\b(?:hit songs?|first hit|number one (?:hit|single)|top (?:\\w+ )?(?:songs?|albums?|charts?)|billboard|"
                    + "album sales|net worth|celebrit(?:y|ies)|actors?|actress(?:es)?|movie stars?|film stars?|"
                    + "height in (?:cm|centimet(?:re|er)s|inches|feet|met(?:er|re)s)|centimet(?:re|er)s|inches tall|feet tall)\\b",
                    Pattern.CASE_INSENSITIVE),
            // Job and career search: not an app or church service.
            Pattern.compile("\\b(?:job search(?:ing)?|job applications?|job offers?|job vacanc(?:y|ies)|job listings?|"
                    + "curriculum vitae|cover letters?|salar(?:y|ies)|career change|recruiters?)\\b", Pattern.CASE_INSENSITIVE),
            // Gambling and property purchase.
            Pattern.compile("\\b(?:sports betting|betting odds|bet on (?:the |a )?(?:match|game|team|horse|race)|casinos?|lottery numbers?|jackpots?|poker|"
                    + "(?:buy|sell) (?:a |an |my |some )?(?:house|apartment|property|plot|land)|(?:house|property|land|plot) prices?)\\b",
                    Pattern.CASE_INSENSITIVE)
    };

    private final Context context;

    public PerazimAiEngine(@NonNull Context context) {
        this.context = context.getApplicationContext();
    }

    /**
     * Processes a user query and returns a structured AI response with intent actions.
     */
    @NonNull
    /**
     * The one safety-first decision for every app entry point (guide dialog, engine, online relay, model refusal).
     * It uses the round 39c routing: suicide, third-party suicide, abuse, both risks, denial and literary context.
     * Returns null when no safety route applies, so the normal path continues.
     */
    @Nullable
    public static AiResponse safetyFirstResponse(@Nullable String query) {
        if (query == null || query.trim().isEmpty()) return null;
        String route = safetyRoute(query);
        if (route == null || "topic-violence".equals(route) || "general-topic".equals(route)) return null;
        return preModelResponse(route, query);
    }

    public AiResponse processQuery(@Nullable String rawQuery) {
        if (rawQuery == null || rawQuery.trim().isEmpty()) {
            return getWelcomeResponse();
        }

        String q = rawQuery.trim().toLowerCase(Locale.US);

        // 0a. Round 39c safety routing first, so abuse is never answered with the suicide-plan question.
        AiResponse safetyFirst = safetyFirstResponse(rawQuery);
        if (safetyFirst != null) return safetyFirst;
        // 0. CRISIS & PASTORAL CARE INTERVENTION
        // Two-step check first: vague exhaustion gets the warm check; crisis wording, or a yes or plan after it, gets the full response.
        SoftSafety.Verdict safety = SoftSafety.judge(rawQuery);
        if (safety == SoftSafety.Verdict.CRISIS) {
            return handleCrisisQuery();
        }
        if (safety == SoftSafety.Verdict.SOFT_CHECK) {
            return softSafetyCheckResponse();
        }
        // Legacy keyword check: only when the safety routing above gave no route (Levi, 2026-11-25). Domestic-violence theology
        // and other violence-topic wording without a speaker's risk never get the suicide-plan reply here.
        if (isLegacyCrisisKeyword(rawQuery)) {
            return handleCrisisQuery();
        }
        if (isOverdoseQuery(rawQuery)) {
            return overdoseResponse();
        }
        if (isHarmToOthersQuery(rawQuery)) {
            return harmToOthersResponse();
        }

        if (isSensitivePastoralDisclosure(rawQuery)) {
            return sensitivePastoralResponse();
        }

        if (isClearlyOutOfScopeQuery(rawQuery)) {
            return boundaryResponseFor(rawQuery);
        }

        // 0b. SCRIPTURE REFERENCE UNDERSTANDING: ask one short question when the book name is unclear
        org.perazimchurch.app.ai.local.ScriptureReferenceNormalizer.Result reference =
                org.perazimchurch.app.ai.local.ScriptureReferenceNormalizer.normalize(rawQuery);
        if (reference != null && !reference.confident && reference.clarify != null) {
            return AiResponse.plainText(reference.clarify, Arrays.asList("📖 Open the Bible"));
        }

        // 1. BIBLE RIDDLES & TRIVIA
        if (containsAny(q, "riddle", "trivia", "quiz", "puzzle")) {
            return handleRiddlesQuery();
        }

        // 2. CHRISTIAN JOKES & HUMOR
        if (containsAny(q, "joke", "jokes", "humor", "funny", "laugh")) {
            return handleJokesQuery();
        }

        // 3. GIVING & TITHE: M-Pesa, Paybill, Offering, Giving
        if (containsAny(q, "give", "giving", "tithe", "tithing", "offering", "mpesa", "m-pesa", "paybill", "contribute", "donate")) {
            return handleGivingQuery();
        }

        // 4. MEDIA & SERMONS: YouTube Live, Broadcast, Video, Audio Messages
        if (containsAny(q, "livestream", "live stream", "broadcast", "youtube", "video", "sermon", "preach", "recording")) {
            return handleSermonsQuery(q);
        }

        // 5. HOLY BIBLE: Scripture, Chapters, KJV, SUV, Swahili
        if (containsAny(q, "daily verse", "verse of the day", "today's verse", "todays verse")) {
            return handleDailyVerseQuery();
        }
        if (containsAny(q, "bible", "scripture", "verse", "chapter", "read word", "kjv", "suv", "swahili bible", "genesis", "exodus", "samuel")) {
            return handleBibleQuery(q);
        }

        // 6a. Fellowship items that contain worship words: praise reports and direct chat
        if (containsAny(q, "praise report", "praise reports", "direct chat")) {
            return handleFellowshipQuery();
        }

        // 6. WORSHIP & HYMNAL: Songs, Praise, Hymns, Nyimbo
        if (containsAny(q, "hymn", "hymnal", "worship", "song", "sing", "praise", "nyimbo", "chords")) {
            return handleWorshipQuery();
        }

        // 7. FELLOWSHIP & PRAYER: Community, Wall, Cell Group, Testimony
        if (containsAny(q, "prayer", "prayers", "pray", "praying", "fellowship", "testimony", "cell group", "community", "intercede")) {
            return handleFellowshipQuery();
        }

        // 8. GAMIFICATION: XP, Level, Streaks, Grace Tokens
        if (containsAny(q, "xp", "experience point", "experience points", "my level", "level up", "my points", "my score", "my rank")) {
            return handleXpQuery(q);
        }
        if (containsAny(q, "streak", "consecutive days", "streak freeze", "freeze", "frozen", "shield")) {
            return handleStreakQuery(q);
        }
        if (containsAny(q, "grace token", "grace tokens", "grace point", "grace points", "purple heart")) {
            return handleGraceQuery(q);
        }

        // 9. DAILY REFLECTION ROUTINE: 3-Minute Breakthrough Altar
        if (containsAny(q, "reflection", "routine", "devotion", "altar", "baal-perazim", "today's word")) {
            return handleReflectionsQuery();
        }

        // 10. CHURCH IDENTITY & LEADERSHIP: Bishop Mutweri, Campuses, Times
        if (containsAny(q, "campus", "campuses", "headquarters", "service time", "church location", "church address", "church contact")) {
            return handleChurchInfoQuery();
        }

        // 11. PROFILE & SAVED OFFLINE CONTENT
        if (containsAny(q, "profile", "account", "download", "saved", "offline", "storage", "settings")) {
            return handleProfileQuery();
        }

        // 12. GREETINGS & SALUTATIONS
        if (q.matches("(?:hello|hi|hey|praise the lord|bwana asifiwe|shalom|good morning|good evening)[!. ]*")) {
            return getWelcomeResponse();
        }

        // 13. DOMAIN GUARDRAIL: Off-topic rejection
        return handleOffTopicGuardrail(rawQuery);
    }

    private AiResponse handleCrisisQuery() {
        return crisisResponse();
    }

    /** The warm safety check for vague exhaustion. It carries the same verified numbers as the full response. */
    static AiResponse softSafetyCheckResponse() {
        return AiResponse.plainText(SoftSafety.checkText(), Arrays.asList("🙏 Open prayer support", "📖 Open the Bible"));
    }

    /** Equivalent openers for the crisis reply, so the same message is not answered with one identical sentence each time (Levi, 2026-11-25). The verified numbers and the questions below never change. */
    private static final String[] CRISIS_OPENERS = {
            "I'm sorry you're facing this.",
            "I'm so sorry you are carrying this right now.",
            "Thank you for telling me. I'm sorry you're facing this."
    };
    private static final java.util.concurrent.atomic.AtomicInteger CRISIS_OPENER_TURN = new java.util.concurrent.atomic.AtomicInteger();

    static String crisisOpener() {
        return CRISIS_OPENERS[Math.floorMod(CRISIS_OPENER_TURN.getAndIncrement(), CRISIS_OPENERS.length)];
    }

    public static AiResponse crisisResponse() {
        String msg = crisisOpener() + " Are you safe right now, and do you have a plan or a way to end your life tonight? If you or someone else may be in immediate danger, call 999 or 112 now, or go to the nearest hospital. If someone else is at risk and it is safe for you to do so, stay with them, or ask a trusted person to stay with them. Do not put yourself or anyone else in danger to do this. For ongoing distress, contact a qualified local health professional.\n\n"
                + "Kenya helplines, by purpose:\n"
                + "- Police emergency and immediate danger: 999 or 112.\n"
                + "- SHA 922 Lifeline, to reach an ambulance for urgent medical emergencies, including overdose or poisoning.\n"
                + "- 1195, the National GBV Helpline, for gender-based violence counselling and referral, open 24 hours.\n"
                + "- 116, the Childline Kenya helpline, for a child at risk.\n"
                + "- 1192, NACADA, for drug and alcohol use, open 24 hours.\n"
                + "- Befrienders Kenya +254 722 178 177, for emotional support on weekdays from 9am to 5pm. It is not an emergency service.\n\n"
                + "You can also use the app's prayer feature or a church contact shown in the app, if you choose. This guide cannot provide emergency monitoring or confirm that a pastoral contact is available.";
        return AiResponse.plainText(msg, Arrays.asList("🙏 Open prayer support", "📖 Open the Bible"));
    }

    /** Conservative local safety gate used before guide, Qwen, and online routing. */
    /** Future-tense self-harm with common typos (for example "hurt myeslf tonight"). Approved safety routing. */
    private static final Pattern SELF_HARM_TYPO_PATTERN = Pattern.compile(
            "\\b(?:going to|gonna|plan to|want to|will|about to)\\s+(?:hurt|harm|kill|end|cut)\\s+my\\s?(?:self|seff|sefl|slef|eslf|myeslf|sel|elf)\\b|" +
            "\\bhurt\\s+my\\s?(?:self|seff|sefl|slef|eslf)\\b|\\bkill\\s+my\\s?(?:self|seff|sefl|slef|eslf)\\b", Pattern.CASE_INSENSITIVE);

    private static final Pattern MEANS_PATTERN = Pattern.compile(
            "\\b(?:gun|guns|pistol|rifle|knife|knives|pills|rope|poison|weapon)\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern DISTRESS_PATTERN = Pattern.compile(
            "\\b(?:suicid\\w*|kill myself|hurt myself|end (?:it|my life)|don'?t know what i'?m going to do|can'?t go on|hopeless|drinking|no reason to live|"
            + "(?:stab|shoot|hang|cut|burn|drown|strangle|poison) (?:my ?self)|cutting myself|want to die|wish i was dead)\\b", Pattern.CASE_INSENSITIVE);

    /** Means plus distress is a risk message and must never reach the scope gate. */
    /** Overdose or poisoning: an emergency now, whatever the symptoms. */
    private static final Pattern OVERDOSE_PATTERN = Pattern.compile(
            "\\b(?:overdos\\w*|poisoned myself|took too many|taken the (?:tablets|pills)|swallowed (?:pills|tablets|poison)|drank (?:bleach|poison)|" +
            "took (?:a|the|all|my) (?:whole )?(?:bottle|box|packet|strip|container) of|took a whole bottle|took all (?:my|the) (?:tablets|pills)|" +
            // Swallowed medicine with any short description in between ("swallowed a packet of tablets an hour ago"), and Swahili "nimemeza vidonge".
            "swallowed (?:\\w+\\s+){0,4}?(?:tablets|pills|capsules|poison|medicine)|nimemeza (?:\\w+\\s+){0,3}?(?:vidonge|sumu))\\b", Pattern.CASE_INSENSITIVE);

    public static boolean isOverdoseQuery(@Nullable String query) {
        return query != null && OVERDOSE_PATTERN.matcher(query).find();
    }

    /** Fixed overdose response: leads with the medical line and never waits for symptoms or a reply. */
    public static AiResponse overdoseResponse() {
        String msg = "This may be an emergency. Call 922 now, or go to the nearest emergency department immediately. "
                + "Do not wait for symptoms, and do not wait for a reply from this app. If you can, ask someone nearby to stay with you and take you. "
                + "Bring any medicine or container you took.";
        return AiResponse.plainText(msg, Arrays.asList("📖 Open the Bible"));
    }

    public static boolean isMeansAndDistress(@Nullable String query) {
        if (query == null) return false;
        return MEANS_PATTERN.matcher(query).find() && DISTRESS_PATTERN.matcher(query).find();
    }

    /** Intent to harm a named or unnamed other person (typos tolerated). */
    private static final Pattern HARM_OTHERS_PATTERN = Pattern.compile(
            "\\b(?:kill|killing|hurt|harm|stab|shoot|beat)\\b.{0,40}\\b(?:s[ou]m[a-z]{0,2}ne|somebody|someon|him|her|them|person|boss|husband|wife|neighbour|neighbor|child)\\b|" +
            "\\bthoughts of (?:killing|hurting)\\b|" +
            // Intent to attack people or a place, as a third party or a member (2026-11-25 safety run, EX-081).
            "\\b(?![^.]*\\b(?:myself|yourself)\\b)(?:shoot up|shoot (?:the |up the )?(?:school|church|college|mall|market|bus|people|students|classmates)|" +
            "(?:wants?|plans?|going|about) to (?:shoot|stab|kill|attack|bomb|blow up|burn)\\b|" +
            "(?:attack|bomb|blow up|burn down) (?:the |a |our )?(?:school|church|college|mall|market|bus|clinic|hospital))\\b|" +
            // Extortion with private photos (Kenya set): police and 999/112 wording, not a scope refusal.
            "threats? to (?:post|share|send|leak) (?:my |his |her |their )?(?:private|intimate|nude|naked) (?:photos?|pictures?|videos?|images?)|" +
            "\\bunless i pay (?:him|her|them|you)\\b", Pattern.CASE_INSENSITIVE);

    public static boolean isHarmToOthersQuery(@Nullable String query) {
        if (query == null) return false;
        return HARM_OTHERS_PATTERN.matcher(query).find();
    }

    /** Fixed safety response for intent to harm another person. Makes no counselling promise. */
    public static AiResponse harmToOthersResponse() {
        String msg = "I'm really concerned about what you just shared. If anyone is in danger right now, call 999 or 112 immediately, or go to the nearest police station or hospital. "
                + "Move away from the person if you can, and don't stay alone with these thoughts. Tell a trusted adult or a leader near you now. "
                + "This guide cannot monitor your safety or promise to counsel you through this, so please reach a qualified professional as soon as you can.";
        return AiResponse.plainText(msg, Arrays.asList("📖 Open the Bible"));
    }

    /** Abuse, threats and child-safety disclosures: fixed safeguarding wording before the model. */
    private static final Pattern SAFEGUARDING_PATTERN = Pattern.compile(
            "\\b(?:abused|abusing|abuse|sexually (?:abused|assaulted|molested)|molest\\w*|raped|rape|beats? me|beat me|" +
            "hits? me|hitting me|threatened to kill me|threatens to kill me|going to kill me|" +
            "touch(?:ing|ed)? (?:my |the )?(?:child|children|kids?|boy|girl|them) (?:inappropriately|sexually)|" +
            "inappropriately touch\\w*|touching (?:them|my child|my kids?|children) inappropriately|" +
            "church worker has been touching|church worker touch\\w*|forced me to|" +
            "(?:touch\\w*|fondl\\w*|grop\\w*|molest\\w*|abus\\w*) (?:my |our |the |a )?(?:\\w+ )?" +
            "(?:daughter|son|child|children|kids?|boy|girl|baby|toddler|teen\\w*|minor|student|pupil)|" +
            "(?:pastor|priest|teacher|youth leader|church leader|deacon|elder|church worker) (?:has |have )?" +
            "(?:touched|molested|abused|raped|fondled)|" +
            // Kenya-set safeguarding wording: beatings, forced child marriage, grooming gifts, locked-in children, sex for money.
            "beats? (?:me|her|him|them|my \\w+)|beating (?:me|her|him|them)|assaults? (?:me|her|him|them)|" +
            "forced into marriage|forced to marry|forcing (?:her|him|them|my \\w+) (?:into )?marriage|" +
            "married (?:at|aged) (?:1[0-7]|[0-9])\\b|" +
            "(?:daughter|son|child|children|kids?|boy|girl|baby|toddler|\\d{1,2}[- ]year[- ]old)\\b.{0,80}?\\b(?:secret|private) (?:gifts?|messages?|photos?|pictures?)|" +
            "locked (?:in|inside) (?:a |the )?(?:room|house|cupboard|bedroom)|" +
            "money for sex|paid for sex|sex for (?:money|food|favou?rs|jobs?)|trade sex|" +
            // Child-abuse disclosures from the Kenya sets: secrecy instructions, a relative sleeping in the child's room, Swahili wording.
            "keep (?:our|the|this|my) secret|keep it a secret|(?:told|tells|telling) me not to tell|" +
            "not to tell (?:mummy|mama|mom|mum|dad|daddy|my parents|anyone)|don'?t tell (?:mummy|mama|mom|mum|dad|daddy|my parents|anyone)|" +
            "sleeps in my room|vitendo vibaya|unyanyasaji|amebakwa|kubakwa|kumbaka|" +
            // Child safety from the Kenya sets: female genital cutting, night deliverance beatings, a minor pregnant, church secrecy, pastor asking for secret favours.
            "circumcis\\w* (?:my |our |the |a )?(?:daughter|girl|girls|baby|child|kids?|mtoto|binti|wasichana)|" +
            "kutahiri (?:mtoto|binti|wasichana)|female genital|genital mutilation|\\bfgm\\b|ukeketaji|kuwakeketa|" +
            "beat(?:s|ing)? (?:people|children|kids|members|congregants)|" +
            "\\b(?:1[0-7]|[0-9])[- ]year[- ]old\\b.{0,60}\\bpregnant|pregnant\\b.{0,60}\\b(?:1[0-7]|[0-9])[- ]year[- ]old\\b|" +
            "\\bis (?:1[0-7])\\b.{0,40}\\bpregnant|" +
            "(?:church|pastor|leaders?|elders?) (?:wants?|is trying|are trying) to (?:hide|cover up)\\b|" +
            "(?:pastor|mchungaji)\\b.{0,160}\\b(?:siri|faragha|mbegu)\\b|\\b(?:siri|faragha|mbegu)\\b.{0,160}\\b(?:pastor|mchungaji)\\b)\\b", Pattern.CASE_INSENSITIVE);

    /** Someone stopping insulin or other medicine, or relying on prayer alone instead of treatment: fixed referral before the model. */
    private static final Pattern HEALTH_STOP_PATTERN = Pattern.compile(
            "\\b(?:stop|stopping|stopped|skip|skipping|quit|quitting|kuacha|anaacha|anaacha|kukataa)\\b.{0,30}\\b(?:insulin|medicine|medication|tablets?|dawa|arvs?|antiretrovirals?|chemo\\w*)\\b", Pattern.CASE_INSENSITIVE);

    public static boolean isHealthStopQuery(@Nullable String query) {
        return query != null && HEALTH_STOP_PATTERN.matcher(query).find();
    }

    public static AiResponse healthStopResponse() {
        return AiResponse.plainText(
                "Please do not stop insulin or any other medicine without speaking to a doctor or nurse today. If someone is very unwell, confused, or vomiting, call 999 or 112 now, or go to the nearest clinic or hospital. Prayer and the support of the church can go alongside medical care, but not instead of it. Please take this to a doctor or nurse at the nearest clinic, and speak with a trusted church leader who can help you go there.",
                Arrays.asList("🙏 Open prayer support", "📖 Open the Bible"));
    }

    /** Pressure to pay out of fear, or setting Scripture aside for a claimed word: fixed wording before the model. */
    private static final Pattern MANIPULATION_PATTERN = Pattern.compile(
            "\\b(?:prophet|prophetess|pastor|apostle|bishop|man of god|mchungaji|nabii)\\b.{0,160}\\b(?:money|pay|offering|seed|give|pesa|sadaka)\\b.{0,120}\\b(?:children|child|family|die|curse|death|sick)\\b|" +
            "generational curse.{0,120}\\b(?:pay|money|how much)\\b|deliverance.{0,80}\\bhow much\\b|" +
            "\\bignore (?:the )?scripture\\b|set aside scripture|holy spirit (?:told|said|tells|says)\\b.{0,80}\\b(?:different|contrary|against)\\b", Pattern.CASE_INSENSITIVE);

    public static boolean isManipulationQuery(@Nullable String query) {
        return query != null && MANIPULATION_PATTERN.matcher(query).find();
    }

    public static AiResponse manipulationResponse() {
        return AiResponse.plainText(
                "You do not have to pay anyone to protect your children or to remove a curse. Money given out of fear or pressure is not required. A warning that a child will die unless you pay is pressure, not the truth of God, and Christ has power over every curse. Scripture is the test for any word or experience, because the Holy Spirit never contradicts the Word. Don't act on fear, and don't set Scripture aside. Please discern what you heard with mature believers, and talk with a trusted church leader or the Bishop before you give or decide anything.",
                Arrays.asList("🙏 Open prayer support", "📖 Open the Bible"));
    }

    /** Requests to set the translation rules aside, or to use translations outside the guide's list. */
    private static final Pattern TRANSLATION_POLICY_PATTERN = Pattern.compile(
            "ignore (?:your|the) (?:rules|guidelines|instructions)|" +
            "\\b(?:ESV|NIV|NKJV)\\b.{0,60}\\b(?:allowed|allow|quote|in the app)\\b|" +
            "\\b(?:allowed|allow)\\b.{0,60}\\b(?:ESV|NIV|NKJV)\\b", Pattern.CASE_INSENSITIVE);

    public static boolean isTranslationPolicyQuery(@Nullable String query) {
        return query != null && TRANSLATION_POLICY_PATTERN.matcher(query).find();
    }

    public static AiResponse translationPolicyResponse() {
        return AiResponse.plainText(
                "The guide can quote only the King James Version (KJV) and the NASB. It cannot quote other translations, and I cannot change this rule, even if asked. Please speak with a trusted church leader if you have questions about translations.",
                Arrays.asList("📖 Open the Bible", "🙏 Open prayer support"));
    }

    /** Requests to forge a letter, approval or membership in a leader's name. */
    private static final Pattern FORGERY_PATTERN = Pattern.compile(
            "\\b(?:fake|forged?|forging|false) (?:letter|message|note|certificate|approval|membership)\\b|" +
            "letter from the (?:bishop|pastor|elder|church)|in the bishop'?s name|approving my membership", Pattern.CASE_INSENSITIVE);

    public static boolean isForgeryQuery(@Nullable String query) {
        return query != null && FORGERY_PATTERN.matcher(query).find();
    }

    public static AiResponse forgeryResponse() {
        return AiResponse.plainText(
                "I cannot write a letter in the Bishop's name, and I am not able to approve membership or sign for anyone. Please contact the church office, who can confirm membership through the proper channel. This guide cannot produce that document.",
                Arrays.asList("🙏 Open prayer support", "📖 Open the Bible"));
    }

    /** Karma and reincarnation questions: identify the teaching and refute it from Scripture (fixed wording). */
    private static final Pattern REINCARNATION_PATTERN = Pattern.compile(
            "reincarnat\\w*|reincarnashun|\\bkarma\\b|sowin and reapin|kufa.{0,40}zaliwa tena|tukifa.{0,40}zaliwa tena", Pattern.CASE_INSENSITIVE);

    public static boolean isReincarnationQuery(@Nullable String query) {
        return query != null && REINCARNATION_PATTERN.matcher(query).find();
    }

    public static AiResponse reincarnationResponse() {
        return AiResponse.plainText(
                "Karma and reincarnation are taught in Hinduism and Buddhism. They say a person is reborn again and again until they reach release. Scripture teaches something different. Hebrews 9:27 says people die once, and after this comes judgment. Galatians 6:7 teaches that we reap what we sow, but Ephesians 2:8–9 says we are saved by grace through faith, not by the cycle of karma. Christ died once for our sins (Hebrews 9:28). Test what you hear by Scripture, and talk with a trusted church leader or your pastor about it.",
                Arrays.asList("📖 Open the Bible", "🙏 Open prayer support"));
    }

    public static boolean isSafeguardingDisclosure(@Nullable String query) {
        if (query != null && HOW_CHURCH_QUESTION.matcher(query).find()) return false;
        return query != null && SAFEGUARDING_PATTERN.matcher(query).find();
    }

    /** A process question about a church ("how should a church handle...") is not a personal disclosure. */
    private static final Pattern HOW_CHURCH_QUESTION = Pattern.compile(
            "(?:how|what) should (?:a|the|our) church", Pattern.CASE_INSENSITIVE);

    private static final Pattern CHILD_CUE = Pattern.compile(
            "\\b(?:child|children|kid|kids|minor|minors|under (?:18|eighteen)|teen|teens|teenager|toddler|baby|babies|nephew|niece|"
            + "pupil|learner|boy|girl|daughter|son|grandchild|grandson|granddaughter)\\b", Pattern.CASE_INSENSITIVE);

    /** Adult disclosure wording: no child helpline as the lead line, and a trusted adult outside the church. */
    public static AiResponse safeguardingResponse(@Nullable String query) {
        if (query != null && CHILD_CUE.matcher(query).find()) return safeguardingResponse();
        return AiResponse.plainText(
                "I'm sorry this happened to you. What was done to you is not your fault. If you are in immediate danger, call 999 or 112 "
                + "now, or go to the nearest police station or hospital, and ask for help with what happened. A doctor or clinic can also "
                + "check your health and help you get support. Please tell a trusted person outside the church, such as a family member "
                + "or friend, who can stay with you. You do not have to keep silent, and no one should ask you to forgive before you are "
                + "ready or to stay quiet about it. For gender-based violence counselling and referral, call 1195, the National GBV Helpline, open 24 hours. "
                + "This guide cannot investigate, promise confidentiality, or act for you.",
                Arrays.asList("🙏 Open prayer support", "📖 Open the Bible"));
    }

    /** Drug-related questions: point to NACADA and the police, never a scope refusal. */
    private static final Pattern DRUG_CUE = Pattern.compile(
            "\\b(?:drugs?|bhang|cannabis|marijuana|weed|miraa|heroin|cocaine|narcotics?|addict(?:ion|ed)?)\\b", Pattern.CASE_INSENSITIVE);

    /** Miraa, khat and muguka questions go to the approved khat route unless they describe selling or trafficking. */
    private static final Pattern MIRAA_CUE = Pattern.compile("\\b(?:miraa|khat|muguka)\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern SELLING_CUE = Pattern.compile(
            "\\b(?:sell|selling|sells|deal|dealing|traffick\\w*|smuggl\\w*|supply|supplying)\\b", Pattern.CASE_INSENSITIVE);

    public static boolean isDrugSafetyQuery(@Nullable String query) {
        if (query == null) return false;
        String withoutMiraa = MIRAA_CUE.matcher(query).replaceAll(" ");
        return DRUG_CUE.matcher(withoutMiraa).find() || (DRUG_CUE.matcher(query).find() && SELLING_CUE.matcher(query).find());
    }

    /** Requests to search the live web or current news. Declined before the Bible cues (see preModelRoute). */
    private static final Pattern WEB_SEARCH_REQUEST = Pattern.compile(
            "\\bsearch (?:for )?(?:the )?(?:internet|web|google|online|current|latest|recent|today)\\b|\\bcurrent (?:news|information)\\b|\\blatest news\\b",
            Pattern.CASE_INSENSITIVE);

    /** Spreadsheet, file and calculation requests: declined as tool tasks. */
    private static final Pattern TOOL_TASK_REQUEST = Pattern.compile(
            "\\b(?:spreadsheet|excel|xlsx|csv|uploaded (?:file|document|photo)|calculate (?:the )?(?:totals?|sums?))\\b",
            Pattern.CASE_INSENSITIVE);

    /** Scripture-topic words that are in scope even though they are not in the general cue list. */
    private static final Pattern BIBLE_TOPIC_CUE = Pattern.compile(
            "\\b(?:word search|search for|search the bible|count the word|how many times|angels?|archangel|agape|agap\\u0113|philia|pharaoh|pharoah|lord,? lord|calls? (?:me|him|jesus|the lord) lord|resurrection|augustine|"
            + "ignatius|justin martyr|tacitus|nicaea|nicea|church fathers?|pre-nicene|ante-nicene|miraa|khat|muguka|uzazi wa mpango|waislamu|jirani zangu|muslim neighbou?rs?|kugongewa|kugongeana|nimegongewa|tukigongeana|mpango wa kando)\\b|"
            // Added 2026-11-25 (round 48): end-times, New Testament reliability, Easton and commentary requests, and the
            // witchcraft and theft requests that have approved routes.
            + "\\blast days|\\bantichrist|\\btongues?\\b|\\bnew testament\\b|\\breliab\\w* (?:of |in )?(?:the )?(?:texts?|manuscripts?|scriptures?|bible)\\b|"
            + "\\beaston'?s?\\b|\\bcross-references?\\b|\\bwesley'?s? note|\\bmatthew henry\\b|\\bmganga\\b|\\bwitch ?doctor\\b|"
            + "\\bremove (?:a |the |my )?curse\\b|\\bchama\\b|\\btreasurer\\b|\\bran away with\\b|"
            + "\\b(?:parents? (?:are )?separat\\w*|come.we.stay|cohabit\\w*|cannot conceive|can'?t conceive|infertil\\w*|afford (?:the |a )?wedding|chronic illness|disabled|disability|cannot afford|can'?t afford)\\b|"
            // Added 2026-11-25 for the marriage and family set (Levi's sections 4-8): these are in scope for the approved routes.
            + "\\b(?:marry|married|marriage|singl\\w*|ndoa|trap|mume|mke|wanaume|wanawake|mchumba|mahari|familia|ananisaliti|kusaliti|"
            + "ujumbe wa kimapenzi|mama yangu|mama mkwe|mother-in-law|in-laws?|mkwe|hafanyi|anafanya kila kitu|kazi za nyumbani|chores|housework|"
            + "household|cheat\\w*|cheated|revenge|final word|manipulative|inferior|superior|feminis\\w*|sexis\\w*|controls? (?:all )?(?:the )?money|"
            + "won'?t let me (?:work|decide)|obey everything|must obey|proving|unfaithful\\w*|adulter\\w*|affair|divorc\\w*|remarr\\w*|50/50|"
            + "split (?:of )?(?:every|the) bill|earns more|makes more|sleeping with|sleep with|bro|sis|ruracio|roora|lobola|dowry|bride|"
            + "vasectomy|injection|sterili[sz]\\w*|contracept\\w*|forces? me|mother|father|dad|mum|mama|baba|waislamu|sisi)\\b|"
            + "rogw|uchawi|mchawi|nimeroga|manabii|nabii|tiktok|youtube|gold dust|visible sign|false teacher|on tv|on television|preacher i saw|judas|keep sinning|unforgivable|place in heaven|salvaiton|salvtion|sin on purpose|aposttae|icons?|explain (?:it )?(?:differently|again|simply)|(?:don.t|do not|dont|udnerstand|understand) (?:understand|udnerstand)|what do you mean", Pattern.CASE_INSENSITIVE);

    public static AiResponse drugSafetyResponse() {
        return AiResponse.plainText(
                "Thank you for telling me. If a young person is in danger, call 999 or 112 now. If someone is selling drugs, report it to "
                + "the police on 999 or 112, or to the nearest police station. For drug or alcohol use, NACADA on 1192 can help. If a child "
                + "may be at risk, call 116 (Childline Kenya). Please tell a trusted adult outside the situation, who can help you decide "
                + "what to do next. This guide cannot investigate, act for you, or promise confidentiality.",
                Arrays.asList("🙏 Open prayer support", "📖 Open the Bible"));
    }

    public static AiResponse safeguardingResponse() {
        return AiResponse.plainText(
                "I'm sorry this is happening to you or to someone you care about. If anyone is in immediate danger, call 999 or 112 now, or go to the nearest police station or hospital. If a child may be at risk, call the Childline Kenya helpline on 116. Tell a trusted adult who can keep you or the child safe. This guide cannot investigate, promise confidentiality, or act for you. Please also contact the church office or a trusted leader about safeguarding, and do not wait for an app reply.",
                Arrays.asList("🙏 Open prayer support", "📖 Open the Bible"));
    }

    /**
     * Requests for other people's private data, or for the guide's own chat or recording records, are refused
     * before the model runs. A claimed role (pastor, leader, admin) grants nothing: the same refusal applies.
     */
    private static final Pattern PRIVACY_DATA_VERB = Pattern.compile(
            "\\b(?:show|give|tell|list|reveal|check|find|send|forward|access|look ?up|what is|what's|" +
            "log ?in|log into|who|what did|what does|what has|what was|how much|the address|the phone|" +
            "the password)\\b", Pattern.CASE_INSENSITIVE);
    /** Strong signals that a request is for personal records, even when it also asks for prayer or support. */
    private static final Pattern PRIVACY_STRONG = Pattern.compile(
            "\\b(?:password|phone number|address|records?|counsell?ing|conversations?|chats?|contact list|contacts|" +
            "account|login|log in|said about me|wrote about me|asked the ai|ask the ai|asked the guide|" +
            "hiv status|hiv test|hiv result|donations?|gave|give the most)\\b", Pattern.CASE_INSENSITIVE);
    /** Pastoral support wording (prayer, advice, comfort). A support request is not a data request unless it is strong. */
    private static final Pattern PRIVACY_SUPPORT_GUARD = Pattern.compile(
            "\\b(?:pray (?:for|with)|how (?:do|can|should|to)|what should i|advise me|encourage|comfort|support)\\b",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern PRIVACY_PERSONAL_DATA = Pattern.compile(
            "\\b(?:phone|number|address|password|email|records?|counsell?ing|counsel|conversations?|conv\\w*|chats?|messages?|personal|" +
            "confession|prayer requests?|donations?|donated|gave|give the most|giving|money|hiv|health|medical|diagnos\\w*|" +
            "contact list|contacts|account|login|log in|private|secret|said about me|wrote about me|asked the ai|" +
            "ask the ai|asked the guide|asked the ai|posted|history)\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern PRIVACY_THIRD_PARTY = Pattern.compile(
            "\\b(?:members?|memb\\w*|mem\\w*b\\w*|someone|somebody|anyone|everyone|another|other (?:members?|people|users?|person)|last \\w+|pes\\w+|" +
            "other user|another user|people|person|persons?|pastor|pastors?|wife|husband|sister|brother|friend|" +
            "neighbou?r|family|group|leaders?|elders?|deacons?|youth|last person|previous user|congregation|" +
            "parent|colleague|mother|father|children|child|boss|employer|pastor's|member's|church member)\\b",
            Pattern.CASE_INSENSITIVE);
    /** Asks what the guide, or any system, holds about the person, records them, or deletes their chat history. */
    private static final Pattern PRIVACY_MEMORY_OR_RECORD = Pattern.compile(
            "\\b(?:what|anything) (?:do|did|can|could) you (?:remember|recall|know|keep)\\b|" +
            "\\bam i being (?:recorded|monitored|watched|tracked|spied)\\b|" +
            "\\bare you (?:recording|monitoring|tracking|watching)\\b|" +
            "\\bis (?:this|the chat|the conversation|our chat) (?:being )?(?:recorded|monitored|tracked)\\b|" +
            "\\bwhat the guide remembers\\b|\\bwhat (?:it|you|the guide|this guide) (?:remember|rmeember|rememeber|rember|remeber|rmember|remembers|rmeembers|remembr)s?\\b|" +
            "\\bremind me (?:of|what)\\b|\\bwhat have we talked about\\b|\\bour last (?:session|conversation|chat)\\b|" +
            "\\bwhat (?:did|do) you (?:remember|recall|know) (?:about|of)\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern PRIVACY_DELETE_HISTORY = Pattern.compile(
            "\\b(?:delete|erase|clear|wipe|remove|forget)\\s+(?:all\\s+)?(?:my|our|this|the)\\s+" +
            "(?:conversation|conv\\w*|chat|chats|messages|memory|notes)(?:\\s+(?:history|hist\\w*))?\\b|" +
            "\\b(?:delete|erase|clear|wipe)\\s+(?:all\\s+)?(?:my\\s+)?(?:conversation|chat)\\s+history\\b|" +
            "\\b(?:delete|erase|clear|wipe)\\s+(?:my|our|this)\\s+history\\b", Pattern.CASE_INSENSITIVE);

    /** Another person's private information, chats, counselling, health, contact or account details. */
    private static final Pattern PRIVACY_EXPLICIT = Pattern.compile(
            "\\bread (?:my |her |his |their |the |our )?(?:\\w+'?s )?(?:phone|messages|texts|chats|whatsapp|emails?|inbox|dms?)\\b|" +
            "\\b(?:wife|husband|partner|spouse|girlfriend|boyfriend|friend|brother|sister)'?s? (?:phone|messages|texts|chats|whatsapp)\\b|" +
            "\\b(?:phone number|namba ya simu|contact details|address|home address) (?:of|for) (?:pastor|a member|the member|members?|someone|somebody|my pastor|the pastor|elder|deacon|\\w+ in \\w+)\\b|" +
            "\\b(?:list|tell me which|which|orodha ya|majina ya)\\b.{0,40}\\b(?:members?|youth group|wanachama|congregation)\\b|" +
            "\\bmembers? .{0,60}\\b(?:struggling|pornography|addiction|debt|hiv|pregnan\\w*)\\b", Pattern.CASE_INSENSITIVE);

    public static boolean isPrivacyDataRequest(@Nullable String query) {
        if (query == null || query.trim().isEmpty()) return false;
        String normalized = query.replace('\u2019', '\'').replaceAll("\\s+", " ");
        // Explicit shapes from the Kenya sets: reading someone's phone or messages, a member's number, a member list.
        if (PRIVACY_EXPLICIT.matcher(normalized).find()) return true;
        if (!PRIVACY_DATA_VERB.matcher(normalized).find()
                || !PRIVACY_PERSONAL_DATA.matcher(normalized).find()
                || !PRIVACY_THIRD_PARTY.matcher(normalized).find()) return false;
        return !PRIVACY_SUPPORT_GUARD.matcher(normalized).find() || PRIVACY_STRONG.matcher(normalized).find();
    }

    public static boolean isDeleteHistoryRequest(@Nullable String query) {
        return query != null && PRIVACY_DELETE_HISTORY.matcher(query).find();
    }

    public static boolean isMemoryOrRecordingQuestion(@Nullable String query) {
        return query != null && PRIVACY_MEMORY_OR_RECORD.matcher(query).find();
    }

    /** Every privacy route: other people's data, the guide's memory or recording questions, and chat deletion. */
    public static boolean isOtherPersonDataRequest(@Nullable String query) {
        return isPrivacyDataRequest(query) || isDeleteHistoryRequest(query) || isMemoryOrRecordingQuestion(query);
    }

    /** Fixed privacy wording. Says what is refused and the real procedure, and never invents a contact. */
    public static AiResponse privacyResponse() {
        return AiResponse.plainText(
                "This is a privacy matter. I cannot share, search for or act on another person's private information. I am not authorised to share that. That includes their conversations, counselling, health details, phone number, address, password, giving or prayer requests. A claimed role, such as pastor or leader, does not change this. For a pastoral matter, please contact the church office or your pastor through the usual channel.",
                Arrays.asList("🏛️ Church information", "🙏 Prayer and fellowship"));
    }

    /** The app's real procedure for clearing this chat and for removing the guide's notes on this phone. */
    public static AiResponse deleteHistoryResponse() {
        return AiResponse.plainText(
                "I cannot delete records kept by the church, and I cannot see other systems. To clear this conversation, open Choose your guide and tap Clear this conversation. To remove what the guide has learned about you on this phone, open What the guide remembers and tap Forget everything. Nothing is uploaded, and the app's Bible and help content stay as they are.",
                Arrays.asList("📖 Read Scripture", "🏛️ Church information"));
    }

    /** Recording and memory questions: the guide's notes stay on this phone. It cannot confirm other systems. */
    public static AiResponse memoryResponse() {
        return AiResponse.plainText(
                "Your guide notes stay on this phone. You can review them under Choose your guide, then What the guide remembers, and tap Forget everything to remove them. I cannot see or share other people's chats, and I cannot confirm whether any other system records you. For a privacy concern, please contact the church office.",
                Arrays.asList("🏛️ Church information", "📖 Read Scripture"));
    }

    /** Pre-model privacy route (null when the question is not a privacy request). */
    @Nullable
    public static AiResponse privacyRouteResponse(@Nullable String query) {
        if (isDeleteHistoryRequest(query)) return deleteHistoryResponse();
        if (isMemoryOrRecordingQuestion(query)) return memoryResponse();
        if (isPrivacyDataRequest(query)) return privacyResponse();
        return null;
    }

    /** A negation just before a crisis phrase, e.g. "I'm not thinking of ending my life" (Levi, 2026-11-25). */
    private static final Pattern NEGATED_BEFORE = Pattern.compile(
            "\\b(?:not|never|no longer|don'?t|do not|am not|i'm not|im not|won'?t)\\b[^.?!,;]{0,30}$", Pattern.CASE_INSENSITIVE);
    /** Words that mark a poem, story, phrase or meaning question. */
    private static final Pattern LITERARY_CONTEXT = Pattern.compile(
            "\\b(?:poem|poetry|novel|story|stories|character|song|lyrics|book|film|movie|play|meaning|phrase|quote|quoted|essay|exam)\\b",
            Pattern.CASE_INSENSITIVE);
    /** Signs that a real person is involved, so the phrase is a real warning rather than a literary question. */
    private static final Pattern REAL_PERSON_CONTEXT = Pattern.compile(
            "\\b(?:my|our) (?:friend|brother|sister|husband|wife|son|daughter|mother|father|mum|dad|child|neighbou?r|colleague|pastor|boyfriend|girlfriend|cousin|uncle|aunt|relative|student|classmate)\\b|" +
                    "\\b(?:texted|messaged|told me|said to me|wrote to me|called me|he texted|she texted|he said|she said)\\b|" +
                    "\\b(?:i|i'm|i am|i will|i'll|tonight|today|right now)\\b", Pattern.CASE_INSENSITIVE);

    /**
     * Context for crisis wording (Levi's decision, 2026-11-25). Returns "crisis-denial" when every crisis phrase is negated,
     * or "crisis-literary" for a poem, story or meaning question with no real person involved. Returns null for everything
     * else, which keeps the full crisis response. A real warning, even one quoted from a text message, is never literary.
     */
    @Nullable
    public static String crisisContextRoute(@Nullable String query) {
        if (query == null) return null;
        String normalized = query.replace('’', '\'').replaceAll("\\s+", " ");
        if (CRISIS_DISCLOSURE_PATTERN.matcher(normalized).find() || SELF_HARM_TYPO_PATTERN.matcher(normalized).find()
                || isMeansAndDistress(normalized)) {
            return null;
        }
        java.util.regex.Matcher m = CRISIS_PATTERN.matcher(normalized);
        boolean any = false;
        boolean allNegated = true;
        while (m.find()) {
            any = true;
            if (!NEGATED_BEFORE.matcher(normalized.substring(0, m.start())).find()) allNegated = false;
        }
        if (!any) return null;
        if (allNegated) return "crisis-denial";
        if (LITERARY_CONTEXT.matcher(normalized).find() && !REAL_PERSON_CONTEXT.matcher(normalized).find()) {
            return "crisis-literary";
        }
        return null;
    }

    public static AiResponse crisisDenialResponse() {
        return AiResponse.plainText(
                "Thank you for telling me that you are not thinking of ending your life. I want to check gently: are you safe right now? "
                + "If thoughts of ending your life come back, or you feel hopeless, please tell a trusted person now. If you may be in danger, call 999 or 112 now. "
                + "For emotional support you can call Befrienders Kenya on +254 722 178 177, which is open on weekdays from 9am to 5pm. "
                + "You can also talk with your pastor or a trusted church leader.",
                Arrays.asList("🙏 Open prayer support", "📖 Open the Bible"));
    }

    public static AiResponse crisisLiteraryResponse() {
        return AiResponse.plainText(
                "This sounds like a question about a poem, a novel or a character, so I will explain the phrase. In stories, 'ending my life' "
                + "usually means that a character is thinking about suicide, and writers treat that theme with care. If someone in your own life "
                + "has said this to you, tell me, and I will give you the urgent steps for helping them. If you are feeling this way yourself, "
                + "please tell a trusted person now, and call 999 or 112 if you may be in danger.",
                Arrays.asList("🙏 Open prayer support", "📖 Open the Bible"));
    }

    /** Violence and abuse wording. Matched in the speaker's own situation only; a general question about abuse is a topic. */
    private static final Pattern VIOLENCE_TERMS = Pattern.compile(
            "\\b(?:domestic violence|physically abused|sexual assault|sexually assaulted|abus\\w*|beats? me|beating me|hits? me|" +
                    "ananipiga|ananinyanyasa|ananitishia|ananiua|ananibaka|kunipiga|ninapigwa|ninanyanyaswa|" +
                    "hitting me|struck me|choked me|attacked me|threat\\w* (?:to )?(?:kill |hurt |harm )?me|rape\\w*|molest\\w*|" +
                    "locked (?:me|us) (?:in|up)|forced me|(?:immediate|imminent) danger|going to kill (?:me|us))\\b",
            Pattern.CASE_INSENSITIVE);
    /** The speaker's own situation, or a real person they are describing. */
    private static final Pattern SPEAKER_SITUATION = Pattern.compile(
            "\\b(?:i|i'm|i am|i've|i'll|me|my|our|us|we)\\b", Pattern.CASE_INSENSITIVE);
    /**
     * Routes crisis, abuse and violence wording to separate responses (Levi's review on PR 83, 2026-11-25).
     * Returns "crisis", "crisis-third-party", "crisis-denial", "crisis-literary", "crisis-and-violence",
     * "violence-safeguarding", "topic-violence" (a general question about abuse, which goes on to the approved routes),
     * or null when neither suicide nor violence wording is present. Abuse wording never gets a suicide-plan question
     * unless suicidal wording is also present.
     */
    @Nullable
    public static String safetyRoute(@Nullable String query) {
        if (query == null || query.trim().isEmpty()) return null;
        String n = query.replace('’', '\'').replaceAll("\\s+", " ");
        boolean violence = CRISIS_DISCLOSURE_PATTERN.matcher(n).find()
                || (VIOLENCE_TERMS.matcher(n).find() && SPEAKER_SITUATION.matcher(n).find());
        String kind = suicideKind(n);
        if (!violence) {
            if ("none".equals(kind)) return null;
            if ("topic".equals(kind)) return "topic-violence";
            if ("real".equals(kind) && isGeneralSuicideQuestion(n)) return "general-topic";
            if ("real".equals(kind)) return isThirdPartyFraming(n) ? "crisis-third-party" : "crisis";
            return "crisis-" + kind; // crisis-denial or crisis-literary
        }
        if ("real".equals(kind)) return isThirdPartyFraming(n) ? "crisis-third-party" : "crisis-and-violence";
        return "violence-safeguarding";
    }

    /** "none", "topic" (only violence-topic wording), "denial", "literary" or "real" (suicidal wording in the speaker's words). */
    private static String suicideKind(String n) {
        if (SELF_HARM_TYPO_PATTERN.matcher(n).find() || isMeansAndDistress(n)) return "real";
        boolean suicideHit = false;
        boolean anyHit = false;
        boolean allNegated = true;
        java.util.regex.Matcher m = CRISIS_PATTERN.matcher(n);
        while (m.find()) {
            anyHit = true;
            if (VIOLENCE_TERMS.matcher(m.group()).find()) continue;
            suicideHit = true;
            if (!NEGATED_BEFORE.matcher(n.substring(0, m.start())).find()) allNegated = false;
        }
        if (!anyHit) return "none";
        if (!suicideHit) return "topic";
        if (allNegated) return "denial";
        if (LITERARY_CONTEXT.matcher(n).find() && !REAL_PERSON_CONTEXT.matcher(n).find()) return "literary";
        return "real";
    }

    private static final Pattern GENERAL_SUBJECT = Pattern.compile(
            "\\b(?:clergy|pastors?|church leaders?|elders?|leaders|youth|teenagers?|young people|students|believers|christians|"
                    + "people|men|women|members|communities|society|many)\\b|\\bin our (?:area|church|community)\\b",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern NAMED_RELATION = Pattern.compile(
            "\\bmy\\b|\\b\\w+ yangu\\b|\\b\\w+ wangu\\b", Pattern.CASE_INSENSITIVE);

    /**
     * A general or educational question about suicide or depression among clergy or groups: no reporter statement,
     * no reported speech, no named relation. Levi's rule: a general mention is not a crisis. It gets the approved pastoral answer.
     */
    static boolean isGeneralSuicideQuestion(String n) {
        return !SELF_STATEMENT.matcher(n).find()
                && !REPORTED_SPEECH.matcher(n).find()
                && !NAMED_RELATION.matcher(n).find()
                && GENERAL_SUBJECT.matcher(n).find();
    }

    /**
     * Reported speech: "texted me ...", "told me ...", "says ...", "ameniambia ...", up to the end of the sentence.
     * "I told ..." is not reported speech, because the reporter is speaking about themselves.
     */
    private static final Pattern REPORTED_SPEECH = Pattern.compile(
            "(?<![Ii] )\\b(?:texted|messaged|wrote|told|sent|posted|called|ameniambia|ananiambia|ananitumia|aliniandikia|aliniambia|"
                    + "anasema|alisema|amesema|says|said|saying)\\b[^.!?\\n]{0,200}",
            Pattern.CASE_INSENSITIVE);
    /** Help-seeking questions such as "what should I do" name the reporter but do not state their own risk. */
    private static final Pattern HELP_SEEKING = Pattern.compile(
            "\\b(?:what|how|should|do|can|must|would|shall|may|where|when|who)\\s+i\\b", Pattern.CASE_INSENSITIVE);
    /** Any statement by the reporter about themselves, including "Im", "I am", "me" and Swahili forms. */
    private static final Pattern SELF_STATEMENT = Pattern.compile(
            "\\bi\\b|\\bim\\b|\\bme\\b|\\bmy life\\b|\\bmyself\\b|\\bmimi\\b|\\bnina\\b|\\bnataka\\b|\\bnimechoka\\b|\\bmaisha yangu\\b",
            Pattern.CASE_INSENSITIVE);
    /** The person at risk is someone else: a named relation, a third-person pronoun, or a Swahili third-person form. */
    private static final Pattern OTHER_PERSON = Pattern.compile(
            "\\b(?:he|she|they|him|her|his|their|them|someone|friend|brother|sister|son|daughter|husband|wife|mum|mom|dad|father|mother|"
                    + "kaka|dada|rafiki|mume|mke|mtoto|anasema|anajiua|atajiua|anataka|atataka|ananiambia|ananitumia)\\b|"
                    + "\\b\\w+ yangu\\b|\\b\\w+ wangu\\b",
            Pattern.CASE_INSENSITIVE);

    /**
     * True when the suicidal words are about someone else. Default rule: a named or pronoun-marked other person is the subject,
     * or the words are reported, and the reporter says nothing about their own risk. Quotes, reported speech, help-seeking
     * questions and punctuation or spelling variants (Im, I'm, I’m, I am) are all handled.
     */
    static boolean isThirdPartyFraming(String n) {
        String s = n.replace('“', '"').replace('”', '"').replace('‘', '\'').replace('’', '\'');
        s = s.replaceAll("\"[^\"]*\"", " ");
        s = s.replaceAll("(?:^|[\\s,(])'[^']{0,200}'(?=[\\s.,!?)]|$)", " ");
        boolean reported = REPORTED_SPEECH.matcher(s).find();
        s = REPORTED_SPEECH.matcher(s).replaceAll(" ");
        s = HELP_SEEKING.matcher(s).replaceAll(" ");
        if (SELF_STATEMENT.matcher(s).find()) return false;
        return reported || OTHER_PERSON.matcher(s).find();
    }

    public static AiResponse violenceSafeguardingResponse() {
        return AiResponse.plainText(
                "I'm sorry this is happening to you. What is being done to you is not your fault, and abuse is never acceptable. "
                + "Are you in immediate danger from someone else right now, and can you reach a safe place? "
                + "If you are in danger, call 999 or 112 now, or go to the nearest police station or hospital. If you can, go to a safe place now, "
                + "and tell a trusted person where you are. For gender-based violence counselling and referral, call 1195, the National GBV Helpline, open 24 hours. "
                + "If a child may be at risk, call the Childline Kenya helpline on 116. Do not confront the person alone, and do not agree to joint "
                + "counselling while you are unsafe. Your safety comes before keeping the peace or forgiveness. This guide cannot investigate, "
                + "promise confidentiality or act for you, so do not wait for an app reply in an emergency.",
                Arrays.asList("🙏 Open prayer support", "📖 Open the Bible"));
    }

    public static AiResponse crisisAndViolenceResponse() {
        return AiResponse.plainText(
                "I'm sorry you are going through this. You are describing two dangers, and both need attention. "
                + "First, if you are in immediate danger from someone else, call 999 or 112 now, and go to a safe place with a trusted person if you can. "
                + "For gender-based violence counselling, call 1195, the National GBV Helpline, open 24 hours. "
                + "Second, you told me about ending your life. Are you safe right now? Do you have a plan or a way to end your life tonight? "
                + "If you feel you may act on this, call 999 or 112 now, or go to the nearest hospital. Please tell a trusted person now.",
                Arrays.asList("🙏 Open prayer support", "📖 Open the Bible"));
    }

    public static AiResponse crisisThirdPartyResponse() {
        return AiResponse.plainText(
                "I'm sorry you are worried about someone. If they may act on these words now, call 999 or 112 now, or go to the nearest hospital or police station. "
                + "Please contact them right away and ask directly whether they are safe and where they are. If it is safe for you to do so, stay with them or ask a safe adult to stay with them. "
                + "Do not put yourself in danger, and if there is violence nearby, call the emergency numbers instead of going in. "
                + "Befrienders Kenya (+254 722 178 177) offers emotional support on weekdays from 9am to 5pm, but it is not an emergency service. "
                + "You can also talk with your pastor or a trusted church leader about how to support them.",
                Arrays.asList("🙏 Open prayer support", "📖 Open the Bible"));
    }

    /**
     * The legacy keyword crisis check. It applies to any crisis-keyword wording except a violence-topic route
     * (domestic-violence theology, for example) or a general question (Levi, 2026-11-25).
     */
    public static boolean isLegacyCrisisKeyword(@Nullable String query) {
        if (!isCrisisQuery(query)) return false;
        String route = safetyRoute(query);
        return !"topic-violence".equals(route) && !"general-topic".equals(route);
    }

    /**
     * A first-person statement of intent to harm or end one's own life, in any of the usual phrasings (2026-11-25 safety run:
     * "I want to stab myself", "I will hang myself this weekend", "I wish I was dead"). Only adds crisis routes.
     */
    private static final Pattern SELF_HARM_INTENT = Pattern.compile(
            "\\bi\\s+(?:will|am going to|am gonna|i'?m going to|i'?m gonna|want to|wanna|plan to|might|gonna|need to)\\s+(?:\\w+\\s+){0,2}?"
            + "(?:kill|hurt|harm|stab|cut|shoot|hang|drown|strangle|poison|end)\\s+(?:my\\s*self|my life)\\b|"
            + "\\b(?:kill|hurt|harm|stab|shoot|hang|drown|strangle|poison)\\s+myself\\b|"
            + "\\bi\\s+(?:wish|want|hope)\\s+(?:i\\s+)?(?:was|were|to)\\s+(?:dead|die|gone|not here)\\b|"
            + "\\bi\\s+(?:want|wish|hope)\\s+to\\s+die\\b|\\bi\\s+(?:wish|want)\\s+i\\s+(?:was|were)\\s+dead\\b|"
            + "\\bend\\s+(?:it|my\\s+life|everything)\\b", Pattern.CASE_INSENSITIVE);

    /**
     * General self-harm and suicide wording, in English and Swahili, that the patterns above miss: a means already at hand
     * ("I have the rope ready"), cutting, jumping, not wanting to live, feeling a burden, a goodbye letter (2026-11-25 safety run,
     * KE-088 to KB-093). Each alternative is anchored to the speaker or to a clear self-harm phrase, so a Bible question about
     * "the burden of sin" or "jumping for joy" does not match.
     */
    private static final Pattern SELF_HARM_GENERAL = Pattern.compile(
            "\\bi\\s+(?:do not|don'?t|no longer|can'?t|cannot|won'?t)\\s+want\\s+to\\s+(?:live|be alive|be here|exist|go on|wake up)\\b|" +
            "\\bi\\s+(?:do not|don'?t)\\s+want\\s+to\\s+be\\s+(?:a\\s+)?burden\\b|\\bi\\s+(?:am|'m)\\s+(?:only\\s+|just\\s+)?(?:a\\s+)?burden\\s+(?:to|on)\\b|" +
            "\\bnobody (?:would|will) miss me\\b|\\bno ?one (?:would|will) miss me\\b|\\bbetter off without me\\b|" +
            "\\b(?:going to|about to|thinking (?:of|about)|want to|plan to)\\s+jump(?:ing)?\\s+(?:off|from|in front)\\b|\\bi\\s+(?:am|'m)\\s+jumping\\s+off\\b|\\bjumping off\\b|" +
            "\\bi\\s+(?:have|'ve)\\s+decided\\s+to\\s+(?:die|end|kill)\\b|\\bsuicide note\\b|\\bgoodbye letter\\b|\\bletter of goodbye\\b|" +
            "\\b(?:hang|hanging)\\s+myself\\b|\\bnoose\\b|\\bi\\s+(?:have|got)\\s+the\\s+(?:rope|pills|tablets|gun|knife|poison)\\b|" +
            "\\b(?:rope|noose|pills|tablets|poison)\\s+(?:is\\s+|are\\s+)?ready\\b|" +
            "\\bcutting\\s+(?:my\\s+)?(?:arms?|wrists?|legs?|skin|self)\\b|\\bcut\\s+my\\s+(?:arms?|wrists?)\\b|" +
            "\\b(?:can(?:'|’)?t|cannot)\\s+(?:take|bear)\\s+(?:it|this|life)\\s+(?:any\\s*more|anymore|any\\s+longer)\\b|" +
            // Swahili: "sitaki kuishi" (I do not want to live), "nitajiua" / "najiua" (I will / I am killing myself), "nimeamua kufa"
            // (I have decided to die), "barua ya kuaga" (goodbye letter), "hakuna mtu atakayenikosa" (no one will miss me).
            "\\bsitaki\\s+(?:tena\\s+)?kuishi\\b|\\bnitajiua\\b|\\bnajiua\\b|\\bnataka\\s+kujiua\\b|\\bnimeamua\\s+kufa\\b|\\bnataka\\s+kufa\\b|" +
            "\\bbarua\\s+ya\\s+kuaga\\b|\\bhakuna\\s+mtu\\s+atakayenikosa\\b", Pattern.CASE_INSENSITIVE);

    public static boolean isCrisisQuery(@Nullable String query) {
        if (query == null) return false;
        String normalized = query.replace('’', '\'').replaceAll("\\s+", " ");
        return CRISIS_PATTERN.matcher(normalized).find() || CRISIS_DISCLOSURE_PATTERN.matcher(normalized).find()
                || SELF_HARM_TYPO_PATTERN.matcher(normalized).find() || isMeansAndDistress(normalized)
                || SELF_HARM_INTENT.matcher(normalized).find() || SELF_HARM_GENERAL.matcher(normalized).find();
    }

    /** Conservative pre-model route for personal pastoral disclosures; not a risk diagnosis. */
    public static boolean isSensitivePastoralDisclosure(@Nullable String query) {
        if (query == null || query.trim().isEmpty()) return false;
        // A question about what someone else says (e.g. "Our pastor says depression is a lack of faith. Is that true?") is a
        // doctrine question, not a personal disclosure, so it goes to the approved routes.
        if (SAYS_CLAIM_QUESTION.matcher(query).find() && !FIRST_PERSON_CONTEXT.matcher(query).find()) return false;
        // KB-047 (2026-11-25): a general question about suicide or depression among clergy or a group ("Clergy in our area...")
        // has no speaker disclosure and no named person, so it goes to the approved depression route, not the sensitive reply.
        if ("general-topic".equals(safetyRoute(query))) return false;
        // Added 2026-11-25 (MG-S15): a factual question about a spouse's conduct ("Is that adultery?") goes to the approved
        // infidelity route. A speaker who discloses their own distress, fear or danger still goes to the sensitive route.
        if (FACTUAL_INFIDELITY_QUESTION.matcher(query).find() && !PERSONAL_DISTRESS.matcher(query).find()) return false;
        return SENSITIVE_PASTORAL_REQUEST.matcher(query).find()
                || (PERSONAL_CONTEXT.matcher(query).find() && SENSITIVE_PASTORAL_TOPIC.matcher(query).find());
    }

    /** Requests for confession or absolution, which only God gives (Mark 2:7). */
    private static final Pattern ABSOLUTION_REQUEST = Pattern.compile(
            "absolution|absolve|hear my confession|give me (?:the )?forgiveness of (?:my )?sins", Pattern.CASE_INSENSITIVE);
    private static final String ABSOLUTION_TEXT = "I am not authorised to hear a confession or give absolution. "
            + "Only God forgives sins (Mark 2:7; 1 John 1:9), and a pastor can pray with you.";

    public static AiResponse sensitivePastoralResponse() {
        return AiResponse.plainText(
                "I'm sorry you're dealing with this. This AI cannot promise confidentiality or notify a pastor, and this message has not been sent for pastoral follow-up. If you want human support, choose a pastoral contact route shown in the app; I will not send it for you. If anyone is in immediate danger, contact local emergency services or go to a hospital now and tell a trusted person nearby.",
                Arrays.asList("🙏 Open pastoral contacts", "🙏 Open prayer support"));
    }

    /** Medical, legal and financial matters: these get the professional-referral boundary, not the church referral. */
    private static final Pattern PROFESSIONAL_MATTER = Pattern.compile(
            "\\b(?:medic\\w*|dose|dosage|diagnos\\w*|symptoms?|fever|chest pain|pills?|antibiotics?|malaria|paracetamol|ibuprofen|" +
            "legal|lawsuit|lawyer|sue|court|contract|agreement|invest\\w*|stocks?|shares|bitcoin|crypto\\w*|forex|bonds?|" +
            "portfolio|loans?|tax|financial|bank|money)\\b", Pattern.CASE_INSENSITIVE);
    /** Questions that ask the assistant to make a leadership decision about a person or membership. */
    private static final Pattern HUMAN_DECISION = Pattern.compile(
            "\\b(?:am i saved|is (?:he|she|they|my \\w+) saved|am i going to heaven|is (?:he|she|they) condemned|" +
            "(?:remove|expel|discipline|restore|reinstate|excommunicate)\\b.{0,40}\\b(?:member|membership|church)|" +
            "(?:accept|reject|approve|deny|revoke)\\b.{0,40}\\bmembership|" +
            "(?:should|can|may|will) (?:i|he|she) (?:be )?(?:ordained|appointed|a pastor|a deacon|an elder)|" +
            "(?:is|am) (?:i|he|she) (?:an? )?(?:accepted |official )?member)\\b", Pattern.CASE_INSENSITIVE);
    /** Replies that present a statement as officially approved or official Perazim position. */
    private static final Pattern CLAIMED_OFFICIAL_STATUS = Pattern.compile(
            "\\b(?:officially approved|approved by the bishop|is the official (?:position|doctrine|policy|teaching) of|" +
            "official church (?:position|policy) is(?! not))\\b", Pattern.CASE_INSENSITIVE);
    public static boolean isProfessionalMatterQuery(@Nullable String query) {
        return query != null && PROFESSIONAL_MATTER.matcher(query).find();
    }

    public static boolean isHumanDecisionQuery(@Nullable String query) {
        return query != null && HUMAN_DECISION.matcher(query.replace('’', '\'')).find();
    }

    /** Delegates to {@link AppActionClaims}: completed or promised app actions and human-authority claims. */
    public static boolean claimsAppActionOrHumanAuthority(@Nullable String answer) {
        return AppActionClaims.claimsAppActionOrHumanAuthority(answer);
    }

    /** True when the reply presents something as official Perazim position, policy or approval. */
    public static boolean claimsOfficialStatus(@Nullable String answer) {
        return answer != null && CLAIMED_OFFICIAL_STATUS.matcher(answer).find();
    }

    /** Authority class that only an owner-approved knowledge package may carry. */
    public static final String APPROVED_OFFICIAL_CLASS = "APPROVED_OFFICIAL";
    private static final Pattern CITATION = Pattern.compile("\\[([^@\\]]+)@([^:]+): ([^\\]]+)\\]");

    /**
     * Provenance check: an official-status claim is allowed only when the reply cites an evidence item that is
     * citation-eligible and marked {@link #APPROVED_OFFICIAL_CLASS} in trusted package metadata. Wording alone never counts.
     */
    public static boolean officialClaimHasApprovedProvenance(@Nullable String answer, @Nullable org.json.JSONArray evidence) {
        if (answer == null || evidence == null) return false;
        java.util.regex.Matcher m = CITATION.matcher(answer);
        while (m.find()) {
            String sourceId = m.group(1).trim();
            String version = m.group(2).trim();
            for (int i = 0; i < evidence.length(); i++) {
                org.json.JSONObject item = evidence.optJSONObject(i);
                if (item == null) continue;
                if (sourceId.equals(item.optString("sourceId"))
                        && version.equals(item.optString("version"))
                        && APPROVED_OFFICIAL_CLASS.equals(item.optString("authorityClass"))
                        && item.optBoolean("citationEligible", false)
                        && !isWithdrawn(item)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * An item that is superseded or revoked can never back an official claim, even if its own metadata says it is
     * approved. The check does not rely on upstream retrieval having filtered such items.
     */
    static boolean isWithdrawn(@NonNull org.json.JSONObject item) {
        return !item.optString("supersededBy", "").trim().isEmpty()
                || item.optBoolean("revoked", false)
                || !item.optString("revokedAt", "").trim().isEmpty();
    }

    @NonNull
    public static AiResponse professionalBoundaryResponse() {
        return AiResponse.plainText(
                "I am designed specifically to assist with matters relating to Perazim Mission Church (PMCI) and Christian faith, so I am not authorised to provide medical, legal or financial advice. Please consult a suitably qualified professional.",
                Arrays.asList("📖 Read Scripture", "🏛️ Church information", "🙏 Prayer and fellowship"));
    }

    @NonNull
    public static AiResponse humanDecisionResponse() {
        return AiResponse.plainText(
                "This decision belongs to church leadership, and I cannot make it. Please speak with a trusted church leader or an authorised leader.",
                Arrays.asList("🏛️ Church information", "🙏 Prayer and fellowship"));
    }

    /** Lower-cased text with emoji, punctuation and extra spaces removed, so chips and typed text match. */
    static String normalisedForIntent(@Nullable String query) {
        if (query == null) return "";
        String t = query.toLowerCase(java.util.Locale.ROOT).replaceAll("[^\\p{L}\\p{N}' ]", " ").replaceAll("\\s+", " ").trim();
        return t;
    }

    private static final Pattern CAPABILITY_QUESTION = Pattern.compile(
            "^(hi|hello|hey|hi there|hello there|good (morning|afternoon|evening)|greetings|habari|jambo|mambo|"
            + "what can you do|what do you do|how can you help|how can you help me|who are you|what are you|"
            + "what is this|help|help me|capabilities|what can i ask|how do i use you|how do you work)$");
    private static final Pattern BIBLE_DEFINITION = Pattern.compile(
            "^(what is the bible|what is scripture|what are the scriptures|what is the word of god|what is the holy bible)$");
    private static final java.util.Set<String> SHORTCUT_CHIPS = new java.util.HashSet<>(java.util.Arrays.asList(
            "read scripture", "church information", "prayer and fellowship", "give tithe", "watch livestream",
            "open the bible", "find my groups", "start a reflection", "open prayer support", "pick up where you left off"));

    /** Greetings, capability questions, the Bible definition and shortcut chips get built-in answers. */
    @Nullable
    public static AiResponse builtInAppAnswer(@Nullable String query) {
        String t = normalisedForIntent(query);
        if (t.isEmpty()) return null;
        if (CAPABILITY_QUESTION.matcher(t).find()) return capabilityResponse();
        if (BIBLE_DEFINITION.matcher(t).find()) return bibleDefinitionResponse();
        for (String chip : SHORTCUT_CHIPS) {
            if (t.equals(chip) || t.startsWith(chip + " ")) return shortcutResponse(chip);
        }
        return null;
    }

    @NonNull
    public static AiResponse capabilityResponse() {
        return AiResponse.plainText(
                "I'm Perazim Guide. I can help you find Bible passages in the King James Version, explain approved church information and prayer resources, and show you how to use the app. For general questions outside the church and the app, I'll let you know I can't help with those. For medical, legal or financial matters, please speak to a qualified professional.",
                java.util.Arrays.asList("📖 Read Scripture", "🏛️ Church information", "🙏 Prayer and fellowship"));
    }

    @NonNull
    public static AiResponse bibleDefinitionResponse() {
        return AiResponse.plainText(
                "The Bible is the Christian scripture: sixty-six books, the Old and New Testaments, which Christians hold as the word of God. Paul wrote that \"All scripture is given by inspiration of God, and is profitable for doctrine, for reproof, for correction, for instruction in righteousness\" (2 Timothy 3:16, KJV). You can read it in the app under Read Scripture.",
                java.util.Arrays.asList("📖 Read Scripture", "🙏 Prayer and fellowship"));
    }

    @NonNull
    private static AiResponse shortcutResponse(String chip) {
        switch (chip) {
            case "read scripture": case "open the bible":
                return AiResponse.plainText("Open the Bible reader from the Home screen or the Bible tab. Choose a book and chapter, tap a verse to highlight or save it, and use search to find a word.",
                        java.util.Arrays.asList("📖 Read Scripture", "🙏 Prayer and fellowship"));
            case "church information":
                return AiResponse.plainText("You'll find approved church information on the Home and Profile screens. For anything not shown there, please ask the church office or a leader.",
                        java.util.Arrays.asList("🙏 Prayer and fellowship", "📖 Read Scripture"));
            case "prayer and fellowship":
                return AiResponse.plainText("Open Prayer from the Home screen to share a prayer request or pray for others. Your prayer request is only shared when you submit it in the app.",
                        java.util.Arrays.asList("🙏 Prayer and fellowship", "📖 Read Scripture"));
            case "give tithe":
                return AiResponse.plainText("To give, open Giving in the app and follow the M-Pesa paybill steps shown there. The app does not send money for you.",
                        java.util.Arrays.asList("📖 Read Scripture", "🙏 Prayer and fellowship"));
            case "watch livestream":
                return AiResponse.plainText("Open Sermons in the app to watch the livestream or recent messages.",
                        java.util.Arrays.asList("📖 Read Scripture", "🙏 Prayer and fellowship"));
            default:
                return AiResponse.plainText("Use the buttons on the Home screen to open Scripture, Sermons, Worship, Prayer or Community.",
                        java.util.Arrays.asList("📖 Read Scripture", "🙏 Prayer and fellowship"));
        }
    }

    /**
     * The single ordered pre-model route, shared by the app and the evaluation harness. Returns the route name, or
     * null when the question may go on to the model. Order: overdose (the medical line leads), crisis, harm to others,
     * sensitive pastoral, human decision, privacy and out-of-scope.
     */
    @Nullable
    public static String preModelRoute(@Nullable String query) {
        if (query == null || query.trim().isEmpty()) return "scope-refusal";
        String safety = safetyRoute(query);
        if (safety != null && !"topic-violence".equals(safety) && !"general-topic".equals(safety)) return safety;
        if (safety == null && isOverdoseQuery(query)) return "overdose";
        if (safety == null && isCrisisQuery(query)) return "crisis";
        if (isHarmToOthersQuery(query)) return "harm-to-others";
        if (isSafeguardingDisclosure(query)) return "safeguarding";
        if (isDrugSafetyQuery(query)) return "drug-safety";
        if (isHealthStopQuery(query)) return "health-stop";
        if (isManipulationQuery(query)) return "manipulation";
        if (isTranslationPolicyQuery(query)) return "translation-policy";
        if (isForgeryQuery(query)) return "forgery";
        if (isReincarnationQuery(query)) return "reincarnation";
        if (org.perazimchurch.app.ai.library.NoGoZones.matchesDestinyZone(query) && !PERSEVERANCE_QUESTION.matcher(query).find()) return "individual-destiny";
        if (isSensitivePastoralDisclosure(query)) return "sensitive";
        if (isHumanDecisionQuery(query)) return "human-decision";
        // Live web or current-news searches are declined even though "search for" is a Bible cue (Levi, 2026-11-25).
        // A quoted word is still a KJV word search, so quotation marks keep it in scope.
        if (WEB_SEARCH_REQUEST.matcher(query).find() && !query.contains("'") && !query.contains("\"")) return "scope-refusal";
        // Spreadsheets, uploaded files and totals are tool tasks the app does not perform (Levi, 2026-11-25).
        if (TOOL_TASK_REQUEST.matcher(query).find()) return "scope-refusal";
        if (BIBLE_TOPIC_CUE.matcher(query).find()) return null;
        if (isOutOfScopeForModel(query)) return "scope-refusal";
        return null;
    }

    /** The fixed response for a route name from {@link #preModelRoute}. */
    @NonNull
    public static AiResponse preModelResponse(@NonNull String route, @Nullable String query) {
        switch (route) {
            case "crisis": return crisisResponse();
            case "crisis-denial": return crisisDenialResponse();
            case "crisis-literary": return crisisLiteraryResponse();
            case "violence-safeguarding": return violenceSafeguardingResponse();
            case "crisis-and-violence": return crisisAndViolenceResponse();
            case "crisis-third-party": return crisisThirdPartyResponse();
            case "overdose": return overdoseResponse();
            case "harm-to-others": return harmToOthersResponse();
            case "safeguarding": return safeguardingResponse(query);
            case "drug-safety": return drugSafetyResponse();
            case "health-stop": return healthStopResponse();
            case "manipulation": return manipulationResponse();
            case "translation-policy": return translationPolicyResponse();
            case "forgery": return forgeryResponse();
            case "reincarnation": return reincarnationResponse();
            case "individual-destiny": return AiResponse.plainText(org.perazimchurch.app.ai.library.NoGoZones.DESTINY_DECLINE, Arrays.asList("🙏 Open prayer support", "📖 Open the Bible"));
            case "sensitive":
                // A request for confession or absolution: the guide cannot give it. The safety reply is unchanged after it.
                if (query != null && ABSOLUTION_REQUEST.matcher(query).find()) {
                    return AiResponse.plainText(ABSOLUTION_TEXT + " " + sensitivePastoralResponse().getMessageText(), null);
                }
                return sensitivePastoralResponse();
            case "human-decision": return humanDecisionResponse();
            default: return boundaryResponseFor(query);
        }
    }

    /**
     * Deterministic refusal or referral that must run before any model check, so it holds whether or not the
     * on-phone model is installed. Returns null when the question may go to the model.
     */
    @Nullable
    public static AiResponse deterministicModelRefusal(@Nullable String query) {
        if (query == null || query.trim().isEmpty()) return null;
        AiResponse safetyFirst = safetyFirstResponse(query);
        if (safetyFirst != null) return safetyFirst;
        if (isLegacyCrisisKeyword(query)) return crisisResponse();
        if (isSensitivePastoralDisclosure(query)) return sensitivePastoralResponse();
        if (isHumanDecisionQuery(query)) return humanDecisionResponse();
        if (isOutOfScopeForModel(query)) return boundaryResponseFor(query);
        return null;
    }

    /** Chooses the boundary wording: professional referral for medical, legal and financial matters, church referral otherwise. */
    @NonNull
    public static AiResponse boundaryResponseFor(@Nullable String query) {
        AiResponse privacy = privacyRouteResponse(query);
        if (privacy != null) return privacy;
        // Live research, links and news: say plainly that the guide cannot browse or verify them.
        if (query != null && LIVE_RESEARCH_PATTERN.matcher(query).find()) {
            return AiResponse.plainText(org.perazimchurch.app.ai.library.NoGoZones.UNCERTAIN_DECLINE, null);
        }
        return isProfessionalMatterQuery(query) ? professionalBoundaryResponse() : outOfScopeResponse();
    }

    /** Church, Bible, faith and app topics the on-phone model may answer. Replies are always in English; Swahili and Sheng are recognised as input. */
    private static final Pattern MODEL_IN_SCOPE_TOPIC = Pattern.compile(
            "\\b(?:bible|biblia|scripture|scriptures|verse|verses|aya|chapter|book of|gospel|injili|psalm|psalms|zaburi|proverbs|" +
            "genesis|exodus|romans|corinthians|ephesians|galatians|hebrews|revelation|matthew|luke|john|isaiah|ruth|" +
            // Swahili book names for the scope gate.
            "mwanzo|kutoka|walawi|hesabu|kumbukumbu|yoshua|waamuzi|ruthu|samweli|wafalme|mambo ya nyakati|nehemia|esta|" +
            "ayubu|mithali|mhubiri|wimbo ulio bora|isaya|yeremia|maombolezo|ezekieli|danieli|yoeli|amosi|obadia|yona|mika|" +
            "nahumu|habakuki|sefania|hagai|zekaria|malaki|mathayo|marko|luka|yohana|matendo|warumi|wakorintho|wagalatia|" +
            "waefeso|wafilipi|wakolosai|wathesalonike|timotheo|tito|filemoni|waebrania|yakobo|petro|yuda|ufunuo|" +
            "david|goliath|moses|paul|peter|abraham|noah|daniel|elijah|jonah|job|joseph|jacob|samuel|solomon|esther|timothy|titus|philippians|thessalonians|lazarus|mary|martha|bethlehem|" +
            "warumi|korintho|wakorintho|waefeso|wagalatia|waebrania|ufunuo|mathayo|yesu|jesus|christ|god|mungu|lord|holy spirit|roho mtakatifu|mtakatifu|roho|spirit|trinity|commandments?|amri|" +
            "prayer|prayers|pray|praying|maombi|sala|devotional|devotion|faith|imani|grace|neema|mercy|forgiveness|parable|sabbath|" +
            // Other groups' teachings named by members (answered from Scripture as false-doctrine examples, never as sources).
            "seventh-day|seventh day|adventist\\w*|saturday|jumamosi|sabato|christian science|oneness|circumcis\\w*|" +
            "commission|baptism|tithe|tithing|offering|offerings|sadaka|paybill|church|kanisa|perazim|bishop|ministry|" +
            "cell group|fellowship|testimony|worship|hymn|hymns|hymnal|nyimbo|wimbo|sermon|sermons|livestream|sunday|" +
            "christian|christians|christianity|churches|pentecostal|pentecostals|presbyterian|presbyterians|catholic|catholics|methodist|anglican|baptist|baptise|baptize|baptising|baptism|infant|infants|mormon|mormonism|jehovah|witness|witnesses|denomination|denominations|arminian|arminians|calvinist|calvinists|calvinism|arminianism|rapture|tribulation|tongues|tongue|prophecy|prophesy|prophet|prophets|gifts|theology|theological|doctrine|doctrines|creed|creeds|nicaea|council|argument|justification|historical|history|believe|prosperity|wealthy|jealous|unforgiveness|islam|muslim|muslims|quran|hindu|buddhism|religion|religious|ethics|moral|morality|kenya|kenyan|family|parents|children|child|husband|wife|marriage|divorce|grief|grieve|bereaved|widow|orphan|" +
            "genesis|exodus|leviticus|numbers|deuteronomy|joshua|judges|ruth|1\\s+samuel|2\\s+samuel|1\\s+kings|2\\s+kings|1\\s+chronicles|2\\s+chronicles|ezra|nehemiah|esther|job|psalms|proverbs|ecclesiastes|song\\s+of\\s+solomon|isaiah|jeremiah|lamentations|ezekiel|daniel|hosea|joel|amos|obadiah|jonah|micah|nahum|habakkuk|zephaniah|haggai|zechariah|malachi|matthew|mark|luke|john|acts|romans|1\\s+corinthians|2\\s+corinthians|galatians|ephesians|philippians|colossians|1\\s+thessalonians|2\\s+thessalonians|1\\s+timothy|2\\s+timothy|titus|philemon|hebrews|james|1\\s+peter|2\\s+peter|1\\s+john|2\\s+john|3\\s+john|jude|revelation|hi|hello|hey|habari|jambo|good morning|good evening|thank you|thanks|asante|antichrist|calvinist|calvinists|arminian|religion|religions|gospel|scripture|sermon|" +
            "afterlife|after death|hereafter|soul|souls|immortal|immortality|resurrection|judgment|judgement|eternity|anger|angry|resent|resentment|sin|sins|sinning|temptation|tempted|hopeless|lonely|loneliness|afraid|fear|anxious|anxiety|depressed|depression|grief|grieving|marriage|husband|wife|brother|sister|family|forgive|forgiveness|guilt|shame|doubt|doubts|salvation|saved|heaven|hell|eternal|struggle|struggling|suffering|hurt|healing|wisdom|purpose|meaning|peace|hope|joy|love|patience|humility|comfort|encouragement|pastor|pastors|elder|elders|counsel|counselling|counseling|worried|worry|stressed|stress|sad|sadness|life|dying|death|" +
            "the app|this app|perazim app|app theme|app account|profile|reader|highlight|highlights|streak|xp|level up|community wall|youtube|reminder|" +
            "discussion questions|bible study|blessing|thanksgiving)\\b", Pattern.CASE_INSENSITIVE);
    /** Instruction-override and role-play attempts. These are refused even when they mention church topics. */
    private static final Pattern MODEL_OVERRIDE = Pattern.compile(
            "\\b(?:ignore|forget|disregard|bypass)\\s+(?:(?:all|the|your|previous|church|these)\\s+)*(?:rules|instructions|guidelines|restrictions|prompt)\\b|" +
            "\\bpretend\\s+(?:you\\s+are|to\\s+be|that)\\b|\\broleplay\\b|\\brole-play\\b|\\byou\\s+are\\s+now\\b|\\bfrom\\s+now\\s+on\\s+you\\b|" +
            "\\bspeak\\s+as\\b|\\bact\\s+as\\s+(?:my|an?)\\s+(?!christian|church|pastor|believer|leader)|\\bsong\\s+lyrics\\b|" +
            "\\banswer\\s+any\\s+question\\b|\\bdeclare\\b.{0,40}\\b(?:doctrine|official|policy|position)\\b|\\byou\\s+are\\s+the\\s+(?:bishop|pastor|leader)\\b|\\bignore\\b.{0,30}\\b(?:previous|prior|above)\\s+instructions\\b|\\b(?:riddles?|trivia|jokes?|poems?|poetry)\\b", Pattern.CASE_INSENSITIVE);

    /**
     * Stricter gate for the on-phone model: refuses anything outside church, Bible, faith and app topics,
     * plus role-play and instruction-override attempts. The rest of the app keeps {@link #isClearlyOutOfScopeQuery}.
     */
    public static boolean isOutOfScopeForModel(@Nullable String query) {
        if (query == null || query.trim().isEmpty()) return true;
        if (isOtherPersonDataRequest(query)) return true;
        if (isMeansAndDistress(query) || isHarmToOthersQuery(query)) return false;
        if (isClearlyOutOfScopeQuery(query)) return true;
        String normalized = query.replace('’', '\'').replaceAll("\\s+", " ").trim();
        if (MODEL_OVERRIDE.matcher(normalized).find()) return true;
        // Faith questions about another faith's idea (karma, purgatory, afterlife) are in scope: the guide answers them from Scripture.
        if (org.perazimchurch.app.ai.guide.DriftDetector.drifts(normalized)) return false;
        return !MODEL_IN_SCOPE_TOPIC.matcher(normalized).find();
    }

    /** Deterministically blocks clear unrelated professional/general-interest advice requests. */
    public static boolean isClearlyOutOfScopeQuery(@Nullable String query) {
        if (query == null) return false;
        String normalized = query.replace('’', '\'').replaceAll("\\s+", " ").trim();
        for (Pattern pattern : CLEARLY_OUT_OF_SCOPE_PATTERNS) {
            if (pattern.matcher(normalized).find()) return true;
        }
        return false;
    }

    @NonNull
    public static AiResponse outOfScopeResponse() {
        return AiResponse.plainText(
                "I am designed specifically to assist with matters relating to Perazim Mission Church (PMCI), Christian faith and religious matters, so I am not authorised to advise on this matter. Please speak with a trusted church leader or an appropriate person.",
                Arrays.asList("📖 Read Scripture", "🏛️ Church information", "🙏 Prayer and fellowship"));
    }

    /** Word-boundary matcher exposed for deterministic intent regression tests. */
    public static boolean containsIntentTerm(@Nullable String text, @NonNull String term) {
        if (text == null || text.trim().isEmpty() || term.trim().isEmpty()) return false;
        String normalizedText = text.toLowerCase(Locale.ROOT).replaceAll("[^\\p{L}\\p{N}]+", " ").trim();
        String normalizedTerm = term.toLowerCase(Locale.ROOT).replaceAll("[^\\p{L}\\p{N}]+", " ").trim();
        return (" " + normalizedText + " ").contains(" " + normalizedTerm + " ");
    }

    private String getEffectiveUserId() {
        if (context == null) return "guest";
        String id = SessionManager.getInstance(context).getActiveUserId();
        return (id != null && !id.trim().isEmpty()) ? id : "guest";
    }

    public AiResponse getWelcomeResponse() {
        String userId = getEffectiveUserId();
        int xp = GamificationStore.getSpiritualXp(context, userId, 0);
        int streak = GamificationStore.getStreak(context, userId, 0);

        String telemetryLine = (streak > 0)
                ? "You currently have **⭐ " + xp + " Spiritual XP** and an active **🔥 " + streak + "-day streak**."
                : (xp > 0)
                ? "You currently have **⭐ " + xp + " Spiritual XP**."
                : "Your app progress is ready whenever you want to explore today's reflection or other activities.";

        String msg = "🔥 **Shalom & Welcome to Perazim Breakthrough Guide!**\n\n"
                + "I am your personal in-app assistant. " + telemetryLine + "\n\n"
                + "How can I guide your spiritual walk or app navigation today?";

        List<String> chips = Arrays.asList(
                "⭐ Check my XP & Level",
                streak > 0 ? "🔥 Check my " + streak + "-day streak" : "⚡ Open Daily Reflection",
                "🎥 Watch YouTube Live",
                "📖 Read Swahili Bible",
                "💚 Give Tithe via M-Pesa",
                "🧩 Play Bible Riddle"
        );

        return AiResponse.plainText(msg, chips);
    }

    private AiResponse handleXpQuery(String q) {
        String userId = getEffectiveUserId();
        int xp = GamificationStore.getSpiritualXp(context, userId, 0);
        int level = SpiritualLevel.levelFor(xp);
        int remaining = SpiritualLevel.xpToNextLevel(xp);

        String msg = "⭐ **Spiritual XP:**\n\n"
                + "• **Current XP:** " + xp + " XP\n"
                + "• **Level:** " + level + " Disciple\n"
                + "• **Progress to Level " + (level + 1) + ":** " + remaining + " XP needed.\n\n"
                + "💡 *XP is earned through app activities. This guide only reads your displayed XP; it does not award points.*";

        List<String> chips = Arrays.asList("⚡ Open Reflection", "🧩 Open Riddles", "🔥 Check Streak");
        return AiResponse.withAction(msg, AiResponse.INTENT_REFLECTIONS, "⚡ Open Reflection", null, chips);
    }

    private AiResponse handleStreakQuery(String q) {
        String userId = getEffectiveUserId();
        int streak = GamificationStore.getStreak(context, userId, 0);
        boolean isFrozen = GamificationStore.isStreakFrozen(context, userId, false);
        int grace = GamificationStore.getGracePoints(context, userId, 0);

        String status = isFrozen ? "❄️ FROZEN / SHIELDED" : (streak > 0 ? "🔥 ACTIVE" : "⚪ NOT STARTED");

        String msg;
        if (streak > 0) {
            msg = "🔥 **Devotional Streak Status:**\n\n"
                    + "• **Current Streak:** " + streak + " Days (" + status + ")\n"
                    + "• **Grace Points:** " + grace + " 💜 available\n\n"
                    + (isFrozen
                    ? "Your streak is protected by a Streak Freeze, which is activated in the app with Grace Points. Complete today's devotion to keep the flame burning."
                    : "Your streak is active! Make sure to read today's scripture and pray before midnight to keep your fire burning.");
        } else {
            msg = "🔥 **Devotional Streak Status:**\n\n"
                    + "• **Current Streak:** 0 Days (" + status + ")\n"
                    + "• **Grace Points:** " + grace + " 💜 available\n\n"
                    + "Your devotional journey begins today! Complete today's 3-minute prayer reflection to ignite Day 1 of your streak fire.";
        }

        List<String> chips = Arrays.asList("💜 What are Grace Points?", "⚡ Complete Today's Devotion", "⭐ Check XP");
        return AiResponse.withAction(msg, AiResponse.INTENT_NAVIGATE_PROFILE, "👤 View Streak in Profile", null, chips);
    }

    private AiResponse handleGraceQuery(String q) {
        String userId = getEffectiveUserId();
        int grace = GamificationStore.getGracePoints(context, userId, 0);

        String msg = "💜 **Grace Points in the app:**\n\n"
                + "You currently have **" + grace + " Grace Points**.\n\n"
                + "These are an app progress feature and are not a measure of God's grace. In the app, Grace Points can activate a 48-hour Streak Freeze (2 Grace Points) or restore a missed day (3 Grace Points).";

        List<String> chips = Arrays.asList("🔥 Check Streak Status", "⭐ Earn More XP", "👤 Go to Profile");
        return AiResponse.withAction(msg, AiResponse.INTENT_NAVIGATE_PROFILE, "❄️ Manage Streak Protection", null, chips);
    }

    private AiResponse handleGivingQuery() {
        String msg = "💚 **Giving information:**\n\n"
                + "Open the Giving area to review the current options and instructions shown in this app. Verify the recipient and payment details in the app before making a payment.";

        List<String> chips = Arrays.asList("💚 Open Giving Dialog", "🏛️ Church Campuses", "🎥 Watch Sermons");
        return AiResponse.withAction(msg, AiResponse.INTENT_NAVIGATE_GIVING, "💚 Open Giving / Tithe Modal", null, chips);
    }

    private AiResponse handleSermonsQuery(String q) {
        String msg = "🎥 **Sermons and worship media:**\n\n"
                + "Open the Sermons section to see the resources currently available in this app. Availability of live streams, recordings, audio, and downloads can vary.";

        List<String> chips = Arrays.asList("▶ Watch Live Broadcast", "📥 Offline Downloads", "💬 Share Sermon");
        return AiResponse.withAction(msg, AiResponse.INTENT_NAVIGATE_LIVE, "🎥 Open Perazim Live & Video", null, chips);
    }

    private AiResponse handleBibleQuery(String q) {
        String msg = "📖 **Holy Scripture Reader:**\n\n"
                + "Open the reader and choose a translation shown in this build. Translation availability and licensing vary; check the reader's own list before choosing.";

        List<String> chips = Arrays.asList("📖 Read 2 Samuel 5", "📖 Read Psalms", "⭐ Check Devotion XP");
        return AiResponse.withAction(msg, AiResponse.INTENT_OPEN_BIBLE, "📖 Open Holy Bible Reader", null, chips);
    }

    private AiResponse handleDailyVerseQuery() {
        String msg = "📖 **Daily verse:**\n\n"
                + "Open the app's Home or Bible area to see the verse currently provided in this build. I will not reconstruct a quotation when the verse text is unavailable.";
        return AiResponse.withAction(msg, AiResponse.INTENT_NAVIGATE_HOME, "🏠 Open Home", null,
                Arrays.asList("📖 Open Bible", "⚡ Open Reflections"));
    }

    private AiResponse handleWorshipQuery() {
        String msg = "🎵 **Perazim Hymnal & Praise Browser:**\n\n"
                + "Explore classic hymns and worship choruses with key signatures, time signatures, and stanzas.\n"
                + "• Categories: *Worship, Faith, Grace, Praise, Favorites*\n"
                + "• Hymns available: *Holy, Holy, Holy!*, *Great Is Thy Faithfulness*, *Amazing Grace*, and more.";

        List<String> chips = Arrays.asList("🎵 Browse Hymns", "🎥 Watch Live Worship", "🙏 Prayer Wall");
        return AiResponse.withAction(msg, AiResponse.INTENT_NAVIGATE_WORSHIP, "🎵 Open Worship Hymnal", null, chips);
    }

    private AiResponse handleFellowshipQuery() {
        String msg = "🤝 **Prayer and fellowship:**\n\n"
                + "The app has prayer and community features. Open them to review what is available and the privacy information before sharing personal details. This guide does not submit a prayer or award progress points.";

        List<String> chips = Arrays.asList("✍️ Submit Prayer Request", "🌟 Praise Reports", "💬 Direct Chat");
        return AiResponse.withAction(msg, AiResponse.INTENT_NAVIGATE_FELLOWSHIP, "🙏 Go to Prayer Wall", null, chips);
    }

    private AiResponse handleRiddlesQuery() {
        String msg = "🧩 **Bible Trivia & Riddles Challenge:**\n\n"
                + "Open the game to see the current questions and rules. This guide does not award points; any game progress is handled by the game itself.";

        List<String> chips = Arrays.asList("🧩 Start Bible Riddle", "😄 Christian Jokes", "⭐ Check My XP");
        return AiResponse.withAction(msg, AiResponse.INTENT_LAUNCH_RIDDLES, "🧩 Play Bible Riddles", null, chips);
    }

    private AiResponse handleJokesQuery() {
        String msg = "😄 **Christian jokes:**\n\n"
                + "Open the jokes section to see the items currently available in this app.";

        List<String> chips = Arrays.asList("😄 Read Bible Jokes", "🧩 Play Bible Riddle", "🤝 Fellowship");
        return AiResponse.withAction(msg, AiResponse.INTENT_LAUNCH_JOKES, "😄 Open Christian Jokes", null, chips);
    }

    private AiResponse handleReflectionsQuery() {
        String msg = "⚡ **Daily reflection:**\n\n"
                + "Open Home to start today's 3-minute reflection routine. The guide does not create a devotional, state an official interpretation, or award points.";

        List<String> chips = Arrays.asList("⚡ Start a Reflection", "🔥 Check Streak", "📖 Open Bible");
        return AiResponse.withAction(msg, AiResponse.INTENT_REFLECTIONS, "⚡ Open Reflections", null, chips);
    }

    private AiResponse handleChurchInfoQuery() {
        return churchInfoResponse();
    }

    static AiResponse churchInfoResponse() {
        String msg = "🏛️ **Church and campus information:**\n\n"
                + "This guide does not have a verified source for the church's official identity, locations, contacts, or service schedule. Check the Church or Campus information currently shown in the app, or contact an authorised church representative.";

        List<String> chips = Arrays.asList("🎥 Watch Livestream", "💚 Give Tithe", "📖 Read Scripture");
        return AiResponse.plainText(msg, chips);
    }

    private AiResponse handleProfileQuery() {
        String msg = "👤 **Member Profile & Offline Manager:**\n\n"
                + "Manage your spiritual credentials and offline storage.\n"
                + "• View downloaded sermon audios and hymnals.\n"
                + "• Use Grace Points to activate a 48-hour Streak Freeze.\n"
                + "• Use only contact routes currently displayed in the app and verify them before sharing sensitive information.";

        List<String> chips = Arrays.asList("📦 Manage Offline Audio", "🔥 Check Streak", "⭐ View XP");
        return AiResponse.withAction(msg, AiResponse.INTENT_NAVIGATE_PROFILE, "👤 Go to My Profile", null, chips);
    }

    private AiResponse handleOffTopicGuardrail(String rawQuery) {
        String generativeAvailability = openEndedModeNotice(
                BuildConfig.OFFLINE_QWEN_ENABLED, BuildConfig.ONLINE_AI_ENABLED);
        String msg = "📱 **Offline Church Guide (On-Device Rules):**\n\n"
                + "I could not find an offline match for \"" + (rawQuery != null ? rawQuery : "") + "\".\n\n"
                + "As an offline guide, I operate on built-in church rules for Scripture, Sermons, Hymns, Giving, and Devotionals. "
                + generativeAvailability + "\n\n"
                + "Here are some things I can help you with right now offline:";

        List<String> chips = Arrays.asList(
                "⭐ Check my Spiritual XP",
                "📖 Read Scripture (2 Samuel 5)",
                "🎥 Watch YouTube Live",
                "💚 Give Tithe via M-Pesa",
                "🎵 Hymnal & Chords"
        );

        return AiResponse.plainText(msg, chips);
    }

    static String openEndedModeNotice(boolean offlineQwenEnabled, boolean onlineAiEnabled) {
        if (onlineAiEnabled) {
            return "For broader questions, you can switch to Online AI when you are signed in, have consented, and your account is eligible.";
        }
        if (offlineQwenEnabled) {
            return "For broader questions, you can switch to on-phone Qwen. It requires the local model and verified supporting knowledge, and it has no live internet access.";
        }
        return "Generative AI is not enabled in this build. You can still open the Bible for local Scripture lookup or use the built-in app guide.";
    }

    private boolean containsAny(String text, String... words) {
        for (String w : words) {
        if (containsIntentTerm(text, w)) {
                return true;
            }
        }
        return false;
    }
}
