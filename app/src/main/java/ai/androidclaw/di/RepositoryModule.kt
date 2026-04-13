package ai.androidclaw.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import ai.androidclaw.data.repository.*
import ai.androidclaw.domain.repository.*
import javax.inject.Singleton

/**
 * 仓储模块
 * 
 * 提供所有 Repository 接口的实现
 */
@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {
    
    @Provides
    @Singleton
    fun provideTaskRepository(
        taskDao: ai.androidclaw.data.local.db.TaskDao
    ): TaskRepository {
        return TaskRepositoryImpl(taskDao)
    }
    
    @Provides
    @Singleton
    fun provideChatRepository(
        messageDao: ai.androidclaw.data.local.db.MessageDao,
        conversationDao: ai.androidclaw.data.local.db.ConversationDao
    ): ChatRepository {
        return ChatRepositoryImpl(messageDao, conversationDao)
    }
    
    @Provides
    @Singleton
    fun provideSkillRepository(
        skillDao: ai.androidclaw.data.local.db.SkillDao
    ): SkillRepository {
        return SkillRepositoryImpl(skillDao)
    }
    
    @Provides
    @Singleton
    fun provideConfigRepository(
        configDataStore: ai.androidclaw.data.local.preferences.ConfigDataStore,
        securePreferences: ai.androidclaw.data.local.preferences.SecurePreferences
    ): ConfigRepository {
        return ConfigRepositoryImpl(configDataStore, securePreferences)
    }
}
