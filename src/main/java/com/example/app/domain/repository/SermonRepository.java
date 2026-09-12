package com.example.app.domain.repository;

import com.example.app.domain.model.Sermon;
import java.util.List;

/**
 * Repository interface defining data operations for Sermons and media broadcasts.
 */
public interface SermonRepository {
    List<Sermon> getRecentSermons();
    Sermon getSermonById(String id);
    List<Sermon> searchSermons(String query);
    List<Sermon> getDownloadedSermons();
    void markAsDownloaded(String sermonId, String localPath);
    void recordPlay(String sermonId);
}
