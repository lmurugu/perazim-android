package com.example.app.data.local.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.app.data.local.entity.CampusEntity;

import java.util.List;

@Dao
public interface CampusDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(CampusEntity campus);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<CampusEntity> campuses);

    @Update
    void update(CampusEntity campus);

    @Delete
    void delete(CampusEntity campus);

    @Query("SELECT * FROM campuses WHERE id = :id LIMIT 1")
    CampusEntity getCampusById(String id);

    @Query("SELECT * FROM campuses WHERE isMainCampus = 1 LIMIT 1")
    CampusEntity getMainCampus();

    @Query("SELECT * FROM campuses ORDER BY name ASC")
    List<CampusEntity> getAllCampuses();

    @Query("DELETE FROM campuses WHERE id = :id")
    void deleteById(String id);

    @Query("DELETE FROM campuses")
    void deleteAll();
}
