package ai.androidclaw.infrastructure.llm

import ai.androidclaw.domain.model.ChatMessage
import ai.androidclaw.domain.model.MessageRole
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.sse.EventSource
import okhttp3.sse.EventSourceListener
import okhttp3.sse.EventSources
import javax.inject.Inject
import javax.inject.Singleton

/**
 * OpenAI API Provider
 * 
 * 实现 OpenAI GPT 系列模型的调用
 * 支持流式输出（SSE）
 */
@Singleton
class OpenAiProvider @Inject constructor(
    private val httpClient: OkHttpClient
) : LlmProvider {
    
    companion object {
        private const val DEFAULT_BASE_URL = "https://api.openai.com/v1"
        private const val CHAT_COMPLETIONS_ENDPOINT = "/chat/completions"
    }
    
    private var _apiKey: String = ""
    private var _model: String = "gpt-4o-mini"
    private var _baseUrl: String = DEFAULT_BASE_URL
    
    override val providerId: String = "openai"
    
    fun initialize(apiKey: String, model: String, baseUrl: String? = null) {
        _apiKey = apiKey
        _model = model
        _baseUrl = baseUrl ?: DEFAULT_BASE_URL
    }
    
    override suspend fun chat(messages: List<ChatMessage>, systemPrompt: String?): String {
        val requestMessages = buildMessagesList(messages, systemPrompt)
        
        val requestBody = """
            {
                "model": "$_model",
                "messages": $requestMessages,
                "temperature": 0.7
            }
        """.trimIndent()
        
        val request = Request.Builder()
            .url("$_baseUrl$CHAT_COMPLETIONS_ENDPOINT")
            .addHeader("Authorization", "Bearer $_apiKey")
            .addHeader("Content-Type", "application/json")
            .post(requestBody.toRequestBody("application/json".toMediaType()))
            .build()
        
        return try {
            val response = httpClient.newCall(request).execute()
            val responseBody = response.body?.string() ?: throw Exception("Empty response")
            
            // 简单解析 JSON 响应
            val json = Json { ignoreUnknownKeys = true }
            val parsed = json.parseToJsonElement(responseBody)
            parsed.jsonObject["choices"]
                ?.jsonArray
                ?.firstOrNull()
                ?.jsonObject
                ?.get("message")
                ?.jsonObject
                ?.get("content")
                ?.jsonPrimitive
                ?.content
                ?: throw Exception("Failed to parse response")
        } catch (e: Exception) {
            throw LlmException("OpenAI API call failed: ${e.message}", e)
        }
    }
    
    override fun chatStream(messages: List<ChatMessage>, systemPrompt: String?): Flow<String> = callbackFlow {
        val requestMessages = buildMessagesList(messages, systemPrompt)
        
        val requestBody = """
            {
                "model": "$_model",
                "messages": $requestMessages,
                "temperature": 0.7,
                "stream": true
            }
        """.trimIndent()
        
        val request = Request.Builder()
            .url("$_baseUrl$CHAT_COMPLETIONS_ENDPOINT")
            .addHeader("Authorization", "Bearer $_apiKey")
            .addHeader("Content-Type", "application/json")
            .post(requestBody.toRequestBody("application/json".toMediaType()))
            .build()
        
        val eventSourceFactory = EventSources.factoryBuilder(httpClient)
            .build()
        
        val eventSourceListener = object : EventSourceListener() {
            override fun onEvent(
                eventSource: EventSource,
                id: String?,
                type: String?,
                data: String
            ): Unit {
                if (data.isNotEmpty() && data != "[DONE]") {
                    try {
                        val json = Json { ignoreUnknownKeys = true }
                        val parsed = json.parseToJsonElement(data)
                        val content = parsed.jsonObject["choices"]
                            ?.jsonArray
                            ?.firstOrNull()
                            ?.jsonObject
                            ?.get("delta")
                            ?.jsonObject
                            ?.get("content")
                            ?.jsonPrimitive
                            ?.content
                        
                        content?.let { trySend(it) }
                    } catch (_: Exception) {
                        // 忽略解析错误
                    }
                }
            }
            
            override fun onClosed(eventSource: EventSource) {
                close()
            }
            
            override fun onFailure(
                eventSource: EventSource,
                t: Throwable?,
                response: Response?
            ): Unit {
                close(t)
            }
        }
        
        val eventSource = eventSourceFactory.newEventSource(request, eventSourceListener)
        
        awaitClose {
            eventSource.cancel()
        }
    }.flowOn(Dispatchers.IO)
    
    override suspend fun testConnection(): Boolean {
        return try {
            chat(listOf(ChatMessage.assistantMessage("test", "test")), "Reply only 'ok'")
            true
        } catch (e: Exception) {
            false
        }
    }
    
    override suspend fun getAvailableModels(): List<String> {
        // OpenAI 模型列表
        return listOf(
            "gpt-4o",
            "gpt-4o-mini",
            "gpt-4-turbo",
            "gpt-4",
            "gpt-3.5-turbo"
        )
    }
    
    override suspend fun isApiKeyValid(): Boolean {
        return testConnection()
    }
    
    private fun buildMessagesList(
        messages: List<ChatMessage>,
        systemPrompt: String?
    ): String {
        val msgList = mutableListOf<String>()
        
        // 添加系统提示
        systemPrompt?.let {
            msgList.add("""{"role": "system", "content": "$it"}""")
        }
        
        // 添加对话历史
        messages.forEach { msg ->
            val role = when (msg.role) {
                MessageRole.USER -> "user"
                MessageRole.ASSISTANT -> "assistant"
                MessageRole.SYSTEM -> "system"
            }
            msgList.add("""{"role": "$role", "content": "${msg.content.replace("\"", "\\\"")}"}""")
        }
        
        return "[${msgList.joinToString(",")}]"
    }
}

/**
 * LLM 异常
 */
class LlmException(message: String, cause: Throwable? = null) : Exception(message, cause)
