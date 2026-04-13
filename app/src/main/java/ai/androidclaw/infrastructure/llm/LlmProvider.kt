package ai.androidclaw.infrastructure.llm

import ai.androidclaw.domain.model.ChatMessage
import kotlinx.coroutines.flow.Flow

/**
 * LLM 提供商接口
 * 
 * 定义与大语言模型交互的统一接口
 * 支持多种 LLM 提供商（OpenAI、Anthropic、Ollama 等）
 */
interface LlmProvider {
    
    /**
     * 提供商标识
     */
    val providerId: String
    
    /**
     * 发送对话消息并获取回复
     *
     * @param messages 对话历史
     * @param systemPrompt 系统提示词（可选）
     * @return 助手的回复内容
     */
    suspend fun chat(messages: List<ChatMessage>, systemPrompt: String? = null): String
    
    /**
     * 发送对话消息并获取流式回复
     *
     * @param messages 对话历史
     * @param systemPrompt 系统提示词（可选）
     * @return 助手的回复内容流
     */
    fun chatStream(messages: List<ChatMessage>, systemPrompt: String? = null): Flow<String>
    
    /**
     * 测试连接
     *
     * @return 连接是否成功
     */
    suspend fun testConnection(): Boolean
    
    /**
     * 获取可用的模型列表
     *
     * @return 模型名称列表
     */
    suspend fun getAvailableModels(): List<String>
    
    /**
     * 检查 API Key 是否有效
     *
     * @return 是否有效
     */
    suspend fun isApiKeyValid(): Boolean
}

/**
 * LLM 响应结果
 */
sealed class LlmResponse {
    data class Success(val content: String) : LlmResponse()
    data class Error(val message: String, val code: String? = null) : LlmResponse()
    data object Loading : LlmResponse()
}

/**
 * 聊天请求
 */
data class ChatRequest(
    val messages: List<ChatMessage>,
    val systemPrompt: String? = null,
    val model: String? = null,
    val temperature: Float = 0.7f,
    val maxTokens: Int = 2048,
    val stream: Boolean = false
)
