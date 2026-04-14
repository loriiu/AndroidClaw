package ai.androidclaw.infrastructure.reminder

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import ai.androidclaw.MainActivity
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 通知帮助类
 * 
 * 管理应用通知的创建和显示
 */
@Singleton
class NotificationHelper @Inject constructor(
    private val context: Context
) {
    
    companion object {
        const val CHANNEL_REMINDERS = "reminders"
        const val CHANNEL_TASK_COMPLETION = "task_completion"
        const val CHANNEL_GENERAL = "general"
        
        const val EXTRA_REMINDER_ID = "reminder_id"
        const val EXTRA_NOTIFICATION_ID = "notification_id"
    }
    
    init {
        createNotificationChannels()
    }
    
    /**
     * 创建通知渠道（Android 8.0+）
     */
    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(NotificationManager::class.java)
            
            // 提醒渠道 - 高优先级
            val reminderChannel = NotificationChannel(
                CHANNEL_REMINDERS,
                "Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Reminder notifications"
                enableVibration(true)
                enableLights(true)
            }
            
            // 任务完成渠道 - 默认优先级
            val taskChannel = NotificationChannel(
                CHANNEL_TASK_COMPLETION,
                "Task Completion",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications when tasks are completed"
            }
            
            // 通用渠道
            val generalChannel = NotificationChannel(
                CHANNEL_GENERAL,
                "General",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "General notifications"
            }
            
            notificationManager.createNotificationChannels(
                listOf(reminderChannel, taskChannel, generalChannel)
            )
        }
    }
    
    /**
     * 显示提醒通知
     */
    fun showReminderNotification(
        notificationId: Int,
        reminderId: String,
        title: String,
        description: String
    ) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra(EXTRA_REMINDER_ID, reminderId)
            putExtra(EXTRA_NOTIFICATION_ID, notificationId)
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = NotificationCompat.Builder(context, CHANNEL_REMINDERS)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(title)
            .setContentText(description)
            .setStyle(
                if (description.isNotEmpty()) {
                    NotificationCompat.BigTextStyle().bigText(description)
                } else {
                    NotificationCompat.BigTextStyle().bigText(title)
                }
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .build()
        
        try {
            NotificationManagerCompat.from(context).notify(notificationId, notification)
        } catch (e: SecurityException) {
            // 权限被拒绝，静默处理
        }
    }
    
    /**
     * 显示任务完成通知
     */
    fun showTaskCompletionNotification(
        notificationId: Int,
        taskName: String,
        feedback: String? = null
    ) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val contentText = feedback ?: "Task has been completed"
        
        val notification = NotificationCompat.Builder(context, CHANNEL_TASK_COMPLETION)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Task Completed: $taskName")
            .setContentText(contentText)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()
        
        try {
            NotificationManagerCompat.from(context).notify(notificationId, notification)
        } catch (e: SecurityException) {
            // 权限被拒绝，静默处理
        }
    }
    
    /**
     * 取消通知
     */
    fun cancelNotification(notificationId: Int) {
        NotificationManagerCompat.from(context).cancel(notificationId)
    }
    
    /**
     * 取消所有通知
     */
    fun cancelAllNotifications() {
        NotificationManagerCompat.from(context).cancelAll()
    }
}
