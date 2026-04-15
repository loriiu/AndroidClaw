package ai.androidclaw.infrastructure.todo.repository

import ai.androidclaw.domain.model.todo.*
import ai.androidclaw.domain.service.todo.*
import ai.androidclaw.infrastructure.todo.db.*
import ai.androidclaw.infrastructure.todo.db.TodoMapper.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TodoServiceImpl @Inject constructor(
    private val todoDao: TodoDao, private val subTaskDao: SubTaskDao,
    private val milestoneDao: MilestoneDao, private val blockReasonDao: BlockReasonDao,
    private val habitDao: UserHabitDao, private val priorityEngine: PriorityEngine,
    private val conflictEngine: ConflictEngine, private val habitEngine: HabitEngine
) : TodoService {
    
    override suspend fun createTodo(todo: Todo) = runCatching {
        val habit = habitEngine.getHabit()
        val priority = priorityEngine.computePriority(todo, habit)
        val finalTodo = todo.copy(computedPriority = priority, createdAt = System.currentTimeMillis(), updatedAt = System.currentTimeMillis())
        todoDao.insert(finalTodo.toEntity()); finalTodo
    }
    
    override suspend fun updateTodo(todo: Todo) = runCatching {
        val habit = habitEngine.getHabit()
        val priority = priorityEngine.computePriority(todo, habit)
        val updated = todo.copy(computedPriority = priority, updatedAt = System.currentTimeMillis())
        todoDao.update(updated.toEntity()); updated
    }
    
    override suspend fun deleteTodo(todoId: String) = runCatching { todoDao.deleteById(todoId) }
    
    override suspend fun getTodoById(todoId: String): Todo? {
        val entity = todoDao.getById(todoId) ?: return null
        val subs = subTaskDao.getByTodoId(todoId).first().map { it.toDomain() }
        val miles = milestoneDao.getByTodoId(todoId).first().map { it.toDomain() }
        return entity.toDomain(subs, miles)
    }
    
    override fun observeTodo(todoId: String) = combine(todoDao.observeById(todoId), subTaskDao.getByTodoId(todoId), milestoneDao.getByTodoId(todoId)) { e, s, m -> e?.toDomain(s.map { it.toDomain() }, m.map { it.toDomain() }) }
    
    override fun getAllTodos() = todoDao.getAllSorted().map { es -> es.map { e -> e.toDomain(subTaskDao.getByTodoId(e.id).first().map { it.toDomain() }) } }
    override fun getActiveTodos() = todoDao.getActiveSorted().map { es -> es.map { e -> e.toDomain(subTaskDao.getByTodoId(e.id).first().map { it.toDomain() }) } }
    override fun getTodosByStatus(status: TodoStatus) = todoDao.getByStatus(status.name).map { es -> es.map { it.toDomain() } }
    override fun getTodosByTag(tag: String) = todoDao.getByTag(tag).map { es -> es.map { it.toDomain() } }
    override fun getOverdueTodos() = todoDao.getOverdue(System.currentTimeMillis()).map { es -> es.map { it.toDomain() } }
    
    override fun getSortedTodos() = getActiveTodos().map { priorityEngine.sortByPriority(it) }
    
    override suspend fun recomputePriority(todoId: String): Float {
        val todo = getTodoById(todoId) ?: return 0f
        val habit = habitEngine.getHabit()
        val priority = priorityEngine.computePriority(todo, habit)
        todoDao.updatePriority(todoId, priority); return priority
    }
    
    override suspend fun recomputeAllPriorities() {
        val todos = getActiveTodos().first()
        val habit = habitEngine.getHabit()
        todos.forEach { todoDao.updatePriority(it.id, priorityEngine.computePriority(it, habit)) }
    }
    
    override suspend fun updateStatus(todoId: String, status: TodoStatus) = runCatching { todoDao.updateStatus(todoId, status.name, System.currentTimeMillis()) }
    override suspend fun updateProgress(todoId: String, progress: Int) = runCatching { todoDao.updateProgress(todoId, progress.coerceIn(0, 100), System.currentTimeMillis()) }
    
    override suspend fun markComplete(todoId: String, actualDuration: Int?) = runCatching {
        val todo = getTodoById(todoId) ?: throw IllegalArgumentException("Todo not found")
        actualDuration?.let { todoDao.updateActualDuration(todoId, it, System.currentTimeMillis()); habitEngine.recordCompletion(todo, it) }
        todoDao.updateStatus(todoId, TodoStatus.COMPLETED.name, System.currentTimeMillis())
        todoDao.updateProgress(todoId, 100, System.currentTimeMillis())
    }
    
    override suspend fun addSubTask(todoId: String, subTask: SubTask) = runCatching { subTaskDao.insert(subTask.toEntity(todoId)); updateProgressFromSubTasks(todoId); subTask }
    override suspend fun updateSubTask(subTask: SubTask) = runCatching { subTaskDao.update(subTask.toEntity(subTask.id)); updateProgressFromSubTasks(subTask.id) }
    override suspend fun deleteSubTask(subTaskId: String) = runCatching { val s = subTaskDao.getById(subTaskId) ?: return@runCatching; subTaskDao.delete(s); updateProgressFromSubTasks(s.todoId) }
    override suspend fun toggleSubTask(subTaskId: String) = runCatching { val s = subTaskDao.getById(subTaskId) ?: throw IllegalArgumentException("SubTask not found"); subTaskDao.updateCompleted(subTaskId, !s.completed); updateProgressFromSubTasks(s.todoId) }
    
    override suspend fun addMilestone(todoId: String, milestone: Milestone) = runCatching { milestoneDao.insert(milestone.toEntity(todoId)); milestone }
    override suspend fun updateMilestone(milestone: Milestone) = runCatching { milestoneDao.update(milestone.toEntity(milestone.id)) }
    override suspend fun deleteMilestone(milestoneId: String) = runCatching { milestoneDao.delete(MilestoneEntity(milestoneId, "", null, 0, false, null)) }
    
    override suspend fun addBlockReason(todoId: String, reason: BlockReason) = runCatching { blockReasonDao.insert(reason.toEntity(todoId)); reason }
    override suspend fun resolveBlockReason(reasonId: String) = runCatching { blockReasonDao.resolve(reasonId, System.currentTimeMillis()) }
    
    override suspend fun detectConflict(todo: Todo) = conflictEngine.detect(todo, getActiveTodos().first())
    override suspend fun getSmartSuggestions(todo: Todo) = conflictEngine.generateSuggestions(todo, detectConflict(todo))
    
    override suspend fun getTodoContext(): TodoContext {
        val todos = getActiveTodos().first()
        val habit = habitEngine.getHabit()
        val now = System.currentTimeMillis()
        val today = Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0) }.timeInMillis
        val tomorrow = today + 24 * 3600 * 1000L
        
        val overdueTodos = todos.filter { it.deadline != null && it.deadline < now && it.status == TodoStatus.PENDING }
        val todayTodos = todos.filter { it.deadline != null && it.deadline in today..tomorrow }
        val upcomingTodos = todos.filter { it.deadline != null && it.deadline in now..now + 7 * 24 * 3600 * 1000L }
        
        val insights = buildString {
            if (habit.procrastinationScore > 0.7f) append("⚠️ 您的拖延指数较高，建议优先处理简单任务\n")
            if (todayTodos.size > 5) append("📋 今天有${todayTodos.size}项待办，建议按优先级处理\n")
            if (overdueTodos.isNotEmpty()) append("🔴 有${overdueTodos.size}项已过期待办，请尽快处理\n")
        }
        
        return TodoContext(todos = priorityEngine.sortByPriority(todos), overdueTodos = overdueTodos, todayTodos = todayTodos, upcomingDeadlines = upcomingTodos.sortedBy { it.deadline }, habitInsights = insights.takeIf { it.isNotBlank() }, summary = "共${todos.size}项待办")
    }
    
    private suspend fun updateProgressFromSubTasks(todoId: String) {
        val total = subTaskDao.getTotalCount(todoId)
        if (total == 0) return
        val completed = subTaskDao.getCompletedCount(todoId)
        todoDao.updateProgress(todoId, (completed * 100) / total, System.currentTimeMillis())
    }
}
