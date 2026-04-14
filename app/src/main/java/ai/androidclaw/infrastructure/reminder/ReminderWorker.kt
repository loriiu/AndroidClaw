package ai.androidclaw.infrastructure.reminder

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.*
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

/**
 * 提醒 Worker
 * 
 * 使用 WorkManager 在指定时间发送提醒通知
 */
@HiltWorker
class ReminderWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters,
    private val notificationHelper: NotificationHelper
) : CoroutineWorker(context, workerParams) {
    
    companion object {
        const val KEY_REMINDER_ID = "reminder_id"
        const val KEY_NOTIFICATION_ID = "notification_id"
        const val KEY_TITLE = "title"
        const val KEY_DESCRIPTION = "description"
        const val KEY_IS_REPEATABLE = "is_repeatable"
        const val KEY_REPEAT_INTERVAL_MINUTES = "repeat_interval_minutes"
        
        /**
         * 创建一次性提醒请求
         */
        fun createOneTimeRequest(
            reminderId: String,
            notificationId: Int,
            title: String,
            description: String,
            delayMinutes: Long
        ): OneTimeWorkRequest {
            val inputData = workDataOf(
                KEY_REMINDER_ID to reminderId,
                KEY_NOTIFICATION_ID to notificationId,
                KEY_TITLE to title,
                KEY_DESCRIPTION to description,
                KEY_IS_REPEATABLE to false
            )
            
            return OneTimeWorkRequestBuilder<ReminderWorker>()
                .setInputData(inputData)
                .setInitialDelay(delayMinutes, TimeUnit.MINUTES)
                .addTag(reminderId)
                .build()
        }
        
        /**
         * 创建重复提醒请求
         */
        fun createRepeatingRequest(
            reminderId: String,
            notificationId: Int,
            title: String,
            description: String,
            repeatIntervalMinutes: Long
        ): PeriodicWorkRequest {
            val inputData = workDataOf(
                KEY_REMINDER_ID to reminderId,
                KEY_NOTIFICATION_ID to notificationId,
                KEY_TITLE to title,
                KEY_DESCRIPTION to description,
                KEY_IS_REPEATABLE to true,
                KEY_REPEAT_INTERVAL_MINUTES to repeatIntervalMinutes
            )
            
            val interval = if (repeatIntervalMinutes < 15) {
                15L  // 最小间隔 15 分钟
            } else {
                repeatIntervalMinutes
            }
            
            return PeriodicWorkRequestBuilder<ReminderWorker>(
                interval, TimeUnit.MINUTES,
                5, TimeUnit.MINUTES  // Flex interval
            )
                .setInputData(inputData)
                .addTag(reminderId)
                .build()
        }
        
        /**
         * 取消某个提醒的所有 Work
         */
        fun cancelReminderWork(workManager: WorkManager, reminderId: String) {
            workManager.cancelAllWorkByTag(reminderId)
        }
    }
    
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val reminderId = inputData.getString(KEY_REMINDER_ID) ?: return@withContext Result.failure()
        val notificationId = inputData.getInt(KEY_NOTIFICATION_ID, reminderId.hashCode())
        val title = inputData.getString(KEY_TITLE) ?: "Reminder"
        val description = inputData.getString(KEY_DESCRIPTION) ?: ""
        val isRepeatable = inputData.getBoolean(KEY_IS_REPEATABLE, false)
        
        try {
            // 显示通知
            notificationHelper.showReminderNotification(
                notificationId = notificationId,
                reminderId = reminderId,
                title = title,
                description = description
            )
            
            Result.success()
        } catch (e: Exception) {
            Result.failure()
        }
    }
}
