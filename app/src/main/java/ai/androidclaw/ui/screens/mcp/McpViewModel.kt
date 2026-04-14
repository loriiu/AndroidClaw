package ai.androidclaw.ui.screens.mcp

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import ai.androidclaw.domain.model.mcp.*
import ai.androidclaw.domain.repository.McpRepository
import javax.inject.Inject

/**
 * MCP 界面 ViewModel
 */
@HiltViewModel
class McpViewModel @Inject constructor(
    private val mcpRepository: McpRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(McpUiState())
    val uiState: StateFlow<McpUiState> = _uiState.asStateFlow()
    
    init {
        loadServers()
        observeStatuses()
    }
    
    private fun loadServers() {
        viewModelScope.launch {
            mcpRepository.getAllServers().collect { servers ->
                _uiState.value = _uiState.value.copy(servers = servers)
            }
        }
    }
    
    private fun observeStatuses() {
        viewModelScope.launch {
            _uiState.value.servers.forEach { server ->
                launch {
                    mcpRepository.getServerStatus(server.id).collect { status ->
                        val currentStatuses = _uiState.value.serverStatuses.toMutableMap()
                        currentStatuses[server.id] = status
                        _uiState.value = _uiState.value.copy(serverStatuses = currentStatuses)
                    }
                }
            }
        }
    }
    
    fun addServer(name: String, url: String, type: McpServerType, authToken: String?) {
        viewModelScope.launch {
            val config = McpServerConfig(
                name = name,
                url = url,
                type = type,
                authToken = authToken
            )
            mcpRepository.addServer(config)
        }
    }
    
    fun updateServer(server: McpServerConfig) {
        viewModelScope.launch {
            mcpRepository.updateServer(server)
        }
    }
    
    fun deleteServer(serverId: String) {
        viewModelScope.launch {
            mcpRepository.deleteServer(serverId)
        }
    }
    
    fun connectServer(serverId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                serverStatuses = _uiState.value.serverStatuses + (serverId to McpServerStatus.Connecting)
            )
            mcpRepository.connect(serverId).collect { status ->
                _uiState.value = _uiState.value.copy(
                    serverStatuses = _uiState.value.serverStatuses + (serverId to status)
                )
            }
        }
    }
    
    fun disconnectServer(serverId: String) {
        mcpRepository.disconnect(serverId)
    }
    
    fun selectServer(serverId: String?) {
        val tools = serverId?.let { mcpRepository.getServerTools(it) } ?: emptyList()
        _uiState.value = _uiState.value.copy(
            selectedServerId = serverId,
            availableTools = tools
        )
    }
    
    fun callTool(toolName: String, arguments: Map<String, Any>) {
        val serverId = _uiState.value.selectedServerId ?: return
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                toolCallResult = ToolCallUiState(isLoading = true)
            )
            
            val result = mcpRepository.callTool(serverId, toolName, arguments)
            
            _uiState.value = _uiState.value.copy(
                toolCallResult = when (result) {
                    is McpToolResult.Success -> {
                        val content = result.content.joinToString("\n") { 
                            when (it) {
                                is McpContent.Text -> it.text
                                is McpContent.Image -> "[Image: ${it.mimeType}]"
                                is McpContent.Resource -> "[Resource: ${it.uri}]"
                            }
                        }
                        ToolCallUiState(result = content, isError = result.isError)
                    }
                    is McpToolResult.Error -> {
                        ToolCallUiState(result = result.message, isError = true)
                    }
                }
            )
        }
    }
    
    fun clearToolResult() {
        _uiState.value = _uiState.value.copy(toolCallResult = null)
    }
    
    fun getAllTools(): Map<String, List<McpTool>> {
        return mcpRepository.getAllTools()
    }
}

/**
 * MCP UI 状态
 */
data class McpUiState(
    val servers: List<McpServerConfig> = emptyList(),
    val serverStatuses: Map<String, McpServerStatus> = emptyMap(),
    val selectedServerId: String? = null,
    val availableTools: List<McpTool> = emptyList(),
    val toolCallResult: ToolCallUiState? = null,
    val error: String? = null
)

/**
 * 工具调用 UI 状态
 */
data class ToolCallUiState(
    val result: String? = null,
    val isLoading: Boolean = false,
    val isError: Boolean = false
)
