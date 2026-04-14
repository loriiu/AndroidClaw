package ai.androidclaw.domain.repository

import ai.androidclaw.domain.model.mcp.*
import kotlinx.coroutines.flow.Flow

/**
 * MCP 仓储接口
 */
interface McpRepository {
    
    /**
     * 获取所有已配置的 MCP 服务器
     */
    fun getAllServers(): Flow<List<McpServerConfig>>
    
    /**
     * 添加 MCP 服务器
     */
    suspend fun addServer(config: McpServerConfig)
    
    /**
     * 更新 MCP 服务器
     */
    suspend fun updateServer(config: McpServerConfig)
    
    /**
     * 删除 MCP 服务器
     */
    suspend fun deleteServer(serverId: String)
    
    /**
     * 连接到 MCP 服务器
     */
    suspend fun connect(serverId: String): Flow<McpServerStatus>
    
    /**
     * 断开 MCP 服务器连接
     */
    fun disconnect(serverId: String)
    
    /**
     * 获取服务器连接状态
     */
    fun getServerStatus(serverId: String): Flow<McpServerStatus>
    
    /**
     * 调用 MCP 工具
     */
    suspend fun callTool(serverId: String, toolName: String, arguments: Map<String, Any>): McpToolResult
    
    /**
     * 获取服务器可用工具
     */
    fun getServerTools(serverId: String): List<McpTool>
    
    /**
     * 获取所有服务器的工具
     */
    fun getAllTools(): Map<String, List<McpTool>>
}
