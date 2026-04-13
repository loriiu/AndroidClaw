package ai.androidclaw.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import ai.androidclaw.infrastructure.llm.*
import javax.inject.Singleton

/**
 * LLM 模块
 * 
 * 提供 LLM Provider 相关依赖
 */
@Module
@InstallIn(SingletonComponent::class)
object LlmModule {
    
    @Provides
    @Singleton
    fun provideOpenAiProvider(
        httpClient: okhttp3.OkHttpClient
    ): OpenAiProvider {
        return OpenAiProvider(httpClient)
    }
    
    @Provides
    @Singleton
    fun provideOllamaProvider(
        httpClient: okhttp3.OkHttpClient
    ): OllamaProvider {
        return OllamaProvider(httpClient)
    }
    
    @Provides
    @Singleton
    fun provideAnthropicProvider(
        httpClient: okhttp3.OkHttpClient
    ): AnthropicProvider {
        return AnthropicProvider(httpClient)
    }
}
