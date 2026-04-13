package ai.androidclaw.domain.repository

import ai.androidclaw.domain.model.ChatMessage
import ai.androidclaw.domain.model.Conversation
import kotlinx.coroutines.flow.Flow

/**
 * 聊天仓储接口
 * 
 * 定义对话和消息的 CRUD 操作
 */
interface ChatRepository {
    
    /**
     * 获取所有对话
     */
    fun getAllConversations(): Flow<List<Conversation>>
    
    /**
     * 获取单个对话
     */
    suspend fun getConversation(conversationId: String): Conversation?
    
    /**
     * 创建新对话
     */
    suspend fun createConversation(conversation: Conversation): Conversation
    
    /**
     * 更新对话
     */
    suspend fun updateConversation(conversation: Conversation)
    
    /**
     * 删除对话及其所有消息
     */
    suspend fun deleteConversation(conversationId: String)
    
    /**
     * 获取对话的所有消息
     */
    fun getMessages(conversationId: String): Flow<List<ChatMessage>>
    
    /**
     * 获取最近的 N 条消息
     */
    fun getRecentMessages(conversationId: String, limit: Int): Flow<List<ChatMessage>>
    
    /**
     * 保存消息
     */
    suspend fun saveMessage(message: ChatMessage): ChatMessage
    
    /**
     * 删除消息
     */
    suspend fun deleteMessage(messageId: String)
    
    /**
     * 清空对话的所有消息
     */
    suspend fun clearMessages(conversationId: String)
}
