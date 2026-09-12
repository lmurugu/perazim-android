package com.example.app.data.mapper;

import com.example.app.data.local.entity.HymnEntity;
import com.example.app.domain.model.Hymn;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Mapper between HymnEntity and Hymn domain model.
 */
public final class HymnMapper {

    private HymnMapper() {
        // Utility class
    }

    public static Hymn toDomain(HymnEntity entity) {
        if (entity == null) {
            return null;
        }
        Hymn hymn = new Hymn(
                entity.getId(),
                entity.getNumber(),
                entity.getTitle(),
                "",
                entity.getKeySignature(),
                0,
                entity.getChords(),
                entity.getLyrics(),
                entity.getCategory(),
                entity.isFavorite()
        );
        hymn.setTimeSignature(entity.getTimeSignature());
        return hymn;
    }

    public static HymnEntity toEntity(Hymn domain) {
        if (domain == null) {
            return null;
        }
        return new HymnEntity(
                domain.getId() != null ? domain.getId() : "",
                domain.getNumber(),
                domain.getTitle(),
                domain.getLyrics(),
                domain.getChords(),
                domain.getKey(),
                "",
                domain.getCategory(),
                domain.isFavorite()
        );
    }

    public static List<Hymn> toDomainList(List<HymnEntity> entities) {
        if (entities == null) {
            return Collections.emptyList();
        }
        List<Hymn> list = new ArrayList<>(entities.size());
        for (HymnEntity entity : entities) {
            Hymn domain = toDomain(entity);
            if (domain != null) {
                list.add(domain);
            }
        }
        return list;
    }

    public static List<HymnEntity> toEntityList(List<Hymn> domains) {
        if (domains == null) {
            return Collections.emptyList();
        }
        List<HymnEntity> list = new ArrayList<>(domains.size());
        for (Hymn domain : domains) {
            HymnEntity entity = toEntity(domain);
            if (entity != null) {
                list.add(entity);
            }
        }
        return list;
    }
}
