package ai.androidclaw.data.repository

import ai.androidclaw.data.local.db.*
import ai.androidclaw.domain.model.ChatMessage
import ai.androidclaw.domain.model.Conversation
import ai.androidclaw.domain.repository.ChatRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 聊天仓储实现
 */
@Singleton
class ChatRepositoryImpl @Inject constructor(
    private val messageDao: MessageDao,
    private val conversationDao: ConversationDao
) : ChatRepository {
    
    override fun getAllConversations(): Flow<List<Conversation>> {
        return conversationDao.getAllConversations().map { entities ->
            entities.map { it.toDomain() }
        }
    }
    
    override suspend fun getConversation(conversationId: String): Conversation? {
        return conversationDao.getConversationById(conversationId)?.toDomain()
    }
    
    override suspend fun createConversation(conversation: Conversation): Conversation {
        conversationDao.insertConversation(conversation.toEntity())
        return conversation
    }
    
    override suspend fun updateConversation(conversation: Conversation) {
        conversationDao.updateConversation(conversation.toEntity())
    }
    
    override suspend fun deleteConversation(conversationId: String) {
        // 先删除所有消息
        messageDao.deleteMessagesByConversation(conversationId)
        // 再删除对话
        conversationDao.deleteConversation(conversationId)
    }
    
    override fun getMessages(conversationId: String): Flow<List<ChatMessage>> {
        return messageDao.getMessages(conversationId).map { entities ->
            entities.map { it.toDomain() }
        }
    }
    
    override fun getRecentMessages(conversationId: String, limit: Int): Flow<List<ChatMessage>> {
        return messageDao.getRecentMessages(conversationId, limit).map { entities ->
            entities.map { it.toDomain() }.reversed()
        }
    }
    
    override suspend fun saveMessage(message: ChatMessage): ChatMessage {
        messageDao.insertMessage(message.toEntity())
        
        // 更新对话的更新时间
        conversationDao.getConversationById(message.conversationId)?.let { conv ->
            conversationDao.updateConversation(
                conv.copy(updatedAt = message.timestamp.toEpochMilli())
            )
        }
        
        return message
    }
    
    override suspend fun deleteMessage(messageId: String) {
        messageDao.getMessageById(messageId)?.let { entity ->
            messageDao.deleteMessage(entity)
        }
    }
    
    override suspend fun clearMessages(conversationId: String) {
        messageDao.deleteMessagesByConversation(conversationId)
    }
}
