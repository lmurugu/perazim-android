package org.perazimchurch.app.ai;

/**
 * Decides whether the guide may accept a new message while an earlier request may still be running (Levi, 2026-11-25).
 *
 * <p>A safety reply (crisis, abuse, third-party or self-harm wording) is never blocked by a request in flight, so a person
 * in danger always gets the safety response. A lock that has been held for longer than {@link #STALE_AFTER_MS} is treated
 * as stuck and no longer blocks sending, so the conversation cannot freeze for good.
 */
public final class GuideSendGate {

    /** A request held longer than this is treated as never answered. */
    public static final long STALE_AFTER_MS = 120_000L;

    private GuideSendGate() {}

    /**
     * True when the message may be accepted now.
     *
     * @param requestInFlight an earlier request has not finished yet
     * @param requestStartedAtMs when that request began (epoch milliseconds)
     * @param nowMs the current time (epoch milliseconds)
     * @param safetyRouted the message gets a safety reply, so it is never blocked
     */
    public static boolean mayAccept(boolean requestInFlight, long requestStartedAtMs, long nowMs, boolean safetyRouted) {
        if (safetyRouted) return true;
        if (!requestInFlight) return true;
        return nowMs - requestStartedAtMs >= STALE_AFTER_MS;
    }
}
