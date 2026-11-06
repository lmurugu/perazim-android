package org.perazimchurch.app.data.local.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import org.perazimchurch.app.data.local.entity.EventRsvpEntity;

import java.util.List;

@Dao
public interface EventRsvpDao {

    @Query("SELECT * FROM event_rsvps WHERE postId = :postId")
    List<EventRsvpEntity> getRsvpsForPost(String postId);

    @Query("SELECT * FROM event_rsvps WHERE postId = :postId AND userId = :userId LIMIT 1")
    EventRsvpEntity getUserRsvp(String postId, String userId);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(EventRsvpEntity rsvp);

    @Query("DELETE FROM event_rsvps WHERE postId = :postId AND userId = :userId")
    void delete(String postId, String userId);
}
