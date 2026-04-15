package ai.androidclaw.infrastructure.todo.service

import ai.androidclaw.domain.model.todo.*
import ai.androidclaw.domain.service.todo.ConflictEngine
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.*

@Singleton
class ConflictEngineImpl @Inject constructor() : ConflictEngine {
    
    companion object {
        private const val ADJACENT_MS = 15 * 60 * 1000L
        private const val OVERLAP_TOLERANCE_MS = 5 * 60 * 1000L
        private const val LOCATION_THRESHOLD_M = 100.0
    }
    
    override fun detect(todo: Todo, existingTodos: List<Todo>): ConflictResult {
        val conflicts = mutableListOf<Todo>()
        var conflictType: ConflictType? = null
        
        for (existing in existingTodos) {
            if (existing.id == todo.id) continue
            if (existing.status == TodoStatus.COMPLETED || existing.status == TodoStatus.CANCELLED) continue
            
            when {
                isTimeOverlap(todo, existing) -> { conflicts.add(existing); conflictType = ConflictType.TIME_OVERLAP }
                isTimeAdjacent(todo, existing) -> { conflicts.add(existing); if (conflictType == null) conflictType = ConflictType.TIME_ADJACENT }
                isLocationConflict(todo, existing) -> { conflicts.add(existing); if (conflictType == null) conflictType = ConflictType.LOCATION_SAME }
            }
        }
        
        return ConflictResult(hasConflict = conflicts.isNotEmpty(), conflictType = conflictType, conflictingTodos = conflicts,
            suggestions = if (conflicts.isNotEmpty()) generateSuggestions(todo, ConflictResult(true, conflictType, conflicts)) else emptyList())
    }
    
    override fun generateSuggestions(todo: Todo, conflict: ConflictResult): List<ConflictSuggestion> {
        val suggestions = mutableListOf<ConflictSuggestion>()
        val nextSlot = findNextAvailableSlot(todo.estimatedDuration ?: 60, conflict.conflictingTodos)
        suggestions.add(ConflictSuggestion(SuggestionType.RESCHEDULE, "建议安排在 ${formatTime(nextSlot)}", nextSlot))
        if ((todo.estimatedDuration ?: 0) > 60) suggestions.add(ConflictSuggestion(SuggestionType.SPLIT, "建议拆分为多个短时段子任务"))
        val lowPriority = conflict.conflictingTodos.filter { it.computedPriority < todo.computedPriority }
        if (lowPriority.isNotEmpty()) suggestions.add(ConflictSuggestion(SuggestionType.CANCEL, "可考虑推迟: ${lowPriority.joinToString { it.title }}"))
        return suggestions
    }
    
    override fun findNextAvailableSlot(duration: Int, conflicts: List<Todo>): Long {
        var candidate = System.currentTimeMillis()
        val sorted = conflicts.sortedBy { it.startTime ?: it.deadline ?: Long.MAX_VALUE }
        for (todo in sorted) {
            val todoStart = todo.startTime ?: todo.deadline ?: continue
            val todoEnd = todoStart + (todo.estimatedDuration ?: 60) * 60 * 1000L
            if (candidate + duration * 60 * 1000L <= todoStart - ADJACENT_MS) return candidate
            candidate = todoEnd + ADJACENT_MS
        }
        return candidate
    }
    
    private fun isTimeOverlap(a: Todo, b: Todo): Boolean {
        val aStart = a.startTime ?: return false
        val aDur = (a.estimatedDuration ?: 60) * 60 * 1000L
        val bStart = b.startTime ?: return false
        val bDur = (b.estimatedDuration ?: 60) * 60 * 1000L
        return aStart - OVERLAP_TOLERANCE_MS < bStart + bDur && bStart - OVERLAP_TOLERANCE_MS < aStart + aDur
    }
    
    private fun isTimeAdjacent(a: Todo, b: Todo): Boolean {
        val aStart = a.startTime ?: return false
        val aEnd = aStart + (a.estimatedDuration ?: 60) * 60 * 1000L
        val bStart = b.startTime ?: return false
        val bEnd = bStart + (b.estimatedDuration ?: 60) * 60 * 1000L
        return abs(aEnd - bStart) < ADJACENT_MS || abs(bEnd - aStart) < ADJACENT_MS
    }
    
    private fun isLocationConflict(a: Todo, b: Todo): Boolean {
        val locA = a.location ?: return false
        val locB = b.location ?: return false
        if (locA.name == locB.name) return true
        return calculateDistance(locA.latitude, locA.longitude, locB.latitude, locB.longitude) < LOCATION_THRESHOLD_M
    }
    
    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat/2).pow(2) + cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon/2).pow(2)
        return r * 2 * atan2(sqrt(a), sqrt(1-a))
    }
    
    private fun formatTime(timestamp: Long): String {
        val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
        return "${cal.get(Calendar.HOUR_OF_DAY)}:${"%02d".format(cal.get(Calendar.MINUTE))}"
    }
}
