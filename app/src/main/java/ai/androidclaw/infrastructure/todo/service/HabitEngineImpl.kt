package ai.androidclaw.infrastructure.todo.service

import ai.androidclaw.domain.model.todo.Todo
import ai.androidclaw.domain.model.todo.UserHabit
import ai.androidclaw.domain.service.todo.HabitEngine
import ai.androidclaw.infrastructure.todo.db.UserHabitDao
import ai.androidclaw.infrastructure.todo.db.toDomain
import ai.androidclaw.infrastructure.todo.db.toEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HabitEngineImpl @Inject constructor(private val habitDao: UserHabitDao) : HabitEngine {
    
    override suspend fun recordCompletion(todo: Todo, actualDuration: Int): UserHabit {
        val current = getHabit()
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        
        val newAvg = if (current.avgCompletionMinutes == 0) actualDuration else (current.avgCompletionMinutes + actualDuration) / 2
        
        val newRates = current.hourlyCompletionRate.toMutableMap()
        newRates[hour] = ((newRates[hour] ?: 0.5f) + 0.1f).coerceAtMost(1f)
        
        var newProcrastination = current.procrastinationScore
        todo.estimatedDuration?.let { est ->
            if (actualDuration > est) newProcrastination = (newProcrastination + 0.15f).coerceAtMost(1f)
            else if (actualDuration < est * 0.8) newProcrastination = (newProcrastination - 0.05f).coerceAtLeast(0f)
        }
        
        val newTags = current.tagPreferences.toMutableMap()
        todo.tags.forEach { tag -> newTags[tag] = ((newTags[tag] ?: 0.5f) + 0.1f).coerceAtMost(1f) }
        
        val habit = UserHabit(id = current.id, avgCompletionMinutes = newAvg, hourlyCompletionRate = newRates, tagPreferences = newTags, procrastinationScore = newProcrastination, lastUpdated = System.currentTimeMillis())
        habitDao.insert(habit.toEntity())
        return habit
    }
    
    override suspend fun recordFailure(todo: Todo): UserHabit {
        val current = getHabit()
        val newTags = current.tagPreferences.toMutableMap()
        todo.tags.forEach { tag -> newTags[tag] = ((newTags[tag] ?: 0.5f) - 0.1f).coerceAtLeast(0f) }
        
        val habit = UserHabit(id = current.id, avgCompletionMinutes = current.avgCompletionMinutes, hourlyCompletionRate = current.hourlyCompletionRate, tagPreferences = newTags, procrastinationScore = (current.procrastinationScore + 0.1f).coerceAtMost(1f), lastUpdated = System.currentTimeMillis())
        habitDao.insert(habit.toEntity())
        return habit
    }
    
    override fun predictCompletionRate(todo: Todo): Float {
        var rate = 0.5f
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        rate += ((getHabitSync().hourlyCompletionRate[hour] ?: 0.5f) - 0.5f) * 0.3f
        if (todo.tags.isNotEmpty()) {
            val avgTag = todo.tags.mapNotNull { getHabitSync().tagPreferences[it] }.average()
            if (!avgTag.isNaN()) rate += (avgTag.toFloat() - 0.5f) * 0.2f
        }
        todo.estimatedDuration?.let { est ->
            val ratio = getHabitSync().avgCompletionMinutes.toFloat() / est
            if (ratio > 1.5f) rate += 0.1f else if (ratio < 0.7f) rate -= 0.1f
        }
        return rate.coerceIn(0f, 1f)
    }
    
    override suspend fun getHabit() = habitDao.get()?.toDomain() ?: UserHabit()
    override fun observeHabit() = habitDao.observe().map { it?.toDomain() ?: UserHabit() }
    
    private suspend fun getHabitSync() = habitDao.get()?.toDomain() ?: UserHabit()
}
