package ai.androidclaw.data.local.preferences

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 安全存储
 * 
 * 使用 EncryptedSharedPreferences 加密存储敏感数据
 * 主要用于存储 API Key 等敏感信息
 */
@Singleton
class SecurePreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    companion object {
        private const val PREF_FILE_NAME = "android_claw_secure_prefs"
        private const val KEY_API_KEY = "api_key"
    }
    
    private val masterKey: MasterKey by lazy {
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
    }
    
    private val securePrefs: SharedPreferences by lazy {
        EncryptedSharedPreferences.create(
            context,
            PREF_FILE_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }
    
    /**
     * 保存 API Key
     *
     * @param apiKey API Key
     */
    fun saveApiKey(apiKey: String) {
        securePrefs.edit().putString(KEY_API_KEY, apiKey).apply()
    }
    
    /**
     * 获取 API Key
     *
     * @return API Key，若不存在则返回 null
     */
    fun getApiKey(): String? {
        return securePrefs.getString(KEY_API_KEY, null)
    }
    
    /**
     * 删除 API Key
     */
    fun deleteApiKey() {
        securePrefs.edit().remove(KEY_API_KEY).apply()
    }
    
    /**
     * 检查是否已存储 API Key
     *
     * @return 是否存在
     */
    fun hasApiKey(): Boolean {
        return securePrefs.contains(KEY_API_KEY)
    }
    
    /**
     * 保存自定义值
     *
     * @param key 键
     * @param value 值
     */
    fun saveString(key: String, value: String) {
        securePrefs.edit().putString(key, value).apply()
    }
    
    /**
     * 获取自定义值
     *
     * @param key 键
     * @param defaultValue 默认值
     * @return 值
     */
    fun getString(key: String, defaultValue: String? = null): String? {
        return securePrefs.getString(key, defaultValue)
    }
    
    /**
     * 删除自定义值
     *
     * @param key 键
     */
    fun remove(key: String) {
        securePrefs.edit().remove(key).apply()
    }
    
    /**
     * 清空所有安全存储
     */
    fun clear() {
        securePrefs.edit().clear().apply()
    }
}
