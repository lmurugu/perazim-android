package org.perazimchurch.app.data.mapper;

import androidx.annotation.Nullable;

import org.perazimchurch.app.data.local.entity.ConversationEntity;
import org.perazimchurch.app.data.local.entity.MessageEntity;
import org.perazimchurch.app.domain.model.Conversation;
import org.perazimchurch.app.domain.model.Message;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Mapper for converting between Room entities ({@link MessageEntity}, {@link ConversationEntity})
 * and domain models ({@link Message}, {@link Conversation}).
 */
public final class MessageMapper {

    private MessageMapper() {
        // Utility class
    }

    // --- Message Mapping ---

    /**
     * Maps a {@link MessageEntity} to a domain {@link Message}.
     */
    @Nullable
    public static Message toDomain(@Nullable MessageEntity entity) {
        if (entity == null) {
            return null;
        }

        Message message = new Message();
        message.setId(entity.getId());
        message.setClientMessageId(entity.getId());
        message.setConversationId(entity.getConversationId());
        message.setSenderId(entity.getSenderId());
        message.setSenderName(entity.getSenderName());
        message.setContent(entity.getContent());
        message.setStatus(entity.getDeliveryStatus());
        message.setTimestamp(entity.getSentMillis());

        return message;
    }

    /**
     * Maps a domain {@link Message} to a Room {@link MessageEntity}.
     */
    @Nullable
    public static MessageEntity toEntity(@Nullable Message domain) {
        if (domain == null) {
            return null;
        }

        String id = domain.getId() != null ? domain.getId()
                : (domain.getClientMessageId() != null ? domain.getClientMessageId() : UUID.randomUUID().toString());
        long sentMillis = domain.getTimestamp() > 0 ? domain.getTimestamp() : System.currentTimeMillis();
        String deliveryStatus = domain.getStatus() != null ? domain.getStatus() : "SENT";
        String syncStatus = "SYNCED";

        return new MessageEntity(
                id,
                domain.getConversationId(),
                domain.getSenderId(),
                domain.getSenderName(),
                domain.getContent(),
                sentMillis,
                deliveryStatus,
                syncStatus
        );
    }

    /**
     * Maps a list of {@link MessageEntity} to a list of domain {@link Message}.
     */
    public static List<Message> toDomainList(@Nullable List<MessageEntity> entities) {
        return toMessageDomainList(entities);
    }

    /**
     * Explicitly named helper: maps a list of {@link MessageEntity} to a list of domain {@link Message}.
     */
    public static List<Message> toMessageDomainList(@Nullable List<MessageEntity> entities) {
        if (entities == null || entities.isEmpty()) {
            return Collections.emptyList();
        }
        List<Message> list = new ArrayList<>(entities.size());
        for (MessageEntity entity : entities) {
            Message msg = toDomain(entity);
            if (msg != null) {
                list.add(msg);
            }
        }
        return list;
    }

    /**
     * Maps a list of domain {@link Message} to a list of {@link MessageEntity}.
     */
    public static List<MessageEntity> toEntityList(@Nullable List<Message> domains) {
        return toMessageEntityList(domains);
    }

    /**
     * Explicitly named helper: maps a list of domain {@link Message} to a list of {@link MessageEntity}.
     */
    public static List<MessageEntity> toMessageEntityList(@Nullable List<Message> domains) {
        if (domains == null || domains.isEmpty()) {
            return Collections.emptyList();
        }
        List<MessageEntity> list = new ArrayList<>(domains.size());
        for (Message domain : domains) {
            MessageEntity entity = toEntity(domain);
            if (entity != null) {
                list.add(entity);
            }
        }
        return list;
    }

    // --- Conversation Mapping ---

    /**
     * Maps a {@link ConversationEntity} to a domain {@link Conversation}.
     */
    @Nullable
    public static Conversation toDomain(@Nullable ConversationEntity entity) {
        if (entity == null) {
            return null;
        }

        Conversation conversation = new Conversation();
        conversation.setId(entity.getId());
        conversation.setTitle(entity.getTitle());
        conversation.setGroup("GROUP".equalsIgnoreCase(entity.getType()));
        conversation.setLastMessageSnippet(entity.getLastMessageText());
        conversation.setLastMessageTimestamp(entity.getLastMessageMillis());
        conversation.setUnreadCount(0);
        conversation.setCreatedAt(entity.getCreatedAt());

        return conversation;
    }

    /**
     * Maps a domain {@link Conversation} to a Room {@link ConversationEntity}.
     */
    @Nullable
    public static ConversationEntity toEntity(@Nullable Conversation domain) {
        if (domain == null) {
            return null;
        }

        String id = domain.getId() != null ? domain.getId() : UUID.randomUUID().toString();
        String type = domain.isGroup() ? "GROUP" : "DIRECT";
        long now = System.currentTimeMillis();
        long createdAt = domain.getCreatedAt() > 0 ? domain.getCreatedAt() : now;
        long lastMessageMillis = domain.getLastMessageTimestamp();
        long updatedAt = lastMessageMillis > 0 ? lastMessageMillis : createdAt;

        return new ConversationEntity(
                id,
                domain.getTitle(),
                type,
                createdAt,
                updatedAt,
                domain.getLastMessageSnippet(),
                lastMessageMillis
        );
    }

    /**
     * Maps a list of {@link ConversationEntity} to a list of domain {@link Conversation}.
     */
    public static List<Conversation> toConversationDomainList(@Nullable List<ConversationEntity> entities) {
        if (entities == null || entities.isEmpty()) {
            return Collections.emptyList();
        }
        List<Conversation> list = new ArrayList<>(entities.size());
        for (ConversationEntity entity : entities) {
            Conversation conv = toDomain(entity);
            if (conv != null) {
                list.add(conv);
            }
        }
        return list;
    }

    /**
     * Maps a list of domain {@link Conversation} to a list of {@link ConversationEntity}.
     */
    public static List<ConversationEntity> toConversationEntityList(@Nullable List<Conversation> domains) {
        if (domains == null || domains.isEmpty()) {
            return Collections.emptyList();
        }
        List<ConversationEntity> list = new ArrayList<>(domains.size());
        for (Conversation domain : domains) {
            ConversationEntity entity = toEntity(domain);
            if (entity != null) {
                list.add(entity);
            }
        }
        return list;
    }
}
