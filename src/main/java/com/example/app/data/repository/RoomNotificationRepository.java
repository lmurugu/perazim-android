package com.example.app.data.repository;

import androidx.annotation.NonNull;

import com.example.app.data.local.dao.NotificationDao;
import com.example.app.data.local.entity.NotificationEntity;
import com.example.app.data.mapper.NotificationMapper;
import com.example.app.domain.model.Notification;
import com.example.app.domain.repository.NotificationRepository;

import java.util.Collections;
import java.util.List;

/**
 * Room implementation of {@link NotificationRepository}.
 * Manages push and in-app notifications, unread counts, and read statuses.
 */
public class RoomNotificationRepository implements NotificationRepository {

    private final NotificationDao notificationDao;

    public RoomNotificationRepository(@NonNull NotificationDao notificationDao) {
        this.notificationDao = notificationDao;
    }

    @Override
    public List<Notification> getNotifications(String userId) {
        if (userId == null) {
            return Collections.emptyList();
        }
        List<NotificationEntity> entities = notificationDao.getNotificationsForUser(userId);
        if (entities == null || entities.isEmpty()) {
            return Collections.emptyList();
        }
        return NotificationMapper.toDomainList(entities);
    }

    @Override
    public int getUnreadCount(String userId) {
        if (userId == null) {
            return 0;
        }
        return notificationDao.getUnreadCount(userId);
    }

    @Override
    public void markAsRead(String notificationId) {
        if (notificationId == null) {
            return;
        }
        notificationDao.markAsRead(notificationId);
    }

    @Override
    public void markAllAsRead(String userId) {
        if (userId == null) {
            return;
        }
        notificationDao.markAllAsRead(userId);
    }

    @Override
    public void addNotification(Notification notification) {
        if (notification == null) {
            return;
        }
        NotificationEntity entity = NotificationMapper.toEntity(notification);
        if (entity != null) {
            notificationDao.insert(entity);
        }
    }

    @Override
    public void deleteNotification(String notificationId) {
        if (notificationId == null) {
            return;
        }
        notificationDao.deleteById(notificationId);
    }
}
