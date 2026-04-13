package ai.androidclaw.domain.model

import java.time.Instant

/**
 * 任务状态枚举
 * 
 * @see Task
 */
enum class TaskStatus {
    TODO,
    IN_PROGRESS,
    COMPLETED,
    AWAITING_HUMAN_INPUT
}

/**
 * 任务领域模型
 * 
 * 对应 JavaClaw 中的 Task 类，用于表示一个可执行的任务
 *
 * @property id 任务唯一标识
 * @property name 任务名称
 * @property description 任务描述
 * @property status 当前状态
 * @property createdAt 创建时间
 * @property scheduledAt 计划执行时间（可选）
 * @property cronExpression Cron 表达式（可选，用于重复任务）
 */
data class Task(
    val id: String,
    val name: String,
    val description: String,
    val status: TaskStatus,
    val createdAt: Instant,
    val scheduledAt: Instant? = null,
    val cronExpression: String? = null,
    val feedback: String? = null
) {
    companion object {
        /**
         * 创建新任务
         */
        fun newTask(name: String, description: String): Task {
            return Task(
                id = java.util.UUID.randomUUID().toString(),
                name = name,
                description = description,
                status = TaskStatus.TODO,
                createdAt = Instant.now()
            )
        }
    }

    /**
     * 更新任务状态
     */
    fun withStatus(newStatus: TaskStatus): Task {
        return copy(status = newStatus)
    }

    /**
     * 添加反馈信息
     */
    fun withFeedback(feedback: String): Task {
        return copy(feedback = feedback)
    }
}

/**
 * 重复任务领域模型
 * 
 * 用于表示定期执行的任务
 */
data class RecurringTask(
    val id: String,
    val name: String,
    val description: String,
    val cronExpression: String,
    val createdAt: Instant
) {
    companion object {
        fun newRecurringTask(name: String, description: String, cronExpression: String): RecurringTask {
            return RecurringTask(
                id = java.util.UUID.randomUUID().toString(),
                name = name,
                description = description,
                cronExpression = cronExpression,
                createdAt = Instant.now()
            )
        }
    }
}
