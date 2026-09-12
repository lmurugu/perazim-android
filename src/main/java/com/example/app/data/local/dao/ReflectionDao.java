package com.example.app.data.local.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.app.data.local.entity.ReflectionEntity;

import java.util.List;

@Dao
public interface ReflectionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(ReflectionEntity reflection);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<ReflectionEntity> reflections);

    @Update
    void update(ReflectionEntity reflection);

    @Delete
    void delete(ReflectionEntity reflection);

    @Query("SELECT * FROM reflections WHERE id = :id LIMIT 1")
    ReflectionEntity getReflectionById(String id);

    @Query("SELECT * FROM reflections ORDER BY createdMillis DESC")
    List<ReflectionEntity> getAllReflections();

    @Query("SELECT * FROM reflections WHERE userId = :userId ORDER BY createdMillis DESC")
    List<ReflectionEntity> getReflectionsByUser(String userId);

    @Query("DELETE FROM reflections WHERE id = :id")
    void deleteById(String id);

    @Query("DELETE FROM reflections")
    void deleteAll();
}
