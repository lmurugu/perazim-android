package com.example.app.data.local.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.app.data.local.entity.HymnEntity;

import java.util.List;

@Dao
public interface HymnDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(HymnEntity hymn);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<HymnEntity> hymns);

    @Update
    void update(HymnEntity hymn);

    @Delete
    void delete(HymnEntity hymn);

    @Query("SELECT * FROM hymns WHERE id = :id LIMIT 1")
    HymnEntity getHymnById(String id);

    @Query("SELECT * FROM hymns WHERE number = :number LIMIT 1")
    HymnEntity getHymnByNumber(int number);

    @Query("SELECT * FROM hymns ORDER BY number ASC")
    List<HymnEntity> getAllHymns();

    @Query("SELECT * FROM hymns WHERE isFavorite = 1 ORDER BY number ASC")
    List<HymnEntity> getFavoriteHymns();

    @Query("SELECT * FROM hymns WHERE category = :category ORDER BY number ASC")
    List<HymnEntity> getHymnsByCategory(String category);

    @Query("SELECT * FROM hymns WHERE title LIKE '%' || :query || '%' OR lyrics LIKE '%' || :query || '%' ORDER BY number ASC")
    List<HymnEntity> searchHymns(String query);

    @Query("DELETE FROM hymns WHERE id = :id")
    void deleteById(String id);

    @Query("DELETE FROM hymns")
    void deleteAll();
}
