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
            "^(?:why are you (?:repeating|saying the same)[^?]*|you (?:didn'?t|did not) (?:answer|respond)[^?]*|you'?re repeating[^?]*|"
                    + "that'?s not (?:what|my)[^?]*|not what i asked|say it (?:another|a different|differently)(?: way)?|try again|"
                    + "answer my question|stop repeating[^?]*|same (?:message|answer|reply|thing|response)|repeating the same[^?]*)[ !.,?]*$");
    private static final Pattern MARRIAGE_INTENT = Pattern.compile(
            "\\b(?:want to (?:get )?marr(?:y|ied)|want to marry|going to (?:get )?marr(?:y|ied)|thinking (?:of|about) (?:getting )?marr(?:y|iage)|"
                    + "getting married|how (?:do|can) i (?:choose|find|know|pick)(?: a| the)? (?:husband|wife|spouse|partner)|"
                    + "want a (?:husband|wife)|need a (?:husband|wife)|looking for (?:a )?(?:husband|wife|spouse))\\b");
    /** Any of these means the message is about force, age or consent: it goes to the safety and approved routes instead. */
    private static final Pattern MARRIAGE_SAFETY = Pattern.compile(
            "\\b(?:forc\\w*|pressur\\w*|threat\\w*|against my will|do not love|don'?t love|under 1[0-8]|aged?|years old|child|minor|"
                    + "school|parents? (?:want|choose|decide|pick)|kidnap\\w*|abus\\w*|dowry|mahari|kugongeana)\\b");

    private static final String[] GREETINGS = {
            "Hello, and welcome. What would you like to explore today: a Bible passage, a question about faith, or prayer?",
            "Hi there. I'm glad you came. Tell me what is on your heart, or ask me something from the Bible.",
            "Good to see you. I can quote the King James Version, talk through a question of faith, or pray with you. Where would you like to begin?",
            "Hello. You can ask about a Bible passage, a faith question, or a situation you are facing. What is on your mind?",
            "Hey, let's begin. Is there a passage you want to read, or something you would like to bring to God today?"
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
    private static final String CAPABILITY_REPLY = "I can quote Bible passages from the King James Version, explain what the approved "
            + "church teaching says on common questions, help you with a prayer or a reflection, and show you how to use the app. "
            + "I can't give medical, legal or financial advice, decide church discipline, or act for church leaders. "
            + "If you are in danger right now, call 999 or 112.";
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

    private static Reply next(int slot, String status, String[] options) {
        int index = Math.floorMod(ROTATION[slot].getAndIncrement(), options.length);
        return new Reply(status, options[index]);
    }
}
