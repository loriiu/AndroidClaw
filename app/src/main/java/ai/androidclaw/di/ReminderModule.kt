package ai.androidclaw.di

import android.content.Context
import androidx.work.WorkManager
import ai.androidclaw.data.repository.ReminderRepositoryImpl
import ai.androidclaw.domain.repository.ReminderRepository
import ai.androidclaw.infrastructure.reminder.NotificationHelper
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * 提醒模块
 */
@Module
@InstallIn(SingletonComponent::class)
object ReminderModule {
    
    @Provides
    @Singleton
    fun provideWorkManager(
        @ApplicationContext context: Context
    ): WorkManager {
        return WorkManager.getInstance(context)
    }
    
    @Provides
    @Singleton
    fun provideNotificationHelper(
        @ApplicationContext context: Context
    ): NotificationHelper {
        return NotificationHelper(context)
    }
    
    @Provides
    @Singleton
    fun provideReminderRepository(
        reminderDao: ai.androidclaw.data.local.db.ReminderDao,
        workManager: WorkManager
    ): ReminderRepository {
        return ReminderRepositoryImpl(reminderDao, workManager)
    }
}
