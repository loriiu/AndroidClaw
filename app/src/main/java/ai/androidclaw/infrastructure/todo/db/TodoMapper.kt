package ai.androidclaw.infrastructure.todo.db

import ai.androidclaw.domain.model.todo.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object TodoMapper {
    private val gson = Gson()
    
    fun TodoEntity.toDomain(subs: List<SubTask> = emptyList(), miles: List<Milestone> = emptyList()) = Todo(
        id = id, title = title, description = description, importance = importance, urgency = urgency,
        deadline = deadline, estimatedDuration = estimatedDuration, actualDuration = actualDuration, startTime = startTime,
        location = locationName?.let { TodoLocation(it, locationLat ?: 0.0, locationLng ?: 0.0, locationRadius ?: 100f) },
        tags = tags.fromJson() ?: emptyList(), category = category,
        status = TodoStatus.valueOf(status), progress = progress, subTasks = subs, milestones = miles,
        computedPriority = computedPriority, habitWeight = habitWeight, weatherSensitive = weatherSensitive,
        blockReason = blockReason, notes = notes, createdAt = createdAt, updatedAt = updatedAt
    )
    
    fun SubTaskEntity.toDomain() = SubTask(id = id, title = title, completed = completed, order = order, description = description, deadline = deadline)
    fun MilestoneEntity.toDomain() = Milestone(id = id, title = title, targetTime = targetTime, targetProgress = targetProgress, achieved = achieved, achievedAt = achievedAt)
    fun BlockReasonEntity.toDomain() = BlockReason(id = id, reason = reason, category = BlockCategory.valueOf(category), createdAt = createdAt, resolved = resolved, resolvedAt = resolvedAt)
    fun UserHabitEntity.toDomain() = UserHabit(id = id, avgCompletionMinutes = avgCompletionMinutes, hourlyCompletionRate = hourlyCompletionRate.fromJson() ?: emptyMap(), tagPreferences = tagPreferences.fromJson() ?: emptyMap(), procrastinationScore = procrastinationScore, lastUpdated = lastUpdated)
    
    fun Todo.toEntity() = TodoEntity(id = id, title = title, description = description, importance = importance, urgency = urgency, deadline = deadline, estimatedDuration = estimatedDuration, actualDuration = actualDuration, startTime = startTime, locationName = location?.name, locationLat = location?.latitude, locationLng = location?.longitude, locationRadius = location?.radius, tags = tags.toJson(), category = category, status = status.name, progress = progress, computedPriority = computedPriority, habitWeight = habitWeight, weatherSensitive = weatherSensitive, blockReason = blockReason, notes = notes, createdAt = createdAt, updatedAt = updatedAt)
    fun SubTask.toEntity(todoId: String) = SubTaskEntity(id = id, todoId = todoId, title = title, completed = completed, order = order, description = description, deadline = deadline)
    fun Milestone.toEntity(todoId: String) = MilestoneEntity(id = id, todoId = todoId, title = title, targetTime = targetTime, targetProgress = targetProgress, achieved = achieved, achievedAt = achievedAt)
    fun BlockReason.toEntity(todoId: String) = BlockReasonEntity(id = id, todoId = todoId, reason = reason, category = category.name, resolved = resolved, createdAt = createdAt, resolvedAt = resolvedAt)
    fun UserHabit.toEntity() = UserHabitEntity(id = id, avgCompletionMinutes = avgCompletionMinutes, hourlyCompletionRate = hourlyCompletionRate.toJson(), tagPreferences = tagPreferences.toJson(), procrastinationScore = procrastinationScore, lastUpdated = lastUpdated)
    
    private fun <T> String?.fromJson(): T? = try { gson.fromJson(this, object : TypeToken<T>() {}.type) } catch (e: Exception) { null }
    private fun <T> T?.toJson() = this?.let { gson.toJson(it) }
}
