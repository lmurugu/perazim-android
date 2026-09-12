package com.example.app.data.mapper;

import com.example.app.data.local.entity.ReflectionEntity;
import com.example.app.domain.model.Reflection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Mapper between ReflectionEntity and Reflection domain model.
 */
public final class ReflectionMapper {

    private ReflectionMapper() {
        // Utility class
    }

    public static Reflection toDomain(ReflectionEntity entity) {
        if (entity == null) {
            return null;
        }
        return new Reflection(
                entity.getId(),
                entity.getTitle(),
                entity.getPassageRef(),
                "",
                entity.getContent(),
                "",
                entity.getUserId(),
                String.valueOf(entity.getCreatedMillis()),
                false
        );
    }

    public static ReflectionEntity toEntity(Reflection domain) {
        if (domain == null) {
            return null;
        }
        long createdMillis = System.currentTimeMillis();
        if (domain.getDate() != null) {
            try {
                createdMillis = Long.parseLong(domain.getDate());
            } catch (NumberFormatException ignored) {
            }
        }
        String content = domain.getCommentary();
        if (content == null || content.isEmpty()) {
            content = domain.getPassageText() != null ? domain.getPassageText() : "";
        }
        return new ReflectionEntity(
                domain.getId() != null ? domain.getId() : "",
                domain.getAuthor() != null ? domain.getAuthor() : "",
                domain.getTitle(),
                content,
                domain.getScriptureReference(),
                createdMillis,
                createdMillis,
                false
        );
    }

    public static List<Reflection> toDomainList(List<ReflectionEntity> entities) {
        if (entities == null) {
            return Collections.emptyList();
        }
        List<Reflection> list = new ArrayList<>(entities.size());
        for (ReflectionEntity entity : entities) {
            Reflection domain = toDomain(entity);
            if (domain != null) {
                list.add(domain);
            }
        }
        return list;
    }

    public static List<ReflectionEntity> toEntityList(List<Reflection> domains) {
        if (domains == null) {
            return Collections.emptyList();
        }
        List<ReflectionEntity> list = new ArrayList<>(domains.size());
        for (Reflection domain : domains) {
            ReflectionEntity entity = toEntity(domain);
            if (entity != null) {
                list.add(entity);
            }
        }
        return list;
    }
}
