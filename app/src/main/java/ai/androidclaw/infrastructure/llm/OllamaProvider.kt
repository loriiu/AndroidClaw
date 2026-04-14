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
 * Ollama 本地 LLM Provider
 * 
 * 实现 Ollama 本地模型的调用
 * 支持流式输出（SSE）
 */
@Singleton
class OllamaProvider @Inject constructor(
    private val httpClient: OkHttpClient
) : LlmProvider {
    
    companion object {
        private const val DEFAULT_BASE_URL = "http://localhost:11434"
        private const val CHAT_ENDPOINT = "/api/chat"
        private const val GENERATE_ENDPOINT = "/api/generate"
        private const val TAGS_ENDPOINT = "/api/tags"
    }
    
    private var _baseUrl: String = DEFAULT_BASE_URL
    private var _model: String = "llama3"
    
    override val providerId: String = "ollama"
    
    fun initialize(baseUrl: String, model: String) {
        _baseUrl = baseUrl.trimEnd('/')
        _model = model
    }
    
    override suspend fun chat(messages: List<ChatMessage>, systemPrompt: String?): String {
        val ollamaMessages = buildMessagesList(messages, systemPrompt)
        
        val requestBody = """
            {
                "model": "$_model",
                "messages": $ollamaMessages,
                "stream": false
            }
        """.trimIndent()
        
        val request = Request.Builder()
            .url("$_baseUrl$CHAT_ENDPOINT")
            .addHeader("Content-Type", "application/json")
            .post(requestBody.toRequestBody("application/json".toMediaType()))
            .build()
        
        return try {
            val response = httpClient.newCall(request).execute()
            val responseBody = response.body?.string() ?: throw Exception("Empty response")
            
            val json = Json { ignoreUnknownKeys = true }
            val parsed = json.parseToJsonElement(responseBody)
            parsed.jsonObject["message"]
                ?.jsonObject
                ?.get("content")
                ?.jsonPrimitive
                ?.content
                ?: throw Exception("Failed to parse response")
        } catch (e: Exception) {
            throw LlmException("Ollama API call failed: ${e.message}", e)
        }
    }
    
    override fun chatStream(messages: List<ChatMessage>, systemPrompt: String?): Flow<String> = callbackFlow {
        val ollamaMessages = buildMessagesList(messages, systemPrompt)
        
        val requestBody = """
            {
                "model": "$_model",
                "messages": $ollamaMessages,
                "stream": true
            }
        """.trimIndent()
        
        val request = Request.Builder()
            .url("$_baseUrl$CHAT_ENDPOINT")
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
                if (data.isNotEmpty()) {
                    try {
                        val json = Json { ignoreUnknownKeys = true }
                        val parsed = json.parseToJsonElement(data)
                        // Ollama SSE 格式: {"message": {"role": "assistant", "content": "..."}, "done": false}
                        val content = parsed.jsonObject["message"]
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
            val request = Request.Builder()
                .url("$_baseUrl$TAGS_ENDPOINT")
                .get()
                .build()
            
            val response = httpClient.newCall(request).execute()
            response.isSuccessful
        } catch (e: Exception) {
            false
        }
    }
    
    override suspend fun getAvailableModels(): List<String> {
        return try {
            val request = Request.Builder()
                .url("$_baseUrl$TAGS_ENDPOINT")
                .get()
                .build()
            
            val response = httpClient.newCall(request).execute()
            val responseBody = response.body?.string() ?: return emptyList()
            
            val json = Json { ignoreUnknownKeys = true }
            val parsed = json.parseToJsonElement(responseBody)
            parsed.jsonObject["models"]
                ?.jsonArray
                ?.map { it.jsonObject["name"]?.jsonPrimitive?.content ?: "" }
                ?.filter { it.isNotEmpty() }
                ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    override suspend fun isApiKeyValid(): Boolean {
        // Ollama 不需要 API Key
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
