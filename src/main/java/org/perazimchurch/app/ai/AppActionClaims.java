package org.perazimchurch.app.ai;

import androidx.annotation.Nullable;

import java.util.regex.Pattern;

/**
 * Detects assistant replies that claim an app operation happened, or that the assistant will perform one, or that
 * it speaks for a human church authority. Pure Java with no Android dependencies so it can be tested on the host.
 *
 * Only the app itself can confirm that a prayer request, membership record, message or other operation succeeded.
 * Replies matching these patterns must be withheld and replaced with a safe boundary message.
 */
public final class AppActionClaims {

    private static final String NOUN = "(?:request|prayer|message|concern|petition|membership|registration|record|"
            + "profile|application|note|testimony|feedback|report|donation|payment|details|information)";

    /** Completed-action claims in the first or third person, including contractions, passive and promise forms. */
    private static final Pattern CLAIMED_APP_ACTION = Pattern.compile(
            // "I have sent", "I've submitted", "we recorded"
            "\\b(?:i|we)(?:'ve| have| had)?\\s+(?:already\\s+)?(?:submitted|sent|registered|updated|contacted|changed|"
            + "forwarded|notified|recorded|saved|added|passed|informed|alerted|booked|cancelled|canceled|deleted|removed|"
            + "logged|posted|done)\\b"
            // "Your request has been submitted", "your message was sent", "the prayer is now recorded"
            + "|\\b(?:your|the|this|my)\\s+(?:[a-z]+\\s+){0,2}?" + NOUN + "s?\\s+(?:has|have|was|were|is|are)\\s+"
            + "(?:now\\s+|already\\s+|successfully\\s+)?(?:been\\s+)?(?:submitted|sent|registered|updated|contacted|"
            + "changed|forwarded|notified|recorded|saved|added|received|logged|posted|booked|cancelled|canceled|removed|"
            + "approved)\\b"
            // "successfully sent"
            + "|\\bsuccessfully\\s+(?:submitted|sent|registered|updated|saved|recorded|added|forwarded|notified|booked)\\b"
            // "passed this on to", "forwarded it to"
            + "|\\b(?:passed|forwarded|sent)\\s+(?:this|it|that|your\\s+\\w+)\\s+(?:on|along|to)\\b"
            // Promises of app action: "I'll submit", "I will notify", "I can now register"
            + "|\\bi(?:'ll| will| am going to| can now)\\s+(?:submit|send|forward|notify|register|record|save|update|"
            + "contact|pass|inform|alert|book|cancel|delete|remove)\\b"
            // "I have let the pastor know"
            + "|\\bi(?:'ve| have)?\\s+(?:let|told|informed|alerted)\\s+(?:the\\s+|your\\s+)?(?:pastor|bishop|leader|leaders|"
            + "elders?|deacons?|church office)\\b",
            Pattern.CASE_INSENSITIVE);

    /** Claims to be a human church authority or to perform a pastoral act. */
    private static final Pattern CLAIMED_HUMAN_AUTHORITY = Pattern.compile(
            "\\bi(?:\\s+am|'m)\\s+(?:your\\s+|the\\s+|a\\s+|an\\s+)?(?:church\\s+)?"
            + "(?:pastor|bishop|priest|elder|deacon|counsellor|counselor|leader)\\b|"
            + "\\bi\\s+(?:now\\s+)?(?:declare|pronounce|forgive)\\s+you\\b",
            Pattern.CASE_INSENSITIVE);

    private AppActionClaims() {}

    /** True when the reply claims a completed or promised app action, or speaks as a human church authority. */
    public static boolean claimsAppActionOrHumanAuthority(@Nullable String answer) {
        if (answer == null) return false;
        String normalised = answer.replace('’', '\'');
        return CLAIMED_APP_ACTION.matcher(normalised).find() || CLAIMED_HUMAN_AUTHORITY.matcher(normalised).find();
    }
}
