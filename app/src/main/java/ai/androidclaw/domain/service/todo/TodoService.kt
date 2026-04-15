package ai.androidclaw.domain.service.todo

import ai.androidclaw.domain.model.todo.*
import kotlinx.coroutines.flow.Flow

interface TodoService {
    suspend fun createTodo(todo: Todo): Result<Todo>
    suspend fun updateTodo(todo: Todo): Result<Todo>
    suspend fun deleteTodo(todoId: String): Result<Unit>
    suspend fun getTodoById(todoId: String): Todo?
    fun observeTodo(todoId: String): Flow<Todo?>
    fun getAllTodos(): Flow<List<Todo>>
    fun getActiveTodos(): Flow<List<Todo>>
    fun getTodosByStatus(status: TodoStatus): Flow<List<Todo>>
    fun getTodosByTag(tag: String): Flow<List<Todo>>
    fun getOverdueTodos(): Flow<List<Todo>>
    fun getSortedTodos(): Flow<List<Todo>>
    suspend fun recomputePriority(todoId: String): Float
    suspend fun recomputeAllPriorities(): Unit
    suspend fun updateStatus(todoId: String, status: TodoStatus): Result<Unit>
    suspend fun updateProgress(todoId: String, progress: Int): Result<Unit>
    suspend fun markComplete(todoId: String, actualDuration: Int?): Result<Unit>
    suspend fun addSubTask(todoId: String, subTask: SubTask): Result<SubTask>
    suspend fun updateSubTask(subTask: SubTask): Result<Unit>
    suspend fun deleteSubTask(subTaskId: String): Result<Unit>
    suspend fun toggleSubTask(subTaskId: String): Result<Unit>
    suspend fun addMilestone(todoId: String, milestone: Milestone): Result<Milestone>
    suspend fun updateMilestone(milestone: Milestone): Result<Unit>
    suspend fun deleteMilestone(milestoneId: String): Result<Unit>
    suspend fun addBlockReason(todoId: String, reason: BlockReason): Result<BlockReason>
    suspend fun resolveBlockReason(reasonId: String): Result<Unit>
    suspend fun detectConflict(todo: Todo): ConflictResult
    suspend fun getSmartSuggestions(todo: Todo): List<ConflictSuggestion>
    suspend fun getTodoContext(): TodoContext
}

interface TodoIntentClassifier {
    suspend fun classify(userInput: String): TodoIntent
    suspend fun extractTodoId(userInput: String): String?
    suspend fun extractCreateParams(userInput: String): Map<String, Any>?
}

interface PriorityEngine {
    fun computePriority(todo: Todo, habit: UserHabit? = null): Float
    fun getEisenhowerQuadrant(todo: Todo): Int
    fun sortByPriority(todos: List<Todo>): List<Todo>
}

interface ConflictEngine {
    fun detect(todo: Todo, existingTodos: List<Todo>): ConflictResult
    fun generateSuggestions(todo: Todo, conflict: ConflictResult): List<ConflictSuggestion>
    fun findNextAvailableSlot(duration: Int, conflicts: List<Todo>): Long
}

interface HabitEngine {
    suspend fun recordCompletion(todo: Todo, actualDuration: Int): UserHabit
    suspend fun recordFailure(todo: Todo): UserHabit
    fun predictCompletionRate(todo: Todo): Float
    suspend fun getHabit(): UserHabit
    fun observeHabit(): Flow<UserHabit>
}

interface WeatherService {
    suspend fun getCurrentWeather(lat: Double, lng: Double): Result<WeatherCondition>
    suspend fun getForecast(lat: Double, lng: Double, days: Int): Result<List<WeatherCondition>>
}

interface HolidayService {
    suspend fun isHoliday(date: Long): Boolean
    suspend fun getHolidayName(date: Long): String?
    suspend fun getUpcomingHolidays(count: Int): List<Pair<Long, String>>
}

interface ReminderScheduler {
    suspend fun scheduleReminder(reminder: TodoReminder): Result<Unit>
    suspend fun cancelReminder(todoId: String): Result<Unit>
    suspend fun rescheduleAll(): Result<Unit>
}
