package com.example.app.data.mapper;

import com.example.app.data.local.entity.EventEntity;
import com.example.app.domain.model.Event;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Mapper between EventEntity and Event domain model.
 */
public final class EventMapper {

    private EventMapper() {
        // Utility class
    }

    public static Event toDomain(EventEntity entity) {
        if (entity == null) {
            return null;
        }
        return new Event(
                entity.getId(),
                entity.getTitle(),
                entity.getDescription(),
                String.valueOf(entity.getStartTimeMillis()),
                entity.getStartTimeMillis(),
                entity.getLocation(),
                entity.getCampusId(),
                entity.getCategory(),
                entity.getImageUrl()
        );
    }

    public static EventEntity toEntity(Event domain) {
        if (domain == null) {
            return null;
        }
        long startTime = domain.getTimestamp();
        if (startTime <= 0 && domain.getDateText() != null) {
            try {
                startTime = Long.parseLong(domain.getDateText());
            } catch (NumberFormatException ignored) {
            }
        }
        if (startTime <= 0) {
            startTime = System.currentTimeMillis();
        }
        return new EventEntity(
                domain.getId() != null ? domain.getId() : "",
                domain.getTitle(),
                domain.getDescription(),
                startTime,
                startTime,
                domain.getLocation(),
                domain.getCampusId(),
                domain.getBannerUrl(),
                domain.getCategory()
        );
    }

    public static List<Event> toDomainList(List<EventEntity> entities) {
        if (entities == null) {
            return Collections.emptyList();
        }
        List<Event> list = new ArrayList<>(entities.size());
        for (EventEntity entity : entities) {
            Event domain = toDomain(entity);
            if (domain != null) {
                list.add(domain);
            }
        }
        return list;
    }

    public static List<EventEntity> toEntityList(List<Event> domains) {
        if (domains == null) {
            return Collections.emptyList();
        }
        List<EventEntity> list = new ArrayList<>(domains.size());
        for (Event domain : domains) {
            EventEntity entity = toEntity(domain);
            if (entity != null) {
                list.add(entity);
            }
        }
        return list;
    }
}
