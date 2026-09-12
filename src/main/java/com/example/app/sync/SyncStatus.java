package com.example.app.sync;

/**
 * Formalized sync queue statuses for offline-first data synchronization.
 * Compliant with Phase 1.6 Sync Queue Mechanics.
 */
public enum SyncStatus {
    PENDING,
    SYNCING,
    SYNCED,
    FAILED,
    CONFLICT,
    CANCELLED
}
