package com.example.app.data.local.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.app.data.local.entity.EventEntity;

import java.util.List;

@Dao
public interface EventDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(EventEntity event);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<EventEntity> events);

    @Update
    void update(EventEntity event);

    @Delete
    void delete(EventEntity event);

    @Query("SELECT * FROM events WHERE id = :id LIMIT 1")
    EventEntity getEventById(String id);

    @Query("SELECT * FROM events ORDER BY startTimeMillis ASC")
    List<EventEntity> getAllEvents();

    @Query("SELECT * FROM events WHERE endTimeMillis >= :currentTimeMillis ORDER BY startTimeMillis ASC")
    List<EventEntity> getUpcomingEvents(long currentTimeMillis);

    @Query("SELECT * FROM events WHERE campusId = :campusId ORDER BY startTimeMillis ASC")
    List<EventEntity> getEventsByCampus(String campusId);

    @Query("DELETE FROM events WHERE id = :id")
    void deleteById(String id);

    @Query("DELETE FROM events")
    void deleteAll();
}
