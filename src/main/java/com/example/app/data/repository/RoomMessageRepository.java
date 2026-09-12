package com.example.app.data.repository;

import androidx.annotation.NonNull;

import com.example.app.data.local.dao.ConversationDao;
import com.example.app.data.local.dao.MessageDao;
import com.example.app.data.local.entity.ConversationEntity;
import com.example.app.data.local.entity.MessageEntity;
import com.example.app.data.mapper.MessageMapper;
import com.example.app.domain.model.Conversation;
import com.example.app.domain.model.Message;
import com.example.app.domain.repository.MessageRepository;

import java.util.Collections;
import java.util.List;

/**
 * Room implementation of {@link MessageRepository}.
 * Manages fellowship conversations, direct messaging, pagination, sending, and read statuses.
 */
public class RoomMessageRepository implements MessageRepository {

    private final MessageDao messageDao;
    private final ConversationDao conversationDao;

    public RoomMessageRepository(@NonNull MessageDao messageDao, @NonNull ConversationDao conversationDao) {
        this.messageDao = messageDao;
        this.conversationDao = conversationDao;
    }

    @Override
    public List<Conversation> getConversations(String userId) {
        List<ConversationEntity> entities = conversationDao.getAllConversations();
        if (entities == null || entities.isEmpty()) {
            return Collections.emptyList();
        }
        return MessageMapper.toConversationDomainList(entities);
    }

    @Override
    public List<Message> getMessages(String conversationId, int limit, int offset) {
        if (conversationId == null) {
            return Collections.emptyList();
        }
        List<MessageEntity> entities = messageDao.getMessagesForConversation(conversationId);
        if (entities == null || entities.isEmpty()) {
            return Collections.emptyList();
        }

        int start = Math.max(0, offset);
        if (start >= entities.size()) {
            return Collections.emptyList();
        }

        int end = limit > 0 ? Math.min(entities.size(), start + limit) : entities.size();
        List<MessageEntity> paginatedList = entities.subList(start, end);
        return MessageMapper.toMessageDomainList(paginatedList);
    }

    @Override
    public void sendMessage(Message message) {
        if (message == null) {
            return;
        }
        MessageEntity entity = MessageMapper.toEntity(message);
        if (entity != null) {
            messageDao.insert(entity);
        }

        // Keep conversation record updated with latest message text and timestamp
        String conversationId = message.getConversationId();
        if (conversationId != null) {
            ConversationEntity conversation = conversationDao.getConversationById(conversationId);
            long messageTimestamp = message.getTimestamp() > 0 ? message.getTimestamp() : System.currentTimeMillis();
            if (conversation != null) {
                conversation.setLastMessageText(message.getContent());
                conversation.setLastMessageMillis(messageTimestamp);
                conversation.setUpdatedAt(System.currentTimeMillis());
                conversationDao.update(conversation);
            } else {
                // If conversation does not exist, create a new conversation record
                ConversationEntity newConversation = new ConversationEntity(
                        conversationId,
                        message.getSenderName() != null ? message.getSenderName() : "Conversation",
                        "DIRECT",
                        messageTimestamp,
                        messageTimestamp,
                        message.getContent(),
                        messageTimestamp
                );
                conversationDao.insert(newConversation);
            }
        }
    }

    @Override
    public void markMessagesAsRead(String conversationId, String userId) {
        if (conversationId == null) {
            return;
        }

        List<MessageEntity> messages = messageDao.getMessagesForConversation(conversationId);
        if (messages != null && !messages.isEmpty()) {
            for (MessageEntity msg : messages) {
                // Mark incoming messages as READ
                if (!"READ".equalsIgnoreCase(msg.getDeliveryStatus())) {
                    if (userId == null || !userId.equals(msg.getSenderId())) {
                        msg.setDeliveryStatus("READ");
                        messageDao.update(msg);
                    }
                }
            }
        }

        ConversationEntity conversation = conversationDao.getConversationById(conversationId);
        if (conversation != null) {
            conversation.setUpdatedAt(System.currentTimeMillis());
            conversationDao.update(conversation);
        }
    }
}
