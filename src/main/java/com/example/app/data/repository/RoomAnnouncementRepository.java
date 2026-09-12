package com.example.app.data.repository;

import com.example.app.data.local.dao.AnnouncementDao;
import com.example.app.data.local.entity.AnnouncementEntity;
import com.example.app.data.mapper.AnnouncementMapper;
import com.example.app.domain.model.Announcement;
import com.example.app.domain.repository.AnnouncementRepository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Room implementation of AnnouncementRepository.
 */
public class RoomAnnouncementRepository implements AnnouncementRepository {

    private final AnnouncementDao announcementDao;
    private final Set<String> readAnnouncementIds = Collections.newSetFromMap(new ConcurrentHashMap<>());

    public RoomAnnouncementRepository(AnnouncementDao announcementDao) {
        this.announcementDao = announcementDao;
    }

    @Override
    public List<Announcement> getActiveAnnouncements() {
        List<AnnouncementEntity> entities = announcementDao.getActiveAnnouncements(System.currentTimeMillis());
        if (entities == null) {
            return Collections.emptyList();
        }
        List<Announcement> list = new ArrayList<>(entities.size());
        for (AnnouncementEntity entity : entities) {
            Announcement announcement = AnnouncementMapper.toDomain(entity);
            if (announcement != null) {
                if (readAnnouncementIds.contains(announcement.getId())) {
                    announcement.setRead(true);
                }
                list.add(announcement);
            }
        }
        return list;
    }

    @Override
    public Announcement getAnnouncementById(String id) {
        if (id == null) {
            return null;
        }
        AnnouncementEntity entity = announcementDao.getAnnouncementById(id);
        if (entity == null) {
            return null;
        }
        Announcement announcement = AnnouncementMapper.toDomain(entity);
        if (announcement != null && readAnnouncementIds.contains(announcement.getId())) {
            announcement.setRead(true);
        }
        return announcement;
    }

    @Override
    public void markAsRead(String announcementId) {
        if (announcementId != null) {
            readAnnouncementIds.add(announcementId);
        }
    }
}
