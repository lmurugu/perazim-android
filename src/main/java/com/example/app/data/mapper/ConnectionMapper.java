package com.example.app.data.mapper;

import androidx.annotation.Nullable;

import com.example.app.data.local.entity.ConnectionEntity;
import com.example.app.domain.model.Connection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Mapper for converting between Room {@link ConnectionEntity} and domain {@link Connection}.
 */
public final class ConnectionMapper {

    private ConnectionMapper() {
        // Utility class
    }

    /**
     * Maps a {@link ConnectionEntity} to a domain {@link Connection}.
     */
    @Nullable
    public static Connection toDomain(@Nullable ConnectionEntity entity) {
        if (entity == null) {
            return null;
        }

        Connection.Status status = Connection.Status.PENDING;
        if (entity.getStatus() != null) {
            try {
                status = Connection.Status.valueOf(entity.getStatus().toUpperCase());
            } catch (IllegalArgumentException e) {
                status = Connection.Status.PENDING;
            }
        }

        Connection connection = new Connection();
        connection.setId(entity.getId());
        connection.setRequesterId(entity.getUserId());
        connection.setRecipientId(entity.getPeerId());
        connection.setPeerName(entity.getPeerName());
        connection.setStatus(status);
        connection.setRequestedAt(entity.getCreatedMillis());
        connection.setRespondedAt(entity.getCreatedMillis());

        return connection;
    }

    /**
     * Maps a domain {@link Connection} to a Room {@link ConnectionEntity}.
     */
    @Nullable
    public static ConnectionEntity toEntity(@Nullable Connection domain) {
        if (domain == null) {
            return null;
        }

        String id = domain.getId() != null ? domain.getId() : UUID.randomUUID().toString();
        String userId = domain.getRequesterId() != null ? domain.getRequesterId() : domain.getUserId();
        String peerId = domain.getRecipientId() != null ? domain.getRecipientId() : domain.getPeerId();
        String peerName = domain.getPeerName() != null ? domain.getPeerName() : "Peer";
        String status = domain.getStatus() != null ? domain.getStatus().name() : Connection.Status.PENDING.name();
        long createdMillis = domain.getRequestedAt() > 0 ? domain.getRequestedAt() : System.currentTimeMillis();

        return new ConnectionEntity(
                id,
                userId,
                peerId,
                peerName,
                status,
                createdMillis
        );
    }

    /**
     * Maps a list of {@link ConnectionEntity} to a list of domain {@link Connection}.
     */
    public static List<Connection> toDomainList(@Nullable List<ConnectionEntity> entities) {
        if (entities == null || entities.isEmpty()) {
            return Collections.emptyList();
        }
        List<Connection> list = new ArrayList<>(entities.size());
        for (ConnectionEntity entity : entities) {
            Connection connection = toDomain(entity);
            if (connection != null) {
                list.add(connection);
            }
        }
        return list;
    }

    /**
     * Maps a list of domain {@link Connection} to a list of {@link ConnectionEntity}.
     */
    public static List<ConnectionEntity> toEntityList(@Nullable List<Connection> domains) {
        if (domains == null || domains.isEmpty()) {
            return Collections.emptyList();
        }
        List<ConnectionEntity> list = new ArrayList<>(domains.size());
        for (Connection domain : domains) {
            ConnectionEntity entity = toEntity(domain);
            if (entity != null) {
                list.add(entity);
            }
        }
        return list;
    }
}
