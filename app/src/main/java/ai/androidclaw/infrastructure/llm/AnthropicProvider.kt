package ai.androidclaw.infrastructure.llm

import ai.androidclaw.domain.model.ChatMessage
import ai.androidclaw.domain.model.MessageRole
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Anthropic Claude API Provider
 * 
 * 实现 Anthropic Claude 系列模型的调用
 */
@Singleton
class AnthropicProvider @Inject constructor(
    private val httpClient: OkHttpClient
) : LlmProvider {
    
    companion object {
        private const val DEFAULT_BASE_URL = "https://api.anthropic.com/v1"
        private const val MESSAGES_ENDPOINT = "/messages"
        private const val API_VERSION = "2023-06-01"
    }
    
    private var _apiKey: String = ""
    private var _model: String = "claude-3-haiku-20240307"
    private var _baseUrl: String = DEFAULT_BASE_URL
    
    override val providerId: String = "anthropic"
    
    fun initialize(apiKey: String, model: String, baseUrl: String? = null) {
        _apiKey = apiKey
        _model = model
        _baseUrl = baseUrl ?: DEFAULT_BASE_URL
    }
    
    override suspend fun chat(messages: List<ChatMessage>, systemPrompt: String?): String {
        val anthropicMessages = buildMessagesList(messages)
        
        val requestBody = buildJsonObject {
            put("model", _model)
            put("messages", anthropicMessages)
            systemPrompt?.let { put("system", it) }
            put("max_tokens", 1024)
        }
        
        val request = Request.Builder()
            .url("$_baseUrl$MESSAGES_ENDPOINT")
            .addHeader("x-api-key", _apiKey)
            .addHeader("anthropic-version", API_VERSION)
            .addHeader("Content-Type", "application/json")
            .post(requestBody.toRequestBody("application/json".toMediaType()))
            .build()
        
        return try {
            val response = httpClient.newCall(request).execute()
            val responseBody = response.body?.string() ?: throw Exception("Empty response")
            
            val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
            val parsed = json.parseToJsonElement(responseBody)
            parsed.jsonObject["content"]
                ?.jsonArray
                ?.firstOrNull()
                ?.jsonObject
                ?.get("text")
                ?.jsonPrimitive
                ?.content
                ?: throw Exception("Failed to parse response")
        } catch (e: Exception) {
            throw LlmException("Anthropic API call failed: ${e.message}", e)
        }
    }
    
    override fun chatStream(messages: List<ChatMessage>, systemPrompt: String?): Flow<String> = flow {
        val anthropicMessages = buildMessagesList(messages)
        
        val requestBody = buildJsonObject {
            put("model", _model)
            put("messages", anthropicMessages)
            systemPrompt?.let { put("system", it) }
            put("max_tokens", 1024)
            put("stream", true)
        }
        
        val request = Request.Builder()
            .url("$_baseUrl$MESSAGES_ENDPOINT")
            .addHeader("x-api-key", _apiKey)
            .addHeader("anthropic-version", API_VERSION)
            .addHeader("Content-Type", "application/json")
            .post(requestBody.toRequestBody("application/json".toMediaType()))
            .build()
        
        httpClient.newCall(request).execute().use { response ->
            response.body?.byteStream()?.bufferedReader()?.use { reader ->
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    line?.let {
                        if (it.startsWith("data: ")) {
                            val data = it.removePrefix("data: ")
                            if (data != "[DONE]") {
                                try {
                                    val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
                                    val parsed = json.parseToJsonElement(data)
                                    val content = parsed.jsonObject["delta"]
                                        ?.jsonObject
                                        ?.get("text")
                                        ?.jsonPrimitive
                                        ?.content
                                    content?.let { emit(it) }
                                } catch (_: Exception) {
                                    // 忽略解析错误
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    
    override suspend fun testConnection(): Boolean {
        return try {
            chat(listOf(ChatMessage.assistantMessage("test", "test")), "Reply only 'ok'")
            true
        } catch (e: Exception) {
            false
        }
    }
    
    override suspend fun getAvailableModels(): List<String> {
        return listOf(
            "claude-3-5-sonnet-20241022",
            "claude-3-5-sonnet-latest",
            "claude-3-opus-20240229",
            "claude-3-sonnet-20240229",
            "claude-3-haiku-20240307"
        )
    }
    
    override suspend fun isApiKeyValid(): Boolean {
        return testConnection()
    }
    
    private fun buildMessagesList(messages: List<ChatMessage>): List<Map<String, String>> {
        return messages.mapNotNull { msg ->
            when (msg.role) {
                MessageRole.USER -> mapOf("role" to "user", "content" to msg.content)
                MessageRole.ASSISTANT -> mapOf("role" to "assistant", "content" to msg.content)
                MessageRole.SYSTEM -> null // 系统消息在单独字段处理
            }
        }
    }
    
    private fun buildJsonObject(block: kotlinx.serialization.json.JsonObjectBuilder.() -> Unit): String {
        val builder = kotlinx.serialization.json.JsonObjectBuilder()
        builder.block()
        return kotlinx.serialization.json.Json.encodeToString(
            kotlinx.serialization.json.JsonObject.serializer(),
            builder.build()
        )
    }
}
