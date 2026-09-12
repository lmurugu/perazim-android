package com.example.app.data.local.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.app.data.local.entity.UserEntity;

import java.util.List;

@Dao
public interface UserDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(UserEntity user);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<UserEntity> users);

    @Update
    void update(UserEntity user);

    @Delete
    void delete(UserEntity user);

    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    UserEntity getUserById(String id);

    @Query("SELECT * FROM users WHERE isActive = 1 LIMIT 1")
    UserEntity getActiveUser();

    @Query("SELECT * FROM users ORDER BY name ASC")
    List<UserEntity> getAllUsers();

    @Query("DELETE FROM users WHERE id = :id")
    void deleteById(String id);

    @Query("DELETE FROM users")
    void deleteAll();
}
