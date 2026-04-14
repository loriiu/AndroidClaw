package ai.androidclaw.domain.repository

import ai.androidclaw.domain.model.reminder.Reminder
import ai.androidclaw.domain.model.reminder.ReminderStatus
import kotlinx.coroutines.flow.Flow

/**
 * 提醒仓储接口
 */
interface ReminderRepository {
    
    /**
     * 获取所有提醒
     */
    fun getAllReminders(): Flow<List<Reminder>>
    
    /**
     * 获取指定状态的提醒
     */
    fun getRemindersByStatus(status: ReminderStatus): Flow<List<Reminder>>
    
    /**
     * 获取已到期的提醒
     */
    fun getDueReminders(): Flow<List<Reminder>>
    
    /**
     * 根据 ID 获取提醒
     */
    suspend fun getReminderById(reminderId: String): Reminder?
    
    /**
     * 创建提醒
     */
    suspend fun createReminder(reminder: Reminder)
    
    /**
     * 更新提醒
     */
    suspend fun updateReminder(reminder: Reminder)
    
    /**
     * 删除提醒
     */
    suspend fun deleteReminder(reminderId: String)
    
    /**
     * 更新提醒状态
     */
    suspend fun updateReminderStatus(reminderId: String, status: ReminderStatus)
    
    /**
     * 启用提醒
     */
    suspend fun enableReminder(reminderId: String)
    
    /**
     * 禁用提醒
     */
    suspend fun disableReminder(reminderId: String)
    
    /**
     * 调度提醒（启动 WorkManager）
     */
    suspend fun scheduleReminder(reminder: Reminder)
    
    /**
     * 取消提醒调度
     */
    suspend fun cancelReminderSchedule(reminderId: String)
    
    /**
     * 获取活跃提醒数量
     */
    fun getActiveRemindersCount(): Flow<Int>
}
