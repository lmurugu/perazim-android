package org.perazimchurch.app.data.repository;

import androidx.annotation.NonNull;

import org.perazimchurch.app.data.local.dao.ConnectionDao;
import org.perazimchurch.app.data.local.entity.ConnectionEntity;
import org.perazimchurch.app.data.mapper.ConnectionMapper;
import org.perazimchurch.app.domain.model.Connection;
import org.perazimchurch.app.domain.repository.ConnectionRepository;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Room implementation of {@link ConnectionRepository}.
 * Manages fellowship connections, connection requests, and member blocking.
 */
public class RoomConnectionRepository implements ConnectionRepository {

    private final ConnectionDao connectionDao;

    public RoomConnectionRepository(@NonNull ConnectionDao connectionDao) {
        this.connectionDao = connectionDao;
    }

    @Override
    public List<Connection> getConnections(String userId) {
        if (userId == null) {
            return Collections.emptyList();
        }
        List<ConnectionEntity> entities = connectionDao.getAcceptedConnections(userId);
        if (entities == null || entities.isEmpty()) {
            return Collections.emptyList();
        }
        return ConnectionMapper.toDomainList(entities);
    }

    @Override
    public List<Connection> getPendingRequests(String userId) {
        if (userId == null) {
            return Collections.emptyList();
        }
        List<ConnectionEntity> entities = connectionDao.getPendingRequests(userId);
        if (entities == null || entities.isEmpty()) {
            return Collections.emptyList();
        }
        return ConnectionMapper.toDomainList(entities);
    }

    @Override
    public Connection getConnection(String userId, String peerId) {
        if (userId == null || peerId == null) {
            return null;
        }
        ConnectionEntity entity = connectionDao.getConnectionBetween(userId, peerId);
        return ConnectionMapper.toDomain(entity);
    }

    @Override
    public void sendConnectionRequest(String userId, String peerId, String peerName) {
        if (userId == null || peerId == null) {
            return;
        }
        ConnectionEntity existing = connectionDao.getConnectionBetween(userId, peerId);
        long now = System.currentTimeMillis();
        if (existing != null) {
            existing.setUserId(userId);
            existing.setPeerId(peerId);
            if (peerName != null && !peerName.trim().isEmpty()) {
                existing.setPeerName(peerName);
            }
            existing.setStatus(Connection.Status.PENDING.name());
            existing.setCreatedMillis(now);
            connectionDao.update(existing);
        } else {
            String id = UUID.randomUUID().toString();
            ConnectionEntity newEntity = new ConnectionEntity(
                    id,
                    userId,
                    peerId,
                    peerName != null && !peerName.trim().isEmpty() ? peerName : "Peer",
                    Connection.Status.PENDING.name(),
                    now
            );
            connectionDao.insert(newEntity);
        }
    }

    @Override
    public void acceptConnection(String connectionId) {
        if (connectionId == null) {
            return;
        }
        ConnectionEntity entity = connectionDao.getConnectionById(connectionId);
        if (entity != null) {
            entity.setStatus(Connection.Status.ACCEPTED.name());
            connectionDao.update(entity);
        }
    }

    @Override
    public void rejectConnection(String connectionId) {
        if (connectionId == null) {
            return;
        }
        ConnectionEntity entity = connectionDao.getConnectionById(connectionId);
        if (entity != null) {
            entity.setStatus(Connection.Status.REMOVED.name());
            connectionDao.update(entity);
        }
    }

    @Override
    public void blockMember(String userId, String peerId) {
        if (userId == null || peerId == null) {
            return;
        }
        ConnectionEntity existing = connectionDao.getConnectionBetween(userId, peerId);
        long now = System.currentTimeMillis();
        if (existing != null) {
            existing.setUserId(userId);
            existing.setPeerId(peerId);
            existing.setStatus(Connection.Status.BLOCKED.name());
            connectionDao.update(existing);
        } else {
            String id = UUID.randomUUID().toString();
            ConnectionEntity entity = new ConnectionEntity(
                    id,
                    userId,
                    peerId,
                    "Blocked Member",
                    Connection.Status.BLOCKED.name(),
                    now
            );
            connectionDao.insert(entity);
        }
    }

    @Override
    public boolean isBlocked(String userId, String peerId) {
        if (userId == null || peerId == null) {
            return false;
        }
        ConnectionEntity entity = connectionDao.getBlockedConnection(userId, peerId);
        return entity != null;
    }
}
