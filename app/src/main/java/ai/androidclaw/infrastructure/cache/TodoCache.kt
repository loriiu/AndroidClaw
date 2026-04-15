package ai.androidclaw.infrastructure.cache

data class CacheConfig(val enabled: Boolean = true, val ttlSeconds: Long = 300, val maxSize: Int = 100, val staleWhileRevalidate: Boolean = true)

data class CacheEntry<T>(val data: T, val timestamp: Long, val ttlSeconds: Long) {
    fun isExpired() = System.currentTimeMillis() - timestamp > ttlSeconds * 1000
    fun isStale(thresholdSeconds: Long = 60) = System.currentTimeMillis() - timestamp > (ttlSeconds - thresholdSeconds) * 1000
}

interface TodoCache {
    suspend fun <T> get(key: String): T?
    suspend fun <T> put(key: String, data: T, ttlSeconds: Long = 300)
    suspend fun invalidate(key: String)
    suspend fun invalidatePattern(pattern: String)
    suspend fun clear()
}

object CacheKeys {
    const val ALL_TODOS = "todo:all"
    const val ACTIVE_TODOS = "todo:active"
    const val TODO_PREFIX = "todo:"
    const val SORTED_TODOS = "todo:sorted"
    const val CONFLICT_PREFIX = "conflict:"
    const val HABIT = "habit:default"
    const val CONTEXT = "todo:context"
    fun todo(todoId: String) = "$TODO_PREFIX$todoId"
    fun conflict(todoId: String) = "$CONFLICT_PREFIX$todoId"
}
