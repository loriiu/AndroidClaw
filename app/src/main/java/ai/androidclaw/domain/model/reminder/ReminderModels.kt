package ai.androidclaw.domain.model.reminder

import java.time.Instant
import java.util.UUID

/**
 * 提醒类型
 */
enum class ReminderType {
    ONCE,          // 一次性提醒
    DAILY,         // 每天重复
    WEEKLY,        // 每周重复
    CUSTOM         // 自定义重复
}

/**
 * 提醒状态
 */
enum class ReminderStatus {
    ACTIVE,        // 活跃
    COMPLETED,     // 已完成（仅一次性）
    DISABLED,      // 已禁用
    CANCELLED      // 已取消
}

/**
 * 提醒领域模型
 */
data class Reminder(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val description: String = "",
    val type: ReminderType = ReminderType.ONCE,
    val scheduledAt: Instant,
    val repeatIntervalMinutes: Long? = null,  // 重复间隔（分钟）
    val repeatDaysOfWeek: List<Int>? = null, // 每周重复的星期（1=周一, 7=周日）
    val status: ReminderStatus = ReminderStatus.ACTIVE,
    val notificationId: Int = id.hashCode(),
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now()
) {
    companion object {
        fun create(
            title: String,
            description: String = "",
            type: ReminderType = ReminderType.ONCE,
            scheduledAt: Instant,
            repeatIntervalMinutes: Long? = null,
            repeatDaysOfWeek: List<Int>? = null
        ): Reminder {
            return Reminder(
                title = title,
                description = description,
                type = type,
                scheduledAt = scheduledAt,
                repeatIntervalMinutes = repeatIntervalMinutes,
                repeatDaysOfWeek = repeatDaysOfWeek
            )
        }
        
        fun createDaily(
            title: String,
            description: String = "",
            hour: Int,
            minute: Int
        ): Reminder {
            val now = Instant.now()
            val scheduledAt = now // 实际时间由 Worker 计算
            
            return Reminder(
                title = title,
                description = description,
                type = ReminderType.DAILY,
                scheduledAt = scheduledAt,
                repeatIntervalMinutes = 24 * 60 // 24 小时
            )
        }
        
        fun createWeekly(
            title: String,
            description: String = "",
            dayOfWeek: Int, // 1-7 (周一到周日)
            hour: Int,
            minute: Int
        ): Reminder {
            return Reminder(
                title = title,
                description = description,
                type = ReminderType.WEEKLY,
                scheduledAt = Instant.now(),
                repeatDaysOfWeek = listOf(dayOfWeek)
            )
        }
    }
    
    /**
     * 计算下次触发时间（用于重复提醒）
     */
    fun getNextTriggerTime(): Instant {
        val now = Instant.now()
        
        return when (type) {
            ReminderType.ONCE -> scheduledAt
            ReminderType.DAILY -> {
                // 计算下一个 24 小时的整点
                val nowMillis = now.toEpochMilli()
                val intervalMillis = (repeatIntervalMinutes ?: 24 * 60) * 60 * 1000
                val nextTrigger = ((nowMillis / intervalMillis) + 1) * intervalMillis
                Instant.ofEpochMilli(nextTrigger)
            }
            ReminderType.WEEKLY -> {
                // TODO: 计算下周同一天的同一时间
                scheduledAt
            }
            ReminderType.CUSTOM -> {
                val intervalMillis = (repeatIntervalMinutes ?: 60) * 60 * 1000
                val nowMillis = now.toEpochMilli()
                val nextTrigger = ((nowMillis / intervalMillis) + 1) * intervalMillis
                Instant.ofEpochMilli(nextTrigger)
            }
        }
    }
}

/**
 * 提醒请求数据（用于 Worker）
 */
data class ReminderData(
    val reminderId: String,
    val title: String,
    val description: String,
    val type: ReminderType,
    val isRepeatable: Boolean
)
