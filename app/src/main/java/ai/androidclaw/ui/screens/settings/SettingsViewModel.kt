package ai.androidclaw.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import ai.androidclaw.domain.model.LlmConfig
import ai.androidclaw.domain.model.LlmProviderType
import ai.androidclaw.domain.model.ThemeMode
import ai.androidclaw.domain.model.UserConfig
import ai.androidclaw.domain.repository.ConfigRepository
import javax.inject.Inject

/**
 * 设置 ViewModel
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val configRepository: ConfigRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()
    
    init {
        loadSettings()
    }
    
    private fun loadSettings() {
        viewModelScope.launch {
            launch {
                configRepository.getLlmConfig().collect { config ->
                    _uiState.value = _uiState.value.copy(llmConfig = config)
                }
            }
            
            launch {
                configRepository.getUserConfig().collect { config ->
                    _uiState.value = _uiState.value.copy(userConfig = config)
                }
            }
        }
    }
    
    fun saveLlmConfig(provider: LlmProviderType, apiKey: String, model: String, baseUrl: String?) {
        viewModelScope.launch {
            val config = LlmConfig(
                provider = provider,
                apiKey = apiKey,
                model = model,
                baseUrl = baseUrl
            )
            configRepository.saveLlmConfig(config)
        }
    }
    
    fun saveUserConfig(config: UserConfig) {
        viewModelScope.launch {
            configRepository.saveUserConfig(config)
        }
    }
    
    fun updateUserName(name: String) {
        viewModelScope.launch {
            configRepository.updateUserName(name)
        }
    }
    
    fun updateUserRole(role: String) {
        viewModelScope.launch {
            configRepository.updateUserRole(role)
        }
    }
    
    fun updateSystemPrompt(prompt: String) {
        viewModelScope.launch {
            configRepository.updateSystemPrompt(prompt)
        }
    }
    
    fun updateThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            configRepository.updateThemeMode(mode)
        }
    }
}

/**
 * 设置 UI 状态
 */
data class SettingsUiState(
    val llmConfig: LlmConfig? = null,
    val userConfig: UserConfig = UserConfig(),
    val isLoading: Boolean = false,
    val error: String? = null
)
