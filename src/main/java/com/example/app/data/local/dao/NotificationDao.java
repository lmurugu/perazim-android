package com.example.app.data.local.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.app.data.local.entity.NotificationEntity;

import java.util.List;

@Dao
public interface NotificationDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(NotificationEntity notification);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<NotificationEntity> notifications);

    @Update
    void update(NotificationEntity notification);

    @Delete
    void delete(NotificationEntity notification);

    @Query("SELECT * FROM notifications WHERE id = :id LIMIT 1")
    NotificationEntity getNotificationById(String id);

    @Query("SELECT * FROM notifications WHERE userId = :userId ORDER BY createdMillis DESC")
    List<NotificationEntity> getNotificationsForUser(String userId);

    @Query("SELECT COUNT(*) FROM notifications WHERE userId = :userId AND isRead = 0")
    int getUnreadCount(String userId);

    @Query("UPDATE notifications SET isRead = 1 WHERE userId = :userId")
    void markAllAsRead(String userId);

    @Query("DELETE FROM notifications WHERE id = :id")
    void deleteById(String id);

    @Query("DELETE FROM notifications")
    void deleteAll();
}
