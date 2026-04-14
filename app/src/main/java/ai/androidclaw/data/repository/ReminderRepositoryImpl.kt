package ai.androidclaw.data.repository

import androidx.work.WorkManager
import ai.androidclaw.data.local.db.EntityMapper.toDomain
import ai.androidclaw.data.local.db.EntityMapper.toEntity
import ai.androidclaw.data.local.db.ReminderDao
import ai.androidclaw.domain.model.reminder.Reminder
import ai.androidclaw.domain.model.reminder.ReminderStatus
import ai.androidclaw.domain.model.reminder.ReminderType
import ai.androidclaw.domain.repository.ReminderRepository
import ai.androidclaw.infrastructure.reminder.ReminderWorker
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 提醒仓储实现
 */
@Singleton
class ReminderRepositoryImpl @Inject constructor(
    private val reminderDao: ReminderDao,
    private val workManager: WorkManager
) : ReminderRepository {
    
    init {
        // 启动观察者，定期检查到期提醒
        observeReminders()
    }
    
    private fun observeReminders() {
        // 这个方法会在每次ReminderRepositoryImpl初始化时运行
        // 用于定期检查到期提醒并重新调度
    }
    
    override fun getAllReminders(): Flow<List<Reminder>> {
        return reminderDao.getAllReminders().map { entities ->
            entities.map { it.toDomain() }
        }
    }
    
    override fun getRemindersByStatus(status: ReminderStatus): Flow<List<Reminder>> {
        return reminderDao.getRemindersByStatus(status.name).map { entities ->
            entities.map { it.toDomain() }
        }
    }
    
    override fun getDueReminders(): Flow<List<Reminder>> {
        return reminderDao.getDueReminders(System.currentTimeMillis()).map { entities ->
            entities.map { it.toDomain() }
        }
    }
    
    override suspend fun getReminderById(reminderId: String): Reminder? {
        return reminderDao.getReminderById(reminderId)?.toDomain()
    }
    
    override suspend fun createReminder(reminder: Reminder) {
        reminderDao.insertReminder(reminder.toEntity())
        
        // 如果提醒是活跃的，立即调度
        if (reminder.status == ReminderStatus.ACTIVE) {
            scheduleReminder(reminder)
        }
    }
    
    override suspend fun updateReminder(reminder: Reminder) {
        val updatedReminder = reminder.copy(updatedAt = Instant.now())
        reminderDao.updateReminder(updatedReminder.toEntity())
        
        // 重新调度
        cancelReminderSchedule(reminder.id)
        if (reminder.status == ReminderStatus.ACTIVE) {
            scheduleReminder(reminder)
        }
    }
    
    override suspend fun deleteReminder(reminderId: String) {
        // 取消调度
        cancelReminderSchedule(reminderId)
        
        // 删除记录
        reminderDao.deleteReminder(reminderId)
    }
    
    override suspend fun updateReminderStatus(reminderId: String, status: ReminderStatus) {
        reminderDao.updateReminderStatus(reminderId, status.name)
        
        val reminder = getReminderById(reminderId)
        if (reminder != null) {
            when (status) {
                ReminderStatus.ACTIVE -> scheduleReminder(reminder)
                else -> cancelReminderSchedule(reminderId)
            }
        }
    }
    
    override suspend fun enableReminder(reminderId: String) {
        val reminder = getReminderById(reminderId)
        if (reminder != null) {
            updateReminderStatus(reminderId, ReminderStatus.ACTIVE)
        }
    }
    
    override suspend fun disableReminder(reminderId: String) {
        updateReminderStatus(reminderId, ReminderStatus.DISABLED)
    }
    
    override suspend fun scheduleReminder(reminder: Reminder) {
        val now = Instant.now()
        val delay = Duration.between(now, reminder.scheduledAt)
        
        val delayMinutes = if (delay.isNegative || delay.isZero) {
            0L  // 立即执行
        } else {
            delay.toMinutes()
        }
        
        when (reminder.type) {
            ReminderType.ONCE -> {
                val request = ReminderWorker.createOneTimeRequest(
                    reminderId = reminder.id,
                    notificationId = reminder.notificationId,
                    title = reminder.title,
                    description = reminder.description,
                    delayMinutes = delayMinutes
                )
                workManager.enqueueUniqueWork(
                    "reminder_${reminder.id}",
                    ExistingWorkPolicy.REPLACE,
                    request
                )
            }
            
            ReminderType.DAILY, ReminderType.WEEKLY, ReminderType.CUSTOM -> {
                val interval = reminder.repeatIntervalMinutes ?: (24 * 60)
                val request = ReminderWorker.createRepeatingRequest(
                    reminderId = reminder.id,
                    notificationId = reminder.notificationId,
                    title = reminder.title,
                    description = reminder.description,
                    repeatIntervalMinutes = interval
                )
                workManager.enqueueUniquePeriodicWork(
                    "reminder_${reminder.id}",
                    ExistingPeriodicWorkPolicy.UPDATE,
                    request
                )
            }
        }
    }
    
    override suspend fun cancelReminderSchedule(reminderId: String) {
        ReminderWorker.cancelReminderWork(workManager, reminderId)
    }
    
    override fun getActiveRemindersCount(): Flow<Int> {
        return reminderDao.getRemindersByStatus(ReminderStatus.ACTIVE.name).map { it.size }
    }
}
