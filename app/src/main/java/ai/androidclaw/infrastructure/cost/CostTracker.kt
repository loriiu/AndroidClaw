package ai.androidclaw.infrastructure.cost

data class CostEntry(val operation: String, val tokens: Int = 0, val latencyMs: Long = 0, val timestamp: Long = System.currentTimeMillis(), val metadata: Map<String, String> = emptyMap())
data class CostStats(val totalTokens: Long = 0, val totalOperations: Int = 0, val totalLatencyMs: Long = 0, val operationsByType: Map<String, Int> = emptyMap())

interface CostTracker {
    fun record(entry: CostEntry)
    fun getStats(): CostStats
    fun getEntries(operation: String? = null): List<CostEntry>
    fun reset()
}

object TodoOperations {
    const val PRIORITY_COMPUTE = "todo.priority.compute"
    const val CONFLICT_DETECT = "todo.conflict.detect"
    const val HABIT_LEARN = "todo.habit.learn"
    const val CONTEXT_BUILD = "todo.context.build"
    const val WEATHER_FETCH = "todo.weather.fetch"
    const val HOLIDAY_CHECK = "todo.holiday.check"
    const val INTENT_CLASSIFY = "todo.intent.classify"
}
