package com.example.app.data.local.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.app.data.local.entity.PrayerEntity;

import java.util.List;

@Dao
public interface PrayerDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(PrayerEntity prayer);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<PrayerEntity> prayers);

    @Update
    void update(PrayerEntity prayer);

    @Delete
    void delete(PrayerEntity prayer);

    @Query("SELECT * FROM prayers WHERE id = :id LIMIT 1")
    PrayerEntity getPrayerById(String id);

    @Query("SELECT * FROM prayers ORDER BY createdMillis DESC")
    List<PrayerEntity> getAllPrayers();

    @Query("SELECT * FROM prayers WHERE userId = :userId ORDER BY createdMillis DESC")
    List<PrayerEntity> getPrayersByUser(String userId);

    @Query("SELECT * FROM prayers WHERE isAnswered = 1 ORDER BY createdMillis DESC")
    List<PrayerEntity> getAnsweredPrayers();

    @Query("DELETE FROM prayers WHERE id = :id")
    void deleteById(String id);

    @Query("DELETE FROM prayers")
    void deleteAll();
}
