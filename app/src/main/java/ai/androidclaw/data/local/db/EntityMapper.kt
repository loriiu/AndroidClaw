package ai.androidclaw.data.local.db

import ai.androidclaw.domain.model.*
import java.time.Instant

/**
 * 实体与领域模型转换器
 */
object EntityMapper {
    
    // ========== Task 转换 ==========
    
    fun TaskEntity.toDomain(): Task {
        return Task(
            id = id,
            name = name,
            description = description,
            status = TaskStatus.valueOf(status),
            createdAt = Instant.ofEpochMilli(createdAt),
            scheduledAt = scheduledAt?.let { Instant.ofEpochMilli(it) },
            cronExpression = cronExpression,
            feedback = feedback
        )
    }
    
    fun Task.toEntity(): TaskEntity {
        return TaskEntity(
            id = id,
            name = name,
            description = description,
            status = status.name,
            createdAt = createdAt.toEpochMilli(),
            scheduledAt = scheduledAt?.toEpochMilli(),
            cronExpression = cronExpression,
            feedback = feedback
        )
    }
    
    // ========== ChatMessage 转换 ==========
    
    fun MessageEntity.toDomain(): ChatMessage {
        return ChatMessage(
            id = id,
            conversationId = conversationId,
            role = MessageRole.valueOf(role),
            content = content,
            timestamp = Instant.ofEpochMilli(timestamp)
        )
    }
    
    fun ChatMessage.toEntity(): MessageEntity {
        return MessageEntity(
            id = id,
            conversationId = conversationId,
            role = role.name,
            content = content,
            timestamp = timestamp.toEpochMilli()
        )
    }
    
    // ========== Conversation 转换 ==========
    
    fun ConversationEntity.toDomain(): Conversation {
        return Conversation(
            id = id,
            title = title,
            createdAt = Instant.ofEpochMilli(createdAt),
            updatedAt = Instant.ofEpochMilli(updatedAt)
        )
    }
    
    fun Conversation.toEntity(): ConversationEntity {
        return ConversationEntity(
            id = id,
            title = title,
            createdAt = createdAt.toEpochMilli(),
            updatedAt = updatedAt.toEpochMilli()
        )
    }
    
    // ========== Skill 转换 ==========
    
    fun SkillEntity.toDomain(): Skill {
        return Skill(
            id = id,
            name = name,
            description = description,
            version = version,
            instructions = instructions,
            tools = parseToolsFromJson(toolsJson),
            enabled = enabled,
            installedAt = Instant.ofEpochMilli(installedAt)
        )
    }
    
    fun Skill.toEntity(): SkillEntity {
        return SkillEntity(
            id = id,
            name = name,
            description = description,
            version = version,
            instructions = instructions,
            toolsJson = toolsToJson(tools),
            enabled = enabled,
            installedAt = installedAt.toEpochMilli()
        )
    }
    
    // ========== 工具 JSON 序列化 ==========
    
    private fun parseToolsFromJson(json: String): List<ToolDefinition> {
        return try {
            kotlinx.serialization.json.Json.decodeFromString<List<ToolDefinition>>(json)
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    private fun toolsToJson(tools: List<ToolDefinition>): String {
        return try {
            kotlinx.serialization.json.Json.encodeToString(tools)
        } catch (e: Exception) {
            "[]"
        }
    }
}
