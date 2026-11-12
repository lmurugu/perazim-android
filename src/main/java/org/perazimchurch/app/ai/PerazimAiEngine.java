package org.perazimchurch.app.ai;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.perazimchurch.app.BuildConfig;
import org.perazimchurch.app.data.local.preference.GamificationStore;
import org.perazimchurch.app.data.local.session.SessionManager;

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

    private static final Pattern CRISIS_PATTERN = Pattern.compile(
            "\\b(suicid(?:e|al)|kill myself|end my life|want to die|better off dead|" +
            "self[ -]?harm|hurt myself|harm myself|cutting myself|unalive myself|" +
            "end it all|can't go on|cannot go on|not worth living|sexual assault|" +
            "domestic violence|physically abused|(?:going to|plan to|want to) (?:kill|shoot|stab) (?:someone|him|her|them)|(?:immediate|imminent) danger)\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern CRISIS_DISCLOSURE_PATTERN = Pattern.compile(
            "\\b(?:(?:my|our) (?:husband|wife|spouse|partner|boyfriend|girlfriend|parent|father|mother|caregiver) " +
                    "(?:hits?|beats?|beating|struck|choked|attacked|threatened) (?:me|us|my child|our child)|" +
                    "(?:hit|hits|hitting|beat|beats|beating|struck|choked|choking|attacked|threatened) (?:me|us|my child|our child))\\b|" +
                    "\\b(?:afraid|scared|fear) (?:that )?(?:my )?(?:partner|husband|wife|spouse|parent|father|mother|caregiver) " +
                    "(?:will|might|may|is going to) (?:hurt|hit|beat|attack|kill) (?:me|my child|our child)\\b",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern SENSITIVE_PASTORAL_REQUEST = Pattern.compile(
            "\\b(?:keep (?:this|it) confidential|keep (?:this|it) secret|don't tell anyone|do not tell anyone|" +
                    "please don't share this|please do not share this|pastoral intervention)\\b",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern PERSONAL_CONTEXT = Pattern.compile(
            "\\b(?:i am|i'm|i feel|i felt|i have been|i was|my|our|we|my friend|someone close to me)\\b",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern SENSITIVE_PASTORAL_TOPIC = Pattern.compile(
            "\\b(?:abuse|assault|rape|violence|hit me|beating|marital conflict|family conflict|divorce|" +
                    "separation|trauma|grief|bereavement|mourning|depress(?:ion|ed)?|anx(?:iety|ious)|" +
                    "panic attack|self[ -]?harm|suicid(?:e|al)|addict(?:ion|ed)|overdose|confession|" +
                    "confidential|secret)\\b",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern[] CLEARLY_OUT_OF_SCOPE_PATTERNS = {
            Pattern.compile("\\b(?:which|what)\\s+(?:stock|stocks|share|shares|cryptocurrency|crypto|bitcoin|forex|bond|bonds)\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\b(?:buy|sell|trade|invest in)\\b.{0,70}\\b(?:stock|stocks|share|shares|cryptocurrency|crypto|bitcoin|forex|bond|bonds)\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\b(?:should i|recommend|help me choose|help me pick)\\b.{0,70}\\b(?:investment portfolio|retirement portfolio|personal budget|payday loan|mortgage|credit card|tax return|insurance policy)\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\b(?:what|which|recommend|prescribe)\\b.{0,50}\\b(?:medicine|medication|drug|dosage|dose|treatment)\\b|\\b(?:diagnose me|diagnose my|medical advice|prescribe medication)\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\b(?:give me legal advice|should i sue|how (?:do|can) i sue|file (?:a )?lawsuit|draft (?:a )?(?:legal )?contract|represent me in court)\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\b(?:who won|latest score|current score|score of)\\b.{0,60}\\b(?:football|match|game|election|sports?)\\b|\\b(?:write|draft) (?:me )?a campaign speech\\b", Pattern.CASE_INSENSITIVE)
    };

    private final Context context;

    public PerazimAiEngine(@NonNull Context context) {
        this.context = context.getApplicationContext();
    }

    /**
     * Processes a user query and returns a structured AI response with intent actions.
     */
    @NonNull
    public AiResponse processQuery(@Nullable String rawQuery) {
        if (rawQuery == null || rawQuery.trim().isEmpty()) {
            return getWelcomeResponse();
        }

        String q = rawQuery.trim().toLowerCase(Locale.US);

        // 0. CRISIS & PASTORAL CARE INTERVENTION
        if (isCrisisQuery(q)) {
            return handleCrisisQuery();
        }

        if (isSensitivePastoralDisclosure(rawQuery)) {
            return sensitivePastoralResponse();
        }

        if (isClearlyOutOfScopeQuery(rawQuery)) {
            return outOfScopeResponse();
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

    public static AiResponse crisisResponse() {
        String msg = "I'm sorry you're facing this. If you or someone else may be in immediate danger, contact local emergency services or go to the nearest hospital now. If you can, tell a trusted person nearby and ask them to stay with you. For ongoing distress, contact a qualified local health professional.\n\n"
                + "You can also use the app's prayer feature or a church contact shown in the app, if you choose. This guide cannot provide emergency monitoring or confirm that a pastoral contact is available.";
        return AiResponse.plainText(msg, Arrays.asList("🙏 Open prayer support", "📖 Open the Bible"));
    }

    /** Conservative local safety gate used before guide, Qwen, and online routing. */
    public static boolean isCrisisQuery(@Nullable String query) {
        if (query == null) return false;
        String normalized = query.replace('’', '\'').replaceAll("\\s+", " ");
        return CRISIS_PATTERN.matcher(normalized).find() || CRISIS_DISCLOSURE_PATTERN.matcher(normalized).find();
    }

    /** Conservative pre-model route for personal pastoral disclosures; not a risk diagnosis. */
    public static boolean isSensitivePastoralDisclosure(@Nullable String query) {
        if (query == null || query.trim().isEmpty()) return false;
        return SENSITIVE_PASTORAL_REQUEST.matcher(query).find()
                || (PERSONAL_CONTEXT.matcher(query).find() && SENSITIVE_PASTORAL_TOPIC.matcher(query).find());
    }

    public static AiResponse sensitivePastoralResponse() {
        return AiResponse.plainText(
                "I'm sorry you're dealing with this. This AI cannot promise confidentiality or notify a pastor, and this message has not been sent for pastoral follow-up. If you want human support, choose a pastoral contact route shown in the app; I will not send it for you. If anyone is in immediate danger, contact local emergency services or go to a hospital now and tell a trusted person nearby.",
                Arrays.asList("🙏 Open pastoral contacts", "🙏 Open prayer support"));
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
                "I can help with Christian faith, approved Perazim information, and using this app. I cannot provide investment, medical, or legal advice, or answer unrelated general-interest questions. Please consult a qualified professional for those matters; for a spiritual or church concern, speak with an authorised human leader.",
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
        int level = calculateLevel(xp);
        String levelName = getLevelTitle(level);
        int nextLevelXp = level * 250;
        int remaining = Math.max(0, nextLevelXp - xp);

        String msg;
        if (xp > 0) {
            msg = "⭐ **Spiritual XP Telemetry:**\n\n"
                    + "• **Current XP:** " + xp + " XP\n"
                    + "• **Rank:** Level " + level + " (" + levelName + ")\n"
                    + "• **Progress to Level " + (level + 1) + ":** " + remaining + " XP needed.\n\n"
                    + "💡 *Open the app's activities to review current progress rules. This guide only reads your displayed XP; it does not award points.*";
        } else {
            msg = "⭐ **Spiritual XP Telemetry:**\n\n"
                    + "• **Current XP:** 0 XP\n"
                    + "• **Rank:** Level 1 (" + levelName + ")\n"
                    + "• **Progress to Level 2:** 250 XP needed.\n\n"
                    + "💡 *Open the app's activities to review current progress rules. This guide only reads your displayed XP; it does not award points.*";
        }

        List<String> chips = Arrays.asList("⚡ Open Reflection", "🧩 Open Riddles", "🔥 Check Streak");
        return AiResponse.withAction(msg, AiResponse.INTENT_REFLECTIONS, "⚡ Open Reflection", null, chips);
    }

    private AiResponse handleStreakQuery(String q) {
        String userId = getEffectiveUserId();
        int streak = GamificationStore.getStreak(context, userId, 0);
        boolean isFrozen = GamificationStore.isStreakFrozen(context, userId, false);
        int grace = GamificationStore.getGracePoints(context, userId, 0);

        String status = isFrozen ? "❄️ FROZEN / SHIELDED" : (streak > 0 ? "🔥 ACTIVE ALTAIRE" : "⚪ NOT STARTED");

        String msg;
        if (streak > 0) {
            msg = "🔥 **Devotional Streak Status:**\n\n"
                    + "• **Current Streak:** " + streak + " Days (" + status + ")\n"
                    + "• **Grace Tokens:** " + grace + " 💜 available\n\n"
                    + (isFrozen
                    ? "Your streak is currently protected by Grace Freeze! Complete today's devotion to unfreeze and keep the flame burning."
                    : "Your streak is active! Make sure to read today's scripture and pray before midnight to keep your fire burning.");
        } else {
            msg = "🔥 **Devotional Streak Status:**\n\n"
                    + "• **Current Streak:** 0 Days (" + status + ")\n"
                    + "• **Grace Tokens:** " + grace + " 💜 available\n\n"
                    + "Your devotional journey begins today! Complete today's 3-minute prayer reflection to ignite Day 1 of your streak fire.";
        }

        List<String> chips = Arrays.asList("💜 What are Grace Tokens?", "⚡ Complete Today's Devotion", "⭐ Check XP");
        return AiResponse.withAction(msg, AiResponse.INTENT_NAVIGATE_PROFILE, "👤 View Streak in Profile", null, chips);
    }

    private AiResponse handleGraceQuery(String q) {
        String userId = getEffectiveUserId();
        int grace = GamificationStore.getGracePoints(context, userId, 0);

        String msg = "💜 **Grace Points in the app:**\n\n"
                + "You currently have **" + grace + " Grace Tokens**.\n\n"
                + "These are an app progress feature and are not a measure of God's grace. Check the current Profile screen for how your streak is handled.";

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
                + "• Hymns available: *Holy Holy Holy*, *Great Is Thy Faithfulness*, *Amazing Grace*, and more.";

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
                + "Open the Reflections section to read today's material and its Scripture reference. The guide does not create a devotional, state an official interpretation, or award points.";

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
                + "• Toggle Grace streak freeze protection.\n"
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

    private int calculateLevel(int xp) {
        if (xp < 250) return 1;
        if (xp < 500) return 2;
        if (xp < 1000) return 3;
        return 4;
    }

    private String getLevelTitle(int level) {
        switch (level) {
            case 1: return "Disciple of the Breach";
            case 2: return "Covenant Seeker";
            case 3: return "Watchman on the Walls";
            default: return "Baal-Perazim Conqueror";
        }
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
