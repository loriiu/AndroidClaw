package ai.androidclaw.data.repository

import ai.androidclaw.data.local.db.EntityMapper.toDomain
import ai.androidclaw.data.local.db.EntityMapper.toEntity
import ai.androidclaw.data.local.db.TaskDao
import ai.androidclaw.domain.model.Task
import ai.androidclaw.domain.model.TaskStatus
import ai.androidclaw.domain.repository.TaskRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 任务仓储实现
 */
@Singleton
class TaskRepositoryImpl @Inject constructor(
    private val taskDao: TaskDao
) : TaskRepository {
    
    override fun getAllTasks(): Flow<List<Task>> {
        return taskDao.getAllTasks().map { entities ->
            entities.map { it.toDomain() }
        }
    }
    
    override fun getTasksByStatus(status: TaskStatus): Flow<List<Task>> {
        return taskDao.getTasksByStatus(status.name).map { entities ->
            entities.map { it.toDomain() }
        }
    }
    
    override suspend fun getTaskById(taskId: String): Task? {
        return taskDao.getTaskById(taskId)?.toDomain()
    }
    
    override suspend fun createTask(task: Task): Task {
        taskDao.insertTask(task.toEntity())
        return task
    }
    
    override suspend fun updateTask(task: Task) {
        taskDao.updateTask(task.toEntity())
    }
    
    override suspend fun deleteTask(taskId: String) {
        taskDao.deleteTask(taskId)
    }
    
    override suspend fun updateTaskStatus(taskId: String, status: TaskStatus) {
        taskDao.updateTaskStatus(taskId, status.name)
    }
}
