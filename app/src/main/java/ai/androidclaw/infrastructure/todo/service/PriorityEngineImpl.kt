package ai.androidclaw.infrastructure.todo.service

import ai.androidclaw.domain.model.todo.Todo
import ai.androidclaw.domain.model.todo.UserHabit
import ai.androidclaw.domain.service.todo.PriorityEngine
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PriorityEngineImpl @Inject constructor() : PriorityEngine {
    
    override fun computePriority(todo: Todo, habit: UserHabit?): Float {
        var priority = todo.importance * todo.urgency * 0.1f
        
        todo.deadline?.let { dl ->
            val hours = (dl - System.currentTimeMillis()) / (1000f * 3600f)
            priority += when {
                hours <= 1 -> 3.0f
                hours <= 4 -> 2.0f
                hours <= 24 -> 1.0f
                hours <= 72 -> 0.5f
                else -> 0f
            }
        }
        
        if (todo.blockReason != null) priority -= 0.5f
        
        habit?.let { h ->
            val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
            val productivity = h.hourlyCompletionRate[hour] ?: 0.5f
            priority *= (0.5f + productivity)
            priority *= (1.0f + h.procrastinationScore * 0.3f)
        }
        
        return priority.coerceIn(0f, 5f)
    }
    
    override fun getEisenhowerQuadrant(todo: Todo): Int {
        val important = todo.importance >= 4
        val urgent = todo.urgency >= 4
        return when {
            important && urgent -> 1
            important && !urgent -> 2
            !important && urgent -> 3
            else -> 4
        }
    }
    
    override fun sortByPriority(todos: List<Todo>): List<Todo> = todos.sortedByDescending { it.computedPriority }
}
