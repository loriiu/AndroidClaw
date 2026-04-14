package ai.androidclaw.domain.model.mcp

import java.time.Instant
import java.util.UUID

/**
 * MCP 服务器配置
 */
data class McpServerConfig(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val url: String,
    val type: McpServerType = McpServerType.SSE,
    val authToken: String? = null,
    val isEnabled: Boolean = true,
    val createdAt: Instant = Instant.now()
)

/**
 * MCP 服务器连接类型
 */
enum class McpServerType {
    SSE,
    WEBSOCKET
}

/**
 * MCP 服务器连接状态
 */
sealed class McpServerStatus {
    data object Disconnected : McpServerStatus()
    data object Connecting : McpServerStatus()
    data class Connected(val tools: List<McpTool>) : McpServerStatus()
    data class Error(val message: String) : McpServerStatus()
}

/**
 * MCP 工具定义
 */
data class McpTool(
    val name: String,
    val description: String,
    val inputSchema: McpInputSchema
)

/**
 * MCP 工具输入参数模式
 */
data class McpInputSchema(
    val type: String = "object",
    val properties: Map<String, McpProperty> = emptyMap(),
    val required: List<String> = emptyList()
)

/**
 * MCP 工具属性
 */
data class McpProperty(
    val type: String,
    val description: String? = null,
    val enum: List<String>? = null,
    val default: Any? = null
)

/**
 * MCP 工具调用请求
 */
data class McpToolCall(
    val serverId: String,
    val toolName: String,
    val arguments: Map<String, Any>
)

/**
 * MCP 工具调用结果
 */
sealed class McpToolResult {
    data class Success(
        val content: List<McpContent>,
        val isError: Boolean = false
    ) : McpToolResult()
    
    data class Error(val message: String) : McpToolResult()
}

/**
 * MCP 内容块
 */
sealed class McpContent {
    data class Text(val text: String) : McpContent()
    data class Image(val data: String, val mimeType: String) : McpContent()
    data class Resource(val uri: String, val mimeType: String?) : McpContent()
}

/**
 * MCP 协议相关类型
 */
object McpProtocol {
    const val VERSION = "2024-11-05"
    
    data class InitializeParams(
        val protocolVersion: String = VERSION,
        val capabilities: ClientCapabilities
    )
    
    data class ClientCapabilities(
        val tools: ToolsCapability? = null,
        val resources: ResourcesCapability? = null
    )
    
    data class ToolsCapability(val listChanged: Boolean? = null)
    data class ResourcesCapability(val subscribe: Boolean? = null)
    
    data class InitializeResult(
        val protocolVersion: String,
        val capabilities: ServerCapabilities,
        val serverInfo: ServerInfo
    )
    
    data class ServerCapabilities(
        val tools: ServerToolsCapability? = null,
        val resources: ServerResourcesCapability? = null
    )
    
    data class ServerToolsCapability(val listChanged: Boolean? = null)
    data class ServerResourcesCapability(val subscribe: Boolean? = null, val listChanged: Boolean? = null)
    
    data class ServerInfo(val name: String, val version: String)
    
    data class ToolListResult(val tools: List<ToolDefinition>)
    
    data class ToolDefinition(
        val name: String,
        val description: String?,
        val inputSchema: Map<String, Any?>
    )
    
    data class ToolCallParams(
        val name: String,
        val arguments: Map<String, Any?>
    )
    
    data class ToolCallResult(
        val content: List<ToolContent>,
        val isError: Boolean = false
    )
    
    data class ToolContent(
        val type: String,
        val text: String? = null,
        val data: String? = null,
        val mimeType: String? = null
    )
}
