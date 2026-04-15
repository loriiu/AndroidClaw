package ai.androidclaw.domain.model.todo

import java.util.UUID

data class Todo(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val description: String = "",
    val importance: Int = 3,
    val urgency: Int = 3,
    val deadline: Long? = null,
    val estimatedDuration: Int? = null,
    val actualDuration: Int? = null,
    val startTime: Long? = null,
    val location: TodoLocation? = null,
    val tags: List<String> = emptyList(),
    val category: String? = null,
    val status: TodoStatus = TodoStatus.PENDING,
    val progress: Int = 0,
    val subTasks: List<SubTask> = emptyList(),
    val milestones: List<Milestone> = emptyList(),
    val computedPriority: Float = 0f,
    val habitWeight: Float = 1f,
    val weatherSensitive: Boolean = false,
    val blockReason: String? = null,
    val notes: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

enum class TodoStatus { PENDING, IN_PROGRESS, COMPLETED, OVERDUE, CANCELLED }

data class SubTask(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val completed: Boolean = false,
    val order: Int = 0,
    val description: String? = null,
    val deadline: Long? = null
)

data class Milestone(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val targetTime: Long? = null,
    val targetProgress: Int,
    val achieved: Boolean = false,
    val achievedAt: Long? = null
)

data class TodoLocation(val name: String, val latitude: Double, val longitude: Double, val radius: Float = 100f)

data class BlockReason(
    val id: String = UUID.randomUUID().toString(),
    val reason: String,
    val category: BlockCategory = BlockCategory.UNKNOWN,
    val createdAt: Long = System.currentTimeMillis(),
    val resolved: Boolean = false,
    val resolvedAt: Long? = null
)

enum class BlockCategory { WAITING_INPUT, RESOURCE_UNAVAILABLE, DEPENDENCY, EXTERNAL, FATIGUE, UNKNOWN }

data class UserHabit(
    val id: String = "default",
    val avgCompletionMinutes: Int = 30,
    val hourlyCompletionRate: Map<Int, Float> = emptyMap(),
    val tagPreferences: Map<String, Float> = emptyMap(),
    val procrastinationScore: Float = 0.5f,
    val lastUpdated: Long = System.currentTimeMillis()
)

data class ConflictResult(
    val hasConflict: Boolean = false,
    val conflictType: ConflictType? = null,
    val conflictingTodos: List<Todo> = emptyList(),
    val suggestions: List<ConflictSuggestion> = emptyList()
)

enum class ConflictType { TIME_OVERLAP, TIME_ADJACENT, LOCATION_SAME, RESOURCE_BUSY }

data class ConflictSuggestion(
    val type: SuggestionType,
    val description: String,
    val alternativeTime: Long? = null,
    val alternativeLocation: TodoLocation? = null
)

enum class SuggestionType { RESCHEDULE, SPLIT, DELEGATE, CANCEL, MOVE }

sealed class TodoReminder {
    data class TimeReminder(val todoId: String, val remindAt: Long, val message: String? = null) : TodoReminder()
    data class LocationReminder(val todoId: String, val location: TodoLocation, val triggerType: LocationTrigger, val message: String? = null) : TodoReminder()
    data class WeatherReminder(val todoId: String, val condition: WeatherCondition, val advice: String) : TodoReminder()
    data class HolidayReminder(val todoId: String, val holidayName: String, val adjustedDeadline: Long? = null) : TodoReminder()
}

enum class LocationTrigger { ARRIVE, LEAVE }
enum class WeatherCondition { RAIN, STORM, SNOW, EXTREME_HEAT, EXTREME_COLD, NORMAL }
enum class TodoIntent { QUERY_TODOS, CREATE_TODO, UPDATE_TODO, DELETE_TODO, CHECK_PROGRESS, SCHEDULE_TODO, DETECT_CONFLICT, SUGGEST_OPTIMIZATION, UNKNOWN }

data class TodoContext(
    val todos: List<Todo> = emptyList(),
    val overdueTodos: List<Todo> = emptyList(),
    val todayTodos: List<Todo> = emptyList(),
    val upcomingDeadlines: List<Todo> = emptyList(),
    val conflictWarnings: List<ConflictResult> = emptyList(),
    val habitInsights: String? = null,
    val summary: String = ""
)
