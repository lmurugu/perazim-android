package org.perazimchurch.app.data.mapper;

import androidx.annotation.Nullable;

import org.perazimchurch.app.data.local.entity.NotificationEntity;
import org.perazimchurch.app.domain.model.Notification;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Mapper for converting between Room {@link NotificationEntity} and domain {@link Notification}.
 */
public final class NotificationMapper {

    private NotificationMapper() {
        // Utility class
    }

    /**
     * Maps a {@link NotificationEntity} to a domain {@link Notification}.
     */
    @Nullable
    public static Notification toDomain(@Nullable NotificationEntity entity) {
        if (entity == null) {
            return null;
        }

        Notification notification = new Notification();
        notification.setId(entity.getId());
        notification.setUserId(entity.getUserId());
        notification.setTitle(entity.getTitle());
        notification.setMessage(entity.getBody());
        notification.setType(entity.getType());
        notification.setTargetRoute(entity.getActionTarget());
        notification.setRead(entity.isRead());
        notification.setTimestamp(entity.getCreatedMillis());

        return notification;
    }

    /**
     * Maps a domain {@link Notification} to a Room {@link NotificationEntity}.
     */
    @Nullable
    public static NotificationEntity toEntity(@Nullable Notification domain) {
        if (domain == null) {
            return null;
        }

        String id = domain.getId() != null ? domain.getId() : UUID.randomUUID().toString();
        long createdMillis = domain.getTimestamp() > 0 ? domain.getTimestamp() : System.currentTimeMillis();
        String type = domain.getType() != null ? domain.getType() : "SYSTEM";
        String body = domain.getMessage() != null ? domain.getMessage() : "";

        return new NotificationEntity(
                id,
                domain.getUserId(),
                domain.getTitle(),
                body,
                type,
                domain.getTargetRoute(),
                domain.isRead(),
                createdMillis
        );
    }

    /**
     * Maps a list of {@link NotificationEntity} to a list of domain {@link Notification}.
     */
    public static List<Notification> toDomainList(@Nullable List<NotificationEntity> entities) {
        if (entities == null || entities.isEmpty()) {
            return Collections.emptyList();
        }
        List<Notification> list = new ArrayList<>(entities.size());
        for (NotificationEntity entity : entities) {
            Notification notification = toDomain(entity);
            if (notification != null) {
                list.add(notification);
            }
        }
        return list;
    }

    /**
     * Maps a list of domain {@link Notification} to a list of {@link NotificationEntity}.
     */
    public static List<NotificationEntity> toEntityList(@Nullable List<Notification> domains) {
        if (domains == null || domains.isEmpty()) {
            return Collections.emptyList();
        }
        List<NotificationEntity> list = new ArrayList<>(domains.size());
        for (Notification domain : domains) {
            NotificationEntity entity = toEntity(domain);
            if (entity != null) {
                list.add(entity);
            }
        }
        return list;
    }
}
