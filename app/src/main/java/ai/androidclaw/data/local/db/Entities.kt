package ai.androidclaw.data.local.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 任务数据库实体
 * 
 * 对应 tasks 表
 */
@Entity(
    tableName = "tasks",
    indices = [
        Index(value = ["status"]),
        Index(value = ["createdAt"])
    ]
)
data class TaskEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val description: String,
    val status: String,
    val createdAt: Long,
    val scheduledAt: Long?,
    val cronExpression: String?,
    val feedback: String?
)

/**
 * 聊天消息数据库实体
 * 
 * 对应 messages 表
 */
@Entity(
    tableName = "messages",
    indices = [
        Index(value = ["conversationId"])
    ]
)
data class MessageEntity(
    @PrimaryKey
    val id: String,
    val conversationId: String,
    val role: String,
    val content: String,
    val timestamp: Long
)

/**
 * 对话数据库实体
 * 
 * 对应 conversations 表
 */
@Entity(
    tableName = "conversations"
)
data class ConversationEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val createdAt: Long,
    val updatedAt: Long
)

/**
 * 技能数据库实体
 * 
 * 对应 skills 表
 */
@Entity(
    tableName = "skills"
)
data class SkillEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val description: String,
    val version: String,
    val instructions: String,
    val toolsJson: String,
    val enabled: Boolean,
    val installedAt: Long
)

/**
 * MCP 连接数据库实体
 * 
 * 对应 mcp_connections 表
 */
@Entity(
    tableName = "mcp_connections"
)
data class McpConnectionEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val url: String,
    val authToken: String?,
    val enabled: Boolean
)

/**
 * 提醒数据库实体
 * 
 * 对应 reminders 表
 */
@Entity(
    tableName = "reminders",
    indices = [
        Index(value = ["status"]),
        Index(value = ["scheduledAt"])
    ]
)
data class ReminderEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val description: String,
    val type: String,
    val scheduledAt: Long,
    val repeatIntervalMinutes: Long?,
    val repeatDaysOfWeek: String?,  // JSON 格式存储
    val status: String,
    val notificationId: Int,
    val createdAt: Long,
    val updatedAt: Long
)
