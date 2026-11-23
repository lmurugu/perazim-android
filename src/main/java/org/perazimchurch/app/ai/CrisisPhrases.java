package org.perazimchurch.app.ai;

import java.util.regex.Pattern;

/**
 * Locked crisis phrases for the on-phone and built-in guide. English phrases were already matched; the Swahili and
 * Sheng phrases were added on 2026-11-23 after Levi approved them. Only the crisis response depends on this pattern.
 */
public final class CrisisPhrases {
    private CrisisPhrases() {}

    public static final Pattern PATTERN = Pattern.compile(
            "\\b(suicid(?:e|al)|kill myself|end my life|want to die|better off dead|" +
            "self[ -]?harm|hurt myself|harm myself|cutting myself|unalive myself|" +
            "end it all|can't go on|cannot go on|not worth living|sexual assault|" +
            // Approved by Levi on 2026-11-23: suicidal wording that was missed.
            "don'?t see the point of (?:living|lviing|life|going on)|see the point of (?:living|lviing)|wants? to die|" +
            "kill (?:herself|himself|themselves)|" +
            "domestic violence|physically abused|(?:going to|plan to|want to) (?:kill|shoot|stab) (?:someone|him|her|them)|" +
            "(?:immediate|imminent) danger|" +
            // Swahili and Sheng: wanting to die, suicide, self-harm, ending one's life.
            "nataka kufa|nataka ku die|nataka kujiua|kujiua|najiua|nitajiua|" +
            "nataka kujidhuru|kujidhuru|najidhuru|nitajidhuru|" +
            "kujiumiza|nikijiumiza|kukomesha maisha|kukomesha maumivu|"+
            "sitaki kuishi|sina sababu ya kuishi|nimechoka kuishi|nataka kujimaliza|ntajimaliza|" +
            // Swahili: a person is hurting, threatening or abusing the speaker.
            "ananipiga|ananinyanyasa|ananitishia|ananiua)\\b", Pattern.CASE_INSENSITIVE);
}
