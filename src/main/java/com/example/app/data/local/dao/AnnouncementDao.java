package com.example.app.data.local.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.app.data.local.entity.AnnouncementEntity;

import java.util.List;

@Dao
public interface AnnouncementDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(AnnouncementEntity announcement);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<AnnouncementEntity> announcements);

    @Update
    void update(AnnouncementEntity announcement);

    @Delete
    void delete(AnnouncementEntity announcement);

    @Query("SELECT * FROM announcements WHERE id = :id LIMIT 1")
    AnnouncementEntity getAnnouncementById(String id);

    @Query("SELECT * FROM announcements WHERE expiresMillis >= :currentTimeMillis ORDER BY publishedMillis DESC")
    List<AnnouncementEntity> getActiveAnnouncements(long currentTimeMillis);

    @Query("SELECT * FROM announcements ORDER BY publishedMillis DESC")
    List<AnnouncementEntity> getAllAnnouncements();

    @Query("DELETE FROM announcements WHERE id = :id")
    void deleteById(String id);

    @Query("DELETE FROM announcements")
    void deleteAll();
}
