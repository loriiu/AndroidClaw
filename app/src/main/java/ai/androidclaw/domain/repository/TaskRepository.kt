package ai.androidclaw.domain.repository

import ai.androidclaw.domain.model.Task
import ai.androidclaw.domain.model.TaskStatus
import kotlinx.coroutines.flow.Flow

/**
 * 任务仓储接口
 * 
 * 定义任务数据的 CRUD 操作
 */
interface TaskRepository {
    
    /**
     * 获取所有任务
     */
    fun getAllTasks(): Flow<List<Task>>
    
    /**
     * 按状态获取任务
     */
    fun getTasksByStatus(status: TaskStatus): Flow<List<Task>>
    
    /**
     * 获取单个任务
     */
    suspend fun getTaskById(taskId: String): Task?
    
    /**
     * 创建新任务
     */
    suspend fun createTask(task: Task): Task
    
    /**
     * 更新任务
     */
    suspend fun updateTask(task: Task)
    
    /**
     * 删除任务
     */
    suspend fun deleteTask(taskId: String)
    
    /**
     * 更新任务状态
     */
    suspend fun updateTaskStatus(taskId: String, status: TaskStatus)
}
