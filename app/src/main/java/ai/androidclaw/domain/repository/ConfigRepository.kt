package ai.androidclaw.domain.repository

import ai.androidclaw.domain.model.LlmConfig
import ai.androidclaw.domain.model.ThemeMode
import ai.androidclaw.domain.model.UserConfig
import kotlinx.coroutines.flow.Flow

/**
 * 配置仓储接口
 * 
 * 定义用户配置和 LLM 配置的管理操作
 */
interface ConfigRepository {
    
    // ========== LLM 配置 ==========
    
    /**
     * 获取当前 LLM 配置
     */
    fun getLlmConfig(): Flow<LlmConfig?>
    
    /**
     * 保存 LLM 配置
     */
    suspend fun saveLlmConfig(config: LlmConfig)
    
    /**
     * 获取 API Key（加密存储）
     */
    suspend fun getApiKey(): String?
    
    /**
     * 保存 API Key（加密存储）
     */
    suspend fun saveApiKey(apiKey: String)
    
    /**
     * 删除 API Key
     */
    suspend fun deleteApiKey()
    
    // ========== 用户配置 ==========
    
    /**
     * 获取用户配置
     */
    fun getUserConfig(): Flow<UserConfig>
    
    /**
     * 保存用户配置
     */
    suspend fun saveUserConfig(config: UserConfig)
    
    /**
     * 更新用户名称
     */
    suspend fun updateUserName(name: String)
    
    /**
     * 更新用户角色
     */
    suspend fun updateUserRole(role: String)
    
    /**
     * 更新自定义系统提示词
     */
    suspend fun updateSystemPrompt(prompt: String)
    
    /**
     * 更新主题模式
     */
    suspend fun updateThemeMode(mode: ThemeMode)
    
    /**
     * 更新通知开关
     */
    suspend fun updateNotificationsEnabled(enabled: Boolean)
    
    // ========== 初始化状态 ==========
    
    /**
     * 检查是否已完成初始化配置
     */
    fun isInitialized(): Flow<Boolean>
    
    /**
     * 设置初始化完成标记
     */
    suspend fun setInitialized(initialized: Boolean)
}
