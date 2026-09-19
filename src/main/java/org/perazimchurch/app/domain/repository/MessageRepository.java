package org.perazimchurch.app.domain.repository;

import org.perazimchurch.app.domain.model.Conversation;
import org.perazimchurch.app.domain.model.Message;
import java.util.List;

/**
 * Repository interface defining operations for Fellowship Messaging and Conversations.
 */
public interface MessageRepository {
    List<Conversation> getConversations(String userId);
    List<Message> getMessages(String conversationId, int limit, int offset);
    void sendMessage(Message message);
    void markMessagesAsRead(String conversationId, String userId);
}
