package com.example.app.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Room Entity for table "connections".
 */
@Entity(tableName = "connections")
public class ConnectionEntity {

    @PrimaryKey
    @NonNull
    private String id;
    private String userId;
    private String peerId;
    private String peerName;
    private String status;
    private long createdMillis;

    public ConnectionEntity(@NonNull String id, String userId, String peerId,
                            String peerName, String status, long createdMillis) {
        this.id = id;
        this.userId = userId;
        this.peerId = peerId;
        this.peerName = peerName;
        this.status = status;
        this.createdMillis = createdMillis;
    }

    @NonNull
    public String getId() { return id; }
    public void setId(@NonNull String id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getPeerId() { return peerId; }
    public void setPeerId(String peerId) { this.peerId = peerId; }

    public String getPeerName() { return peerName; }
    public void setPeerName(String peerName) { this.peerName = peerName; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public long getCreatedMillis() { return createdMillis; }
    public void setCreatedMillis(long createdMillis) { this.createdMillis = createdMillis; }
}
