package org.perazimchurch.app.media.download;

/**
 * Status of local media downloads.
 * Complies with Master Directive §21.
 */
public enum DownloadStatus {
    QUEUED,
    DOWNLOADING,
    COMPLETED,
    FAILED
}
