package ai.androidclaw.ui.screens.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import ai.androidclaw.domain.model.ChatMessage
import ai.androidclaw.domain.model.Conversation
import ai.androidclaw.domain.model.MessageRole
import ai.androidclaw.domain.repository.ChatRepository
import ai.androidclaw.domain.repository.ConfigRepository
import ai.androidclaw.infrastructure.llm.AnthropicProvider
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
    
    init {
        loadConversations()
        initializeLlmProvider()
    }
    
    private fun initializeLlmProvider() {
        viewModelScope.launch {
            configRepository.getLlmConfig().collect { config ->
                config?.let {
                    when (it.provider) {
                        ai.androidclaw.domain.model.LlmProviderType.OPENAI -> {
                            val apiKey = configRepository.getApiKey() ?: ""
                            openAiProvider.initialize(apiKey, it.model, it.baseUrl)
                        }
                        ai.androidclaw.domain.model.LlmProviderType.OLLAMA -> {
                            ollamaProvider.initialize(it.baseUrl ?: "http://localhost:11434", it.model)
                        }
                        ai.androidclaw.domain.model.LlmProviderType.ANTHROPIC -> {
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
    
    fun sendMessage(content: String) {
        if (content.isBlank()) return
        
        val conversationId = currentConversationId ?: return
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            // 保存用户消息
            val userMessage = ChatMessage.userMessage(conversationId, content)
            chatRepository.saveMessage(userMessage)
            
            // 获取配置
            val config = configRepository.getLlmConfig()
            val userConfig = configRepository.getUserConfig()
            
            // 获取历史消息（用于上下文）
            val recentMessages = mutableListOf<ChatMessage>()
            chatRepository.getRecentMessages(conversationId, 20).collect { msgs ->
                recentMessages.addAll(msgs)
            }
            
            try {
                // 调用 LLM
                val response = when (config.let { null }) {
                    else -> {
                        val llmConfig = kotlinx.coroutines.flow.first { it != null }
                        when (llmConfig?.provider) {
                            ai.androidclaw.domain.model.LlmProviderType.OPENAI -> {
                                openAiProvider.chat(recentMessages, buildSystemPrompt(userConfig.let { null }))
                            }
                            ai.androidclaw.domain.model.LlmProviderType.OLLAMA -> {
                                ollamaProvider.chat(recentMessages, buildSystemPrompt(userConfig.let { null }))
                            }
                            ai.androidclaw.domain.model.LlmProviderType.ANTHROPIC -> {
                                anthropicProvider.chat(recentMessages, buildSystemPrompt(userConfig.let { null }))
                            }
                            else -> "Please configure an LLM provider in Settings."
                        }
                    }
                }
                
                // 保存助手回复
                val assistantMessage = ChatMessage.assistantMessage(conversationId, response)
                chatRepository.saveMessage(assistantMessage)
                
            } catch (e: Exception) {
                // 保存错误消息
                val errorMessage = ChatMessage.assistantMessage(
                    conversationId,
                    "Sorry, I encountered an error: ${e.message}"
                )
                chatRepository.saveMessage(errorMessage)
            } finally {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }
    
    private fun buildSystemPrompt(userConfig: ai.androidclaw.domain.model.UserConfig?): String {
        val builder = StringBuilder()
        builder.append("You are AndroidClaw, a helpful AI assistant running on Android.")
        
        userConfig?.let { config ->
            if (config.name.isNotBlank()) {
                builder.append(" The user's name is ${config.name}.")
            }
            if (config.role.isNotBlank()) {
                builder.append(" Their role is: ${config.role}.")
            }
            if (config.systemPrompt.isNotBlank()) {
                builder.append("\n\nAdditional instructions: ${config.systemPrompt}")
            }
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
    val error: String? = null
)
