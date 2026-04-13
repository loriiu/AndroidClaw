package ai.androidclaw.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * 应用数据库
 * 
 * 包含所有本地数据表
 */
@Database(
    entities = [
        TaskEntity::class,
        MessageEntity::class,
        ConversationEntity::class,
        SkillEntity::class,
        McpConnectionEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    
    abstract fun taskDao(): TaskDao
    abstract fun messageDao(): MessageDao
    abstract fun conversationDao(): ConversationDao
    abstract fun skillDao(): SkillDao
    abstract fun mcpConnectionDao(): McpConnectionDao
    
    companion object {
        const val DATABASE_NAME = "androidclaw_db"
    }
}
