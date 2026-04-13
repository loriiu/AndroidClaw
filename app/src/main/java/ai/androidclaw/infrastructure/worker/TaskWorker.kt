package ai.androidclaw.infrastructure.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.*
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ai.androidclaw.domain.model.TaskStatus
import ai.androidclaw.domain.repository.TaskRepository
import ai.androidclaw.infrastructure.llm.LlmProvider
import java.util.concurrent.TimeUnit

/**
 * 任务执行 Worker
 * 
 * 使用 WorkManager 在后台执行任务
 */
@HiltWorker
class TaskWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters,
    private val taskRepository: TaskRepository,
    private val llmProvider: LlmProvider
) : CoroutineWorker(context, workerParams) {
    
    companion object {
        const val KEY_TASK_ID = "task_id"
        
        /**
         * 创建一次性任务请求
         */
        fun createOneTimeRequest(taskId: String): OneTimeWorkRequest {
            val inputData = workDataOf(KEY_TASK_ID to taskId)
            
            return OneTimeWorkRequestBuilder<TaskWorker>()
                .setInputData(inputData)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .build()
        }
        
        /**
         * 创建定时任务请求
         */
        fun createDelayedRequest(taskId: String, delayMinutes: Long): OneTimeWorkRequest {
            val inputData = workDataOf(KEY_TASK_ID to taskId)
            
            return OneTimeWorkRequestBuilder<TaskWorker>()
                .setInputData(inputData)
                .setInitialDelay(delayMinutes, TimeUnit.MINUTES)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .build()
        }
    }
    
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val taskId = inputData.getString(KEY_TASK_ID) ?: return@withContext Result.failure()
        
        try {
            // 获取任务
            val task = taskRepository.getTaskById(taskId) 
                ?: return@withContext Result.failure()
            
            // 更新状态为进行中
            taskRepository.updateTaskStatus(taskId, TaskStatus.IN_PROGRESS)
            
            // 执行任务逻辑
            // TODO: 根据任务类型执行不同的操作
            
            // 更新状态为完成
            taskRepository.updateTaskStatus(taskId, TaskStatus.COMPLETED)
            
            // 显示通知
            showCompletionNotification(task.name)
            
            Result.success()
        } catch (e: Exception) {
            if (runAttemptCount < 3) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }
    
    private fun showCompletionNotification(taskName: String) {
        val notificationManager = 
            context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        
        val channel = android.app.NotificationChannel(
            "task_completion",
            "Task Completion",
            android.app.NotificationManager.IMPORTANCE_DEFAULT
        )
        notificationManager.createNotificationChannel(channel)
        
        val notification = androidx.core.app.NotificationCompat.Builder(context, "task_completion")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Task Completed")
            .setContentText("'$taskName' has been completed")
            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
        
        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }
}
