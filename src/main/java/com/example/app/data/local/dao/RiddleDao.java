package com.example.app.data.local.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.app.data.local.entity.RiddleEntity;

import java.util.List;

@Dao
public interface RiddleDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(RiddleEntity riddle);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<RiddleEntity> riddles);

    @Update
    void update(RiddleEntity riddle);

    @Delete
    void delete(RiddleEntity riddle);

    @Query("SELECT * FROM riddles WHERE id = :id LIMIT 1")
    RiddleEntity getRiddleById(String id);

    @Query("SELECT * FROM riddles")
    List<RiddleEntity> getAllRiddles();

    @Query("SELECT * FROM riddles WHERE difficulty = :difficulty")
    List<RiddleEntity> getRiddlesByDifficulty(String difficulty);

    @Query("SELECT * FROM riddles ORDER BY RANDOM() LIMIT 1")
    RiddleEntity getRandomRiddle();

    @Query("DELETE FROM riddles WHERE id = :id")
    void deleteById(String id);

    @Query("DELETE FROM riddles")
    void deleteAll();
}
