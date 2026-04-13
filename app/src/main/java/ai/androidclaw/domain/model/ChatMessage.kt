package ai.androidclaw.domain.model

import java.time.Instant

/**
 * 消息角色枚举
 */
enum class MessageRole {
    USER,
    ASSISTANT,
    SYSTEM
}

/**
 * 聊天消息领域模型
 * 
 * 表示对话中的一条消息
 *
 * @property id 消息唯一标识
 * @property conversationId 对话 ID
 * @property role 消息角色
 * @property content 消息内容
 * @property timestamp 时间戳
 */
data class ChatMessage(
    val id: String,
    val conversationId: String,
    val role: MessageRole,
    val content: String,
    val timestamp: Instant
) {
    companion object {
        /**
         * 创建用户消息
         */
        fun userMessage(conversationId: String, content: String): ChatMessage {
            return ChatMessage(
                id = java.util.UUID.randomUUID().toString(),
                conversationId = conversationId,
                role = MessageRole.USER,
                content = content,
                timestamp = Instant.now()
            )
        }

        /**
         * 创建助手消息
         */
        fun assistantMessage(conversationId: String, content: String): ChatMessage {
            return ChatMessage(
                id = java.util.UUID.randomUUID().toString(),
                conversationId = conversationId,
                role = MessageRole.ASSISTANT,
                content = content,
                timestamp = Instant.now()
            )
        }

        /**
         * 创建系统消息
         */
        fun systemMessage(conversationId: String, content: String): ChatMessage {
            return ChatMessage(
                id = java.util.UUID.randomUUID().toString(),
                conversationId = conversationId,
                role = MessageRole.SYSTEM,
                content = content,
                timestamp = Instant.now()
            )
        }
    }
}

/**
 * 对话领域模型
 * 
 * 表示一个完整的对话会话
 */
data class Conversation(
    val id: String,
    val title: String,
    val createdAt: Instant,
    val updatedAt: Instant
) {
    companion object {
        fun newConversation(title: String = "New Chat"): Conversation {
            val now = Instant.now()
            return Conversation(
                id = java.util.UUID.randomUUID().toString(),
                title = title,
                createdAt = now,
                updatedAt = now
            )
        }
    }
}
