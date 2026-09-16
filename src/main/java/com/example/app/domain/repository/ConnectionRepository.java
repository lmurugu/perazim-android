package com.example.app.domain.repository;

import com.example.app.domain.model.Connection;
import java.util.List;

public interface ConnectionRepository {
    List<Connection> getConnections(String userId);
    List<Connection> getPendingRequests(String userId);
    Connection getConnection(String userId, String peerId);
    void sendConnectionRequest(String userId, String peerId, String peerName);
    void acceptConnection(String connectionId);
    void rejectConnection(String connectionId);
    void blockMember(String userId, String peerId);
    boolean isBlocked(String userId, String peerId);
}
