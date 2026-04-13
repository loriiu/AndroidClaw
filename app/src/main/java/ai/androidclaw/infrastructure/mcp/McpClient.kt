package ai.androidclaw.infrastructure.mcp

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * MCP (Model Context Protocol) 客户端接口
 * 
 * 用于连接外部 MCP 服务器以获取额外的工具能力
 */
interface McpClient {
    
    /**
     * 连接状态
     */
    val isConnected: Boolean
    
    /**
     * 连接到 MCP 服务器
     */
    suspend fun connect(serverUrl: String, authToken: String? = null): Boolean
    
    /**
     * 断开连接
     */
    suspend fun disconnect()
    
    /**
     * 获取可用的工具列表
     */
    suspend fun getAvailableTools(): List<McpTool>
    
    /**
     * 调用工具
     */
    fun callTool(toolName: String, arguments: Map<String, Any>): Flow<ToolCallResult>
}

/**
 * MCP 工具定义
 */
data class McpTool(
    val name: String,
    val description: String,
    val inputSchema: Map<String, Any>
)

/**
 * 工具调用结果
 */
sealed class ToolCallResult {
    data class Progress(val message: String) : ToolCallResult()
    data class Success(val result: Any) : ToolCallResult()
    data class Error(val message: String) : ToolCallResult()
}
