package ai.androidclaw.di

import ai.androidclaw.data.repository.McpRepositoryImpl
import ai.androidclaw.domain.repository.McpRepository
import ai.androidclaw.infrastructure.mcp.McpClient
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import javax.inject.Singleton

/**
 * MCP 模块
 */
@Module
@InstallIn(SingletonComponent::class)
object McpModule {
    
    @Provides
    @Singleton
    fun provideMcpClient(
        okHttpClient: OkHttpClient
    ): McpClient {
        return McpClient(okHttpClient)
    }
    
    @Provides
    @Singleton
    fun provideMcpRepository(
        mcpClient: McpClient,
        configDataStore: ai.androidclaw.data.local.preferences.ConfigDataStore
    ): McpRepository {
        return McpRepositoryImpl(mcpClient, configDataStore)
    }
}
