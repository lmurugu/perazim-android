package com.example.app.community.privacy;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.app.domain.repository.ConnectionRepository;

/**
 * Helper class for enforcing community safety, abuse protection, and privacy boundaries.
 * Governs message dispatch eligibility and member blocking/reporting operations.
 */
public final class CommunityPrivacyHelper {

    private static final String TAG = "CommunityPrivacyHelper";

    private CommunityPrivacyHelper() {
        // Non-instantiable utility class
    }

    /**
     * Checks if messaging is permitted between the sender and recipient according to blocklists.
     * Both sender and recipient must not have blocked each other.
     *
     * @param connRepo    the connection repository to inspect blocklists
     * @param senderId    the ID of the messaging sender
     * @param recipientId the ID of the messaging recipient
     * @return true if messaging is allowed; false if either party has blocked the other or parameters are null
     */
    public static boolean canMessage(@Nullable ConnectionRepository connRepo,
                                     @Nullable String senderId,
                                     @Nullable String recipientId) {
        if (connRepo == null || senderId == null || recipientId == null) {
            return false;
        }
        if (senderId.trim().isEmpty() || recipientId.trim().isEmpty()) {
            return false;
        }
        return !connRepo.isBlocked(senderId, recipientId) && !connRepo.isBlocked(recipientId, senderId);
    }

    /**
     * Blocks an offending member and records an abuse report reason.
     *
     * @param connRepo        the connection repository
     * @param reporterId      the reporting member's ID
     * @param offendingPeerId the offending peer's ID to block
     * @param reason          the reason for reporting/blocking
     */
    public static void blockAndReport(@Nullable ConnectionRepository connRepo,
                                      @NonNull String reporterId,
                                      @NonNull String offendingPeerId,
                                      @Nullable String reason) {
        if (connRepo == null || reporterId == null || offendingPeerId == null) {
            return;
        }
        Log.w(TAG, "Abuse report filed by [" + reporterId + "] against offending peer [" + offendingPeerId + "]. Reason: " + (reason != null ? reason : "Unspecified"));
        connRepo.blockMember(reporterId, offendingPeerId);
    }
}
