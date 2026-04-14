package ai.androidclaw.data.repository

import ai.androidclaw.data.local.preferences.ConfigDataStore
import ai.androidclaw.domain.model.mcp.*
import ai.androidclaw.domain.repository.McpRepository
import ai.androidclaw.infrastructure.mcp.McpClient
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * MCP 仓储实现
 */
@Singleton
class McpRepositoryImpl @Inject constructor(
    private val mcpClient: McpClient,
    private val configDataStore: ConfigDataStore
) : McpRepository {
    
    private val _servers = MutableStateFlow<List<McpServerConfig>>(emptyList())
    private val _serverStatuses = MutableStateFlow<Map<String, McpServerStatus>>(emptyMap())
    
    init {
        // 监听配置变化
        viewModelScope.launch {
            configDataStore.mcpServers.collect { serversJson ->
                _servers.value = serversJson.mapNotNull { parseServerConfig(it) }
            }
        }
    }
    
    private val viewModelScope = kotlinx.coroutines.CoroutineScope(
        kotlinx.coroutines.Dispatchers.IO + kotlinx.coroutines.SupervisorJob()
    )
    
    override fun getAllServers(): Flow<List<McpServerConfig>> = _servers.asStateFlow()
    
    override suspend fun addServer(config: McpServerConfig) {
        val current = _servers.value.toMutableList()
        current.add(config)
        _servers.value = current
        saveServers(current)
    }
    
    override suspend fun updateServer(config: McpServerConfig) {
        val current = _servers.value.toMutableList()
        val index = current.indexOfFirst { it.id == config.id }
        if (index >= 0) {
            current[index] = config
            _servers.value = current
            saveServers(current)
        }
    }
    
    override suspend fun deleteServer(serverId: String) {
        // 先断开连接
        disconnect(serverId)
        
        val current = _servers.value.toMutableList()
        current.removeAll { it.id == serverId }
        _servers.value = current
        saveServers(current)
    }
    
    override suspend fun connect(serverId: String): Flow<McpServerStatus> = flow {
        val config = _servers.value.find { it.id == serverId } 
            ?: throw IllegalArgumentException("Server not found: $serverId")
        
        _serverStatuses.value = _serverStatuses.value + (serverId to McpServerStatus.Connecting)
        
        mcpClient.connect(config).collect { status ->
            _serverStatuses.value = _serverStatuses.value + (serverId to status)
            emit(status)
        }
    }
    
    override fun disconnect(serverId: String) {
        mcpClient.disconnect(serverId)
        _serverStatuses.value = _serverStatuses.value + (serverId to McpServerStatus.Disconnected)
    }
    
    override fun getServerStatus(serverId: String): Flow<McpServerStatus> = 
        _serverStatuses.map { statuses ->
            statuses[serverId] ?: McpServerStatus.Disconnected
        }
    
    override suspend fun callTool(serverId: String, toolName: String, arguments: Map<String, Any>): McpToolResult {
        return mcpClient.callTool(serverId, toolName, arguments)
    }
    
    override fun getServerTools(serverId: String): List<McpTool> {
        return mcpClient.getTools(serverId)
    }
    
    override fun getAllTools(): Map<String, List<McpTool>> {
        return mcpClient.getAllTools()
    }
    
    private suspend fun saveServers(servers: List<McpServerConfig>) {
        // 保存到 DataStore
        // 注意：这里需要扩展 ConfigDataStore 来支持 MCP 服务器配置
    }
    
    private fun parseServerConfig(json: String): McpServerConfig? {
        return try {
            kotlinx.serialization.json.Json.decodeFromString<McpServerConfig>(json)
        } catch (e: Exception) {
            null
        }
    }
}
