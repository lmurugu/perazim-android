package org.perazimchurch.app.data.local.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

/** Room entity for table "bible_notes": a study note that can link to several verses. */
@Entity(tableName = "bible_notes")
public class BibleNoteEntity {

    @PrimaryKey(autoGenerate = true)
    private long id;
    private String body;
    private long updatedMillis;
    /** Comma-separated topic labels linked to this note, for example "healing,grace". */
    private String topics;

    public BibleNoteEntity(long id, String body, long updatedMillis, String topics) {
        this.id = id;
        this.body = body;
        this.updatedMillis = updatedMillis;
        this.topics = topics;
    }

    public String getTopics() { return topics; }
    public void setTopics(String topics) { this.topics = topics; }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }

    public long getUpdatedMillis() { return updatedMillis; }
    public void setUpdatedMillis(long updatedMillis) { this.updatedMillis = updatedMillis; }
}
