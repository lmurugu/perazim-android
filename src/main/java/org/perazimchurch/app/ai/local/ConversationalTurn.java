package org.perazimchurch.app.ai.local;

import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

/**
 * Plain, varied replies for greetings, thanks, goodbyes, "how are you", "what can you do", complaints about repeated or
 * missed answers, and the first line of a marriage conversation (Levi, 2026-11-25: the guide must not sound scripted).
 *
 * <p>These replies carry no doctrine and no Scripture claim. A marriage reply only asks what the member needs; marriage
 * teaching stays behind the review gate. Replies rotate through several wordings, so the same greeting is not repeated
 * twice in a row. Safety wording (crisis, abuse, forced marriage) never reaches this class: {@link PreModel} checks safety
 * first, and a coercion word makes the marriage pattern decline to match.
 */
public final class ConversationalTurn {

    /** A routed conversational reply. */
    public static final class Reply {
        public final String status;
        public final String text;

        Reply(String status, String text) {
            this.status = status;
            this.text = text;
        }
    }

    private static final Pattern GREETING = Pattern.compile(
            "^(?:hello|hi|hey|hiya|helo|good (?:morning|afternoon|evening)|habari(?: yako| za asubuhi| za mchana)?|mambo|jambo|hujambo|shalom|"
                    + "bwana asifiwe|praise the lord|vipi)(?: there| again| sir| madam| pastor| guide)?[ !.,?]*$");
    private static final Pattern HOW_ARE_YOU = Pattern.compile(
            "^(?:how are you(?: doing)?(?: today)?|how is your day|how's your day|how do you do|unaendeleaje|hujambo sana)[ !.,?]*$");
    private static final Pattern THANKS = Pattern.compile(
            "^(?:thank you(?: so much| very much| a lot)?|thanks(?: a lot| so much)?|asante(?: sana)?|god bless you)(?:,? that helps| for your help)?[ !.,]*$");
    private static final Pattern GOODBYE = Pattern.compile(
            "^(?:bye|bye bye|goodbye|good bye|see you(?: later)?|kwaheri|tutaonana|lala salama|good night)(?:,? (?:god|may god|the lord) bless you)?[ !.,]*$");
    private static final Pattern ACKNOWLEDGE = Pattern.compile("^(?:ok|okay|alright|sure|got it|noted|hmm+)[ !.,]*$");
    private static final Pattern HELP_REQUEST = Pattern.compile(
            "^(?:help|i need help|can you help|can you help me|please help|please help me|naomba msaada|nisaidie|msaada)[ !.,?]*$");
    private static final Pattern FOLLOW_UP = Pattern.compile(
            "^(?:tell me more|more|go on|continue|explain more|can you explain more|say more|what else|more please|and\\?)[ !.,?]*$");
    /** Whole-message capability questions only, so a real question that mentions help is not answered with this reply. */
    private static final Pattern CAPABILITY = Pattern.compile(
            "^(?:what can you (?:do|answer|help with)|what can you help(?: me)? with|what (?:can|do) you (?:do|know)|"
                    + "what can(?:'t| not) you (?:do|answer|help)(?: with)?|what can you and can(?:'t| not) you(?: answer)?|"
                    + "who are you|how can you help|how do i use you|what can i ask)[ !.,?]*$");
    /** Complaints about a repeated or missed answer, matched on the whole message. */
    private static final Pattern META = Pattern.compile(
            "^(?:why are you (?:repeating|saying the same)[^?]{0,200}|you (?:didn'?t|did not) (?:answer|respond)[^?]{0,200}|you'?re repeating[^?]{0,200}|"
                    + "that'?s not (?:what|my)[^?]{0,200}|not what i asked|say it (?:another|a different|differently)(?: way)?|try again|"
                    + "answer my question|stop repeating[^?]{0,200}|same (?:message|answer|reply|thing|response)|repeating the same[^?]{0,200})[ !.,?]*$");
    private static final Pattern MARRIAGE_INTENT = Pattern.compile(
            "\\b(?:want to (?:get )?marr(?:y|ied)|want to marry|going to (?:get )?marr(?:y|ied)|thinking (?:of|about) (?:getting )?marr(?:y|iage)|"
                    + "getting married|how (?:do|can) i (?:choose|find|know|pick)(?: a| the)? (?:husband|wife|spouse|partner)|"
                    + "want a (?:husband|wife)|need a (?:husband|wife)|looking for (?:a )?(?:husband|wife|spouse))\\b");
    /** Any of these means the message is about force, age or consent: it goes to the safety and approved routes instead. */
    private static final Pattern MARRIAGE_SAFETY = Pattern.compile(
            "\\b(?:forc\\w*|pressur\\w*|threat\\w*|against my will|do not love|don'?t love|under 1[0-8]|aged?|years old|child|minor|"
                    + "school|parents? (?:want|choose|decide|pick)|kidnap\\w*|abus\\w*|dowry|mahari|kugongeana)\\b");

    private static final String[] GREETINGS = {
            "Hello, and welcome. Please tell me briefly what you need. I can find a Bible verse for you, search a word in the King James Version, answer a question of faith, or pray with you.",
            "Hi there, I'm glad you came. I can look up a verse or a phrase, count how often a word appears in the Bible, or talk through a question of faith. Please tell me what is on your heart.",
            "Good to see you. I can quote verses word for word from the King James Version, search a word, explain the approved teaching on common questions, or pray with you. Please tell me where you would like to begin.",
            "Hello. You can ask me for a verse such as John 3:16, search a word such as 'grace', ask a Bible or doctrine question, or bring me a prayer. Please tell me what is on your mind.",
            "Hey, let's begin. I can find a passage, search a word, or help you with a question of faith or a prayer. Tell me what you need, and please keep it simple."
    };
    private static final String[] HOW_ARE_YOU_REPLIES = {
            "Thank you for asking. I'm ready to help. Is there a Bible question or a prayer request on your mind?",
            "I'm doing well, thanks. How are you? What can I help you with today?",
            "Thank you, that's kind. I'm here to help with Scripture, faith questions and prayer. What would you like to talk about?"
    };
    private static final String[] THANKS_REPLIES = {
            "You're welcome. I'm glad it helped. Is there anything else you would like to ask?",
            "You're very welcome. God bless you. Would you like another passage, or a prayer?",
            "Thank you for telling me. Ask anything else whenever you're ready."
    };
    private static final String[] GOODBYE_REPLIES = {
            "Goodbye, and God bless you. You can come back anytime.",
            "Take care, and may the Lord keep you. Come back whenever you need Scripture or prayer.",
            "Kwaheri, and God bless you. Come back anytime you want to read or pray together."
    };
    private static final String CAPABILITY_REPLY = "I'm glad you asked. I can find a Bible verse for you and quote it word for word from the "
            + "King James Version: try John 3:16, Psalm 23, or a phrase like 'Jesus wept'. I can search a single word and tell you how often "
            + "it appears, for example how many times 'faith' appears in Romans. I can explain what the approved church teaching says on "
            + "common questions, and I can pray with you or help you through something hard. I can't give medical, legal or financial "
            + "advice, or decide church discipline. If you are in danger right now, call 999 or 112.";
    private static final String[] META_REPLIES = {
            "You're right, my last reply did not help. Tell me in a few words what you need, such as a Bible passage, a prayer, or help with a situation, and I'll answer that directly.",
            "Sorry about that. Let me try again. Would a verse, an explanation, or a prayer help you most?"
    };
    private static final String[] MARRIAGE_REPLIES = {
            "Marriage is a big decision, and it is good to bring it to God in prayer. Are you asking how to prepare, how to know if someone is right for you, or what the Bible says about marriage? Tell me which one, and I'll help you with it.",
            "That is a serious step, and it is wise to think it through with God and with trusted people. What would help most right now: a Bible passage about marriage, guidance on preparing, or a prayer?"
    };

    private static final String[] ACKNOWLEDGE_REPLIES = {
            "Alright. Is there anything else you would like to ask?",
            "Good. Tell me when you are ready for the next question."
    };
    private static final String[] HELP_REPLIES = {
            "Of course. What do you need help with: a Bible passage, a prayer, or a situation you are facing?",
            "I'm glad you reached out. Tell me a little more. Is it a Bible question, a prayer, or something hard in your life?"
    };
    private static final String[] FOLLOW_UP_REPLIES = {
            "Gladly. Which part would you like more on: the passage itself, what it means for daily life, or a question that is still unclear?",
            "Yes. Tell me which part to go deeper into, and I'll keep to the King James Version and the approved teaching."
    };

    private static final AtomicInteger[] ROTATION = {
            new AtomicInteger(), new AtomicInteger(), new AtomicInteger(), new AtomicInteger(),
            new AtomicInteger(), new AtomicInteger(), new AtomicInteger(), new AtomicInteger(), new AtomicInteger()
    };

    private ConversationalTurn() {}

    /** The reply for a conversational message, or null when the message is not conversational. */
    public static Reply reply(String question) {
        if (question == null) return null;
        String q = question.trim().toLowerCase(Locale.ROOT).replace('’', '\'').replaceAll("\\s+", " ");
        if (q.isEmpty() || q.length() > 160) return null;
        if (GREETING.matcher(q).find()) return next(0, "routed:conversational-greeting", GREETINGS);
        if (ACKNOWLEDGE.matcher(q).find()) return next(6, "routed:conversational-acknowledge", ACKNOWLEDGE_REPLIES);
        if (HELP_REQUEST.matcher(q).find()) return next(7, "routed:conversational-help", HELP_REPLIES);
        if (FOLLOW_UP.matcher(q).find()) return next(8, "routed:conversational-follow-up", FOLLOW_UP_REPLIES);
        if (HOW_ARE_YOU.matcher(q).find()) return next(1, "routed:conversational-how-are-you", HOW_ARE_YOU_REPLIES);
        if (THANKS.matcher(q).find()) return next(2, "routed:conversational-thanks", THANKS_REPLIES);
        if (GOODBYE.matcher(q).find()) return next(3, "routed:conversational-goodbye", GOODBYE_REPLIES);
        if (q.length() <= 120 && META.matcher(q).find()) return next(4, "routed:conversational-meta", META_REPLIES);
        if (q.length() <= 120 && CAPABILITY.matcher(q).find()) {
            return new Reply("routed:conversational-capability", CAPABILITY_REPLY);
        }
        if (MARRIAGE_INTENT.matcher(q).find() && !MARRIAGE_SAFETY.matcher(q).find()) {
            return next(5, "routed:conversational-marriage-intent", MARRIAGE_REPLIES);
        }
        return null;
    }

    /** Approved facts every composed conversational reply may draw on. The model words them; it adds no claims. */
    static final String FACT_SHEET = "You are Perazim Guide, the digital assistant of Perazim Mission Church (PMCI). "
            + "You can find Bible verses and quote them word for word from the King James Version, for example John 3:16, Psalm 23, "
            + "or a phrase such as 'Jesus wept'. You can search a single word and say how often it appears and where, for example "
            + "how many times 'faith' appears in Romans. You can explain what the approved church teaching says on common questions, "
            + "and you can pray with a member or support them through a hard situation. You cannot give medical, legal or financial "
            + "advice, decide church discipline, or act for church leaders. If someone is in danger, they should call 999 or 112.";

    /**
     * The approved grounding for a composed reply to a conversational route, or null when the route is not composed.
     * The fixed wordings above are not used as output; the model composes from this material.
     */
    static String groundingFor(String status) {
        if (status == null) return null;
        switch (status) {
            case "routed:conversational-greeting":
                return FACT_SHEET + "\nThe member greeted the guide. Greet them warmly in one or two sentences, name two things you can do (for example finding a verse or searching a word), and ask what they would like.";
            case "routed:conversational-acknowledge":
                return FACT_SHEET + "\nThe member acknowledged the last reply. Reply briefly and invite the next question.";
            case "routed:conversational-help":
                return FACT_SHEET + "\nThe member asked for help without saying what with. Ask whether they need a Bible passage, a prayer, or help with a situation.";
            case "routed:conversational-follow-up":
                return FACT_SHEET + "\nThe member asked for more. Ask which part they want to go deeper into.";
            case "routed:conversational-how-are-you":
                return FACT_SHEET + "\nThe member asked how the guide is. Say briefly that you are ready to help, and invite a question.";
            case "routed:conversational-thanks":
                return FACT_SHEET + "\nThe member thanked the guide. Reply warmly and offer further help.";
            case "routed:conversational-goodbye":
                return FACT_SHEET + "\nThe member is leaving. Say goodbye warmly and invite them back.";
            case "routed:conversational-meta":
                return FACT_SHEET + "\nThe member says the last reply missed the point or repeated itself. Apologise briefly and ask them to say in a few words what they need.";
            case "routed:conversational-capability":
                return FACT_SHEET + "\nThe member asked what the guide can do. Explain it in plain words.";
            default:
                return null;
        }
    }

    private static Reply next(int slot, String status, String[] options) {
        int index = Math.floorMod(ROTATION[slot].getAndIncrement(), options.length);
        return new Reply(status, options[index]);
    }
}
