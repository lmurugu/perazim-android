package com.example.app.data.mapper;

import com.example.app.data.local.entity.SermonEntity;
import com.example.app.domain.model.Sermon;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Mapper between SermonEntity and Sermon domain model.
 */
public final class SermonMapper {

    private SermonMapper() {
        // Utility class
    }

    public static Sermon toDomain(SermonEntity entity) {
        if (entity == null) {
            return null;
        }
        return new Sermon(
                entity.getId(),
                entity.getTitle(),
                entity.getPreacher(),
                entity.getPassage(),
                entity.getSeries(),
                entity.getAudioUrl(),
                entity.getVideoUrl(),
                "",
                entity.getDurationSeconds(),
                String.valueOf(entity.getDateMillis()),
                entity.getSummary(),
                entity.isDownloaded(),
                0L
        );
    }

    public static SermonEntity toEntity(Sermon domain) {
        if (domain == null) {
            return null;
        }
        long dateMillis = System.currentTimeMillis();
        if (domain.getDatePreached() != null) {
            try {
                dateMillis = Long.parseLong(domain.getDatePreached());
            } catch (NumberFormatException ignored) {
            }
        }
        return new SermonEntity(
                domain.getId() != null ? domain.getId() : "",
                domain.getTitle(),
                domain.getPreacher(),
                domain.getSeriesName(),
                domain.getScriptureReference(),
                domain.getAudioUrl(),
                domain.getVideoUrl(),
                dateMillis,
                (int) domain.getDurationSeconds(),
                domain.getDescription(),
                "",
                domain.isDownloaded(),
                ""
        );
    }

    public static List<Sermon> toDomainList(List<SermonEntity> entities) {
        if (entities == null) {
            return Collections.emptyList();
        }
        List<Sermon> list = new ArrayList<>(entities.size());
        for (SermonEntity entity : entities) {
            Sermon domain = toDomain(entity);
            if (domain != null) {
                list.add(domain);
            }
        }
        return list;
    }

    public static List<SermonEntity> toEntityList(List<Sermon> domains) {
        if (domains == null) {
            return Collections.emptyList();
        }
        List<SermonEntity> list = new ArrayList<>(domains.size());
        for (Sermon domain : domains) {
            SermonEntity entity = toEntity(domain);
            if (entity != null) {
                list.add(entity);
            }
        }
        return list;
    }
}
