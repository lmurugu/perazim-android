package com.example.app.data.local.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.app.data.local.entity.JokeEntity;

import java.util.List;

@Dao
public interface JokeDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(JokeEntity joke);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<JokeEntity> jokes);

    @Update
    void update(JokeEntity joke);

    @Delete
    void delete(JokeEntity joke);

    @Query("SELECT * FROM jokes WHERE id = :id LIMIT 1")
    JokeEntity getJokeById(String id);

    @Query("SELECT * FROM jokes")
    List<JokeEntity> getAllJokes();

    @Query("SELECT * FROM jokes WHERE category = :category")
    List<JokeEntity> getJokesByCategory(String category);

    @Query("SELECT * FROM jokes ORDER BY RANDOM() LIMIT 1")
    JokeEntity getRandomJoke();

    @Query("DELETE FROM jokes WHERE id = :id")
    void deleteById(String id);

    @Query("DELETE FROM jokes")
    void deleteAll();
}
