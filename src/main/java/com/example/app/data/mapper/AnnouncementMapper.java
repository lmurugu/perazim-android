package com.example.app.data.mapper;

import com.example.app.data.local.entity.AnnouncementEntity;
import com.example.app.domain.model.Announcement;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Mapper between AnnouncementEntity and Announcement domain model.
 */
public final class AnnouncementMapper {

    private AnnouncementMapper() {
        // Utility class
    }

    public static Announcement toDomain(AnnouncementEntity entity) {
        if (entity == null) {
            return null;
        }
        return new Announcement(
                entity.getId(),
                entity.getTitle(),
                entity.getBody(),
                "",
                entity.getPublishedMillis(),
                entity.getPriority(),
                false
        );
    }

    public static AnnouncementEntity toEntity(Announcement domain) {
        if (domain == null) {
            return null;
        }
        long published = domain.getTimestamp() > 0 ? domain.getTimestamp() : System.currentTimeMillis();
        long expires = published + (7L * 24 * 60 * 60 * 1000);
        return new AnnouncementEntity(
                domain.getId() != null ? domain.getId() : "",
                domain.getTitle(),
                domain.getBody(),
                domain.getPriority(),
                published,
                expires,
                ""
        );
    }

    public static List<Announcement> toDomainList(List<AnnouncementEntity> entities) {
        if (entities == null) {
            return Collections.emptyList();
        }
        List<Announcement> list = new ArrayList<>(entities.size());
        for (AnnouncementEntity entity : entities) {
            Announcement domain = toDomain(entity);
            if (domain != null) {
                list.add(domain);
            }
        }
        return list;
    }

    public static List<AnnouncementEntity> toEntityList(List<Announcement> domains) {
        if (domains == null) {
            return Collections.emptyList();
        }
        List<AnnouncementEntity> list = new ArrayList<>(domains.size());
        for (Announcement domain : domains) {
            AnnouncementEntity entity = toEntity(domain);
            if (entity != null) {
                list.add(entity);
            }
        }
        return list;
    }
}
