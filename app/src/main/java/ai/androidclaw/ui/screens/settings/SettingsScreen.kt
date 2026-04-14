package ai.androidclaw.ui.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import ai.androidclaw.domain.model.LlmProviderType
import ai.androidclaw.domain.model.ThemeMode

/**
 * 设置界面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
    onNavigateToMcp: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    var showLlmDialog by remember { mutableStateOf(false) }
    var showUserDialog by remember { mutableStateOf(false) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // LLM 配置
            item {
                SettingsSection(title = "LLM Configuration") {
                    SettingsItem(
                        icon = Icons.Default.Layers,
                        title = "Provider",
                        subtitle = uiState.llmConfig?.provider?.name ?: "Not configured",
                        onClick = { showLlmDialog = true }
                    )
                    
                    uiState.llmConfig?.let { config ->
                        SettingsItem(
                            icon = Icons.Default.Model,
                            title = "Model",
                            subtitle = config.model,
                            onClick = { showLlmDialog = true }
                        )
                    }
                }
            }
            
            // MCP 配置
            item {
                SettingsSection(title = "Extensions") {
                    SettingsItem(
                        icon = Icons.Default.Extension,
                        title = "MCP Servers",
                        subtitle = "Connect external tools and data sources",
                        onClick = onNavigateToMcp
                    )
                }
            }
            
            // 用户信息
            item {
                SettingsSection(title = "User Information") {
                    SettingsItem(
                        icon = Icons.Default.Person,
                        title = "Name",
                        subtitle = uiState.userConfig.name.ifBlank { "Not set" },
                        onClick = { showUserDialog = true }
                    )
                    
                    SettingsItem(
                        icon = Icons.Default.Work,
                        title = "Role",
                        subtitle = uiState.userConfig.role.ifBlank { "Not set" },
                        onClick = { showUserDialog = true }
                    )
                    
                    SettingsItem(
                        icon = Icons.Default.TextFields,
                        title = "Custom Prompt",
                        subtitle = if (uiState.userConfig.systemPrompt.isBlank()) "Default" else "Custom",
                        onClick = { showUserDialog = true }
                    )
                }
            }
            
            // 外观
            item {
                SettingsSection(title = "Appearance") {
                    var expanded by remember { mutableStateOf(false) }
                    
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = it }
                    ) {
                        ListItem(
                            headlineContent = { Text("Theme") },
                            supportingContent = { Text(uiState.userConfig.themeMode.name) },
                            leadingContent = { Icon(Icons.Default.Palette, contentDescription = null) },
                            modifier = Modifier
                                .menuAnchor()
                                .clickable { expanded = true }
                        )
                        
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            ThemeMode.entries.forEach { mode ->
                                DropdownMenuItem(
                                    text = { Text(mode.name) },
                                    onClick = {
                                        viewModel.updateThemeMode(mode)
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
            
            // 关于
            item {
                SettingsSection(title = "About") {
                    SettingsItem(
                        icon = Icons.Default.Info,
                        title = "Version",
                        subtitle = "0.1.0",
                        onClick = { }
                    )
                }
            }
        }
    }
    
    // LLM 配置对话框
    if (showLlmDialog) {
        LlmConfigDialog(
            currentConfig = uiState.llmConfig,
            onDismiss = { showLlmDialog = false },
            onSave = { provider, apiKey, model, baseUrl ->
                viewModel.saveLlmConfig(provider, apiKey, model, baseUrl)
                showLlmDialog = false
            }
        )
    }
    
    // 用户信息对话框
    if (showUserDialog) {
        UserConfigDialog(
            currentConfig = uiState.userConfig,
            onDismiss = { showUserDialog = false },
            onSave = { config ->
                viewModel.saveUserConfig(config)
                showUserDialog = false
            }
        )
    }
}

/**
 * 设置分组
 */
@Composable
fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        content()
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
    }
}

/**
 * 设置项
 */
@Composable
fun SettingsItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(subtitle) },
        leadingContent = { Icon(icon, contentDescription = null) },
        trailingContent = {
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        modifier = Modifier.clickable(onClick = onClick)
    )
}

/**
 * LLM 配置对话框
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LlmConfigDialog(
    currentConfig: ai.androidclaw.domain.model.LlmConfig?,
    onDismiss: () -> Unit,
    onSave: (LlmProviderType, String, String, String?) -> Unit
) {
    var selectedProvider by remember { mutableStateOf(currentConfig?.provider ?: LlmProviderType.OPENAI) }
    var apiKey by remember { mutableStateOf(currentConfig?.apiKey ?: "") }
    var model by remember { mutableStateOf(currentConfig?.model ?: "gpt-4o-mini") }
    var baseUrl by remember { mutableStateOf(currentConfig?.baseUrl ?: "") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("LLM Configuration") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 提供商选择
                Text("Provider", style = MaterialTheme.typography.labelLarge)
                Column {
                    LlmProviderType.entries.forEach { provider ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { selectedProvider = provider }
                        ) {
                            RadioButton(
                                selected = selectedProvider == provider,
                                onClick = { selectedProvider = provider }
                            )
                            Text(provider.name)
                        }
                    }
                }
                
                // API Key
                if (selectedProvider != LlmProviderType.OLLAMA) {
                    OutlinedTextField(
                        value = apiKey,
                        onValueChange = { apiKey = it },
                        label = { Text("API Key") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                
                // 模型
                OutlinedTextField(
                    value = model,
                    onValueChange = { model = it },
                    label = { Text("Model") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                // Base URL (Ollama 或自定义)
                OutlinedTextField(
                    value = baseUrl,
                    onValueChange = { baseUrl = it },
                    label = { Text("Base URL (optional)") },
                    placeholder = { Text("http://localhost:11434") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(selectedProvider, apiKey, model, baseUrl.ifBlank { null }) }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

/**
 * 用户配置对话框
 */
@Composable
fun UserConfigDialog(
    currentConfig: ai.androidclaw.domain.model.UserConfig,
    onDismiss: () -> Unit,
    onSave: (ai.androidclaw.domain.model.UserConfig) -> Unit
) {
    var name by remember { mutableStateOf(currentConfig.name) }
    var role by remember { mutableStateOf(currentConfig.role) }
    var systemPrompt by remember { mutableStateOf(currentConfig.systemPrompt) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("User Information") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                OutlinedTextField(
                    value = role,
                    onValueChange = { role = it },
                    label = { Text("Role") },
                    placeholder = { Text("e.g., Software Developer") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                OutlinedTextField(
                    value = systemPrompt,
                    onValueChange = { systemPrompt = it },
                    label = { Text("Custom System Prompt") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSave(currentConfig.copy(
                    name = name,
                    role = role,
                    systemPrompt = systemPrompt
                ))
            }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
