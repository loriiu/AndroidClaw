package ai.androidclaw.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import ai.androidclaw.domain.repository.ConfigRepository
import javax.inject.Inject

/**
 * 主 ViewModel
 */
@HiltViewModel
class MainViewModel @Inject constructor(
    private val configRepository: ConfigRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()
    
    init {
        viewModelScope.launch {
            configRepository.isInitialized().collect { initialized ->
                _uiState.value = _uiState.value.copy(isInitialized = initialized)
            }
        }
    }
    
    fun setInitialized() {
        viewModelScope.launch {
            configRepository.setInitialized(true)
        }
    }
}

/**
 * 主界面 UI 状态
 */
data class MainUiState(
    val isInitialized: Boolean = false,
    val isLoading: Boolean = true
)
