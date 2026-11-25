package org.perazimchurch.app.ai;

import java.util.regex.Pattern;

/**
 * Locked crisis phrases for the on-phone and built-in guide. English phrases were already matched; the Swahili and
 * Sheng phrases were added on 2026-11-23 after Levi approved them. Only the crisis response depends on this pattern.
 */
public final class CrisisPhrases {
    private CrisisPhrases() {}

    public static final Pattern PATTERN = Pattern.compile(
            // Added 2026-11-25 (MG-S24, "I'm thinking of ending my life"): ending-life wording. Levi to review this addition.
            "\\b(suicid(?:e|al)|kill myself|end my life|ending (?:my|his|her|their|our) life|end (?:his|her|their) life|want to die|better off dead|" +
            "self[ -]?harm|hurt myself|harm myself|cutting myself|unalive myself|" +
            "end it all|can't go on|cannot go on|not worth living|sexual assault|" +
            // Approved by Levi on 2026-11-23: suicidal wording that was missed.
            "don'?t see the point of (?:living|lviing|life|going on)|see the point of (?:living|lviing)|wants? to die|" +
            // Added 2026-11-25 from the Kenya sets (Levi to review this crisis-list addition): hopelessness and giving things away.
            "nothing to live for|no reason to live|gave (?:his|her|my|their) (?:things|belongings|possessions) away|" +
            "won'?t be here (?:next|any)|will not be here (?:next|any)|" +
            "kill (?:herself|himself|themselves)|" +
            // Added 2026-11-25 (Swahili, KB-072 and Kenya sets; Levi to review this crisis-list addition): "tired of this life", "want to die", "I do not want to live".
            "nataka kufa|sitaki kuishi|najiua|nitajiua|" +
            "domestic violence|physically abused|(?:going to|plan to|want to) (?:kill|shoot|stab) (?:someone|him|her|them)|" +
            "(?:immediate|imminent) danger|" +
            // Added 2026-11-24 from the evaluation's safety failures (EX-074, EX-082, EX-083): plain suicidal wording.
            "don'?t want to (?:wake up|live|be alive|exist|be here)|better off without me|everyone would be better off|" +
            "(?:life|it)(?:'s| is| isn'?t| is not) (?:not )?worth (?:living|it)|" +
            "(?:have|has) a plan to (?:die|end|kill|hurt)|thinking (?:about|of) (?:ways|how) to (?:end|kill|die|hurt)|" +
            // Swahili and Sheng: wanting to die, suicide, self-harm, ending one's life.
            "nataka kufa|nataka ku die|nataka kujiua|kujiua|najiua|nitajiua|" +
            "nataka kujidhuru|kujidhuru|najidhuru|nitajidhuru|" +
            "kujiumiza|nikijiumiza|kukomesha maisha|kukomesha maumivu|"+
            "sitaki kuishi|sina sababu ya kuishi|nimechoka kuishi|nataka kujimaliza|ntajimaliza|" +
            // Approved by Levi as a native Kiswahili speaker, 2026-11-25 (message cmsg_01TZR4y8Vg1KsGsVuQZmh5wy6mZdLtfvqKyZG6SZVRvYUj).
            // Status APPROVED_LEVI for the five phrases above and these four variants. Tiers are recorded in
            // ai-eval checklist: "nimechoka na maisha" is a compassionate safety check, the others are urgent.
            // Variants added here only strengthen detection; no phrase was removed.
            "nina mawazo ya kujiua|nataka kujitoa uhai|nimefikiria kujiua|nina mpango wa kujiua|" +
            // Added 2026-11-25 from the Kenya set (KE-092): "it is better that I die", "my children would be better without me". Levi to review this crisis-list addition.
            "bora nife|watakuwa bora bila mimi|bora bila mimi|" +
            // Swahili: a person is hurting, threatening or abusing the speaker.
            "ananipiga|ananinyanyasa|ananitishia|ananiua)\\b", Pattern.CASE_INSENSITIVE);
}
