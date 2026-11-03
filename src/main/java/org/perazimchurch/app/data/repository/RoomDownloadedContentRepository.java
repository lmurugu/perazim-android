package org.perazimchurch.app.data.repository;

import org.perazimchurch.app.data.local.dao.DownloadedContentDao;
import org.perazimchurch.app.data.local.entity.DownloadedContentEntity;
import org.perazimchurch.app.data.mapper.DownloadedContentMapper;
import org.perazimchurch.app.domain.model.DownloadedContent;
import org.perazimchurch.app.domain.repository.DownloadedContentRepository;

import java.util.Collections;
import java.util.List;

/**
 * Room implementation of DownloadedContentRepository.
 * Manages physical downloaded media records via DownloadedContentDao.
 */
public class RoomDownloadedContentRepository implements DownloadedContentRepository {

    private final DownloadedContentDao downloadedContentDao;

    public RoomDownloadedContentRepository(DownloadedContentDao downloadedContentDao) {
        this.downloadedContentDao = downloadedContentDao;
    }

    @Override
    public List<DownloadedContent> getAllDownloads() {
        if (downloadedContentDao == null) {
            return Collections.emptyList();
        }
        List<DownloadedContentEntity> entities = downloadedContentDao.getAllDownloaded();
        return DownloadedContentMapper.toDomainList(entities);
    }

    @Override
    public DownloadedContent getDownloadByContentId(String contentId) {
        if (downloadedContentDao == null || contentId == null) {
            return null;
        }
        // 1. Direct query by primary key ID
        DownloadedContentEntity entity = downloadedContentDao.getContentById(contentId);
        if (entity != null) {
            return DownloadedContentMapper.toDomain(entity);
        }
        // 2. Query by prefixed ID (e.g., "dl_" + contentId)
        entity = downloadedContentDao.getContentById("dl_" + contentId);
        if (entity != null) {
            return DownloadedContentMapper.toDomain(entity);
        }
        // 3. Query by contentType "SERMON" and contentId
        entity = downloadedContentDao.getContentByTypeAndId("SERMON", contentId);
        if (entity != null) {
            return DownloadedContentMapper.toDomain(entity);
        }
        // 4. Scan all records
        List<DownloadedContentEntity> all = downloadedContentDao.getAllDownloaded();
        if (all != null) {
            for (DownloadedContentEntity e : all) {
                if (contentId.equals(e.getContentId()) || contentId.equals(e.getId())) {
                    return DownloadedContentMapper.toDomain(e);
                }
            }
        }
        return null;
    }

    @Override
    public void saveDownload(DownloadedContent content) {
        if (downloadedContentDao == null || content == null) {
            return;
        }
        DownloadedContentEntity entity = DownloadedContentMapper.toEntity(content);
        if (entity != null) {
            downloadedContentDao.insert(entity);
        }
    }

    @Override
    public void deleteDownload(String contentId) {
        if (downloadedContentDao == null || contentId == null) {
            return;
        }
        downloadedContentDao.deleteById(contentId);
        downloadedContentDao.deleteById("dl_" + contentId);
        List<DownloadedContentEntity> all = downloadedContentDao.getAllDownloaded();
        if (all != null) {
            for (DownloadedContentEntity e : all) {
                if (contentId.equals(e.getContentId()) || contentId.equals(e.getId())) {
                    downloadedContentDao.delete(e);
                    downloadedContentDao.deleteById(e.getId());
                }
            }
        }
    }

    @Override
    public boolean isDownloaded(String contentId) {
        if (contentId == null) {
            return false;
        }
        DownloadedContent content = getDownloadByContentId(contentId);
        return content != null && content.isCompleted();
    }
}
