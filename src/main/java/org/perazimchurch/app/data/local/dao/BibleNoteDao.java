package org.perazimchurch.app.data.local.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import org.perazimchurch.app.data.local.entity.BibleNoteEntity;
import org.perazimchurch.app.data.local.entity.BibleNoteLinkEntity;

import java.util.List;

/** Study notes and their verse links. */
@Dao
public interface BibleNoteDao {

    @Insert
    long insertNote(BibleNoteEntity note);

    @Query("UPDATE bible_notes SET body = :body, updatedMillis = :updatedMillis, topics = :topics WHERE id = :id")
    void updateNote(long id, String body, long updatedMillis, String topics);

    @Query("DELETE FROM bible_notes WHERE id = :id")
    void deleteNote(long id);

    @Query("DELETE FROM bible_note_links WHERE noteId = :noteId")
    void deleteLinks(long noteId);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertLinks(List<BibleNoteLinkEntity> links);

    @Query("SELECT * FROM bible_notes ORDER BY updatedMillis DESC")
    List<BibleNoteEntity> getAllNotes();

    @Query("SELECT * FROM bible_note_links")
    List<BibleNoteLinkEntity> getAllLinks();
}
