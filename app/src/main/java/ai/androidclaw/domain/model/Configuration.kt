package ai.androidclaw.domain.model

/**
 * LLM 提供商类型
 */
enum class LlmProviderType {
    OPENAI,
    ANTHROPIC,
    OLLAMA,
    GEMINI
}

/**
 * LLM 配置模型
 * 
 * 包含当前 LLM 连接的配置信息
 *
 * @property provider 提供商类型
 * @property apiKey API Key（加密存储）
 * @property model 模型名称
 * @property baseUrl API 基础地址（可选，用于自定义端点或 Ollama）
 */
data class LlmConfig(
    val provider: LlmProviderType,
    val apiKey: String = "",
    val model: String,
    val baseUrl: String? = null
) {
    companion object {
        /**
         * OpenAI 默认配置
         */
        fun defaultOpenAI(apiKey: String): LlmConfig {
            return LlmConfig(
                provider = LlmProviderType.OPENAI,
                apiKey = apiKey,
                model = "gpt-4o-mini"
            )
        }

        /**
         * Ollama 本地配置
         */
        fun defaultOllama(baseUrl: String = "http://localhost:11434"): LlmConfig {
            return LlmConfig(
                provider = LlmProviderType.OLLAMA,
                apiKey = "",
                model = "llama3",
                baseUrl = baseUrl
            )
        }

        /**
         * Anthropic 配置
         */
        fun defaultAnthropic(apiKey: String): LlmConfig {
            return LlmConfig(
                provider = LlmProviderType.ANTHROPIC,
                apiKey = apiKey,
                model = "claude-3-haiku-20240307"
            )
        }
    }
}

/**
 * 用户配置模型
 * 
 * 包含用户个性化设置
 *
 * @property name 用户名称
 * @property role 用户角色描述
 * @property systemPrompt 自定义系统提示词
 * @property themeMode 主题模式
 * @property notificationsEnabled 通知开关
 */
data class UserConfig(
    val name: String = "",
    val role: String = "",
    val systemPrompt: String = "",
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val notificationsEnabled: Boolean = true
)

/**
 * 主题模式
 */
enum class ThemeMode {
    LIGHT,
    DARK,
    SYSTEM
}
