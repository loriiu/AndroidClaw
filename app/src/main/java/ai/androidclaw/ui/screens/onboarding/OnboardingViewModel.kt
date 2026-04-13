package ai.androidclaw.ui.screens.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import ai.androidclaw.domain.model.LlmConfig
import ai.androidclaw.domain.model.LlmProviderType
import ai.androidclaw.domain.repository.ConfigRepository
import javax.inject.Inject

/**
 * 引导流程 ViewModel
 */
@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val configRepository: ConfigRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()
    
    fun selectProvider(provider: LlmProviderType) {
        _uiState.value = _uiState.value.copy(
            selectedProvider = provider,
            currentStep = OnboardingStep.CREDENTIALS
        )
    }
    
    fun updateApiKey(apiKey: String) {
        _uiState.value = _uiState.value.copy(apiKey = apiKey)
    }
    
    fun updateBaseUrl(baseUrl: String) {
        _uiState.value = _uiState.value.copy(baseUrl = baseUrl)
    }
    
    fun updateModel(model: String) {
        _uiState.value = _uiState.value.copy(model = model)
    }
    
    fun nextStep() {
        val currentStep = _uiState.value.currentStep
        val nextStep = when (currentStep) {
            OnboardingStep.WELCOME -> OnboardingStep.PROVIDER
            OnboardingStep.PROVIDER -> OnboardingStep.CREDENTIALS
            OnboardingStep.CREDENTIALS -> OnboardingStep.COMPLETE
            OnboardingStep.COMPLETE -> OnboardingStep.COMPLETE
        }
        _uiState.value = _uiState.value.copy(currentStep = nextStep)
    }
    
    fun previousStep() {
        val currentStep = _uiState.value.currentStep
        val prevStep = when (currentStep) {
            OnboardingStep.WELCOME -> OnboardingStep.WELCOME
            OnboardingStep.PROVIDER -> OnboardingStep.WELCOME
            OnboardingStep.CREDENTIALS -> OnboardingStep.PROVIDER
            OnboardingStep.COMPLETE -> OnboardingStep.CREDENTIALS
        }
        _uiState.value = _uiState.value.copy(currentStep = prevStep)
    }
    
    fun complete() {
        viewModelScope.launch {
            val state = _uiState.value
            
            val config = LlmConfig(
                provider = state.selectedProvider ?: LlmProviderType.OPENAI,
                apiKey = state.apiKey,
                model = state.model,
                baseUrl = state.baseUrl.ifBlank { null }
            )
            
            configRepository.saveLlmConfig(config)
            configRepository.setInitialized(true)
        }
    }
}

/**
 * 引导步骤
 */
enum class OnboardingStep {
    WELCOME,
    PROVIDER,
    CREDENTIALS,
    COMPLETE
}

/**
 * 引导 UI 状态
 */
data class OnboardingUiState(
    val currentStep: OnboardingStep = OnboardingStep.WELCOME,
    val selectedProvider: LlmProviderType? = null,
    val apiKey: String = "",
    val model: String = "gpt-4o-mini",
    val baseUrl: String = "http://localhost:11434",
    val isLoading: Boolean = false
)
