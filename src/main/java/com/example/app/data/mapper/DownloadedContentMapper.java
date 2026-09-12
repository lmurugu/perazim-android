package com.example.app.data.mapper;

import com.example.app.data.local.entity.DownloadedContentEntity;
import com.example.app.domain.model.DownloadedContent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Bi-directional mapper between DownloadedContentEntity and DownloadedContent domain model.
 * Complies with Guidebook §20 and Master Directive §21 (Architectural Separation).
 */
public final class DownloadedContentMapper {

    private DownloadedContentMapper() {
        // Utility class
    }

    public static DownloadedContent toDomain(DownloadedContentEntity entity) {
        if (entity == null) {
            return null;
        }
        boolean isCompleted = "COMPLETED".equalsIgnoreCase(entity.getStatus())
                || (entity.getStatus() == null && entity.getLocalUri() != null && !entity.getLocalUri().isEmpty());
        int progress = isCompleted ? 100 : ("DOWNLOADING".equalsIgnoreCase(entity.getStatus()) ? 50 : 0);

        return new DownloadedContent(
                entity.getId(),
                entity.getContentId(),
                entity.getContentType(),
                "", // Title is not stored in device-local downloaded_content table
                entity.getLocalUri(),
                entity.getFileSizeBytes(),
                entity.getDownloadMillis(),
                isCompleted,
                progress
        );
    }

    public static DownloadedContentEntity toEntity(DownloadedContent domain) {
        if (domain == null) {
            return null;
        }
        String id = domain.getId();
        if (id == null || id.trim().isEmpty()) {
            id = domain.getContentId() != null && !domain.getContentId().trim().isEmpty()
                    ? domain.getContentId()
                    : UUID.randomUUID().toString();
        }

        String status;
        if (domain.isCompleted() || domain.getDownloadProgressPercent() >= 100) {
            status = "COMPLETED";
        } else if (domain.getDownloadProgressPercent() > 0) {
            status = "DOWNLOADING";
        } else {
            status = "QUEUED";
        }

        long downloadMillis = domain.getDownloadedAt() > 0
                ? domain.getDownloadedAt()
                : System.currentTimeMillis();

        return new DownloadedContentEntity(
                id,
                domain.getContentType() != null ? domain.getContentType() : "SERMON",
                domain.getContentId() != null ? domain.getContentId() : id,
                domain.getLocalFilePath() != null ? domain.getLocalFilePath() : "",
                domain.getFileSizeBytes(),
                downloadMillis,
                status
        );
    }

    public static List<DownloadedContent> toDomainList(List<DownloadedContentEntity> entities) {
        if (entities == null) {
            return Collections.emptyList();
        }
        List<DownloadedContent> list = new ArrayList<>(entities.size());
        for (DownloadedContentEntity entity : entities) {
            DownloadedContent domain = toDomain(entity);
            if (domain != null) {
                list.add(domain);
            }
        }
        return list;
    }

    public static List<DownloadedContentEntity> toEntityList(List<DownloadedContent> domains) {
        if (domains == null) {
            return Collections.emptyList();
        }
        List<DownloadedContentEntity> list = new ArrayList<>(domains.size());
        for (DownloadedContent domain : domains) {
            DownloadedContentEntity entity = toEntity(domain);
            if (entity != null) {
                list.add(entity);
            }
        }
        return list;
    }
}
