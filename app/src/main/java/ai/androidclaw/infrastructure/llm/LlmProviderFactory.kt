package ai.androidclaw.infrastructure.llm

import ai.androidclaw.domain.model.LlmProviderType

/**
 * LLM Provider 工厂
 * 
 * 用于根据配置创建对应的 LLM Provider 实例
 */
object LlmProviderFactory {
    
    /**
     * 创建 LLM Provider
     *
     * @param type 提供商类型
     * @param apiKey API Key
     * @param model 模型名称
     * @param baseUrl 基础 URL（可选）
     * @return LLM Provider 实例
     */
    fun create(
        type: LlmProviderType,
        apiKey: String = "",
        model: String,
        baseUrl: String? = null
    ): LlmProvider {
        return when (type) {
            LlmProviderType.OPENAI -> OpenAiProvider(apiKey, model)
            LlmProviderType.ANTHROPIC -> AnthropicProvider(apiKey, model)
            LlmProviderType.OLLAMA -> OllamaProvider(baseUrl ?: "http://localhost:11434", model)
            LlmProviderType.GEMINI -> GeminiProvider(apiKey, model)
        }
    }
}
