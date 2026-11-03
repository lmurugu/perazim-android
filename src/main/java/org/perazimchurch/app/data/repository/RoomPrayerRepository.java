package org.perazimchurch.app.data.repository;

import androidx.annotation.NonNull;

import org.perazimchurch.app.data.local.dao.PrayerDao;
import org.perazimchurch.app.data.local.entity.PrayerEntity;
import org.perazimchurch.app.data.mapper.PrayerMapper;
import org.perazimchurch.app.domain.model.Prayer;
import org.perazimchurch.app.domain.repository.PrayerRepository;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Room implementation of {@link PrayerRepository}.
 * Manages prayer petitions, public prayer wall feed, personal prayers, amen counts, and answered prayers.
 */
public class RoomPrayerRepository implements PrayerRepository {

    private final PrayerDao prayerDao;
    private final Map<String, Set<String>> userAmenedMap = new ConcurrentHashMap<>();

    public RoomPrayerRepository(@NonNull PrayerDao prayerDao) {
        this.prayerDao = prayerDao;
    }

    @Override
    public List<Prayer> getPublicPrayers() {
        List<PrayerEntity> entities = prayerDao.getAllPrayers();
        if (entities == null || entities.isEmpty()) {
            return Collections.emptyList();
        }
        return PrayerMapper.toDomainList(entities);
    }

    @Override
    public List<Prayer> getUserPrayers(String userId) {
        if (userId == null) {
            return Collections.emptyList();
        }
        List<PrayerEntity> entities = prayerDao.getPrayersByUser(userId);
        if (entities == null || entities.isEmpty()) {
            return Collections.emptyList();
        }
        return PrayerMapper.toDomainList(entities);
    }

    @Override
    public Prayer getPrayerById(String id) {
        if (id == null) {
            return null;
        }
        PrayerEntity entity = prayerDao.getPrayerById(id);
        return PrayerMapper.toDomain(entity);
    }

    @Override
    public void submitPrayer(Prayer prayer) {
        if (prayer == null) {
            return;
        }
        PrayerEntity entity = PrayerMapper.toEntity(prayer);
        if (entity != null) {
            prayerDao.insert(entity);
        }
    }

    @Override
    public void amenPrayer(String prayerId, String userId) {
        if (prayerId == null) {
            return;
        }
        PrayerEntity entity = prayerDao.getPrayerById(prayerId);
        if (entity != null) {
            entity.setPrayerCount(entity.getPrayerCount() + 1);
            prayerDao.update(entity);
        }
        if (userId != null) {
            userAmenedMap.computeIfAbsent(userId, k -> ConcurrentHashMap.newKeySet()).add(prayerId);
        }
    }

    @Override
    public void markAnswered(String prayerId) {
        if (prayerId == null) {
            return;
        }
        PrayerEntity entity = prayerDao.getPrayerById(prayerId);
        if (entity != null) {
            entity.setAnswered(true);
            prayerDao.update(entity);
        }
    }

    @Override
    public List<Prayer> getAnsweredPrayers() {
        List<PrayerEntity> entities = prayerDao.getAnsweredPrayers();
        if (entities == null || entities.isEmpty()) {
            return Collections.emptyList();
        }
        return PrayerMapper.toDomainList(entities);
    }

    @Override
    public void markAnsweredWithTestimony(String prayerId, String testimonyText) {
        if (prayerId == null) {
            return;
        }
        PrayerEntity entity = prayerDao.getPrayerById(prayerId);
        if (entity != null) {
            entity.setAnswered(true);
            if (testimonyText != null && !testimonyText.trim().isEmpty()) {
                String existing = entity.getContent();
                String updatedContent = (existing != null && !existing.isEmpty())
                        ? existing + "\n\n[Testimony]: " + testimonyText.trim()
                        : "[Testimony]: " + testimonyText.trim();
                entity.setContent(updatedContent);
            }
            prayerDao.update(entity);
        }
    }

    @Override
    public void deletePrayer(String prayerId) {
        if (prayerId == null) {
            return;
        }
        prayerDao.deleteById(prayerId);
    }
}
