package ai.androidclaw.ui.screens.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import ai.androidclaw.domain.model.ChatMessage
import ai.androidclaw.domain.model.Conversation
import ai.androidclaw.domain.model.LlmProviderType
import ai.androidclaw.domain.model.MessageRole
import ai.androidclaw.domain.repository.ChatRepository
import ai.androidclaw.domain.repository.ConfigRepository
import ai.androidclaw.infrastructure.llm.AnthropicProvider
import ai.androidclaw.infrastructure.llm.LlmProvider
import ai.androidclaw.infrastructure.llm.OllamaProvider
import ai.androidclaw.infrastructure.llm.OpenAiProvider
import javax.inject.Inject

/**
 * 聊天界面 ViewModel
 */
@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val configRepository: ConfigRepository,
    private val openAiProvider: OpenAiProvider,
    private val ollamaProvider: OllamaProvider,
    private val anthropicProvider: AnthropicProvider
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()
    
    private var currentConversationId: String? = null
    private var streamingJob: Job? = null
    
    init {
        loadConversations()
        initializeLlmProvider()
    }
    
    private fun initializeLlmProvider() {
        viewModelScope.launch {
            configRepository.getLlmConfig().collect { config ->
                config?.let {
                    when (it.provider) {
                        LlmProviderType.OPENAI -> {
                            val apiKey = configRepository.getApiKey() ?: ""
                            openAiProvider.initialize(apiKey, it.model, it.baseUrl)
                        }
                        LlmProviderType.OLLAMA -> {
                            ollamaProvider.initialize(it.baseUrl ?: "http://localhost:11434", it.model)
                        }
                        LlmProviderType.ANTHROPIC -> {
                            val apiKey = configRepository.getApiKey() ?: ""
                            anthropicProvider.initialize(apiKey, it.model, it.baseUrl)
                        }
                        else -> {}
                    }
                }
            }
        }
    }
    
    private fun loadConversations() {
        viewModelScope.launch {
            chatRepository.getAllConversations().collect { conversations ->
                _uiState.value = _uiState.value.copy(conversations = conversations)
                if (currentConversationId == null && conversations.isNotEmpty()) {
                    selectConversation(conversations.first().id)
                }
            }
        }
    }
    
    fun selectConversation(conversationId: String) {
        currentConversationId = conversationId
        viewModelScope.launch {
            chatRepository.getMessages(conversationId).collect { messages ->
                _uiState.value = _uiState.value.copy(messages = messages)
            }
        }
    }
    
    fun createNewConversation() {
        viewModelScope.launch {
            val conversation = Conversation.newConversation()
            chatRepository.createConversation(conversation)
            selectConversation(conversation.id)
        }
    }
    
    /**
     * 发送消息 - 使用流式输出
     */
    fun sendMessage(content: String) {
        if (content.isBlank()) return
        
        val conversationId = currentConversationId ?: return
        
        // 取消之前的流式任务
        streamingJob?.cancel()
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            // 保存用户消息
            val userMessage = ChatMessage.userMessage(conversationId, content)
            chatRepository.saveMessage(userMessage)
            
            // 获取配置
            val config = configRepository.getLlmConfig().first()
            val userConfig = configRepository.getUserConfig().first()
            val systemPrompt = buildSystemPrompt(userConfig)
            
            // 获取历史消息（用于上下文）
            val recentMessages = chatRepository.getRecentMessages(conversationId, 20).first()
            
            // 初始化流式输出状态
            val streamingMessageId = java.util.UUID.randomUUID().toString()
            _uiState.value = _uiState.value.copy(
                isStreaming = true,
                streamingContent = "",
                streamingMessageId = streamingMessageId
            )
            
            // 选择对应的 Provider
            val provider: LlmProvider = when (config?.provider) {
                LlmProviderType.OPENAI -> openAiProvider
                LlmProviderType.OLLAMA -> ollamaProvider
                LlmProviderType.ANTHROPIC -> anthropicProvider
                else -> {
                    // 没有配置时发送错误消息
                    val errorMsg = ChatMessage.assistantMessage(
                        conversationId,
                        "Please configure an LLM provider in Settings."
                    )
                    chatRepository.saveMessage(errorMsg)
                    _uiState.value = _uiState.value.copy(isLoading = false, isStreaming = false)
                    return@launch
                }
            }
            
            // 收集流式输出
            streamingJob = launch {
                val fullContent = StringBuilder()
                
                try {
                    provider.chatStream(recentMessages, systemPrompt).collect { chunk ->
                        fullContent.append(chunk)
                        // 实时更新 UI
                        _uiState.value = _uiState.value.copy(
                            streamingContent = fullContent.toString()
                        )
                    }
                    
                    // 流结束，保存完整消息
                    val assistantMessage = ChatMessage.assistantMessage(
                        conversationId,
                        fullContent.toString()
                    )
                    chatRepository.saveMessage(assistantMessage)
                    
                } catch (e: Exception) {
                    // 流中断时，保存已获取的内容或保存错误消息
                    val finalContent = fullContent.toString()
                    if (finalContent.isNotEmpty()) {
                        val partialMessage = ChatMessage.assistantMessage(
                            conversationId,
                            "$finalContent\n\n[Stream interrupted: ${e.message}]"
                        )
                        chatRepository.saveMessage(partialMessage)
                    } else {
                        val errorMessage = ChatMessage.assistantMessage(
                            conversationId,
                            "Sorry, I encountered an error: ${e.message}"
                        )
                        chatRepository.saveMessage(errorMessage)
                    }
                } finally {
                    // 清空流式状态
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isStreaming = false,
                        streamingContent = "",
                        streamingMessageId = null
                    )
                }
            }
        }
    }
    
    /**
     * 取消流式输出
     */
    fun cancelStreaming() {
        streamingJob?.cancel()
        _uiState.value = _uiState.value.copy(
            isLoading = false,
            isStreaming = false,
            streamingContent = "",
            streamingMessageId = null
        )
    }
    
    private fun buildSystemPrompt(userConfig: ai.androidclaw.domain.model.UserConfig?): String? {
        if (userConfig == null) return null
        
        val builder = StringBuilder()
        builder.append("You are AndroidClaw, a helpful AI assistant running on Android.")
        
        if (userConfig.name.isNotBlank()) {
            builder.append(" The user's name is ${userConfig.name}.")
        }
        if (userConfig.role.isNotBlank()) {
            builder.append(" Their role is: ${userConfig.role}.")
        }
        if (userConfig.systemPrompt.isNotBlank()) {
            builder.append("\n\nAdditional instructions: ${userConfig.systemPrompt}")
        }
        
        return builder.toString()
    }
    
    fun clearConversation() {
        currentConversationId?.let { conversationId ->
            viewModelScope.launch {
                chatRepository.clearMessages(conversationId)
            }
        }
    }
    
    fun deleteConversation(conversationId: String) {
        viewModelScope.launch {
            chatRepository.deleteConversation(conversationId)
            if (currentConversationId == conversationId) {
                currentConversationId = null
            }
        }
    }
}

/**
 * 聊天界面 UI 状态
 */
data class ChatUiState(
    val conversations: List<Conversation> = emptyList(),
    val messages: List<ChatMessage> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    // 流式输出相关状态
    val isStreaming: Boolean = false,
    val streamingContent: String = "",
    val streamingMessageId: String? = null
)
