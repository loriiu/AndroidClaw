package ai.androidclaw.infrastructure.todo.db

import androidx.room.*

@Entity(tableName = "todos", indices = [Index("status"), Index("deadline"), Index("computedPriority")])
data class TodoEntity(
    @PrimaryKey val id: String,
    val title: String, val description: String, val importance: Int, val urgency: Int,
    val deadline: Long?, val estimatedDuration: Int?, val actualDuration: Int?, val startTime: Long?,
    val locationName: String?, val locationLat: Double?, val locationLng: Double?, val locationRadius: Float?,
    val tags: String?, val category: String?, val status: String, val progress: Int,
    val computedPriority: Float, val habitWeight: Float, val weatherSensitive: Boolean,
    val blockReason: String?, val notes: String?, val createdAt: Long, val updatedAt: Long
)

@Entity(tableName = "sub_tasks", foreignKeys = [ForeignKey(entity = TodoEntity::class, parentColumns = ["id"], childColumns = ["todoId"], onDelete = ForeignKey.CASCADE)], indices = [Index("todoId")])
data class SubTaskEntity(@PrimaryKey val id: String, val todoId: String, val title: String, val completed: Boolean, val order: Int, val description: String?, val deadline: Long?)

@Entity(tableName = "milestones", foreignKeys = [ForeignKey(entity = TodoEntity::class, parentColumns = ["id"], childColumns = ["todoId"], onDelete = ForeignKey.CASCADE)], indices = [Index("todoId")])
data class MilestoneEntity(@PrimaryKey val id: String, val todoId: String, val title: String, val targetTime: Long?, val targetProgress: Int, val achieved: Boolean, val achievedAt: Long?)

@Entity(tableName = "block_reasons", foreignKeys = [ForeignKey(entity = TodoEntity::class, parentColumns = ["id"], childColumns = ["todoId"], onDelete = ForeignKey.CASCADE)], indices = [Index("todoId")])
data class BlockReasonEntity(@PrimaryKey val id: String, val todoId: String, val reason: String, val category: String, val resolved: Boolean, val createdAt: Long, val resolvedAt: Long?)

@Entity(tableName = "user_habits")
data class UserHabitEntity(@PrimaryKey val id: String, val avgCompletionMinutes: Int, val hourlyCompletionRate: String, val tagPreferences: String, val procrastinationScore: Float, val lastUpdated: Long)

@Dao interface TodoDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insert(todo: TodoEntity)
    @Update suspend fun update(todo: TodoEntity)
    @Delete suspend fun delete(todo: TodoEntity)
    @Query("DELETE FROM todos WHERE id = :id") suspend fun deleteById(id: String)
    @Query("SELECT * FROM todos WHERE id = :id") suspend fun getById(id: String): TodoEntity?
    @Query("SELECT * FROM todos WHERE id = :id") fun observeById(id: String): Flow<TodoEntity?>
    @Query("SELECT * FROM todos ORDER BY computedPriority DESC, deadline ASC") fun getAllSorted(): Flow<List<TodoEntity>>
    @Query("SELECT * FROM todos WHERE status IN ('PENDING','IN_PROGRESS') ORDER BY computedPriority DESC") fun getActiveSorted(): Flow<List<TodoEntity>>
    @Query("SELECT * FROM todos WHERE status = :status ORDER BY computedPriority DESC") fun getByStatus(status: String): Flow<List<TodoEntity>>
    @Query("SELECT * FROM todos WHERE tags LIKE '%' || :tag || '%'") fun getByTag(tag: String): Flow<List<TodoEntity>>
    @Query("SELECT * FROM todos WHERE deadline BETWEEN :start AND :end") fun getByDeadlineRange(start: Long, end: Long): Flow<List<TodoEntity>>
    @Query("SELECT * FROM todos WHERE deadline < :now AND status NOT IN ('COMPLETED','CANCELLED')") fun getOverdue(now: Long): Flow<List<TodoEntity>>
    @Query("UPDATE todos SET status = :status, updatedAt = :ts WHERE id = :id") suspend fun updateStatus(id: String, status: String, ts: Long)
    @Query("UPDATE todos SET progress = :progress, updatedAt = :ts WHERE id = :id") suspend fun updateProgress(id: String, progress: Int, ts: Long)
    @Query("UPDATE todos SET computedPriority = :priority WHERE id = :id") suspend fun updatePriority(id: String, priority: Float)
    @Query("UPDATE todos SET actualDuration = :duration, updatedAt = :ts WHERE id = :id") suspend fun updateActualDuration(id: String, duration: Int, ts: Long)
    @Query("UPDATE todos SET status = 'OVERDUE' WHERE deadline < :now AND status = 'PENDING'") suspend fun markOverdue(now: Long)
}

@Dao interface SubTaskDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insert(subTask: SubTaskEntity)
    @Update suspend fun update(subTask: SubTaskEntity)
    @Delete suspend fun delete(subTask: SubTaskEntity)
    @Query("SELECT * FROM sub_tasks WHERE todoId = :todoId ORDER BY `order`") fun getByTodoId(todoId: String): Flow<List<SubTaskEntity>>
    @Query("SELECT * FROM sub_tasks WHERE id = :id") suspend fun getById(id: String): SubTaskEntity?
    @Query("SELECT COUNT(*) FROM sub_tasks WHERE todoId = :todoId AND completed = 1") suspend fun getCompletedCount(todoId: String): Int
    @Query("SELECT COUNT(*) FROM sub_tasks WHERE todoId = :todoId") suspend fun getTotalCount(todoId: String): Int
    @Query("UPDATE sub_tasks SET completed = :completed WHERE id = :id") suspend fun updateCompleted(id: String, completed: Boolean)
}

@Dao interface MilestoneDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insert(milestone: MilestoneEntity)
    @Update suspend fun update(milestone: MilestoneEntity)
    @Delete suspend fun delete(milestone: MilestoneEntity)
    @Query("SELECT * FROM milestones WHERE todoId = :todoId ORDER BY targetProgress ASC") fun getByTodoId(todoId: String): Flow<List<MilestoneEntity>>
    @Query("UPDATE milestones SET achieved = 1, achievedAt = :ts WHERE id = :id") suspend fun markAchieved(id: String, ts: Long)
}

@Dao interface BlockReasonDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insert(blockReason: BlockReasonEntity)
    @Query("SELECT * FROM block_reasons WHERE todoId = :todoId AND resolved = 0") fun getActiveByTodoId(todoId: String): Flow<List<BlockReasonEntity>>
    @Query("UPDATE block_reasons SET resolved = 1, resolvedAt = :ts WHERE id = :id") suspend fun resolve(id: String, ts: Long)
}

@Dao interface UserHabitDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insert(habit: UserHabitEntity)
    @Query("SELECT * FROM user_habits WHERE id = 'default'") suspend fun get(): UserHabitEntity?
    @Query("SELECT * FROM user_habits WHERE id = 'default'") fun observe(): Flow<UserHabitEntity?>
}

@Database(entities = [TodoEntity::class, SubTaskEntity::class, MilestoneEntity::class, BlockReasonEntity::class, UserHabitEntity::class], version = 1, exportSchema = false)
abstract class TodoDatabase : RoomDatabase() {
    abstract fun todoDao(): TodoDao
    abstract fun subTaskDao(): SubTaskDao
    abstract fun milestoneDao(): MilestoneDao
    abstract fun blockReasonDao(): BlockReasonDao
    abstract fun userHabitDao(): UserHabitDao
    companion object { const val NAME = "todo_database" }
}
