package ai.androidclaw.data.repository

import ai.androidclaw.data.local.preferences.ConfigDataStore
import ai.androidclaw.data.local.preferences.SecurePreferences
import ai.androidclaw.domain.model.LlmConfig
import ai.androidclaw.domain.model.ThemeMode
import ai.androidclaw.domain.model.UserConfig
import ai.androidclaw.domain.repository.ConfigRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 配置仓储实现
 */
@Singleton
class ConfigRepositoryImpl @Inject constructor(
    private val configDataStore: ConfigDataStore,
    private val securePreferences: SecurePreferences
) : ConfigRepository {
    
    // ========== LLM 配置 ==========
    
    override fun getLlmConfig(): Flow<LlmConfig?> {
        return configDataStore.getLlmConfig()
    }
    
    override suspend fun saveLlmConfig(config: LlmConfig) {
        configDataStore.saveLlmConfig(config)
        // 如果有 API Key，也保存到安全存储
        if (config.apiKey.isNotEmpty()) {
            securePreferences.saveApiKey(config.apiKey)
        }
    }
    
    override suspend fun getApiKey(): String? {
        return securePreferences.getApiKey()
    }
    
    override suspend fun saveApiKey(apiKey: String) {
        securePreferences.saveApiKey(apiKey)
    }
    
    override suspend fun deleteApiKey() {
        securePreferences.deleteApiKey()
    }
    
    // ========== 用户配置 ==========
    
    override fun getUserConfig(): Flow<UserConfig> {
        return configDataStore.getUserConfig()
    }
    
    override suspend fun saveUserConfig(config: UserConfig) {
        configDataStore.saveUserConfig(config)
    }
    
    override suspend fun updateUserName(name: String) {
        configDataStore.updateUserName(name)
    }
    
    override suspend fun updateUserRole(role: String) {
        configDataStore.updateUserRole(role)
    }
    
    override suspend fun updateSystemPrompt(prompt: String) {
        configDataStore.updateSystemPrompt(prompt)
    }
    
    override suspend fun updateThemeMode(mode: ThemeMode) {
        configDataStore.updateThemeMode(mode)
    }
    
    override suspend fun updateNotificationsEnabled(enabled: Boolean) {
        configDataStore.updateNotificationsEnabled(enabled)
    }
    
    // ========== 初始化状态 ==========
    
    override fun isInitialized(): Flow<Boolean> {
        return configDataStore.isInitialized()
    }
    
    override suspend fun setInitialized(initialized: Boolean) {
        configDataStore.setInitialized(initialized)
    }
}
