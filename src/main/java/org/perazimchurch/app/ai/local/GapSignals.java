package org.perazimchurch.app.ai.local;

import org.perazimchurch.app.ai.gaps.GapReason;

import java.util.Locale;

/**
 * Maps the guide's own outcomes to gap reason codes. Lives beside OnDeviceAi so it can use the same reply constants
 * and cannot drift from them. Returns null for anything that is a real answer or a system notice, not a gap.
 */
public final class GapSignals {

    /** Errors that are system notices, not failed answers. */
    private static final String[] SYSTEM_MARKERS = {
            "integrity", "installed", "Model download", "Download the on-device", "runtime", "cancel", "already running",
            "unavailable", "too long", "could not run", "not available"};

    private GapSignals() {}

    /** Reason for a routed status (see GuideTurnRouter), or null when the status is an answer. */
    public static String reasonForRouted(String status) {
        if (status == null) return null;
        if (status.startsWith("routed:no-go:")) return GapReason.NO_GO_DECLINE;
        if ("routed:scope-refusal".equals(status)) return GapReason.SCOPE_DECLINE;
        if ("refused:no-verified-source".equals(status)) return GapReason.NO_VERIFIED_SOURCE;
        if ("scripture-only".equals(status)) return GapReason.SCRIPTURE_ONLY;
        switch (status) {
            case "routed:verse-phrase-not-found":
            case "routed:verse-about-not-found":
            case "routed:word-search-none":
            case "routed:verse-not-in-offline-text":
            case "routed:missing-reference":
            case "routed:strongs-not-found":
                return GapReason.NO_VERIFIED_SOURCE;
            default:
                return null;
        }
    }

    /** Reason for a model answer that was returned as text: the Scripture-only fallback or the no-source line. */
    public static String reasonForAnswer(String answer) {
        if (answer == null) return null;
        if (answer.startsWith(OwnKnowledge.SCRIPTURE_ONLY_LEAD)) return GapReason.SCRIPTURE_ONLY;
        if (answer.equals(OnDeviceAi.NO_SOURCE_MESSAGE)) return GapReason.NO_VERIFIED_SOURCE;
        return null;
    }

    /** Reason for a model error message: no source, or an answer rejected by the checks. System notices return null. */
    public static String reasonForModelError(String message) {
        if (message == null || message.trim().isEmpty()) return null;
        if (message.equals(OnDeviceAi.NO_SOURCE_MESSAGE)) return GapReason.NO_VERIFIED_SOURCE;
        String lower = message.toLowerCase(Locale.ROOT);
        for (String marker : SYSTEM_MARKERS) {
            if (lower.contains(marker.toLowerCase(Locale.ROOT))) return null;
        }
        return GapReason.MODEL_REJECTED;
    }
}
