package ai.androidclaw.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import ai.androidclaw.domain.service.todo.*
import ai.androidclaw.infrastructure.todo.db.*
import ai.androidclaw.infrastructure.todo.repository.TodoServiceImpl
import ai.androidclaw.infrastructure.todo.service.*
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object TodoModule {
    
    @Provides @Singleton
    fun provideTodoDatabase(@ApplicationContext ctx: Context) = Room.databaseBuilder(ctx, TodoDatabase::class.java, TodoDatabase.NAME).build()
    
    @Provides fun provideTodoDao(db: TodoDatabase) = db.todoDao()
    @Provides fun provideSubTaskDao(db: TodoDatabase) = db.subTaskDao()
    @Provides fun provideMilestoneDao(db: TodoDatabase) = db.milestoneDao()
    @Provides fun provideBlockReasonDao(db: TodoDatabase) = db.blockReasonDao()
    @Provides fun provideUserHabitDao(db: TodoDatabase) = db.userHabitDao()
    
    @Provides @Singleton
    fun providePriorityEngine() = PriorityEngineImpl()
    
    @Provides @Singleton
    fun provideConflictEngine() = ConflictEngineImpl()
    
    @Provides @Singleton
    fun provideHabitEngine(habitDao: UserHabitDao) = HabitEngineImpl(habitDao)
    
    @Provides @Singleton
    fun provideIntentClassifier() = TodoIntentClassifierImpl()
    
    @Provides @Singleton
    fun provideTodoService(
        todoDao: TodoDao, subTaskDao: SubTaskDao, milestoneDao: MilestoneDao, blockReasonDao: BlockReasonDao, habitDao: UserHabitDao,
        priorityEngine: PriorityEngine, conflictEngine: ConflictEngine, habitEngine: HabitEngine
    ) = TodoServiceImpl(todoDao, subTaskDao, milestoneDao, blockReasonDao, habitDao, priorityEngine, conflictEngine, habitEngine)
}
