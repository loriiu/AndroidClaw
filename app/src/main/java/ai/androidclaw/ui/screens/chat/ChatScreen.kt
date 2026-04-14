package ai.androidclaw.ui.screens.chat

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import ai.androidclaw.domain.model.ChatMessage
import ai.androidclaw.domain.model.MessageRole
import ai.androidclaw.ui.theme.AssistantBubbleColor
import ai.androidclaw.ui.theme.OnAssistantBubbleColor
import ai.androidclaw.ui.theme.OnUserBubbleColor
import ai.androidclaw.ui.theme.UserBubbleColor
import kotlinx.coroutines.launch

/**
 * 聊天界面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: ChatViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    var inputText by remember { mutableStateOf("") }
    
    // 自动滚动到底部（消息变化时）
    LaunchedEffect(uiState.messages.size, uiState.isStreaming) {
        val targetItem = if (uiState.isStreaming) {
            uiState.messages.size // 显示在最后一条消息之后
        } else {
            uiState.messages.size - 1
        }
        if (targetItem >= 0) {
            listState.animateScrollToItem(targetItem)
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AndroidClaw") },
                actions = {
                    // 流式输出时显示取消按钮
                    if (uiState.isStreaming) {
                        IconButton(onClick = { viewModel.cancelStreaming() }) {
                            Icon(Icons.Default.Close, contentDescription = "Cancel")
                        }
                    }
                    IconButton(onClick = { viewModel.createNewConversation() }) {
                        Icon(Icons.Default.Add, contentDescription = "New Chat")
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
            // 消息列表
            if (uiState.messages.isEmpty() && !uiState.isStreaming) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No messages yet. Start a conversation!",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    state = listState,
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(uiState.messages) { message ->
                        ChatBubble(message = message)
                    }
                    
                    // 流式输出中的消息气泡
                    if (uiState.isStreaming && uiState.streamingContent.isNotEmpty()) {
                        item {
                            StreamingMessageBubble(
                                content = uiState.streamingContent,
                                onCancel = { viewModel.cancelStreaming() }
                            )
                        }
                    }
                }
            }
            
            // 输入区域
            Surface(
                modifier = Modifier.fillMaxWidth(),
                tonalElevation = 3.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Type a message...") },
                        maxLines = 4,
                        shape = RoundedCornerShape(24.dp),
                        enabled = !uiState.isStreaming
                    )
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    FilledIconButton(
                        onClick = {
                            if (inputText.isNotBlank()) {
                                viewModel.sendMessage(inputText)
                                inputText = ""
                            }
                        },
                        enabled = inputText.isNotBlank() && !uiState.isLoading && !uiState.isStreaming
                    ) {
                        if (uiState.isLoading || uiState.isStreaming) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Default.Send, contentDescription = "Send")
                        }
                    }
                }
            }
        }
    }
}

/**
 * 聊天气泡
 */
@Composable
fun ChatBubble(message: ChatMessage) {
    val isUser = message.role == MessageRole.USER
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            modifier = Modifier.widthIn(max = 280.dp),
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isUser) 16.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 16.dp
            ),
            color = if (isUser) UserBubbleColor else AssistantBubbleColor
        ) {
            Text(
                text = message.content,
                modifier = Modifier.padding(12.dp),
                color = if (isUser) OnUserBubbleColor else OnAssistantBubbleColor,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

/**
 * 流式消息气泡 - 带打字机效果和闪烁光标
 */
@Composable
fun StreamingMessageBubble(
    content: String,
    onCancel: () -> Unit
) {
    // 闪烁光标动画
    val infiniteTransition = rememberInfiniteTransition(label = "cursor")
    val cursorAlpha by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "cursorAlpha"
    )
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start
    ) {
        Surface(
            modifier = Modifier.widthIn(max = 280.dp),
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = 4.dp,
                bottomEnd = 16.dp
            ),
            color = AssistantBubbleColor
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = content,
                    modifier = Modifier.weight(1f, fill = false),
                    color = OnAssistantBubbleColor,
                    style = MaterialTheme.typography.bodyMedium
                )
                
                // 闪烁光标
                Text(
                    text = "▋",
                    modifier = Modifier.alpha(cursorAlpha),
                    color = OnAssistantBubbleColor,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}
