package ai.androidclaw.ui.screens.mcp

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import ai.androidclaw.domain.model.mcp.*

/**
 * MCP 设置屏幕
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun McpScreen(
    viewModel: McpViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var expandedServerId by remember { mutableStateOf<String?>(null) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("MCP Servers") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Add Server")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (uiState.servers.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Cloud,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "No MCP Servers",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Add an MCP server to extend AI capabilities",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(onClick = { showAddDialog = true }) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Add Server")
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(uiState.servers) { server ->
                        McpServerCard(
                            server = server,
                            status = uiState.serverStatuses[server.id] ?: McpServerStatus.Disconnected,
                            isExpanded = expandedServerId == server.id,
                            onExpand = { 
                                expandedServerId = if (expandedServerId == server.id) null else server.id
                                viewModel.selectServer(server.id)
                            },
                            onConnect = { viewModel.connectServer(server.id) },
                            onDisconnect = { viewModel.disconnectServer(server.id) },
                            onDelete = { viewModel.deleteServer(server.id) },
                            onCallTool = { name, args -> viewModel.callTool(name, args) },
                            availableTools = if (expandedServerId == server.id) uiState.availableTools else emptyList(),
                            toolCallResult = if (expandedServerId == server.id) uiState.toolCallResult else null,
                            onClearResult = { viewModel.clearToolResult() }
                        )
                    }
                }
            }
        }
    }
    
    if (showAddDialog) {
        AddMcpServerDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { name, url, type, token ->
                viewModel.addServer(name, url, type, token)
                showAddDialog = false
            }
        )
    }
}

@Composable
fun McpServerCard(
    server: McpServerConfig,
    status: McpServerStatus,
    isExpanded: Boolean,
    onExpand: () -> Unit,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
    onDelete: () -> Unit,
    onCallTool: (String, Map<String, Any>) -> Unit,
    availableTools: List<McpTool>,
    toolCallResult: ToolCallUiState?,
    onClearResult: () -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var selectedTool by remember { mutableStateOf<McpTool?>(null) }
    var toolArgs by remember { mutableStateOf("{}") }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onExpand
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        server.name,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        server.url,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                McpStatusChip(status)
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                when (status) {
                    is McpServerStatus.Connected -> {
                        FilledTonalButton(onClick = onDisconnect) {
                            Icon(Icons.Default.LinkOff, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Disconnect")
                        }
                    }
                    is McpServerStatus.Connecting -> {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Connecting...")
                    }
                    else -> {
                        Button(onClick = onConnect) {
                            Icon(Icons.Default.Link, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Connect")
                        }
                    }
                }
                
                Spacer(modifier = Modifier.width(8.dp))
                
                IconButton(onClick = { showDeleteConfirm = true }) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete")
                }
            }
            
            // 展开的工具列表
            if (isExpanded && status is McpServerStatus.Connected) {
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    "Available Tools (${availableTools.size})",
                    style = MaterialTheme.typography.titleSmall
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                if (availableTools.isEmpty()) {
                    Text(
                        "No tools available",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    availableTools.forEach { tool ->
                        ToolItem(
                            tool = tool,
                            isSelected = selectedTool == tool,
                            onSelect = { selectedTool = if (selectedTool == tool) null else tool }
                        )
                    }
                    
                    // 工具调用区域
                    if (selectedTool != null) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "Call: ${selectedTool!!.name}",
                            style = MaterialTheme.typography.titleSmall
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        OutlinedTextField(
                            value = toolArgs,
                            onValueChange = { toolArgs = it },
                            label = { Text("Arguments (JSON)") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 2
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Button(
                            onClick = {
                                val args = try {
                                    kotlinx.serialization.json.Json.decodeFromString<Map<String, Any>>(toolArgs)
                                } catch (e: Exception) {
                                    emptyMap()
                                }
                                onCallTool(selectedTool!!.name, args)
                            }
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Execute")
                        }
                    }
                    
                    // 工具调用结果
                    toolCallResult?.let { result ->
                        Spacer(modifier = Modifier.height(16.dp))
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (result.isError) 
                                    MaterialTheme.colorScheme.errorContainer 
                                else MaterialTheme.colorScheme.secondaryContainer
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        if (result.isError) "Error" else "Result",
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    if (result.isLoading) {
                                        CircularProgressIndicator(modifier = Modifier.size(16.dp))
                                    } else {
                                        Text(
                                            result.result ?: "",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                }
                                IconButton(onClick = onClearResult) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Server") },
            text = { Text("Are you sure you want to delete '${server.name}'?") },
            confirmButton = {
                TextButton(onClick = {
                    onDelete()
                    showDeleteConfirm = false
                }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun McpStatusChip(status: McpServerStatus) {
    val (color, text, icon) = when (status) {
        is McpServerStatus.Connected -> Triple(
            MaterialTheme.colorScheme.primary,
            "Connected (${status.tools.size} tools)",
            Icons.Default.CheckCircle
        )
        is McpServerStatus.Connecting -> Triple(
            MaterialTheme.colorScheme.tertiary,
            "Connecting",
            Icons.Default.Sync
        )
        is McpServerStatus.Error -> Triple(
            MaterialTheme.colorScheme.error,
            "Error",
            Icons.Default.Error
        )
        McpServerStatus.Disconnected -> Triple(
            MaterialTheme.colorScheme.outline,
            "Disconnected",
            Icons.Default.LinkOff
        )
    }
    
    AssistChip(
        onClick = { },
        label = { Text(text, style = MaterialTheme.typography.labelSmall) },
        leadingIcon = {
            Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
        },
        colors = AssistChipDefaults.assistChipColors(
            containerColor = color.copy(alpha = 0.1f),
            labelColor = color,
            leadingIconContentColor = color
        )
    )
}

@Composable
fun ToolItem(
    tool: McpTool,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) 
                MaterialTheme.colorScheme.primaryContainer 
            else MaterialTheme.colorScheme.surfaceVariant
        ),
        onClick = onSelect
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Build,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    tool.name,
                    style = MaterialTheme.typography.titleSmall
                )
            }
            if (tool.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    tool.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMcpServerDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, url: String, type: McpServerType, authToken: String?) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(McpServerType.SSE) }
    var authToken by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add MCP Server") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("Server URL") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri)
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedType == McpServerType.SSE,
                        onClick = { selectedType = McpServerType.SSE },
                        label = { Text("SSE") }
                    )
                    FilterChip(
                        selected = selectedType == McpServerType.WEBSOCKET,
                        onClick = { selectedType = McpServerType.WEBSOCKET },
                        label = { Text("WebSocket") }
                    )
                }
                
                OutlinedTextField(
                    value = authToken,
                    onValueChange = { authToken = it },
                    label = { Text("Auth Token (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { 
                    onConfirm(name, url, selectedType, authToken.ifBlank { null })
                },
                enabled = name.isNotBlank() && url.isNotBlank()
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
