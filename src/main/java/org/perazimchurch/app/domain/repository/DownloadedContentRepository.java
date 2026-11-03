package org.perazimchurch.app.domain.repository;

import org.perazimchurch.app.domain.model.DownloadedContent;

import java.util.List;

/**
 * Domain repository interface for managing device-local downloaded media assets.
 * Complies with Master Directive §21 (Architectural Separation).
 */
public interface DownloadedContentRepository {

    List<DownloadedContent> getAllDownloads();

    DownloadedContent getDownloadByContentId(String contentId);

    void saveDownload(DownloadedContent content);

    void deleteDownload(String contentId);

    boolean isDownloaded(String contentId);
}
