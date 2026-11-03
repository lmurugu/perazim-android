package org.perazimchurch.app.data.repository;

import org.perazimchurch.app.data.local.dao.EventDao;
import org.perazimchurch.app.data.local.entity.EventEntity;
import org.perazimchurch.app.data.mapper.EventMapper;
import org.perazimchurch.app.domain.model.Event;
import org.perazimchurch.app.domain.repository.EventRepository;

import java.util.List;

/**
 * Room implementation of EventRepository.
 */
public class RoomEventRepository implements EventRepository {

    private final EventDao eventDao;

    public RoomEventRepository(EventDao eventDao) {
        this.eventDao = eventDao;
    }

    @Override
    public List<Event> getUpcomingEvents() {
        List<EventEntity> entities = eventDao.getUpcomingEvents(System.currentTimeMillis());
        return EventMapper.toDomainList(entities);
    }

    @Override
    public List<Event> getEventsByCampus(String campusId) {
        if (campusId == null) {
            return getUpcomingEvents();
        }
        List<EventEntity> entities = eventDao.getEventsByCampus(campusId);
        return EventMapper.toDomainList(entities);
    }

    @Override
    public Event getEventById(String id) {
        if (id == null) {
            return null;
        }
        EventEntity entity = eventDao.getEventById(id);
        return EventMapper.toDomain(entity);
    }
}
