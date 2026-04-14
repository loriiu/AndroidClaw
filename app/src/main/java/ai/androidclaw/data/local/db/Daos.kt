package ai.androidclaw.data.local.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * 任务 DAO
 */
@Dao
interface TaskDao {
    
    @Query("SELECT * FROM tasks ORDER BY createdAt DESC")
    fun getAllTasks(): Flow<List<TaskEntity>>
    
    @Query("SELECT * FROM tasks WHERE status = :status ORDER BY createdAt DESC")
    fun getTasksByStatus(status: String): Flow<List<TaskEntity>>
    
    @Query("SELECT * FROM tasks WHERE id = :taskId")
    suspend fun getTaskById(taskId: String): TaskEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskEntity)
    
    @Update
    suspend fun updateTask(task: TaskEntity)
    
    @Query("DELETE FROM tasks WHERE id = :taskId")
    suspend fun deleteTask(taskId: String)
    
    @Query("UPDATE tasks SET status = :status WHERE id = :taskId")
    suspend fun updateTaskStatus(taskId: String, status: String)
}

/**
 * 消息 DAO
 */
@Dao
interface MessageDao {
    
    @Query("SELECT * FROM messages WHERE conversationId = :conversationId ORDER BY timestamp ASC")
    fun getMessages(conversationId: String): Flow<List<MessageEntity>>
    
    @Query("SELECT * FROM messages WHERE conversationId = :conversationId ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentMessages(conversationId: String, limit: Int): Flow<List<MessageEntity>>
    
    @Query("SELECT * FROM messages WHERE id = :messageId")
    suspend fun getMessageById(messageId: String): MessageEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: MessageEntity)
    
    @Delete
    suspend fun deleteMessage(message: MessageEntity)
    
    @Query("DELETE FROM messages WHERE conversationId = :conversationId")
    suspend fun deleteMessagesByConversation(conversationId: String)
}

/**
 * 对话 DAO
 */
@Dao
interface ConversationDao {
    
    @Query("SELECT * FROM conversations ORDER BY updatedAt DESC")
    fun getAllConversations(): Flow<List<ConversationEntity>>
    
    @Query("SELECT * FROM conversations WHERE id = :conversationId")
    suspend fun getConversationById(conversationId: String): ConversationEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConversation(conversation: ConversationEntity)
    
    @Update
    suspend fun updateConversation(conversation: ConversationEntity)
    
    @Query("DELETE FROM conversations WHERE id = :conversationId")
    suspend fun deleteConversation(conversationId: String)
}

/**
 * 技能 DAO
 */
@Dao
interface SkillDao {
    
    @Query("SELECT * FROM skills ORDER BY installedAt DESC")
    fun getAllSkills(): Flow<List<SkillEntity>>
    
    @Query("SELECT * FROM skills WHERE enabled = 1 ORDER BY installedAt DESC")
    fun getEnabledSkills(): Flow<List<SkillEntity>>
    
    @Query("SELECT * FROM skills WHERE id = :skillId")
    suspend fun getSkillById(skillId: String): SkillEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSkill(skill: SkillEntity)
    
    @Update
    suspend fun updateSkill(skill: SkillEntity)
    
    @Query("DELETE FROM skills WHERE id = :skillId")
    suspend fun deleteSkill(skillId: String)
    
    @Query("UPDATE skills SET enabled = 1 WHERE id = :skillId")
    suspend fun enableSkill(skillId: String)
    
    @Query("UPDATE skills SET enabled = 0 WHERE id = :skillId")
    suspend fun disableSkill(skillId: String)
}

/**
 * MCP 连接 DAO
 */
@Dao
interface McpConnectionDao {
    
    @Query("SELECT * FROM mcp_connections")
    fun getAllConnections(): Flow<List<McpConnectionEntity>>
    
    @Query("SELECT * FROM mcp_connections WHERE id = :connectionId")
    suspend fun getConnectionById(connectionId: String): McpConnectionEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConnection(connection: McpConnectionEntity)
    
    @Update
    suspend fun updateConnection(connection: McpConnectionEntity)
    
    @Query("DELETE FROM mcp_connections WHERE id = :connectionId")
    suspend fun deleteConnection(connectionId: String)
}

/**
 * 提醒 DAO
 */
@Dao
interface ReminderDao {
    
    @Query("SELECT * FROM reminders ORDER BY scheduledAt ASC")
    fun getAllReminders(): Flow<List<ReminderEntity>>
    
    @Query("SELECT * FROM reminders WHERE status = :status ORDER BY scheduledAt ASC")
    fun getRemindersByStatus(status: String): Flow<List<ReminderEntity>>
    
    @Query("SELECT * FROM reminders WHERE status = 'ACTIVE' AND scheduledAt <= :currentTime ORDER BY scheduledAt ASC")
    fun getDueReminders(currentTime: Long): Flow<List<ReminderEntity>>
    
    @Query("SELECT * FROM reminders WHERE id = :reminderId")
    suspend fun getReminderById(reminderId: String): ReminderEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminder(reminder: ReminderEntity)
    
    @Update
    suspend fun updateReminder(reminder: ReminderEntity)
    
    @Query("DELETE FROM reminders WHERE id = :reminderId")
    suspend fun deleteReminder(reminderId: String)
    
    @Query("UPDATE reminders SET status = :status WHERE id = :reminderId")
    suspend fun updateReminderStatus(reminderId: String, status: String)
}
