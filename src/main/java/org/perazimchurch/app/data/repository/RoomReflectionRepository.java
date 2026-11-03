package org.perazimchurch.app.data.repository;

import org.perazimchurch.app.data.local.dao.ReflectionDao;
import org.perazimchurch.app.data.local.entity.ReflectionEntity;
import org.perazimchurch.app.data.mapper.ReflectionMapper;
import org.perazimchurch.app.domain.model.Reflection;
import org.perazimchurch.app.domain.repository.ReflectionRepository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Room implementation of ReflectionRepository.
 */
public class RoomReflectionRepository implements ReflectionRepository {

    private final ReflectionDao reflectionDao;
    private final Set<String> completedReflectionIds = Collections.newSetFromMap(new ConcurrentHashMap<>());

    public RoomReflectionRepository(ReflectionDao reflectionDao) {
        this.reflectionDao = reflectionDao;
    }

    @Override
    public Reflection getTodayReflection() {
        List<ReflectionEntity> entities = reflectionDao.getAllReflections();
        if (entities != null && !entities.isEmpty()) {
            Reflection reflection = ReflectionMapper.toDomain(entities.get(0));
            if (reflection != null && completedReflectionIds.contains(reflection.getId())) {
                reflection.setCompleted(true);
            }
            return reflection;
        }
        return null;
    }

    @Override
    public List<Reflection> getRecentReflections() {
        List<ReflectionEntity> entities = reflectionDao.getAllReflections();
        if (entities == null) {
            return Collections.emptyList();
        }
        List<Reflection> list = new ArrayList<>(entities.size());
        for (ReflectionEntity entity : entities) {
            Reflection reflection = ReflectionMapper.toDomain(entity);
            if (reflection != null) {
                if (completedReflectionIds.contains(reflection.getId())) {
                    reflection.setCompleted(true);
                }
                list.add(reflection);
            }
        }
        return list;
    }

    @Override
    public Reflection getReflectionById(String id) {
        if (id == null) {
            return null;
        }
        ReflectionEntity entity = reflectionDao.getReflectionById(id);
        if (entity == null) {
            return null;
        }
        Reflection reflection = ReflectionMapper.toDomain(entity);
        if (reflection != null && completedReflectionIds.contains(reflection.getId())) {
            reflection.setCompleted(true);
        }
        return reflection;
    }

    @Override
    public void markReflectionCompleted(String reflectionId) {
        if (reflectionId != null) {
            completedReflectionIds.add(reflectionId);
        }
    }
}
