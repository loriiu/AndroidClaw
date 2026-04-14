package ai.androidclaw.infrastructure.mcp

import ai.androidclaw.domain.model.mcp.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import kotlinx.serialization.*
import kotlinx.serialization.json.*
import okhttp3.*
import okhttp3.sse.*
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * MCP 客户端
 * 
 * 管理 MCP 服务器连接和工具调用
 */
@Singleton
class McpClient @Inject constructor(
    private val okHttpClient: OkHttpClient
) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }
    
    private val connections = mutableMapOf<String, McpConnection>()
    
    /**
     * 连接到 MCP 服务器
     */
    suspend fun connect(config: McpServerConfig): Flow<McpServerStatus> = flow {
        emit(McpServerStatus.Connecting)
        
        try {
            val connection = when (config.type) {
                McpServerType.SSE -> SseMcpConnection(config, okHttpClient, json)
                McpServerType.WEBSOCKET -> WebSocketMcpConnection(config, okHttpClient, json)
            }
            
            connections[config.id] = connection
            
            val tools = connection.initialize()
            emit(McpServerStatus.Connected(tools))
            
            // 持续监听连接状态
            connection.status.collect { status ->
                emit(status)
            }
        } catch (e: Exception) {
            emit(McpServerStatus.Error(e.message ?: "Connection failed"))
        }
    }
    
    /**
     * 断开 MCP 服务器连接
     */
    fun disconnect(serverId: String) {
        connections[serverId]?.close()
        connections.remove(serverId)
    }
    
    /**
     * 断开所有连接
     */
    fun disconnectAll() {
        connections.values.forEach { it.close() }
        connections.clear()
    }
    
    /**
     * 获取已连接服务器的工具列表
     */
    fun getTools(serverId: String): List<McpTool> {
        return (connections[serverId] as? SseMcpConnection)?.tools ?: emptyList()
    }
    
    /**
     * 调用 MCP 工具
     */
    suspend fun callTool(serverId: String, toolName: String, arguments: Map<String, Any>): McpToolResult {
        val connection = connections[serverId] ?: return McpToolResult.Error("Server not connected")
        return connection.callTool(toolName, arguments)
    }
    
    /**
     * 调用 MCP 工具（通过 McpToolCall）
     */
    suspend fun callTool(call: McpToolCall): McpToolResult {
        return callTool(call.serverId, call.toolName, call.arguments)
    }
    
    /**
     * 获取所有已连接服务器的工具
     */
    fun getAllTools(): Map<String, List<McpTool>> {
        return connections.mapValues { (_, conn) ->
            (conn as? SseMcpConnection)?.tools ?: emptyList()
        }
    }
}

/**
 * MCP 连接接口
 */
interface McpConnection {
    val status: Flow<McpServerStatus>
    suspend fun initialize(): List<McpTool>
    suspend fun callTool(name: String, arguments: Map<String, Any>): McpToolResult
    fun close()
}

/**
 * SSE 方式的 MCP 连接
 */
class SseMcpConnection(
    private val config: McpServerConfig,
    private val httpClient: OkHttpClient,
    private val json: Json
) : McpConnection {
    
    private val _status = MutableStateFlow<McpServerStatus>(McpServerStatus.Disconnected)
    override val status: Flow<McpServerStatus> = _status.asStateFlow()
    
    private var eventSource: EventSource? = null
    private val pendingRequests = mutableMapOf<String, CompletableDeferred<JsonObject>>()
    var tools: List<McpTool> = emptyList()
        private set
    
    private val eventSourceFactory = EventSources.factoryBuilder(httpClient).build()
    
    override suspend fun initialize(): List<McpTool> = withContext(Dispatchers.IO) {
        val requestId = UUID.randomUUID().toString()
        val deferred = CompletableDeferred<JsonObject>()
        pendingRequests[requestId] = deferred
        
        // 初始化请求
        val initRequest = mapOf(
            "jsonrpc" to "2.0",
            "id" to requestId,
            "method" to "initialize",
            "params" to mapOf(
                "protocolVersion" to McpProtocol.VERSION,
                "capabilities" to mapOf(
                    "tools" to mapOf("listChanged" to true)
                ),
                "clientInfo" to mapOf(
                    "name" to "androidclaw",
                    "version" to "0.1.0"
                )
            )
        )
        
        val request = Request.Builder()
            .url(config.url)
            .addHeader("Content-Type", "application/json")
            .apply { config.authToken?.let { addHeader("Authorization", "Bearer $it") } }
            .post(RequestBody.create(
                MediaType.parse("application/json"),
                json.encodeToString(JsonObject.serializer(), JsonObject(initRequest))
            ))
            .build()
        
        eventSource = eventSourceFactory.newEventSource(request, object : EventSourceListener() {
            override fun onOpen(eventSource: EventSource, response: Response) {
                _status.value = McpServerStatus.Connected(emptyList())
            }
            
            override fun onEvent(eventSource: EventSource, id: String?, type: String?, data: String) {
                try {
                    val jsonData = json.parseToJsonElement(data).jsonObject
                    handleServerMessage(jsonData)
                } catch (e: Exception) {
                    // 忽略解析错误
                }
            }
            
            override fun onClosed(eventSource: EventSource) {
                _status.value = McpServerStatus.Disconnected
            }
            
            override fun onFailure(eventSource: EventSource, t: Throwable?, response: Response?) {
                _status.value = McpServerStatus.Error(t?.message ?: "Connection failed")
            }
        })
        
        // 发送初始请求并等待响应
        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw Exception("HTTP ${response.code}: ${response.message}")
            }
        }
        
        // 发送 tools/list 请求
        val listToolsRequest = mapOf(
            "jsonrpc" to "2.0",
            "id" to UUID.randomUUID().toString(),
            "method" to "tools/list",
            "params" to emptyMap<String, Any>()
        )
        
        httpClient.newCall(Request.Builder()
            .url(config.url)
            .addHeader("Content-Type", "application/json")
            .apply { config.authToken?.let { addHeader("Authorization", "Bearer $it") } }
            .post(RequestBody.create(
                MediaType.parse("application/json"),
                json.encodeToString(JsonObject.serializer(), JsonObject(listToolsRequest))
            ))
            .build()).execute().use { response ->
            val body = response.body?.string() ?: return@use
            val jsonData = json.parseToJsonElement(body).jsonObject
            tools = parseToolsFromResult(jsonData)
        }
        
        // 发送 initialized 通知
        val initializedNotification = mapOf(
            "jsonrpc" to "2.0",
            "method" to "notifications/initialized",
            "params" to emptyMap<String, Any>()
        )
        
        httpClient.newCall(Request.Builder()
            .url(config.url)
            .addHeader("Content-Type", "application/json")
            .apply { config.authToken?.let { addHeader("Authorization", "Bearer $it") } }
            .post(RequestBody.create(
                MediaType.parse("application/json"),
                json.encodeToString(JsonObject.serializer(), JsonObject(initializedNotification))
            ))
            .build()).execute()
        
        _status.value = McpServerStatus.Connected(tools)
        tools
    }
    
    private fun handleServerMessage(data: JsonObject) {
        val id = data["id"]?.jsonPrimitive?.content
        if (id != null && pendingRequests.containsKey(id)) {
            pendingRequests[id]?.complete(data)
            pendingRequests.remove(id)
        }
    }
    
    private fun parseToolsFromResult(data: JsonObject): List<McpTool> {
        val result = data["result"]?.jsonObject ?: return emptyList()
        val toolsArray = result["tools"]?.jsonArray ?: return emptyList()
        
        return toolsArray.mapNotNull { toolElement ->
            val tool = toolElement.jsonObject
            val name = tool["name"]?.jsonPrimitive?.content ?: return@mapNotNull null
            val description = tool["description"]?.jsonPrimitive?.content ?: ""
            val inputSchema = parseInputSchema(tool["inputSchema"]?.jsonObject)
            
            McpTool(name, description, inputSchema)
        }
    }
    
    private fun parseInputSchema(schema: JsonObject?): McpInputSchema {
        if (schema == null) return McpInputSchema()
        
        val type = schema["type"]?.jsonPrimitive?.content ?: "object"
        val properties = mutableMapOf<String, McpProperty>()
        val required = mutableListOf<String>()
        
        schema["properties"]?.jsonObject?.forEach { (key, value) ->
            val prop = value.jsonObject
            properties[key] = McpProperty(
                type = prop["type"]?.jsonPrimitive?.content ?: "string",
                description = prop["description"]?.jsonPrimitive?.content,
                enum = prop["enum"]?.jsonArray?.map { it.jsonPrimitive.content }
            )
        }
        
        schema["required"]?.jsonArray?.forEach {
            required.add(it.jsonPrimitive.content)
        }
        
        return McpInputSchema(type, properties, required)
    }
    
    override suspend fun callTool(name: String, arguments: Map<String, Any>): McpToolResult = 
        withContext(Dispatchers.IO) {
            try {
                val requestId = UUID.randomUUID().toString()
                val request = mapOf(
                    "jsonrpc" to "2.0",
                    "id" to requestId,
                    "method" to "tools/call",
                    "params" to mapOf(
                        "name" to name,
                        "arguments" to arguments
                    )
                )
                
                val httpRequest = Request.Builder()
                    .url(config.url)
                    .addHeader("Content-Type", "application/json")
                    .apply { config.authToken?.let { addHeader("Authorization", "Bearer $it") } }
                    .post(RequestBody.create(
                        MediaType.parse("application/json"),
                        json.encodeToString(JsonObject.serializer(), JsonObject(request))
                    ))
                    .build()
                
                httpClient.newCall(httpRequest).execute().use { response ->
                    val body = response.body?.string() ?: 
                        return@withContext McpToolResult.Error("Empty response")
                    
                    val jsonData = json.parseToJsonElement(body).jsonObject
                    parseToolResult(jsonData)
                }
            } catch (e: Exception) {
                McpToolResult.Error(e.message ?: "Tool call failed")
            }
        }
    
    private fun parseToolResult(data: JsonObject): McpToolResult {
        val error = data["error"]
        if (error != null) {
            return McpToolResult.Error(error.jsonObject["message"]?.jsonPrimitive?.content ?: "Unknown error")
        }
        
        val result = data["result"]?.jsonObject ?: 
            return McpToolResult.Error("No result in response")
        
        val content = mutableListOf<McpContent>()
        result["content"]?.jsonArray?.forEach { item ->
            val itemObj = item.jsonObject
            when (itemObj["type"]?.jsonPrimitive?.content) {
                "text" -> {
                    itemObj["text"]?.jsonPrimitive?.content?.let { 
                        content.add(McpContent.Text(it)) 
                    }
                }
                "image" -> {
                    val dataStr = itemObj["data"]?.jsonPrimitive?.content
                    val mimeType = itemObj["mimeType"]?.jsonPrimitive?.content
                    if (dataStr != null) {
                        content.add(McpContent.Image(dataStr, mimeType ?: "image/png"))
                    }
                }
            }
        }
        
        val isError = result["isError"]?.jsonPrimitive?.content?.toBoolean() ?: false
        return McpToolResult.Success(content, isError)
    }
    
    override fun close() {
        eventSource?.cancel()
        _status.value = McpServerStatus.Disconnected
    }
}

/**
 * WebSocket 方式的 MCP 连接
 */
class WebSocketMcpConnection(
    private val config: McpServerConfig,
    private val httpClient: OkHttpClient,
    private val json: Json
) : McpConnection {
    
    private val _status = MutableStateFlow<McpServerStatus>(McpServerStatus.Disconnected)
    override val status: Flow<McpServerStatus> = _status.asStateFlow()
    
    private var webSocket: WebSocket? = null
    private val pendingRequests = mutableMapOf<String, CompletableDeferred<JsonObject>>()
    var tools: List<McpTool> = emptyList()
        private set
    
    override suspend fun initialize(): List<McpTool> = withContext(Dispatchers.IO) {
        val wsUrl = config.url.replace("http://", "ws://").replace("https://", "wss://")
        
        val request = Request.Builder()
            .url(wsUrl)
            .apply { config.authToken?.let { addHeader("Authorization", "Bearer $it") } }
            .build()
        
        val deferred = CompletableDeferred<List<McpTool>>()
        
        webSocket = httpClient.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                _status.value = McpServerStatus.Connected(emptyList())
            }
            
            override fun onMessage(webSocket: WebSocket, text: String) {
                try {
                    val data = json.parseToJsonElement(text).jsonObject
                    handleMessage(data)
                } catch (e: Exception) {
                    // 忽略
                }
            }
            
            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                _status.value = McpServerStatus.Disconnected
            }
            
            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                _status.value = McpServerStatus.Error(t.message ?: "Connection failed")
            }
        })
        
        // 发送初始化请求
        val initRequest = mapOf(
            "jsonrpc" to "2.0",
            "id" to UUID.randomUUID().toString(),
            "method" to "initialize",
            "params" to mapOf(
                "protocolVersion" to McpProtocol.VERSION,
                "capabilities" to mapOf("tools" to mapOf("listChanged" to true)),
                "clientInfo" to mapOf("name" to "androidclaw", "version" to "0.1.0")
            )
        )
        
        webSocket?.send(json.encodeToString(JsonObject.serializer(), JsonObject(initRequest)))
        
        tools
    }
    
    private fun handleMessage(data: JsonObject) {
        val id = data["id"]?.jsonPrimitive?.content
        if (id != null && pendingRequests.containsKey(id)) {
            pendingRequests[id]?.complete(data)
            pendingRequests.remove(id)
        }
    }
    
    override suspend fun callTool(name: String, arguments: Map<String, Any>): McpToolResult = 
        withContext(Dispatchers.IO) {
            try {
                val request = mapOf(
                    "jsonrpc" to "2.0",
                    "id" to UUID.randomUUID().toString(),
                    "method" to "tools/call",
                    "params" to mapOf("name" to name, "arguments" to arguments)
                )
                
                webSocket?.send(json.encodeToString(JsonObject.serializer(), JsonObject(request)))
                McpToolResult.Success(listOf(McpContent.Text("Tool call sent via WebSocket")))
            } catch (e: Exception) {
                McpToolResult.Error(e.message ?: "Tool call failed")
            }
        }
    
    override fun close() {
        webSocket?.close(1000, "Client closing")
        _status.value = McpServerStatus.Disconnected
    }
}
