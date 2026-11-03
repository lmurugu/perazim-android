package org.perazimchurch.app.domain.repository;

import org.perazimchurch.app.domain.model.Notification;
import java.util.List;

public interface NotificationRepository {
    List<Notification> getNotifications(String userId);
    int getUnreadCount(String userId);
    void markAsRead(String notificationId);
    void markAllAsRead(String userId);
    void addNotification(Notification notification);
    void deleteNotification(String notificationId);
}
