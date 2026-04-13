package ai.androidclaw.data.local.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import ai.androidclaw.domain.model.LlmConfig
import ai.androidclaw.domain.model.LlmProviderType
import ai.androidclaw.domain.model.ThemeMode
import ai.androidclaw.domain.model.UserConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "android_claw_prefs")

/**
 * 配置 DataStore
 * 
 * 存储非敏感的配置文件
 */
@Singleton
class ConfigDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    private object PreferencesKeys {
        // LLM 配置
        val LLM_PROVIDER = stringPreferencesKey("llm_provider")
        val LLM_MODEL = stringPreferencesKey("llm_model")
        val LLM_BASE_URL = stringPreferencesKey("llm_base_url")
        
        // 用户配置
        val USER_NAME = stringPreferencesKey("user_name")
        val USER_ROLE = stringPreferencesKey("user_role")
        val SYSTEM_PROMPT = stringPreferencesKey("system_prompt")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        
        // 初始化状态
        val IS_INITIALIZED = booleanPreferencesKey("is_initialized")
    }
    
    // ========== LLM 配置 ==========
    
    fun getLlmConfig(): Flow<LlmConfig?> {
        return context.dataStore.data.map { preferences ->
            val provider = preferences[PreferencesKeys.LLM_PROVIDER] ?: return@map null
            LlmConfig(
                provider = LlmProviderType.valueOf(provider),
                model = preferences[PreferencesKeys.LLM_MODEL] ?: "",
                baseUrl = preferences[PreferencesKeys.LLM_BASE_URL]
            )
        }
    }
    
    suspend fun saveLlmConfig(config: LlmConfig) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.LLM_PROVIDER] = config.provider.name
            preferences[PreferencesKeys.LLM_MODEL] = config.model
            config.baseUrl?.let { preferences[PreferencesKeys.LLM_BASE_URL] = it }
        }
    }
    
    // ========== 用户配置 ==========
    
    fun getUserConfig(): Flow<UserConfig> {
        return context.dataStore.data.map { preferences ->
            UserConfig(
                name = preferences[PreferencesKeys.USER_NAME] ?: "",
                role = preferences[PreferencesKeys.USER_ROLE] ?: "",
                systemPrompt = preferences[PreferencesKeys.SYSTEM_PROMPT] ?: "",
                themeMode = preferences[PreferencesKeys.THEME_MODE]?.let {
                    ThemeMode.valueOf(it)
                } ?: ThemeMode.SYSTEM,
                notificationsEnabled = preferences[PreferencesKeys.NOTIFICATIONS_ENABLED] ?: true
            )
        }
    }
    
    suspend fun saveUserConfig(config: UserConfig) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.USER_NAME] = config.name
            preferences[PreferencesKeys.USER_ROLE] = config.role
            preferences[PreferencesKeys.SYSTEM_PROMPT] = config.systemPrompt
            preferences[PreferencesKeys.THEME_MODE] = config.themeMode.name
            preferences[PreferencesKeys.NOTIFICATIONS_ENABLED] = config.notificationsEnabled
        }
    }
    
    suspend fun updateUserName(name: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.USER_NAME] = name
        }
    }
    
    suspend fun updateUserRole(role: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.USER_ROLE] = role
        }
    }
    
    suspend fun updateSystemPrompt(prompt: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.SYSTEM_PROMPT] = prompt
        }
    }
    
    suspend fun updateThemeMode(mode: ThemeMode) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.THEME_MODE] = mode.name
        }
    }
    
    suspend fun updateNotificationsEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.NOTIFICATIONS_ENABLED] = enabled
        }
    }
    
    // ========== 初始化状态 ==========
    
    fun isInitialized(): Flow<Boolean> {
        return context.dataStore.data.map { preferences ->
            preferences[PreferencesKeys.IS_INITIALIZED] ?: false
        }
    }
    
    suspend fun setInitialized(initialized: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.IS_INITIALIZED] = initialized
        }
    }
}
