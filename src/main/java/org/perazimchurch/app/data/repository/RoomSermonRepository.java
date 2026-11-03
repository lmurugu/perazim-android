package org.perazimchurch.app.data.repository;

import org.perazimchurch.app.data.local.dao.SermonDao;
import org.perazimchurch.app.data.local.entity.SermonEntity;
import org.perazimchurch.app.data.mapper.SermonMapper;
import org.perazimchurch.app.domain.model.Sermon;
import org.perazimchurch.app.domain.repository.SermonRepository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Room implementation of SermonRepository.
 */
public class RoomSermonRepository implements SermonRepository {

    private final SermonDao sermonDao;
    private final Map<String, Long> playCounts = new ConcurrentHashMap<>();

    public RoomSermonRepository(SermonDao sermonDao) {
        this.sermonDao = sermonDao;
    }

    @Override
    public List<Sermon> getRecentSermons() {
        List<SermonEntity> entities = sermonDao.getAllSermons();
        List<Sermon> sermons = SermonMapper.toDomainList(entities);
        attachPlayCounts(sermons);
        return sermons;
    }

    @Override
    public Sermon getSermonById(String id) {
        if (id == null) {
            return null;
        }
        SermonEntity entity = sermonDao.getSermonById(id);
        Sermon sermon = SermonMapper.toDomain(entity);
        if (sermon != null) {
            Long count = playCounts.get(sermon.getId());
            if (count != null) {
                sermon.setPlayCount(count);
            }
        }
        return sermon;
    }

    @Override
    public List<Sermon> searchSermons(String query) {
        if (query == null || query.trim().isEmpty()) {
            return getRecentSermons();
        }
        String lowerQuery = query.toLowerCase().trim();
        List<SermonEntity> entities = sermonDao.getAllSermons();
        if (entities == null) {
            return Collections.emptyList();
        }
        List<SermonEntity> matched = new ArrayList<>();
        for (SermonEntity entity : entities) {
            boolean matchesTitle = entity.getTitle() != null && entity.getTitle().toLowerCase().contains(lowerQuery);
            boolean matchesPreacher = entity.getPreacher() != null && entity.getPreacher().toLowerCase().contains(lowerQuery);
            boolean matchesSeries = entity.getSeries() != null && entity.getSeries().toLowerCase().contains(lowerQuery);
            if (matchesTitle || matchesPreacher || matchesSeries) {
                matched.add(entity);
            }
        }
        List<Sermon> sermons = SermonMapper.toDomainList(matched);
        attachPlayCounts(sermons);
        return sermons;
    }

    @Override
    public List<Sermon> getDownloadedSermons() {
        List<SermonEntity> entities = sermonDao.getDownloadedSermons();
        List<Sermon> sermons = SermonMapper.toDomainList(entities);
        attachPlayCounts(sermons);
        return sermons;
    }

    @Override
    public void markAsDownloaded(String sermonId, String localPath) {
        if (sermonId == null) {
            return;
        }
        SermonEntity entity = sermonDao.getSermonById(sermonId);
        if (entity != null) {
            entity.setDownloaded(true);
            entity.setLocalAudioPath(localPath != null ? localPath : "");
            sermonDao.update(entity);
        }
    }

    @Override
    public void recordPlay(String sermonId) {
        if (sermonId == null) {
            return;
        }
        playCounts.compute(sermonId, (k, v) -> (v == null) ? 1L : v + 1L);
        SermonEntity entity = sermonDao.getSermonById(sermonId);
        if (entity != null) {
            sermonDao.update(entity);
        }
    }

    private void attachPlayCounts(List<Sermon> sermons) {
        if (sermons == null) {
            return;
        }
        for (Sermon sermon : sermons) {
            if (sermon != null) {
                Long count = playCounts.get(sermon.getId());
                if (count != null) {
                    sermon.setPlayCount(count);
                }
            }
        }
    }
}
